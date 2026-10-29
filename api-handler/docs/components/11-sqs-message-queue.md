[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > SQS Message Queue

# SQS Message Queue

**Type**: Message Queue (Tier 3 — Data Layer)

---

## Purpose

Provides asynchronous, durable event messaging between `api-handler` (producer) and `reports-handler` (consumer). Booking and review lifecycle events are published to SQS immediately after the domain operation succeeds, enabling decoupled reporting without blocking the user-facing API.

---

## Configuration

| Attribute | Value |
|-----------|-------|
| **Queue Name** | `tm7-Queue-prod` |
| **Queue Type** | Standard Queue (at-least-once delivery) |
| **Region** | `ap-southeast-2` |
| **SDK** | Spring Cloud AWS SQS (`io.awspring.cloud:spring-cloud-aws-starter-sqs 3.0.3`) |
| **Auth** | STS role assumption (`org/DeveloperAccessRoleTeam7`) |
| **Role** | Producer only (api-handler does NOT consume from this queue) |

---

## Events Published

| Event Class | Trigger | Consumer |
|-------------|---------|---------|
| `BookingStatusChangedEvent` | Booking created / status updated / cancelled | `reports-handler` |
| `FeedbackEvent` | Review submitted or updated | `reports-handler` |

**Payload format**: JSON (serialised by Spring Cloud AWS SQS)

**Example `BookingStatusChangedEvent`**:
```json
{
  "bookingId": "bk-12345",
  "userId": "usr-abc",
  "tourId": "tour-xyz",
  "status": "CONFIRMED",
  "timestamp": "2026-07-10T12:00:00Z"
}
```

---

## Architecture Role

```
api-handler (Producer)          SQS Queue                reports-handler (Consumer)
─────────────────────           ──────────               ──────────────────────────
BookingService ──────────────► tm7-Queue-prod ──────────► Aggregate report updates
ReviewService  ──────────────►                           Weekly summary email
```

**Principle**: [P4 — Asynchronous Event Publishing for Side Effects](../02-architecture-principles.md#p4--asynchronous-event-publishing-for-side-effects)

**ADR**: [ADR-003](../../adr/ADR-003-sqs-event-driven-reporting.md)

---

## Delivery Guarantees

| Property | Value |
|----------|-------|
| Delivery | At-least-once (standard queue) |
| Ordering | Best-effort (not guaranteed) |
| Message retention | 4 days (AWS default) |
| Dead-letter queue | [To be defined] |
| Visibility timeout | [AWS default: 30s] |

**Consumer idempotency**: `reports-handler` must handle duplicate messages (at-least-once delivery). This is an implementation concern for the consumer service.

---

## Technologies
- **Producer SDK**: Spring Cloud AWS SQS `io.awspring.cloud:spring-cloud-aws-starter-sqs 3.0.3`
- **Serialisation**: JSON via Spring Cloud AWS message converter
- **Sending class**: `EventPublisherService` → `SqsTemplate` or `SqsAsyncClient`
- **Location**: `src/main/java/com/epam/edp/demo/service/reports/EventPublisherService.java`

---

## Dependencies
- **Depends on (External)**: AWS SQS service (`ap-southeast-2`), AWS STS (credentials)
- **Depended by (Tier 2)**: [EventPublisherService](08-report-event-service.md)

---

## Failure Handling

- SQS publish failures are caught by `EventPublisherService` and logged as `EventPublisherException`
- Failures do NOT propagate to the calling booking/review service — the user-facing operation succeeds
- If messages are lost (SQS error), reporting data may be incomplete — no automatic retry is implemented beyond the SDK default
- **TODO**: Implement dead-letter queue (DLQ) for undeliverable messages + alert on DLQ depth

---

## Scaling
- **SQS throughput**: Standard queues support unlimited messages/second
- **Producer scaling**: Stateless pods can all publish to the same queue concurrently
- **Consumer scaling**: Managed by `reports-handler` service configuration (outside this repository)

---

## Monitoring
- **Key metrics**: `NumberOfMessagesSent` (CloudWatch), `ApproximateNumberOfMessagesNotVisible`, DLQ depth (if configured)
- **Alerts**: SQS send errors > 0, DLQ depth > 0 (undeliverable messages)
- **Logs**: Event type and entity ID logged at INFO on each successful publish; errors at ERROR

