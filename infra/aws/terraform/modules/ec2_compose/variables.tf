variable "name_prefix" {
  description = "Prefix used to name EC2 resources."
  type        = string
}

variable "vpc_id" {
  description = "VPC id where the instance security group is created."
  type        = string
}

variable "subnet_id" {
  description = "Public subnet id where the instance runs."
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type for the Docker Compose test host."
  type        = string
}

variable "root_volume_size_gb" {
  description = "Root EBS volume size in GiB."
  type        = number
}

variable "root_volume_type" {
  description = "Root EBS volume type."
  type        = string
  default     = "gp3"
}

variable "existing_key_pair_name" {
  description = "Existing EC2 key pair name. Leave null if using ssh_public_key or no SSH key."
  type        = string
  default     = null
}

variable "ssh_public_key" {
  description = "Public SSH key content used to create an EC2 key pair. Leave null to use existing_key_pair_name or no SSH key."
  type        = string
  default     = null
  sensitive   = true
}

variable "ssh_allowed_cidrs" {
  description = "CIDR blocks allowed to SSH into the instance."
  type        = list(string)
  default     = []
}

variable "app_allowed_cidrs" {
  description = "CIDR blocks allowed to access the public gateway port."
  type        = list(string)
}

variable "app_port" {
  description = "Public gateway port exposed by docker-compose.yml."
  type        = number
}

variable "app_bucket_arn" {
  description = "S3 bucket ARN used by the application."
  type        = string
}

variable "app_bucket_name" {
  description = "S3 bucket name used by the application."
  type        = string
}

variable "aws_region" {
  description = "AWS region for generated local helper files."
  type        = string
}

variable "tags" {
  description = "Common tags applied to resources."
  type        = map(string)
  default     = {}
}
