#!/usr/bin/env bash
# Runs the whole stack on a new local kind cluster:  ./scripts/setup-local.sh
# Stops at the first error (-e). Delete the cluster first if it already exists.
set -euo pipefail
cd "$(dirname "$0")/.."

kind create cluster --name cloudpulse
docker build -t cloudpulse:local .
kind load docker-image cloudpulse:local --name cloudpulse   # copy the image into the cluster

kubectl apply -f k8s/namespace.yaml     # first, because everything else lives in this namespace
kubectl apply -f k8s/
kubectl apply -f monitoring/
kubectl -n cloudpulse rollout status deployment/cloudpulse --timeout=300s   # wait until the app is ready

cat <<'MSG'
CloudPulse is running. Open each in its own terminal:
  kubectl -n cloudpulse port-forward svc/cloudpulse 8080:8080   -> http://localhost:8080
  kubectl -n cloudpulse port-forward svc/prometheus 9090:9090   -> http://localhost:9090
  kubectl -n cloudpulse port-forward svc/grafana 3000:3000      -> http://localhost:3000 (admin / admin)
MSG
