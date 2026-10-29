# Architecture Decision Records

[Architecture](../ARCHITECTURE.md)

This directory contains Architecture Decision Records (ADRs) for the `api-handler` service.

## What is an ADR?

An ADR documents a significant architectural decision — the context that led to it, the decision itself, and its consequences. ADRs are immutable records: once accepted, they are not deleted but may be superseded by newer ADRs.

## ADR Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [ADR-001](ADR-001-dynamodb-as-primary-database.md) | DynamoDB as Primary Data Store | Accepted | 2026-07-10 |
| [ADR-002](ADR-002-jwt-stateless-authentication.md) | JWT Stateless Authentication | Accepted | 2026-07-10 |
| [ADR-003](ADR-003-sqs-event-driven-reporting.md) | SQS for Event-Driven Reporting | Accepted | 2026-07-10 |
| [ADR-004](ADR-004-s3-document-storage.md) | S3 Presigned URLs for Document Storage | Accepted | 2026-07-10 |

## ADR Template

Use the template at `.github/skills/adr/ADR-000-template.md` for new ADRs.

## Status Values

- **Proposed** — under discussion
- **Accepted** — decision made and implemented
- **Deprecated** — no longer in use
- **Superseded by ADR-NNN** — replaced by a newer decision

