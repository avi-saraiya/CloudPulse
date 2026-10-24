#!/usr/bin/env bash
# Removes CloudPulse (and its database) from EKS. The cluster and ECR repository keep running and
# costing money until you delete them with the AWS CLI.
# Usage:  AWS_PROFILE=cloudpulse ./scripts/destroy-aws.sh
set -euo pipefail

PREVIOUS=$(kubectl config current-context 2>/dev/null || true)
aws eks update-kubeconfig --region us-east-1 --name cloudpulse-eks --alias cloudpulse-eks
[ -n "$PREVIOUS" ] && kubectl config use-context "$PREVIOUS"

kubectl --context cloudpulse-eks delete namespace cloudpulse
kubectl config delete-context cloudpulse-eks
