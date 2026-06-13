# Repository Guidelines

## Project Structure & Module Organization

This Java 21/Spring Boot repository is split into four Maven projects:

- `producer/`: WebFlux API for receiving PDF uploads, storing files, and publishing Kafka events.
- `pdf-extractor/`: Kafka worker that extracts PDF text; production code is under `src/main/java`.
- `question-generator/`: Kafka worker that turns extracted text into structured study problems.
- `generics/`: shared models, validation, exception handling, and AWS S3 utilities. Install this module before building dependent services.
- `docs/`: architecture image and sample PDF files. Root-level `docker-compose.yml` provides Kafka and Kafka UI.

Keep packages under `com.learnia` and place resources in each module's `src/main/resources`.

## Build and Development Commands

Run commands from the repository root unless noted:

```bash
docker compose up -d
mvn -f generics/pom.xml install
mvn -f producer/pom.xml spring-boot:run
mvn -f pdf-extractor/pom.xml spring-boot:run
mvn -f question-generator/pom.xml spring-boot:run
```

Docker Compose starts Kafka on `localhost:9092` and Kafka UI on `http://localhost:8080`. Use `docker compose down` to stop them.

## Coding Style & Naming Conventions

Use Java 21, four-space indentation, and existing Spring conventions. Name classes and records in PascalCase, methods and fields in camelCase, DTOs with a `Dto` suffix, service interfaces with an `I` prefix, and implementations with an `Impl` suffix. Keep controllers thin and place business logic in services. No formatter or linter is configured, so match nearby code and remove unused imports.

## Commit & Pull Request Guidelines

Recent commits use short imperative summaries such as `Add Kafka configuration` or `Refactor code structure`. Keep each commit focused and describe the affected behavior. Pull requests should explain purpose, key changes, and verification commands; link relevant issues and include request/response examples when API behavior changes.

## Security & Configuration

Copy each module's `application-example.yml` to the ignored `application.yml` for local use. Never commit AWS credentials, secrets, or environment-specific configuration.
