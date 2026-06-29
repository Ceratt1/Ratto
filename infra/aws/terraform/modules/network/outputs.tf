output "vpc_id" {
  description = "Created VPC id."
  value       = aws_vpc.this.id
}

output "public_subnet_id" {
  description = "Public subnet id."
  value       = aws_subnet.public.id
}
