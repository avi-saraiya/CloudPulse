# Outputs are printed after `terraform apply` (or with `terraform output`).
# Jenkins and the README use them to know where to push images and which cluster to deploy to.

output "ecr_repository_url" {
  description = "Push CloudPulse images here, e.g. <url>:<git-commit>"
  value       = aws_ecr_repository.cloudpulse.repository_url
}

output "cluster_name" {
  description = "Name of the EKS cluster"
  value       = module.eks.cluster_name
}

output "configure_kubectl" {
  description = "Run this command to point kubectl at the new cluster"
  value       = "aws eks update-kubeconfig --region ${var.region} --name ${module.eks.cluster_name}"
}
