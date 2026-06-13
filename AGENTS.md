# Repository Guidelines

## Project Structure & Processing Order

This repository contains a Next.js frontend and four Java 21/Spring Boot Maven projects:

- `frontend/`: Next.js App Router UI and BFF for preparing/confirming direct S3 uploads of up to two PDFs.
- `generics/`: shared event contracts, study-domain models, validation, and AWS S3 utilities. Install it before dependent services.
- `producer/`: WebFlux API for receiving PDF uploads, storing files, and publishing Kafka events.
- `pdf-extractor/`: consumes PDF events, extracts text, stores `extracted.txt`, and publishes completion events.
- `question-generator/`: consumes extracted-text events, calls Gemini, stores typed `questions.json`, and publishes completion events.
- `docs/`: architecture material and sample PDFs.

The runtime order is `producer` -> `pdf-extractor` -> `question-generator`. Kafka carries references and trace metadata; S3 carries document contents and generated artifacts.

## Build and Development Commands

Run commands from the repository root unless noted:

```bash
cp .env.example .env
docker compose up --build -d
cd frontend && npm run dev
mvn -f generics/pom.xml install
mvn -f producer/pom.xml spring-boot:run
mvn -f pdf-extractor/pom.xml spring-boot:run
mvn -f question-generator/pom.xml spring-boot:run
```

Compose starts the complete pipeline, frontend on `http://localhost:3000`, Kafka on `localhost:9092`, Kafka UI on `http://localhost:8080`, and the producer API on `http://localhost:8070/api`. Actuator and Prometheus endpoints are exposed on ports `9070` (producer), `9071` (pdf-extractor), and `9072` (question-generator).

## Coding Style & Naming Conventions

Use Java 21, four-space indentation, and existing Spring conventions. Name classes and records in PascalCase and methods and fields in camelCase. Use descriptive suffixes such as `Request`, `Response`, or `Event` for integration contracts. Keep controllers thin and place business logic in services. No formatter or linter is configured, so match nearby code and remove unused imports.

Use records or classes for domain objects, API payloads, and Kafka events. Do not model business payloads with `Map<String, Object>`, raw JSON strings, or `JsonNode`. Convert typed models to JSON or bytes only at integration boundaries such as Gemini and S3.

In `frontend/`, keep business flows under `src/features/`, shared visual components under `src/components/`, and integration routes under `src/app/api/`. Define TypeScript interfaces for every API request and response.

Place every contract, model, utility, configuration component, or abstraction used by multiple services in `generics/`. If a new component is expected to be reused by another service, create it in `generics/` from the start. Keep service-specific API DTOs and implementation details inside their owning service.

## Commit & Pull Request Guidelines

Recent commits use short imperative summaries such as `Add Kafka configuration` or `Refactor code structure`. Keep each commit focused and describe the affected behavior. Pull requests should explain purpose, key changes, and verification commands; link relevant issues and include request/response examples when API behavior changes.

## Security & Configuration

Create `.env` from `.env.example` for Compose. Never commit AWS credentials, Gemini keys, secrets, or environment-specific configuration.
