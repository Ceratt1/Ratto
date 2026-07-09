# Produção simples em uma EC2

Este roteiro sobe o Ratto em uma única EC2 usando Docker Compose. A ideia é uma produção pequena, barata e fácil de operar, para poucas requisições e uso por poucas horas ao dia.

Não é a arquitetura ideal de alta disponibilidade. É o caminho pragmático para colocar o sistema no ar com o menor número de peças: VPC pública, EC2, S3, IAM role, Docker, `git pull` e `docker compose`.

## Arquitetura alvo

- Uma VPC própria.
- Uma subnet pública.
- Internet Gateway com rota para internet.
- Uma EC2 Ubuntu com Docker.
- Um Elastic IP para o endereço não mudar.
- Um bucket S3 privado para PDFs e artefatos de estudo.
- Uma IAM role na EC2 para acessar somente esse bucket.
- Security Group abrindo apenas SSH para seu IP e a porta pública do gateway.

O `api-gateway` continua sendo a única entrada HTTP pública do sistema. No `docker-compose.yml`, ele publica a porta `3000`. Bancos, Kafka, serviços internos, Actuator, Prometheus e Grafana ficam presos no host ou na rede interna do Docker.

## Tamanho mínimo da máquina

O Compose atual roda vários containers pesados ao mesmo tempo: Keycloak, Kafka, três PostgreSQL, Next.js e serviços Java. Para uma EC2 pequena, use:

| Uso | Instância | RAM | Disco | Observação |
| --- | --- | ---: | ---: | --- |
| Mínimo prático | `t3a.large` | 8 GB | 60 GB gp3 | Use swap e não suba Prometheus/Grafana/cAdvisor no dia a dia. |
| Mais confortável | `t3a.xlarge` | 16 GB | 80-100 GB gp3 | Melhor se quiser observabilidade local e builds mais tranquilos. |

Evite `t3a.medium` para este Compose completo. Pode até iniciar com muito swap, mas Keycloak + Kafka + Java tende a ficar apertado e instável.

Para economizar, pare a EC2 quando não estiver usando. EC2 parada não cobra computação, mas EBS, S3 e Elastic IP ainda podem gerar custo.

## 1. Escolher região

Use a mesma região para EC2 e S3. Exemplo:

```text
us-east-1
```

Se você estiver no Brasil e quiser menor latência, `sa-east-1` também funciona, mas costuma ser mais caro. Para teste barato, `us-east-1` geralmente é melhor.

## 2. Criar a VPC

No Console AWS:

1. Abra **VPC > Your VPCs > Create VPC**.
2. Escolha **VPC only**.
3. Nome: `ratto-vpc`.
4. IPv4 CIDR: `10.30.0.0/16`.
5. Crie a VPC.

## 3. Criar a subnet pública

1. Abra **VPC > Subnets > Create subnet**.
2. VPC: `ratto-vpc`.
3. Nome: `ratto-public-1`.
4. Availability Zone: uma zona da região escolhida, por exemplo `us-east-1a`.
5. IPv4 CIDR: `10.30.1.0/24`.
6. Crie a subnet.
7. Selecione a subnet criada.
8. Abra **Actions > Edit subnet settings**.
9. Habilite **Auto-assign public IPv4 address**.

## 4. Criar Internet Gateway e rota pública

1. Abra **VPC > Internet gateways > Create internet gateway**.
2. Nome: `ratto-igw`.
3. Crie e depois clique em **Attach to VPC**.
4. Escolha `ratto-vpc`.

Agora crie a rota:

1. Abra **VPC > Route tables**.
2. Crie uma route table chamada `ratto-public-rt` na `ratto-vpc`.
3. Em **Subnet associations**, associe `ratto-public-1`.
4. Em **Routes > Edit routes**, adicione:

```text
Destination: 0.0.0.0/0
Target: ratto-igw
```

## 5. Criar Security Group

Crie um Security Group chamado `ratto-ec2-sg` na `ratto-vpc`.

Inbound rules:

| Tipo | Porta | Origem | Uso |
| --- | ---: | --- | --- |
| SSH | `22` | `SEU_IP/32` | Acesso administrativo. |
| Custom TCP | `3000` | `0.0.0.0/0` | Gateway público do Ratto. |

Para descobrir seu IP:

```bash
curl https://checkip.amazonaws.com
```

Use o resultado com `/32`, por exemplo:

```text
203.0.113.10/32
```

Outbound rules:

```text
Allow all outbound
```

Se quiser restringir o acesso ao app durante testes, troque a origem da porta `3000` para `SEU_IP/32`.

## 6. Criar bucket S3

1. Abra **S3 > Create bucket**.
2. Nome global único, por exemplo `ratto-prod-seunome-2026`.
3. Região: a mesma da EC2.
4. Deixe **Block all public access** habilitado.
5. Bucket Versioning: opcional. Para barato, pode deixar desativado.
6. Crie o bucket.

Configure CORS no bucket em **Permissions > Cross-origin resource sharing**:

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["PUT", "GET", "HEAD"],
    "AllowedOrigins": ["http://SEU_ELASTIC_IP:3000"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3000
  }
]
```

Depois que tiver domínio e HTTPS, troque `AllowedOrigins` para o domínio real, por exemplo:

```text
https://app.seudominio.com
```

## 7. Criar IAM role para a EC2

Crie uma policy chamada `ratto-s3-app-bucket-policy` com acesso apenas ao bucket da aplicação.

Troque `NOME_DO_BUCKET` pelo bucket criado:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::NOME_DO_BUCKET"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::NOME_DO_BUCKET/*"
    }
  ]
}
```

Depois:

1. Abra **IAM > Roles > Create role**.
2. Trusted entity: **AWS service**.
3. Use case: **EC2**.
4. Anexe a policy `ratto-s3-app-bucket-policy`.
5. Nome da role: `ratto-ec2-role`.

Assim você não precisa colocar `AWS_S3_ACCESS_KEY` nem `AWS_S3_SECRET_KEY` no `.env`.

## 8. Criar chave SSH

Se você ainda não tem uma chave local:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/ratto-ec2 -C "ratto-ec2"
```

No Console AWS:

1. Abra **EC2 > Key Pairs > Import key pair**.
2. Nome: `ratto-ec2`.
3. Cole o conteúdo de:

```bash
cat ~/.ssh/ratto-ec2.pub
```

## 9. Criar EC2

1. Abra **EC2 > Instances > Launch instances**.
2. Name: `ratto-prod-1`.
3. AMI: **Ubuntu Server 24.04 LTS**.
4. Instance type: `t3a.large`.
5. Key pair: `ratto-ec2`.
6. Network: `ratto-vpc`.
7. Subnet: `ratto-public-1`.
8. Auto-assign public IP: enabled.
9. Security Group: `ratto-ec2-sg`.
10. IAM instance profile: `ratto-ec2-role`.
11. Storage: `60 GiB gp3`.

Em **Advanced details > User data**, cole:

```bash
#!/usr/bin/env bash
set -euxo pipefail

apt-get update
apt-get install -y ca-certificates curl git unzip htop

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc

. /etc/os-release
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" > /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

usermod -aG docker ubuntu

fallocate -l 4G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

mkdir -p /opt/ratto
chown ubuntu:ubuntu /opt/ratto
```

Crie a instância.

## 10. Associar Elastic IP

1. Abra **EC2 > Elastic IPs > Allocate Elastic IP address**.
2. Aloque um IP.
3. Clique em **Actions > Associate Elastic IP address**.
4. Associe à EC2 `ratto-prod-1`.

Guarde esse IP. Ele será usado em:

- Acesso SSH.
- `GATEWAY_PUBLIC_URL`.
- `KEYCLOAK_PUBLIC_URL`.
- CORS do S3.
- Provedores sociais do Keycloak, se usados.

## 11. Acessar a EC2

Na sua máquina:

```bash
ssh -i ~/.ssh/ratto-ec2 ubuntu@SEU_ELASTIC_IP
```

Valide Docker:

```bash
docker version
docker compose version
```

Se o grupo `docker` ainda não estiver ativo na sessão:

```bash
exit
ssh -i ~/.ssh/ratto-ec2 ubuntu@SEU_ELASTIC_IP
```

## 12. Clonar o repositório

Na EC2:

```bash
cd /opt/ratto
git clone URL_DO_REPOSITORIO learn_ia
cd learn_ia
git status
```

Se o repositório for privado, use uma deploy key ou token conforme seu provedor Git.

Para testar que você consegue atualizar:

```bash
git pull
```

## 13. Criar o `.env` de produção simples

Na EC2:

```bash
cd /opt/ratto/learn_ia
cp .env.example .env
nano .env
```

Preencha pelo menos:

```env
AWS_S3_BUCKET=NOME_DO_BUCKET
AWS_S3_REGION=us-east-1
AWS_S3_ACCESS_KEY=
AWS_S3_SECRET_KEY=
AWS_S3_PATH_STYLE_ACCESS_ENABLED=false
AWS_S3_PRESIGNED_URL_DURATION_MINUTES=15

GEMINI_API_KEY=SUA_CHAVE_GEMINI
GEMINI_MODEL=gemini-3.5-flash
PERFORMANCE_GEMINI_MODEL=gemini-3.1-flash-lite
GEMINI_TEMPERATURE=0.2
GEMINI_MAX_OUTPUT_TOKENS=8192

LEDGER_POSTGRES_DB=ratto_ledger
LEDGER_POSTGRES_USER=ledger
LEDGER_POSTGRES_PASSWORD=SENHA_FORTE_LEDGER

KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=SENHA_FORTE_ADMIN_KEYCLOAK
KEYCLOAK_POSTGRES_DB=keycloak
KEYCLOAK_POSTGRES_USER=keycloak
KEYCLOAK_POSTGRES_PASSWORD=SENHA_FORTE_KEYCLOAK_DB

CORE_POSTGRES_DB=ratto_core
CORE_POSTGRES_USER=core
CORE_POSTGRES_PASSWORD=SENHA_FORTE_CORE_DB

KEYCLOAK_PUBLIC_URL=http://SEU_ELASTIC_IP:3000
KEYCLOAK_REALM=ratto
KEYCLOAK_CLIENT_ID=ratto-frontend

GATEWAY_PUBLIC_URL=http://SEU_ELASTIC_IP:3000
GATEWAY_ROUTES_FRONTEND_URL=http://frontend:3000
GATEWAY_ROUTES_KEYCLOAK_URL=http://keycloak:8080
GATEWAY_ROUTES_CORE_URL=http://core-service:8071
GATEWAY_ROUTES_PRODUCER_URL=http://producer:8070

SSO_GOOGLE_ENABLED=false
SSO_AZURE_ENABLED=false

GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=SENHA_FORTE_GRAFANA
```

Como a EC2 tem IAM role, deixe `AWS_S3_ACCESS_KEY` e `AWS_S3_SECRET_KEY` vazios.

## 14. Subir a aplicação sem observabilidade pesada

Para economizar memória, suba somente o que o fluxo principal precisa:

```bash
docker compose up --build -d \
  api-gateway \
  pdf-extractor \
  question-generator \
  event-ledger
```

Esse comando sobe também as dependências necessárias: frontend, producer, core-service, Keycloak, Kafka e PostgreSQL.

Verifique:

```bash
docker compose ps
docker compose logs -f api-gateway
```

Acesse:

```text
http://SEU_ELASTIC_IP:3000
```

## 15. Subir com Grafana/Prometheus quando precisar

Se estiver em `t3a.xlarge` ou quiser investigar algo pontualmente:

```bash
docker compose up -d prometheus grafana cadvisor
```

Grafana fica publicado apenas no localhost da EC2 em `127.0.0.1:3001`. Para acessar da sua máquina sem abrir porta pública:

```bash
ssh -i ~/.ssh/ratto-ec2 -L 3001:127.0.0.1:3001 ubuntu@SEU_ELASTIC_IP
```

Depois abra:

```text
http://localhost:3001
```

## 16. Rotina de deploy com git pull

Na EC2:

```bash
cd /opt/ratto/learn_ia
git pull
docker compose up --build -d \
  api-gateway \
  pdf-extractor \
  question-generator \
  event-ledger
docker compose ps
```

Para limpar imagens antigas de vez em quando:

```bash
docker image prune -f
```

Não rode `docker compose down -v` em produção simples, porque isso apaga volumes dos bancos locais.

## 17. Parar e ligar para economizar

Parar a aplicação, mantendo volumes:

```bash
cd /opt/ratto/learn_ia
docker compose stop
```

Subir de novo:

```bash
cd /opt/ratto/learn_ia
docker compose up -d \
  api-gateway \
  pdf-extractor \
  question-generator \
  event-ledger
```

Parar a EC2 pelo Console:

1. EC2 > Instances.
2. Selecione `ratto-prod-1`.
3. Instance state > Stop instance.

Ou pela AWS CLI:

```bash
aws ec2 stop-instances --instance-ids ID_DA_INSTANCIA
```

Para ligar:

```bash
aws ec2 start-instances --instance-ids ID_DA_INSTANCIA
```

Com Elastic IP, o endereço público permanece o mesmo.

## 18. Backups mínimos

O banco está em volumes Docker no disco EBS da EC2. Para uma produção simples, faça pelo menos snapshots do volume EBS.

No Console:

1. EC2 > Volumes.
2. Selecione o volume da instância.
3. Actions > Create snapshot.

Para backup lógico dos Postgres:

```bash
cd /opt/ratto/learn_ia
docker compose exec postgres-core pg_dump -U core ratto_core > ratto_core.sql
docker compose exec postgres-ledger pg_dump -U ledger ratto_ledger > ratto_ledger.sql
docker compose exec postgres-keycloak pg_dump -U keycloak keycloak > ratto_keycloak.sql
```

Guarde os `.sql` fora da instância se os dados forem importantes.

## 19. HTTPS e domínio

Para o primeiro teste, `http://SEU_ELASTIC_IP:3000` funciona.

Antes de divulgar para usuários reais, prefira:

1. Criar um domínio ou subdomínio, por exemplo `app.seudominio.com`.
2. Apontar o DNS para o Elastic IP.
3. Colocar um proxy na EC2, como Caddy ou Nginx, terminando HTTPS na porta `443`.
4. Alterar `.env`:

```env
KEYCLOAK_PUBLIC_URL=https://app.seudominio.com
GATEWAY_PUBLIC_URL=https://app.seudominio.com
```

5. Atualizar CORS do S3 para `https://app.seudominio.com`.
6. Recriar containers:

```bash
docker compose up --build -d \
  api-gateway \
  pdf-extractor \
  question-generator \
  event-ledger
```

## 20. Checklist final

- VPC criada.
- Subnet pública criada.
- Internet Gateway anexado.
- Route table com `0.0.0.0/0` para o Internet Gateway.
- Security Group com SSH apenas para seu IP.
- Porta `3000` aberta para o público desejado.
- Bucket S3 privado criado.
- CORS do S3 apontando para a URL pública.
- IAM role anexada à EC2.
- EC2 Ubuntu criada com Docker.
- Elastic IP associado.
- SSH funcionando.
- Repositório clonado em `/opt/ratto/learn_ia`.
- `git pull` funcionando.
- `.env` preenchido com senhas fortes, Gemini e URL pública.
- `docker compose up --build -d api-gateway pdf-extractor question-generator event-ledger` executado.
- `docker compose ps` sem serviços reiniciando em loop.
- App acessível em `http://SEU_ELASTIC_IP:3000`.

## Atalho com Terraform existente

Este repositório já tem uma base Terraform em `infra/aws/terraform/` para subir uma versão parecida:

```bash
cd infra/aws/terraform/envs/dev
cp terraform.tfvars.example terraform.tfvars
```

Edite `terraform.tfvars`, principalmente:

```hcl
instance_type       = "t3a.large"
root_volume_size_gb = 60
ssh_allowed_cidrs   = ["SEU_IP/32"]
app_allowed_cidrs   = ["0.0.0.0/0"]
```

Depois:

```bash
terraform init
terraform plan -out dev.tfplan
terraform apply dev.tfplan
terraform output
```

O Terraform cria VPC, subnet, EC2, IAM role, S3, Security Group e Elastic IP. Depois siga a partir da seção de acesso SSH, clone do repo, `.env` e `docker compose`.
