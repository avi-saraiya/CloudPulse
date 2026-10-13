# Input variables: the values you might want to change without editing main.tf.
# Override them in terraform.tfvars (copy terraform.tfvars.example).

variable "region" {
  description = "AWS region to create everything in"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefix used to name all resources"
  type        = string
  default     = "cloudpulse"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes version (check the EKS docs for currently supported versions)"
  type        = string
  default     = "1.36"
}

variable "node_instance_type" {
  description = "EC2 instance type for the worker nodes"
  type        = string
  default     = "t3.medium"
}

variable "node_count" {
  description = "Number of worker nodes"
  type        = number
  default     = 2
}
