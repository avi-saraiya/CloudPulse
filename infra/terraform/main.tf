# CloudPulse AWS infrastructure: a network (VPC), a container registry (ECR) and a Kubernetes cluster (EKS).
#
# WARNING: `terraform apply` creates resources that COST MONEY (EKS control plane, EC2 nodes, NAT gateway).
# Always run `terraform plan` first, read what it will create, and run `terraform destroy` when finished.

# ---------- Versions ----------
# Pin versions so the same code always behaves the same way. The exact versions that were downloaded
# are recorded in .terraform.lock.hcl, which is committed to Git.
terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.68" # any 6.x from 6.68 upward, never 7.0
    }
  }
}

# ---------- Provider ----------
# Credentials are NOT written here. Terraform reads them from your environment
# (aws configure / AWS_PROFILE / an IAM role), so nothing secret ends up in Git.
provider "aws" {
  region = var.region

  # Every resource gets these tags, which makes it easy to find (and pay for) everything CloudPulse created.
  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "terraform"
    }
  }
}

# Looks up which availability zones (separate data centers) exist in the chosen region.
data "aws_availability_zones" "available" {
  state = "available"
}

# ---------- Network (VPC) ----------
# A "module" is a reusable package of Terraform code. This official community module creates the VPC,
# subnets, route tables, internet gateway and NAT gateway for us (dozens of resources in ~15 lines).
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 6.7"

  name = "${var.project_name}-vpc"
  cidr = "10.0.0.0/16"

  # Two availability zones (EKS requires at least two)
  azs             = slice(data.aws_availability_zones.available.names, 0, 2)
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24"]     # worker nodes live here (no public IPs)
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24"] # load balancers + NAT gateway live here

  # Private nodes reach the internet (to pull images) through ONE NAT gateway, the cheapest option.
  enable_nat_gateway = true
  single_nat_gateway = true

  # Tags that tell Kubernetes which subnets to use for public and internal load balancers
  public_subnet_tags  = { "kubernetes.io/role/elb" = 1 }
  private_subnet_tags = { "kubernetes.io/role/internal-elb" = 1 }
}

# ---------- Container registry (ECR) ----------
# Jenkins pushes the CloudPulse Docker image here, and EKS pulls it from here.
resource "aws_ecr_repository" "cloudpulse" {
  name = var.project_name

  # IMMUTABLE: a tag (we use the git commit hash) can never be overwritten, so every deploy is traceable.
  image_tag_mutability = "IMMUTABLE"

  # Lets `terraform destroy` delete the repository even if it still contains images.
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true # AWS checks each pushed image for known vulnerabilities
  }
}

# ---------- Kubernetes cluster (EKS) ----------
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 21.29"

  name               = "${var.project_name}-eks"
  kubernetes_version = var.kubernetes_version

  # Lets you run kubectl from your laptop / Jenkins. Access still requires valid AWS IAM credentials.
  endpoint_public_access = true
  # Gives the IAM identity that runs `terraform apply` admin access to the cluster.
  enable_cluster_creator_admin_permissions = true

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  # Cluster add-ons: networking, DNS, kube-proxy, plus the two needed for disks
  addons = {
    vpc-cni                = { before_compute = true }
    eks-pod-identity-agent = { before_compute = true } # lets pods receive AWS permissions (used by the disk driver)
    kube-proxy             = {}
    coredns                = {}

    # EBS CSI driver: creates AWS disks (EBS volumes) for PersistentVolumeClaims such as postgres-data.
    # EKS does not include it by default, so without it the database's disk would stay "Pending" forever.
    aws-ebs-csi-driver = {
      pod_identity_association = [{
        role_arn        = module.ebs_csi_pod_identity.iam_role_arn
        service_account = "ebs-csi-controller-sa" # the driver's Kubernetes identity
      }]
    }
  }

  # The EC2 machines (worker nodes) that actually run our pods
  eks_managed_node_groups = {
    default = {
      instance_types = [var.node_instance_type]
      min_size       = 1
      max_size       = var.node_count
      desired_size   = var.node_count
    }
  }
}

# ---------- Permissions for the disk driver ----------
# An IAM role that allows ONLY creating/attaching/deleting EBS volumes. EKS Pod Identity hands it to the
# EBS CSI driver's pods, so no AWS keys are stored anywhere in the cluster.
module "ebs_csi_pod_identity" {
  source  = "terraform-aws-modules/eks-pod-identity/aws"
  version = "~> 2.9"

  name                      = "${var.project_name}-ebs-csi"
  attach_aws_ebs_csi_policy = true # AWS's managed policy for the EBS CSI driver
}
