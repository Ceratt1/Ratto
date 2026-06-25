# Ratto

Plataforma de apoio à aprendizagem que transforma PDFs de estudo em questões de múltipla escolha com ajuda de IA. O objetivo é tirar o aluno de uma rotina passiva de leitura e apoiar prática, revisão, identificação de lacunas e evolução de desempenho.

## Artefatos de Arquitetura

- [architecture.excalidraw](architecture.excalidraw): diagrama simples e editável do fluxo da arquitetura, pensado para explicar o sistema para pessoas técnicas e não técnicas.
- [docs/fluxo-ratto.svg](docs/fluxo-ratto.svg): diagrama renderizado do fluxo assíncrono, do upload do PDF até a geração das questões.
- [docs/arquitetura-ratto.md](docs/arquitetura-ratto.md): versão detalhada em Mermaid, com tópicos Kafka, consumidores, produtores e convenção dos paths no S3.

## Visão Geral

O fluxo principal do Ratto é:

1. O aluno acessa o frontend pelo gateway público.
2. O frontend autentica com Keycloak e usa um BFF Next.js para preparar o upload.
3. O `producer` gera uma URL pré-assinada para upload direto no S3.
4. O navegador envia o PDF diretamente ao S3.
5. O `producer` confirma o arquivo e publica um evento Kafka.
6. O `pdf-extractor` extrai o texto e salva `extracted.txt` no S3.
7. O `question-generator` usa Gemini para gerar questões e salva `questions.json` no S3.
8. O `event-ledger` observa os tópicos Kafka e registra eventos de forma append-only.

O Kafka carrega IDs, metadados de rastreio e paths. Os conteúdos ficam no S3.

```text
requests/{uuidUser}/{uuidRequest}/{fileUuid}/original.pdf
requests/{uuidUser}/{uuidRequest}/{fileUuid}/extracted.txt
requests/{uuidUser}/{uuidRequest}/{fileUuid}/questions.json
```

## Componentes

- `api-gateway/`: único HTTP público de negócio. Entrega o frontend, publica Keycloak na mesma origem e roteia APIs permitidas.
- `frontend/`: Next.js App Router, landing pública, área autenticada em `/app` e BFF para upload/perfil.
- `core-service/`: monólito modular Spring Modulith. Hoje mantém projeções de perfil de usuário; no plano do produto, concentra estudo, questões, tentativas, progresso e dashboard.
- `generics/`: biblioteca compartilhada com contratos de eventos, modelos comuns, validação e utilitários AWS S3.
- `producer/`: API WebFlux que prepara upload direto para S3, confirma arquivos e publica o início do processamento.
- `pdf-extractor/`: worker Kafka que baixa o PDF, extrai texto com PDFBox e grava `extracted.txt`.
- `question-generator/`: worker Kafka que lê o texto extraído, chama Gemini e grava `questions.json`.
- `event-ledger/`: consumidor de auditoria que observa os tópicos e grava eventos imutáveis no PostgreSQL.
- `infra/keycloak/`: realm, configuração idempotente e tema visual customizado do Ratto.

## Tópicos Kafka

| Tópico | Publicado por | Consumido por | Função |
| --- | --- | --- | --- |
| `knowledgement-topic` | `producer` | `pdf-extractor`, `event-ledger` | Avisar que um PDF foi recebido e pode ser processado. |
| `pdf-text-extracted-topic` | `pdf-extractor` | `question-generator`, `event-ledger` | Avisar que o texto extraído está pronto no S3. |
| `study-problems-generated-topic` | `question-generator` | `event-ledger` no estado atual | Avisar que as questões foram geradas e salvas no S3. |
| `pdf-ingestion-errors` | `pdf-extractor`, `question-generator` | `event-ledger` | Registrar falhas do processamento assíncrono. |

## Segurança e Entrada Única

O `api-gateway` é a única entrada HTTP pública do ambiente e fica disponível em `http://localhost:3000`.

- `/` é público e apresenta o produto.
- `/app` inicia a experiência autenticada com Keycloak.
- `/api/uploads/**`, `/api/users/me` e `/api/v1/**` exigem JWT.
- `core-service` e `producer` validam novamente o token com suas próprias audiências.
- O binário do PDF não passa pelo gateway; o navegador envia o arquivo diretamente ao S3 por URL pré-assinada.
- Cada requisição recebe `X-Correlation-Id`; falhas de upstream passam por circuit breaker.

## Rodando com Docker Compose

Crie o `.env` a partir do exemplo e suba o ambiente:

```bash
cp .env.example .env
docker compose up --build -d
```

Entrada principal:

```text
http://localhost:3000
```

Serviços internos como frontend, Keycloak, `core-service` e `producer` não devem ser expostos diretamente como entrada de negócio. Kafka, PostgreSQL e Actuator são portas de desenvolvimento vinculadas ao host local.

Para parar:

```bash
docker compose down
```

## Desenvolvimento Local

Instale primeiro a biblioteca compartilhada quando for rodar serviços Java fora do Compose:

```bash
mvn -f generics/pom.xml install
```

Comandos úteis:

```bash
cd frontend && npm run dev
mvn -f producer/pom.xml spring-boot:run
mvn -f pdf-extractor/pom.xml spring-boot:run
mvn -f question-generator/pom.xml spring-boot:run
mvn -f event-ledger/pom.xml spring-boot:run
```

## Upload Direto para S3

Antes de testar pelo navegador com bucket real, configure CORS para permitir `PUT` vindo do frontend local:

```bash
aws s3api put-bucket-cors \
  --bucket "$AWS_S3_BUCKET" \
  --cors-configuration file://docs/s3-cors.json
```

O frontend atual aceita 1 PDF por envio, com limite de 30 MB.

## Login Social

O Keycloak é o issuer único da aplicação e também centraliza provedores sociais.

Para ativar Google:

1. Crie um cliente OAuth 2.0 do tipo aplicação Web no Google Cloud Console.
2. Cadastre `http://localhost:3000/realms/ratto/broker/google/endpoint` como URI de redirecionamento autorizada.
3. Preencha `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` e `SSO_GOOGLE_ENABLED=true` no `.env`.
4. Execute:

```bash
docker compose up -d --force-recreate keycloak-config
```

Azure/Entra ID segue o mesmo padrão com `SSO_AZURE_ENABLED`, `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET` e `AZURE_TENANT_ID`.

## Observações do Estado Atual

- `event-ledger` está implementado, mas seu serviço e PostgreSQL estão comentados no `docker-compose.yml` atual.
- O `core-service` ainda não consome `study-problems-generated-topic`; isso aparece como direção planejada no material do projeto.
- O fluxo documentado mantém a separação entre mensagens Kafka e artefatos S3: eventos carregam referências, S3 guarda os documentos e resultados.
