[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > Event Publisher & Report Service

# Event Publisher & Report Service

**Type**: Business Service (Tier 2 — Application/Business Logic Layer)

---

## Purpose

Publishes booking and review lifecycle events to AWS SQS for asynchronous consumption by the `reports-handler` service. Also provides the admin reporting API by proxying requests to `reports-handler` via REST.

---

## Classes

| Class | Role |
|-------|------|
| `EventPublisherService` | Serialises and publishes domain events to SQS |
| `EventMapper` | Maps domain objects to SQS event payloads |
| `ReportService` | Proxies admin report requests to `reports-handler` REST API |
| `Service2Client` | `RestTemplate`-based HTTP client for `reports-handler` |

---

## Event Publishing (SQS)

**Trigger**: Called by `BookingService` and `ReviewService` after successful domain operations.

**Events Published**:

| Event Type | Class | Trigger | Key Fields |
|------------|-------|---------|------------|
| `BookingStatusChangedEvent` | `BookingStatusChangedEvent` | Booking created, updated, cancelled | bookingId, status, customerId, tourId, timestamp |
| `FeedbackEvent` | `FeedbackEvent` | Review submitted or updated | reviewId, tourId, userId, rating, timestamp |

**Publishing Pattern**:
- Spring Cloud AWS SQS (`io.awspring.cloud:spring-cloud-aws-starter-sqs`)
- Queue: `tm7-Queue-prod` (standard SQS queue, at-least-once delivery)
- Serialisation: JSON
- Async: `EventPublisherService.publish()` is called from Tier 2 services; SQS send is non-blocking relative to the user-facing request per [P4](../02-architecture-principles.md#p4--asynchronous-event-publishing-for-side-effects)

**ADR**: [ADR-003](../../adr/ADR-003-sqs-event-driven-reporting.md)

---

## Report Service (REST Proxy)

**Purpose**: Retrieve aggregate reports from the `reports-handler` service for admin consumption.

**Integration**:
- HTTP GET to `${REPORTS_HANDLER_URL}/...`
- Auth: static API key `secretKey` in header (**TODO: move to env var**)
- Client: `RestTemplate` (`Service2Client`)

**Report Types**:

| Type | Class | Description |
|------|-------|-------------|
| `AGGREGATE` | `AggregateReport` | General aggregate report |
| `SALES` | `SalesAggregateReport` | Sales and revenue metrics |
| `PERFORMANCE` | `PerformanceAggregateReport` | Performance metrics |

**Export Formats**:
- `CSV` — tabular data as comma-separated values
- `PDF` — formatted report document

---

## Technologies
- **SQS SDK**: Spring Cloud AWS SQS 3.0.3
- **REST Client**: Spring `RestTemplate` (`RestTemplateConfig`)
- **Reactive Client**: Spring `WebClient` (`WebClientConfig`) — used where async REST is needed
- **Location**: `src/main/java/com/epam/edp/demo/service/reports/`, `service/rest/`

---

## Dependencies
- **Depends on (Tier 3)**: [SQS Message Queue](11-sqs-message-queue.md)
- **Depends on (External)**: [reports-handler service](../05-integration-points.md#int-008-reports-handler-internal-kubernetes-service)
- **Depended by (Tier 2)**: [BookingService](05-booking-service.md), [ReviewService](06-review-service.md)
- **Depended by (Tier 1)**: [ReportController](01-rest-api-controllers.md)

---

## Business Rules

- `EventPublisherService` exceptions are caught and logged; they do NOT fail the calling operation (booking/review creation)
- Report queries are admin-only (enforced by `@PreAuthorize` in `ReportController`)
- Export format is validated as `CSV` or `PDF` enum before processing

---

## Failure Modes

| Failure | Response |
|---------|---------|
| SQS publish failure | Logged as ERROR; booking/review operation succeeds (decoupled) |
| `EventPublisherException` | Logged; not propagated to caller |
| `reports-handler` unavailable | HTTP 503 (propagated to admin user) |
| Unknown export format | HTTP 400 |

---

## Monitoring
- **Key metrics**: SQS publish rate, SQS publish error rate, report query latency
- **Alerts**: SQS error rate > 1% (events lost), `reports-handler` response time > 2s
- **Logs**: Event type, bookingId/reviewId logged on each SQS publish; errors at ERROR level

