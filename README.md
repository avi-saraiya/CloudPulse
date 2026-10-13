# CloudPulse

A cloud-hosted service reliability and incident-response application. It's an **educational
Site Reliability Engineering (SRE) project** that shows how an application, a database, containers,
Kubernetes, monitoring, CI/CD and infrastructure-as-code fit together.

CloudPulse simulates three services (**Payment API**, **Authentication API**, **Order API**). Every 5 seconds
it generates telemetry for them, stores it in PostgreSQL, opens alerts and incidents when a threshold is
breached, and lets an operator acknowledge and resolve those incidents from a web UI. Prometheus and Grafana
watch the same numbers from the outside.

> This is a simulation for learning, not a production monitoring product. The failures are fake and
> deterministic, which makes the demo repeatable.

---

## Architecture

```text
 Browser (JavaScript UI) ──HTTP──▶ Spring Boot app ──JDBC──▶ PostgreSQL
                                     │  every 5s: generate metric → check rules → alert + incident
                                     │
                                     └─ /actuator/prometheus ◀──scrape every 5s── Prometheus ──▶ Grafana
                                                                                  (alert rules)   (dashboard)

 GitHub push ──▶ GitHub Actions: test → package → docker build → deploy to kind → smoke test
 Terraform (infra/terraform) ──▶ AWS: VPC + ECR + EKS   (written and validated, never applied)
```

| Technology | What it does in CloudPulse |
|---|---|
| Java 21 + Spring Boot 4.1 | REST API, simulation, alert rules, incident lifecycle, health and metrics endpoints |
| PostgreSQL 16 | Stores services, metrics, alerts and incidents |
| HTML/CSS/JavaScript | The operator UI: service status, incidents, acknowledge/resolve, failure buttons |
| Docker / Docker Compose | Packages the app as an image; runs app + database together locally |
| Kubernetes (kind) | Runs the containers with probes, resource limits and rolling updates |
| Prometheus | Scrapes the app's metrics and evaluates alert rules |
| Grafana | Dashboard of latency, error rate, incidents, HTTP traffic and JVM memory |
| GitHub Actions | CI/CD: tests, builds and deploys every push to a temporary Kubernetes cluster |
| Terraform + AWS | Infrastructure as code for a VPC, an ECR registry and an EKS cluster |

**Two dashboards, two jobs.** The CloudPulse UI *manages* incidents (it's the source of truth).
Grafana *diagnoses* behavior with charts. The UI never talks to Prometheus.

---

## Prerequisites (versions this was tested with)

| Tool | Version | Needed for |
|---|---|---|
| Java (JDK) | 21 | building and running the app |
| Docker | 29 | Compose, images, kind |
| kind | 0.33 | local Kubernetes |
| kubectl | 1.37 | talking to Kubernetes |
| Terraform | 1.16 | `infra/terraform` (optional) |

Maven doesn't need to be installed: the project includes the Maven Wrapper (`./mvnw`).

---

## 1. Run with Docker Compose (quickest)

```bash
cp .env.example .env          # then change DB_PASSWORD in .env (.env is gitignored)
docker compose up --build     # builds the image and starts PostgreSQL + CloudPulse
```

Open http://localhost:8080. Stop with `docker compose down`; add `-v` to also delete the database volume.

### Environment variables

| Variable | Meaning | Default when unset |
|---|---|---|
| `DB_URL` | JDBC URL of the database | `jdbc:postgresql://localhost:5432/cloudpulse` |
| `DB_USERNAME` | database user | `postgres` |
| `DB_PASSWORD` | database password | `postgres` (local development only) |

Tables are created by `schema.sql` and the three services are seeded by `data.sql` on startup. Both are safe
to re-run and never delete data.

## 2. Run the tests

```bash
./mvnw test     # 13 tests; uses an in-memory H2 database, so no PostgreSQL is needed
```

## 3. Run on local Kubernetes (kind)

```bash
kind create cluster --name cloudpulse
docker build -t cloudpulse:local .
kind load docker-image cloudpulse:local --name cloudpulse   # copy the image into the cluster

kubectl apply -f k8s/namespace.yaml
# The database password lives only in the cluster, never in Git:
kubectl -n cloudpulse create secret generic cloudpulse-db \
  --from-literal=username=cloudpulse --from-literal=password="$(openssl rand -hex 16)"
kubectl apply -f k8s/

kubectl -n cloudpulse get pods                              # wait until both are 1/1 Running
kubectl -n cloudpulse port-forward svc/cloudpulse 8080:8080 # then open http://localhost:8080
```

The app may restart once on first deploy if it starts before PostgreSQL is ready. Kubernetes restarts it
automatically, and it's ready after that.

## 4. Add Prometheus and Grafana

```bash
kubectl apply -f monitoring/
kubectl -n cloudpulse port-forward svc/prometheus 9090:9090   # http://localhost:9090  (Alerts tab)
kubectl -n cloudpulse port-forward svc/grafana 3000:3000      # http://localhost:3000  (CloudPulse Overview)
```

Alert rules (in `monitoring/prometheus.yaml`), each of which must hold for 30 seconds before it fires:

| Alert | Condition |
|---|---|
| `CloudPulseHighLatency` | simulated latency > 1000 ms |
| `CloudPulseHighErrorRate` | simulated error rate > 10% |
| `CloudPulseTargetDown` | Prometheus cannot scrape the app |

## 5. CI/CD (GitHub Actions)

`.github/workflows/ci.yml` runs on every push and pull request to `main`:
test → upload test reports → package → build image `cloudpulse:<commit>` → create a temporary kind cluster →
deploy → check readiness and the API. A failing test stops the pipeline before anything is deployed.
Results are in the repository's **Actions** tab. The AWS (ECR/EKS) steps are present but commented out.

## 6. AWS with Terraform (optional, costs money)

See [`infra/terraform/README.md`](infra/terraform/README.md). The configuration validates, but **nothing has
been created in AWS**. EKS, EC2 nodes and the NAT gateway are billed hourly, so check pricing first,
run `terraform plan` before `apply`, and `terraform destroy` afterwards.

---

## API

| Method | Path | What it does |
|---|---|---|
| GET | `/api/services` | the three services and their status |
| GET | `/api/services/{id}` | one service (404 if missing) |
| GET | `/api/services/{id}/metrics` | the 50 newest readings for a service |
| GET | `/api/alerts?status=OPEN` | alerts, optionally filtered by status |
| GET | `/api/incidents` / `/api/incidents/{id}` | incidents (404 if missing) |
| POST | `/api/incidents/{id}/acknowledge` | OPEN → ACKNOWLEDGED (otherwise 409) |
| POST | `/api/incidents/{id}/resolve` | OPEN/ACKNOWLEDGED → RESOLVED (409 if already resolved) |
| GET | `/api/simulation` | which services are currently in a failure mode |
| POST | `/api/simulation/latency-spike?service=…` | latency 1500 ms (default service: Payment API) |
| POST | `/api/simulation/error-spike?service=…` | error rate 25% |
| POST | `/api/simulation/outage?service=…` | latency 3000 ms and 90% errors, so the status is DOWN |
| POST | `/api/simulation/restore` | every service back to normal |
| GET | `/actuator/health` | health, including `/liveness` and `/readiness` |
| GET | `/actuator/prometheus` | metrics for Prometheus |

**Status rules:** DOWN if error rate ≥ 50% (DOWN always wins); DEGRADED if latency > 1000 ms or
error rate > 10%; otherwise HEALTHY.

---

## Demo (about 3 minutes)

1. Open the CloudPulse UI: all three services are **HEALTHY**.
2. Show the GitHub Actions run for the latest commit (tests → build → deploy → smoke test).
3. Click **Latency spike** for Payment API. Within 5 seconds the service becomes **DEGRADED** and an **OPEN** incident appears.
4. In Prometheus → Alerts, `CloudPulseHighLatency` goes *pending*, then *firing* after 30 seconds.
5. In Grafana, the latency line jumps to 1500 ms, and the open-incident and firing-alert counters go up.
6. In the UI, click **Acknowledge**.
7. Click **Restore all**. The service returns to HEALTHY, the alert is resolved, and the Prometheus alert clears.
8. Click **Resolve** on the incident.
9. Show `infra/terraform/main.tf` and explain what it would create in AWS.
10. Name a limitation (below) and the next improvement.

The step-by-step operator procedure is in `docs/runbooks/high-latency.md`.

---

## Cleanup

```bash
docker compose down -v                     # Compose: stop and delete the database volume
kind delete cluster --name cloudpulse      # Kubernetes: deletes the cluster and everything in it
# AWS (only if it was ever applied): cd infra/terraform && terraform destroy
```

## Limitations

- **One app replica.** The simulator runs inside the app, so two replicas would write every metric twice.
  The fix would be a separate single "worker" deployment.
- The simulation mode is kept in memory, so restarting the app returns every service to NORMAL.
- PostgreSQL runs inside the cluster with a small volume, which is fine for a demo but not durable production storage.
- Grafana allows anonymous read-only viewing, and is only reachable through `kubectl port-forward`.
- No Kubernetes-level metrics (pod restarts, CPU per pod), because kube-state-metrics is not installed.
- The metrics table grows forever; there is no retention job.

## Deployment status

| Layer | Status |
|---|---|
| App + PostgreSQL, UI, tests | Locally verified |
| Docker / Compose | Locally verified |
| Kubernetes (kind) | Locally verified |
| Prometheus + Grafana | Locally verified (alert fired and cleared) |
| GitHub Actions | Verified on GitHub (build, deploy to kind, smoke test) |
| Terraform | `validate` passes; `plan` not run (no AWS credentials) |
| AWS / EKS | **Not deployed** |
