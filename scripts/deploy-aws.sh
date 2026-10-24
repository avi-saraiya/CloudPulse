#!/usr/bin/env bash
# Deploys CloudPulse to an existing EKS cluster:
#   connect kubectl -> build image -> push to ECR -> disk class + secret -> PostgreSQL -> CloudPulse
#   -> Prometheus + Grafana -> health check
#
# Prerequisites: an EKS cluster (with the EBS CSI driver add-on) and an ECR repository named "cloudpulse"
# (immutable tags) already exist, and you are logged in to AWS (aws sso login --profile cloudpulse). Usage:
#   AWS_PROFILE=cloudpulse ./scripts/deploy-aws.sh
#   CLUSTER=my-cluster REGION=eu-west-1 AWS_PROFILE=cloudpulse ./scripts/deploy-aws.sh   # other names
# Safe to run again: it skips or updates what already exists.

set -euo pipefail

NAMESPACE="cloudpulse"
KUBE_CONTEXT="cloudpulse-eks"   # a name for the EKS entry in ~/.kube/config, so we never touch the kind cluster
CLUSTER="${CLUSTER:-cloudpulse-eks}"
REGION="${REGION:-us-east-1}"

cd "$(dirname "$0")/.."

for tool in aws docker kubectl openssl git; do
    command -v "$tool" >/dev/null 2>&1 || { echo "ERROR: '$tool' is not installed or not on PATH." >&2; exit 1; }
done

# 1. Who am I? Fails early if the AWS login has expired.
echo "==> AWS identity"
aws sts get-caller-identity --query Arn --output text

# 2. Look up the ECR repository (<account>.dkr.ecr.<region>.amazonaws.com/cloudpulse)
ECR_URL=$(aws ecr describe-repositories --region "$REGION" --repository-names cloudpulse \
    --query 'repositories[0].repositoryUri' --output text) \
    || { echo "ERROR: ECR repository 'cloudpulse' not found in $REGION. Create it first." >&2; exit 1; }
TAG=$(git rev-parse --short HEAD)                 # immutable image tag = current commit
IMAGE="$ECR_URL:$TAG"

# 3. Connect kubectl to EKS. Every kubectl command below names this context explicitly.
#    update-kubeconfig also makes EKS the default context, so we switch the default back afterwards:
#    a plain `kubectl ...` typed later should never hit AWS by accident.
echo "==> Connecting kubectl to $CLUSTER"
PREVIOUS_CONTEXT=$(kubectl config current-context 2>/dev/null || true)
aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER" --alias "$KUBE_CONTEXT" >/dev/null
if [ -n "$PREVIOUS_CONTEXT" ] && [ "$PREVIOUS_CONTEXT" != "$KUBE_CONTEXT" ]; then
    kubectl config use-context "$PREVIOUS_CONTEXT" >/dev/null
fi
k() { kubectl --context "$KUBE_CONTEXT" "$@"; }
k get nodes

# 4. Build the image and push it to ECR (skipped if this commit's image is already there:
#    ECR tags are immutable, so the same tag can never be pushed twice)
if aws ecr describe-images --region "$REGION" --repository-name cloudpulse --image-ids imageTag="$TAG" >/dev/null 2>&1; then
    echo "==> $IMAGE already in ECR, skipping build/push"
else
    echo "==> Building and pushing $IMAGE"
    docker build -t "$IMAGE" .
    aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "${ECR_URL%%/*}"
    docker push "$IMAGE"
fi

# 5. Namespace, default disk type (AWS only) and database secret (random, never printed or saved)
k apply -f k8s/namespace.yaml
k apply -f k8s/aws/storageclass.yaml
if k -n "$NAMESPACE" get secret cloudpulse-db >/dev/null 2>&1; then
    echo "==> Secret cloudpulse-db already exists, keeping it"
else
    k -n "$NAMESPACE" create secret generic cloudpulse-db \
        --from-literal=username=cloudpulse --from-literal=password="$(openssl rand -hex 16)"
fi

# 6. PostgreSQL first (its 1 GiB disk is created as an encrypted EBS volume), then wait until ready
echo "==> Deploying PostgreSQL"
k apply -f k8s/postgres.yaml
k -n "$NAMESPACE" rollout status deployment/postgres --timeout=300s

# 7. CloudPulse: same manifest as kind, but with the image name swapped for the ECR image
echo "==> Deploying CloudPulse $IMAGE"
sed "s|image: cloudpulse:local|image: $IMAGE|" k8s/cloudpulse.yaml | k apply -f -
k -n "$NAMESPACE" rollout status deployment/cloudpulse --timeout=300s

# 8. Monitoring
echo "==> Deploying Prometheus and Grafana"
k apply -f monitoring/
k -n "$NAMESPACE" rollout status deployment/prometheus --timeout=300s
k -n "$NAMESPACE" rollout status deployment/grafana --timeout=300s

# 9. Health check
echo "==> Checking app health"
k -n "$NAMESPACE" exec deployment/cloudpulse -- wget -qO- localhost:8080/actuator/health/readiness
echo

cat <<EOF

CloudPulse is running on EKS ($CLUSTER, $REGION), image $IMAGE.
Nothing is exposed to the internet. Open each in its own terminal:
  kubectl --context $KUBE_CONTEXT -n $NAMESPACE port-forward svc/cloudpulse 8080:8080   -> http://localhost:8080
  kubectl --context $KUBE_CONTEXT -n $NAMESPACE port-forward svc/prometheus 9090:9090   -> http://localhost:9090
  kubectl --context $KUBE_CONTEXT -n $NAMESPACE port-forward svc/grafana 3000:3000      -> http://localhost:3000

Remove the app and its database disk:  AWS_PROFILE=$AWS_PROFILE ./scripts/destroy-aws.sh
(the cluster itself keeps running and costing money until you delete it with the AWS CLI)
EOF
