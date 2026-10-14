#!/usr/bin/env bash
# Shuts down ALL CloudPulse AWS resources so they stop costing money. THIS DELETES THE DATABASE.
#   1. delete the app's namespace -> Kubernetes deletes the Postgres EBS disk it created
#      (Terraform did not create that disk, so `terraform destroy` alone would leave it behind, still billed)
#   2. terraform destroy -> deletes the cluster, nodes, NAT gateway, VPC, ECR images and IAM roles
#
# Usage:  AWS_PROFILE=cloudpulse ./scripts/destroy-aws.sh
# Terraform shows what it will delete and asks you to type "yes" before anything is removed.
# Bring everything back later with:  terraform apply  (in infra/terraform), then ./scripts/deploy-aws.sh

set -euo pipefail

NAMESPACE="cloudpulse"
KUBE_CONTEXT="cloudpulse-eks"
TF_DIR="infra/terraform"

cd "$(dirname "$0")/.."

echo "==> AWS identity"
aws sts get-caller-identity --query Arn --output text

# Step 1 only applies if the cluster still exists
if CLUSTER=$(terraform -chdir="$TF_DIR" output -raw cluster_name 2>/dev/null) && [ -n "$CLUSTER" ] \
   && aws eks describe-cluster --name "$CLUSTER" --query cluster.status --output text >/dev/null 2>&1; then
    REGION=$(terraform -chdir="$TF_DIR" output -raw ecr_repository_url | cut -d. -f4)
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
    echo "==> Cluster not found, skipping the Kubernetes cleanup"
fi

# Step 2: delete everything Terraform created. Pass -auto-approve as an argument to skip the "yes" prompt.
echo "==> terraform destroy"
terraform -chdir="$TF_DIR" destroy "$@"

# Forget the deleted cluster in ~/.kube/config
kubectl config delete-context "$KUBE_CONTEXT" >/dev/null 2>&1 || true
echo "==> Done. AWS resources for CloudPulse have been destroyed."
