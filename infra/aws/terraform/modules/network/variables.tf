variable "name_prefix" {
  description = "Prefix used to name networking resources."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the test VPC."
  type        = string
}

variable "public_subnet_cidr" {
  description = "CIDR block for the public subnet that hosts the EC2 instance."
  type        = string
}

variable "availability_zone" {
  description = "Availability zone for the public subnet."
  type        = string
}

variable "tags" {
  description = "Common tags applied to resources."
  type        = map(string)
  default     = {}
}
