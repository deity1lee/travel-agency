# ADR-003: SQS for Event-Driven Reporting

**Status**: Accepted
**Date**: 2026-07-10
**Authors**: Architecture Team
**Related**: [ADR-001](ADR-001-dynamodb-as-primary-database.md)

---

## Context

Booking and review lifecycle events need to be captured for:
- Aggregate reporting (sales, performance metrics)
- Weekly automated email summaries sent to the travel agency manager

The reporting pipeline is implemented in a separate `reports-handler` service. The `api-handler` must notify the `reports-handler` when domain events occur (booking created/cancelled, review submitted) without creating a synchronous dependency that could degrade booking performance if `reports-handler` is slow or unavailable.

## Decision

Use **AWS SQS Standard Queue** (`tm7-Queue-prod`) as the event bus. `api-handler` publishes events to SQS as a producer. `reports-handler` consumes events independently.

## Rationale

- **Decoupling**: `api-handler` booking/review operations are not blocked by `reports-handler` availability or speed
- **Durability**: SQS retains messages for 4 days by default — messages survive `reports-handler` restarts
- **AWS-native**: SQS integrates with the existing AWS infrastructure and IAM credential model
- **Spring Cloud AWS**: First-class integration via `spring-cloud-aws-starter-sqs 3.0.3`
- **Operational simplicity**: Standard queue with at-least-once delivery is sufficient for aggregate reporting (not strict financial transactions)

## Consequences

**Positive**:
- Booking/review creation latency is unaffected by reporting pipeline health
- `reports-handler` can be independently deployed, scaled, and restarted
- SQS provides durable message storage — no event loss during `reports-handler` downtime (within 4-day retention)

**Negative**:
- Reporting data is eventually consistent — aggregate reports may lag booking operations
- At-least-once delivery requires `reports-handler` to handle duplicate events idempotently
- No guaranteed message ordering (standard queue) — `reports-handler` must handle out-of-order events
- No dead-letter queue (DLQ) configured yet — failed message handling is not defined

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|----------------|
| Synchronous REST call to `reports-handler` | Creates coupling; `api-handler` booking fails if `reports-handler` is down |
| AWS SNS + SQS fan-out | Unnecessary complexity for a single consumer |
| Apache Kafka | Significant operational overhead; AWS SQS is sufficient |
| Database-based outbox pattern | Additional complexity; SQS provides sufficient durability |

