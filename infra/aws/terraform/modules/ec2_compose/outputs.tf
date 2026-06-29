output "instance_id" {
  description = "EC2 instance id."
  value       = aws_instance.this.id
}

output "public_ip" {
  description = "Elastic public IP attached to the instance."
  value       = aws_eip.this.public_ip
}

output "public_dns" {
  description = "Public DNS name."
  value       = aws_instance.this.public_dns
}

output "ssh_command" {
  description = "SSH command. Add -i <key> when using a local private key."
  value       = "ssh ubuntu@${aws_eip.this.public_ip}"
}

output "app_url" {
  description = "Public Ratto gateway URL."
  value       = "http://${aws_eip.this.public_ip}:${var.app_port}"
}
