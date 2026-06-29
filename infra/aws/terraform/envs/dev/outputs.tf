output "app_url" {
  description = "Public Ratto gateway URL."
  value       = module.compose_host.app_url
}

output "app_bucket_name" {
  description = "S3 bucket name to put in the application .env."
  value       = module.app_bucket.bucket_name
}

output "instance_id" {
  description = "EC2 instance id."
  value       = module.compose_host.instance_id
}

output "public_ip" {
  description = "Elastic public IP."
  value       = module.compose_host.public_ip
}

output "ssh_command" {
  description = "SSH command. Add -i <key> when needed."
  value       = module.compose_host.ssh_command
}
