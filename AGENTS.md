# Repository Guidelines

## Project Structure & Processing Order

This repository contains a Next.js frontend, a modular core service, an API gateway, and five Java 21/Spring Boot Maven projects:

- `api-gateway/`: the only public HTTP edge. Routes the frontend, Keycloak, BFF, core API, and producer direct-upload API; validates the `gateway-api` audience and applies correlation IDs, timeouts, and circuit breakers.
- `frontend/`: Next.js App Router UI and BFF for preparing/confirming direct S3 uploads of up to two PDFs.
- `core-service/`: Spring Modulith service for authenticated user profile projections. Organize features using Clean Architecture packages such as `api/controllers`, `api/dtos`, `models`, `services`, `services/impl`, `repositories`, `repositories/entities`, and `repositories/impl`.
- `generics/`: shared event contracts, study-domain models, validation, and AWS S3 utilities. Install it before dependent services.
- `producer/`: WebFlux API for receiving PDF uploads, storing files, and publishing Kafka events.
- `pdf-extractor/`: consumes PDF events, extracts text, stores `extracted.txt`, and publishes completion events.
- `question-generator/`: consumes extracted-text events, calls Gemini, stores typed `questions.json`, and publishes completion events.
- `event-ledger/`: consumes every application topic and appends immutable, idempotent event records to PostgreSQL.
- `docs/`: architecture material and sample PDFs.

The runtime order is `producer` -> `pdf-extractor` -> `question-generator`. `event-ledger` observes every stage independently. Kafka carries references and trace metadata; S3 carries document contents and generated artifacts.

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
mvn -f event-ledger/pom.xml spring-boot:run
```

Compose starts the complete pipeline with the gateway as the single public business entry at `http://localhost:3000`. Frontend, Keycloak, core-service, and producer business ports must remain internal. Kafka, Kafka UI, PostgreSQL, and Actuator ports are development-only and bound to `127.0.0.1`; Actuator and Prometheus endpoints use ports `9070` through `9075`.

## Coding Style & Naming Conventions

Use Java 21, four-space indentation, and existing Spring conventions. Name classes and records in PascalCase and methods and fields in camelCase. Use descriptive suffixes such as `Request`, `Response`, or `Event` for integration contracts. Keep controllers thin and place business logic in services. No formatter or linter is configured, so match nearby code and remove unused imports.

Use records or classes for domain objects, API payloads, and Kafka events. Do not model business payloads with `Map<String, Object>`, raw JSON strings, or `JsonNode`. Convert typed models to JSON or bytes only at integration boundaries such as Gemini and S3.

In `frontend/`, keep business flows under `src/features/`, shared visual components under `src/components/`, and integration routes under `src/app/api/`. Define TypeScript interfaces for every API request and response.

Build the authenticated frontend experience around SPA concepts. Keep `/app` as a client-oriented, in-app study workspace with fluid client-side navigation, persistent shell state, localized loading and error states, and data fetching that updates views without full page reloads. Use Next.js App Router features intentionally: public routes may use server rendering when it improves first load and SEO, but authenticated study flows should feel like a single-page application after login. Prefer feature-scoped client components, shared hooks, and BFF/API route contracts over duplicating fetch logic across pages.

Treat Ratto first and foremost as a study system. Every frontend page, component, icon, empty state, CTA, and visible message must reinforce learning, active practice, revision, progress, performance, or identification of knowledge gaps. Avoid presenting the product as a generic SaaS dashboard, file uploader, document processor, AI wrapper, or infrastructure control panel. Do not expose technical implementation language such as Kafka, S3, workers, pipelines, paths, or asynchronous processing in student-facing copy.

Use a mobile-first, young clean white-mode visual identity across the frontend and Keycloak: off-white and gray backgrounds, breathable spacing, rounded but restrained cards, clear hierarchy, and controlled blue accents for actions, icons, active states, and progress details. Do not add dark mode. Avoid excessive gradients, large solid-blue surfaces, corporate dashboard styling, decorative clutter, and black logo containers. Use the repeated subtle `ratto` background pattern where it helps the page feel branded without hurting readability. Keep the custom Keycloak login theme under `infra/keycloak/themes/ratto/` and preserve visual consistency with the frontend.

The `/` route is a public study-focused landing page. The authenticated study experience lives under `/app` and must initialize Keycloak using Authorization Code + PKCE with `login-required`. Public landing CTAs may start login or registration, but visitors must be able to understand the product without authenticating.

Place every contract, model, utility, configuration component, or abstraction used by multiple services in `generics/`. If a new component is expected to be reused by another service, create it in `generics/` from the start. Keep service-specific API DTOs and implementation details inside their owning service.

Treat `event_ledger` as append-only. Never add application code that updates or deletes ledger rows. New event fields belong in the JSON payload; add indexed generated columns only for established query requirements.

## Commit & Pull Request Guidelines

Recent commits use short imperative summaries such as `Add Kafka configuration` or `Refactor code structure`. Keep each commit focused and describe the affected behavior. Pull requests should explain purpose, key changes, and verification commands; link relevant issues and include request/response examples when API behavior changes.

## Security & Configuration

Create `.env` from `.env.example` for Compose. Never commit AWS credentials, Gemini keys, secrets, or environment-specific configuration.

Treat `api-gateway` as the only public HTTP ingress. New browser-facing or external HTTP APIs must be explicitly routed and secured there; never publish a backend business port directly in Compose. Keep JWT validation in each backend as defense in depth, preserve the authenticated `sub` through every service, and use configurable upstream DNS names instead of hardcoding container IPs. Kafka workers remain internal and event-driven rather than being exposed through the gateway.

Manage social and enterprise login providers idempotently through `infra/keycloak/configure-realm.sh` and environment variables. Never commit provider client secrets. New providers must use the gateway-hosted broker callback, keep the custom Ratto login theme, request only necessary scopes, and preserve Keycloak as the single issuer consumed by the application.
