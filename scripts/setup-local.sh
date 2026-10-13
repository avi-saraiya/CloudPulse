#!/usr/bin/env bash
# Sets up the complete CloudPulse stack on a local kind cluster:
#   kind cluster -> Docker image -> database secret -> PostgreSQL -> CloudPulse -> Prometheus + Grafana
#
# Usage (from anywhere):   ./scripts/setup-local.sh
# Optional cluster name:   CLUSTER_NAME=my-cluster ./scripts/setup-local.sh
# Safe to run again: every step skips or updates what already exists.

# Stop at the first error (-e), treat unset variables as errors (-u), and fail if any part of a pipe fails.
set -euo pipefail

CLUSTER_NAME="${CLUSTER_NAME:-cloudpulse}"
NAMESPACE="cloudpulse"
IMAGE="cloudpulse:local"   # must match the image in k8s/cloudpulse.yaml

# Run from the repository root, no matter where the script was started from
cd "$(dirname "$0")/.."

# 1. Check that the required tools are installed
for tool in docker kind kubectl openssl; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "ERROR: '$tool' is not installed or not on PATH." >&2
        exit 1
    fi
done

# 2. Create the kind cluster (skipped if it already exists)
if kind get clusters | grep -qx "$CLUSTER_NAME"; then
    echo "==> Cluster '$CLUSTER_NAME' already exists, reusing it"
else
    echo "==> Creating kind cluster '$CLUSTER_NAME'"
    kind create cluster --name "$CLUSTER_NAME"
fi
# Point kubectl at this cluster for all following commands
kubectl config use-context "kind-$CLUSTER_NAME" >/dev/null

# 3. Build the app image and copy it into the cluster
echo "==> Building Docker image $IMAGE"
docker build -t "$IMAGE" .
echo "==> Loading image into the cluster"
kind load docker-image "$IMAGE" --name "$CLUSTER_NAME"

# 4. Namespace and database secret. The password is random, never printed, and never written to a file.
kubectl apply -f k8s/namespace.yaml
if kubectl -n "$NAMESPACE" get secret cloudpulse-db >/dev/null 2>&1; then
    echo "==> Secret cloudpulse-db already exists, keeping it (the database was created with that password)"
else
    echo "==> Creating secret cloudpulse-db"
    kubectl -n "$NAMESPACE" create secret generic cloudpulse-db \
        --from-literal=username=cloudpulse \
        --from-literal=password="$(openssl rand -hex 16)"
fi

# 5. Start PostgreSQL first and wait until it is ready, so the app does not crash on startup
echo "==> Deploying PostgreSQL"
kubectl apply -f k8s/postgres.yaml
kubectl -n "$NAMESPACE" rollout status deployment/postgres --timeout=180s

# 6. Start CloudPulse. "rollout restart" makes an existing pod pick up a freshly loaded image.
echo "==> Deploying CloudPulse"
kubectl apply -f k8s/cloudpulse.yaml
kubectl -n "$NAMESPACE" rollout restart deployment/cloudpulse >/dev/null
kubectl -n "$NAMESPACE" rollout status deployment/cloudpulse --timeout=300s

# 7. Monitoring
echo "==> Deploying Prometheus and Grafana"
kubectl apply -f monitoring/
kubectl -n "$NAMESPACE" rollout status deployment/prometheus --timeout=180s
kubectl -n "$NAMESPACE" rollout status deployment/grafana --timeout=180s

# 8. Smoke test: the app is ready (database connected)
echo "==> Checking app health"
kubectl -n "$NAMESPACE" exec deployment/cloudpulse -- wget -qO- localhost:8080/actuator/health/readiness
echo

cat <<EOF

CloudPulse is running on kind cluster '$CLUSTER_NAME'. Open each in its own terminal:
  kubectl -n $NAMESPACE port-forward svc/cloudpulse 8080:8080   -> http://localhost:8080
  kubectl -n $NAMESPACE port-forward svc/prometheus 9090:9090   -> http://localhost:9090
  kubectl -n $NAMESPACE port-forward svc/grafana 3000:3000      -> http://localhost:3000

Delete everything with:  kind delete cluster --name $CLUSTER_NAME
EOF
