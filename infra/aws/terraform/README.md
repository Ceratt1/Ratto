# Ratto AWS Terraform

IaC para subir um ambiente barato de teste do Ratto em uma única EC2 rodando `docker compose`.

Esta primeira versão é propositalmente simples:

- VPC própria com uma subnet pública.
- EC2 Ubuntu com Docker e Docker Compose plugin.
- Security group expondo apenas SSH, se configurado, e a porta pública do gateway.
- S3 privado para os PDFs e artefatos de estudo.
- IAM role anexada à EC2 com acesso apenas ao bucket criado.
- Elastic IP para a URL não mudar quando a instância reiniciar.
- AWS Budget opcional para alerta de custo.

Não coloque segredos em `terraform.tfvars`. Secrets passados ao Terraform ficam no state.

## Estrutura

```txt
infra/aws/terraform/
  envs/
    dev/                 # ambiente de teste
  modules/
    ec2_compose/         # EC2 + IAM + security group
    network/             # VPC, subnet, internet gateway e rota pública
    s3_app_bucket/       # bucket privado da aplicação
```

## Pré-requisitos

- Terraform `>= 1.6`
- AWS CLI autenticado com uma conta/perfil que possa criar VPC, EC2, IAM e S3
- Uma chave SSH, se quiser acessar a instância por SSH

## Subir o dev

```bash
cd infra/aws/terraform/envs/dev
cp terraform.tfvars.example terraform.tfvars
```

Edite `terraform.tfvars`.

Para liberar SSH, prefira seu IP atual com `/32`:

```hcl
ssh_allowed_cidrs = ["SEU_IP/32"]
```

Use uma key pair já existente:

```hcl
existing_key_pair_name = "minha-chave"
```

Ou crie uma nova key pair a partir da sua chave pública:

```hcl
ssh_public_key = "ssh-ed25519 AAAA... voce@maquina"
```

Para receber alertas de custo:

```hcl
budget_limit_usd   = 25
budget_alert_email = "voce@example.com"
```

Depois:

```bash
terraform init
terraform plan -out dev.tfplan
terraform apply dev.tfplan
```

Veja os outputs:

```bash
terraform output
```

## Colocar a aplicação na EC2

Depois que a EC2 subir:

```bash
ssh ubuntu@$(terraform output -raw public_ip)
```

Na instância, o user-data cria `/opt/ratto/README.txt` com os próximos passos.

O fluxo recomendado para teste é:

```bash
sudo mkdir -p /opt/ratto
sudo chown ubuntu:ubuntu /opt/ratto
cd /opt/ratto
git clone <URL_DO_REPO> learn_ia
cd learn_ia
cp .env.example .env
```

No `.env`, ajuste pelo menos:

```env
AWS_S3_BUCKET=<terraform output app_bucket_name>
AWS_S3_REGION=us-east-1
AWS_S3_ACCESS_KEY=
AWS_S3_SECRET_KEY=
KEYCLOAK_PUBLIC_URL=http://<terraform output public_ip>:3000
GATEWAY_PUBLIC_URL=http://<terraform output public_ip>:3000
```

Deixe `AWS_S3_ACCESS_KEY` e `AWS_S3_SECRET_KEY` vazios para a aplicação usar a IAM role da EC2. Depois preencha `GEMINI_API_KEY` e os demais secrets localmente na EC2.

Suba:

```bash
docker compose up --build -d
```

Abra:

```bash
terraform output -raw app_url
```

## Parar para economizar

Para parar a EC2 sem destruir o ambiente:

```bash
aws ec2 stop-instances --instance-ids "$(terraform output -raw instance_id)"
```

Para ligar de novo:

```bash
aws ec2 start-instances --instance-ids "$(terraform output -raw instance_id)"
```

O Elastic IP continua reservado. A EC2 parada não cobra computação, mas EBS, S3 e Elastic IP podem continuar gerando custo.

## Derrubar tudo

```bash
terraform destroy
```

Se você usou o bucket e ele tiver arquivos, ou apague os objetos antes, ou configure:

```hcl
force_destroy_app_bucket = true
```
