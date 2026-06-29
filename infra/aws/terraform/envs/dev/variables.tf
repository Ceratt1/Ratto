variable "aws_region" {
  description = "AWS region used by the test environment."
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Project name used in tags and names."
  type        = string
  default     = "ratto"
}

variable "environment" {
  description = "Environment name."
  type        = string
  default     = "dev"
}

variable "availability_zone" {
  description = "Availability zone for the single public subnet."
  type        = string
  default     = "us-east-1a"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.30.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR block for the public subnet."
  type        = string
  default     = "10.30.1.0/24"
}

variable "instance_type" {
  description = "EC2 instance type for Docker Compose."
  type        = string
  default     = "t3a.xlarge"
}

variable "root_volume_size_gb" {
  description = "Root EBS volume size in GiB."
  type        = number
  default     = 100
}

variable "existing_key_pair_name" {
  description = "Existing EC2 key pair name. Use this or ssh_public_key."
  type        = string
  default     = null
}

variable "ssh_public_key" {
  description = "Public SSH key content used to create an EC2 key pair. Use this or existing_key_pair_name."
  type        = string
  default     = null
  sensitive   = true
}

variable "ssh_allowed_cidrs" {
  description = "CIDR blocks allowed to SSH. Prefer your own /32 IP."
  type        = list(string)
  default     = []
}

variable "app_allowed_cidrs" {
  description = "CIDR blocks allowed to access the Ratto gateway. For quick tests, your own /32 is best."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "app_port" {
  description = "Public gateway port mapped by docker-compose.yml."
  type        = number
  default     = 3000
}

variable "app_bucket_name" {
  description = "Optional fixed S3 bucket name. Leave null to generate one."
  type        = string
  default     = null
}

variable "force_destroy_app_bucket" {
  description = "Allow destroying the S3 bucket with test files inside. Keep false if you want extra protection."
  type        = bool
  default     = false
}

variable "budget_limit_usd" {
  description = "Monthly budget limit in USD for this test environment."
  type        = number
  default     = 25
}

variable "budget_alert_email" {
  description = "Email that receives AWS Budget alerts. Leave null to skip budget creation."
  type        = string
  default     = null
}
