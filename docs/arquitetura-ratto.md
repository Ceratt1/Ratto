# Arquitetura Atual do Ratto


## Fluxo Principal

```mermaid
sequenceDiagram
    autonumber
    actor Aluno
    participant Front as Frontend /app
    participant BFF as Next.js BFF
    participant Prod as producer
    participant S3 as AWS S3 privado
    participant K1 as Kafka: knowledgement-topic
    participant Ext as pdf-extractor
    participant K2 as Kafka: pdf-text-extracted-topic
    participant QG as question-generator
    participant AI as Gemini API
    participant K3 as Kafka: study-problems-generated-topic
    participant EL as event-ledger
    participant DBL as PostgreSQL event_ledger

    Aluno->>Front: seleciona 1 PDF e confirma estudo
    Front->>BFF: POST /api/uploads/prepare
    BFF->>Prod: POST /api/v1/receiver/{uuidRequest}/uploads
    Prod->>Prod: cria fileUuid e s3Path
    Prod-->>BFF: uploadUrl, fileUuid, s3Path
    BFF-->>Front: dados de upload direto
    Front->>S3: PUT original.pdf usando presigned URL
    Front->>BFF: POST /api/uploads/confirm
    BFF->>Prod: POST /api/v1/receiver/{uuidRequest}/confirm
    Prod->>S3: valida existência de original.pdf
    Prod->>K1: publica PdfProcessingEvent
    K1-->>EL: evento observado
    EL->>DBL: append idempotente
    K1->>Ext: consome PdfProcessingEvent
    Ext->>S3: baixa original.pdf
    Ext->>Ext: extrai texto com PDFBox
    Ext->>S3: grava extracted.txt
    Ext->>K2: publica PdfTextExtractedEvent
    K2-->>EL: evento observado
    EL->>DBL: append idempotente
    K2->>QG: consome PdfTextExtractedEvent
    QG->>S3: baixa extracted.txt
    QG->>AI: gera 5 questões estruturadas
    AI-->>QG: questões, alternativas, dificuldade e explicações
    QG->>S3: grava questions.json
    QG->>K3: publica StudyProblemsGeneratedEvent
    K3-->>EL: evento observado
    EL->>DBL: append idempotente
```

## Tópicos Kafka

| Tópico | Publicado por | Consumido por | Payload | Papel |
| --- | --- | --- | --- | --- |
| `knowledgement-topic` | `producer` | `pdf-extractor`, `event-ledger` | `PdfProcessingEvent` | Solicita processamento de um PDF já enviado ao S3. |
| `pdf-text-extracted-topic` | `pdf-extractor` | `question-generator`, `event-ledger` | `PdfTextExtractedEvent` | Informa que o texto foi extraído e está disponível no S3. |
| `study-problems-generated-topic` | `question-generator` | `event-ledger` no estado atual | `StudyProblemsGeneratedEvent` | Informa que as questões foram geradas e salvas no S3. No plano do `# Ratto.md`, este evento também alimenta a persistência de estudo no monólito modular. |
| `pdf-ingestion-errors` | `pdf-extractor`, `question-generator` | `event-ledger` | `PdfIngestionErrorEvent` | Registra falhas técnicas do processamento assíncrono com correlação ao evento de origem. |

## Convenção de Paths no S3

Todos os artefatos de um arquivo ficam na mesma pasta lógica:

```text
requests/{uuidUser}/{uuidRequest}/{fileUuid}/original.pdf
requests/{uuidUser}/{uuidRequest}/{fileUuid}/extracted.txt
requests/{uuidUser}/{uuidRequest}/{fileUuid}/questions.json
```

O evento Kafka carrega referências para esses caminhos, não o conteúdo dos documentos. O PDF, o texto extraído e o JSON final trafegam pelo S3; o Kafka carrega metadados, correlação, autoria, IDs e ponteiros para os artefatos.

## Observações do Estado Atual

- `api-gateway` é o único ingresso HTTP público de negócio em Compose, exposto em `localhost:3000`.
- O frontend usa BFF interno para preparar e confirmar uploads, mas o binário do PDF vai direto do navegador para o S3 via URL pré-assinada.
- `core-service` é o monólito modular atual e persiste projeções de perfil de usuário em `postgres-core`.
- `producer`, `pdf-extractor` e `question-generator` compõem a cadeia principal de microserviços do pipeline de PDFs.
- `event-ledger` está implementado para observar todos os tópicos e gravar eventos de forma append-only, mas seus serviços de Compose estão comentados no arquivo atual.
- `generics` centraliza contratos de eventos, modelos compartilhados e utilitários S3 usados pelos serviços Java.
