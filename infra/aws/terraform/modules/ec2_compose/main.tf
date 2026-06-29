data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"]

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_key_pair" "this" {
  count      = var.ssh_public_key == null ? 0 : 1
  key_name   = "${var.name_prefix}-ssh"
  public_key = var.ssh_public_key

  tags = var.tags
}

locals {
  key_name = var.ssh_public_key == null ? var.existing_key_pair_name : aws_key_pair.this[0].key_name
}

resource "aws_security_group" "this" {
  name        = "${var.name_prefix}-compose-sg"
  description = "Ratto Docker Compose test host"
  vpc_id      = var.vpc_id

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-compose-sg"
  })
}

resource "aws_vpc_security_group_ingress_rule" "app" {
  for_each          = toset(var.app_allowed_cidrs)
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = each.value
  from_port         = var.app_port
  ip_protocol       = "tcp"
  to_port           = var.app_port
  description       = "Ratto gateway"
}

resource "aws_vpc_security_group_ingress_rule" "ssh" {
  for_each          = toset(var.ssh_allowed_cidrs)
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = each.value
  from_port         = 22
  ip_protocol       = "tcp"
  to_port           = 22
  description       = "SSH"
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
  description       = "Outbound internet access"
}

resource "aws_iam_role" "this" {
  name = "${var.name_prefix}-compose-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = var.tags
}

resource "aws_iam_role_policy" "s3_app_bucket" {
  name = "${var.name_prefix}-s3-app-bucket"
  role = aws_iam_role.this.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:DeleteObject",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:PutObject"
        ]
        Resource = [
          var.app_bucket_arn,
          "${var.app_bucket_arn}/*"
        ]
      }
    ]
  })
}

resource "aws_iam_instance_profile" "this" {
  name = "${var.name_prefix}-compose-profile"
  role = aws_iam_role.this.name
}

resource "aws_instance" "this" {
  ami                         = data.aws_ami.ubuntu.id
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.this.name
  instance_type               = var.instance_type
  key_name                    = local.key_name
  subnet_id                   = var.subnet_id
  user_data_replace_on_change = true
  vpc_security_group_ids      = [aws_security_group.this.id]

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  root_block_device {
    delete_on_termination = true
    encrypted             = true
    volume_size           = var.root_volume_size_gb
    volume_type           = var.root_volume_type
  }

  user_data = templatefile("${path.module}/user-data.sh.tftpl", {
    app_bucket_name = var.app_bucket_name
    app_port        = var.app_port
    aws_region      = var.aws_region
  })

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-compose"
  })
}

resource "aws_eip" "this" {
  domain = "vpc"

  tags = merge(var.tags, {
    Name = "${var.name_prefix}-compose"
  })
}

resource "aws_eip_association" "this" {
  allocation_id = aws_eip.this.id
  instance_id   = aws_instance.this.id
}
