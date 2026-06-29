variable "bucket_name" {
  description = "Name of the S3 bucket used by the application. Leave null to generate one."
  type        = string
  default     = null
}

variable "name_prefix" {
  description = "Prefix used when Terraform generates the bucket name."
  type        = string
}

variable "force_destroy" {
  description = "Allow Terraform to delete the bucket even when it contains test objects."
  type        = bool
  default     = false
}

variable "tags" {
  description = "Common tags applied to resources."
  type        = map(string)
  default     = {}
}
