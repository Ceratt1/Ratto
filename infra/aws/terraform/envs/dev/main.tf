locals {
  name_prefix = "${var.project}-${var.environment}"

  tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
    Workload    = "ratto-compose-test"
  }
}

module "network" {
  source = "../../modules/network"

  availability_zone  = var.availability_zone
  name_prefix        = local.name_prefix
  public_subnet_cidr = var.public_subnet_cidr
  tags               = local.tags
  vpc_cidr           = var.vpc_cidr
}

module "app_bucket" {
  source = "../../modules/s3_app_bucket"

  bucket_name   = var.app_bucket_name
  force_destroy = var.force_destroy_app_bucket
  name_prefix   = "${local.name_prefix}-app"
  tags          = local.tags
}

module "compose_host" {
  source = "../../modules/ec2_compose"

  app_allowed_cidrs      = var.app_allowed_cidrs
  app_bucket_arn         = module.app_bucket.bucket_arn
  app_bucket_name        = module.app_bucket.bucket_name
  app_port               = var.app_port
  aws_region             = var.aws_region
  existing_key_pair_name = var.existing_key_pair_name
  instance_type          = var.instance_type
  name_prefix            = local.name_prefix
  root_volume_size_gb    = var.root_volume_size_gb
  ssh_allowed_cidrs      = var.ssh_allowed_cidrs
  ssh_public_key         = var.ssh_public_key
  subnet_id              = module.network.public_subnet_id
  tags                   = local.tags
  vpc_id                 = module.network.vpc_id
}

resource "aws_budgets_budget" "monthly" {
  count = var.budget_alert_email == null ? 0 : 1

  budget_type  = "COST"
  limit_amount = tostring(var.budget_limit_usd)
  limit_unit   = "USD"
  name         = "${local.name_prefix}-monthly-budget"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator        = "GREATER_THAN"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.budget_alert_email]
    threshold                  = 50
    threshold_type             = "PERCENTAGE"
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.budget_alert_email]
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.budget_alert_email]
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
  }
}
