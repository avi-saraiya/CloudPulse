#!/usr/bin/env bash
# Removes CloudPulse from the EKS cluster. THIS DELETES THE DATABASE.
#   Deletes the app's namespace -> Kubernetes deletes the Postgres EBS disk it created
#   (if the cluster were deleted first, that disk would be left behind, still billed).
#
# The cluster, its nodes, the VPC and the ECR repository are NOT deleted by this script.
# They keep costing money until you delete them yourself with the AWS CLI.
#
# Usage:  AWS_PROFILE=cloudpulse ./scripts/destroy-aws.sh
#         CLUSTER=my-cluster REGION=eu-west-1 AWS_PROFILE=cloudpulse ./scripts/destroy-aws.sh   # other names
# Bring the app back later with:  ./scripts/deploy-aws.sh

set -euo pipefail

NAMESPACE="cloudpulse"
KUBE_CONTEXT="cloudpulse-eks"
CLUSTER="${CLUSTER:-cloudpulse-eks}"
REGION="${REGION:-us-east-1}"

cd "$(dirname "$0")/.."

echo "==> AWS identity"
aws sts get-caller-identity --query Arn --output text

if aws eks describe-cluster --region "$REGION" --name "$CLUSTER" --query cluster.status --output text >/dev/null 2>&1; then
    PREVIOUS_CONTEXT=$(kubectl config current-context 2>/dev/null || true)
    aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER" --alias "$KUBE_CONTEXT" >/dev/null
    [ -n "$PREVIOUS_CONTEXT" ] && [ "$PREVIOUS_CONTEXT" != "$KUBE_CONTEXT" ] && kubectl config use-context "$PREVIOUS_CONTEXT" >/dev/null

    echo "==> Deleting namespace $NAMESPACE (app, database and its EBS disk)"
    kubectl --context "$KUBE_CONTEXT" delete namespace "$NAMESPACE" --ignore-not-found --timeout=300s

    # Wait until the EBS CSI driver has actually deleted the disk(s) in AWS
    echo "==> Waiting for the Postgres disk to be deleted"
    for _ in $(seq 1 60); do
        [ -z "$(kubectl --context "$KUBE_CONTEXT" get pv -o name 2>/dev/null)" ] && break
        sleep 5
    done
    kubectl --context "$KUBE_CONTEXT" get pv
else
    echo "==> Cluster $CLUSTER not found in $REGION, nothing to clean up"
fi

# Forget the cluster in ~/.kube/config
kubectl config delete-context "$KUBE_CONTEXT" >/dev/null 2>&1 || true
cat <<EOF
==> Done. CloudPulse has been removed from $CLUSTER.
The EKS cluster, nodes, VPC/NAT gateway and ECR repository still exist and are still billed.
Delete them with the AWS CLI when you no longer need them.
EOF
