# Travel Agency Backend — Architecture

> Spring Boot 3-tier REST API powering a travel agency platform: tour discovery, booking management, document upload, reviews with AI moderation, and automated reporting — deployed on AWS Kubernetes.

<!-- PO_SPEC_GATE: SKIPPED (user override at 2026-07-10) -->

## Documentation

| # | Section | File | Description |
|---|---------|------|-------------|
| 1 | Executive Summary & System Overview | [docs/01-system-overview.md](docs/01-system-overview.md) | Problem statement, solution scope, use cases, key metrics |
| 2 | Architecture Principles | [docs/02-architecture-principles.md](docs/02-architecture-principles.md) | Design principles with rationale and trade-offs |
| 3 | Architecture Layers | [docs/03-architecture-layers.md](docs/03-architecture-layers.md) | 3-Tier model, layer diagram, data flow |
| 4 | Component Details | [docs/components/README.md](docs/components/README.md) | All architectural components by tier |
| 5 | Data Flow Patterns | [docs/04-data-flow-patterns.md](docs/04-data-flow-patterns.md) | Request/response flows, async event patterns |
| 6 | Integration Points | [docs/05-integration-points.md](docs/05-integration-points.md) | AWS services, external APIs, third-party integrations |
| 7 | Technology Stack | [docs/06-technology-stack.md](docs/06-technology-stack.md) | Languages, frameworks, libraries, versions |
| 8 | Security Architecture | [docs/07-security-architecture.md](docs/07-security-architecture.md) | JWT/OAuth2, CORS, rate limiting, threat model |
| 9 | Scalability & Performance | [docs/08-scalability-and-performance.md](docs/08-scalability-and-performance.md) | Kubernetes HPA, SLA targets, capacity planning |
| 10 | Operational Considerations | [docs/09-operational-considerations.md](docs/09-operational-considerations.md) | Deployment pipeline, monitoring, DR, environment config |

## Architecture Decision Records

| ADR | Title | Status | Date | Impact |
|-----|-------|--------|------|--------|
| [ADR-001](adr/ADR-001-dynamodb-as-primary-database.md) | DynamoDB as Primary Data Store | Accepted | 2026-07-10 | High |
| [ADR-002](adr/ADR-002-jwt-stateless-authentication.md) | JWT Stateless Authentication | Accepted | 2026-07-10 | High |
| [ADR-003](adr/ADR-003-sqs-event-driven-reporting.md) | SQS for Event-Driven Reporting | Accepted | 2026-07-10 | Medium |
| [ADR-004](adr/ADR-004-s3-document-storage.md) | S3 Presigned URLs for Document Storage | Accepted | 2026-07-10 | Medium |

See [adr/README.md](adr/README.md) for ADR process | [References](docs/10-references.md)

