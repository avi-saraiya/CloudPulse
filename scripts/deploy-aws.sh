#!/usr/bin/env bash
# Deploys CloudPulse to the existing EKS cluster "cloudpulse-eks" and ECR repository "cloudpulse" (us-east-1).
# Usage:  AWS_PROFILE=cloudpulse ./scripts/deploy-aws.sh
set -euo pipefail
cd "$(dirname "$0")/.."

REGION=us-east-1
CONTEXT=cloudpulse-eks
ECR_URL=$(aws ecr describe-repositories --region $REGION --repository-names cloudpulse \
    --query 'repositories[0].repositoryUri' --output text)
IMAGE=$ECR_URL:$(git rev-parse --short HEAD)   # tag = current commit

# Add EKS to ~/.kube/config, then switch the default context back so a plain `kubectl` never hits AWS
PREVIOUS=$(kubectl config current-context 2>/dev/null || true)
aws eks update-kubeconfig --region $REGION --name cloudpulse-eks --alias $CONTEXT
[ -n "$PREVIOUS" ] && kubectl config use-context "$PREVIOUS"

docker build -t "$IMAGE" .
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin "${ECR_URL%%/*}"
docker push "$IMAGE"

kubectl --context $CONTEXT apply -f k8s/namespace.yaml
kubectl --context $CONTEXT apply -f k8s/postgres.yaml
# Same manifest as kind, with the image swapped for the ECR one
sed "s|image: cloudpulse:local|image: $IMAGE|" k8s/cloudpulse.yaml | kubectl --context $CONTEXT apply -f -
kubectl --context $CONTEXT apply -f monitoring/
kubectl --context $CONTEXT -n cloudpulse rollout status deployment/cloudpulse --timeout=300s

echo "Open it with: kubectl --context $CONTEXT -n cloudpulse port-forward svc/cloudpulse 8080:8080"
