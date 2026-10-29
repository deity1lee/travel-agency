# Section 9 — Scalability & Performance

[Architecture](../ARCHITECTURE.md)

---

## Overview

The `api-handler` is a stateless Spring Boot service designed for horizontal scalability via Kubernetes. All state is externalised to DynamoDB, S3, and SQS per [P1](02-architecture-principles.md#p1--stateless-application-layer).

---

## SLA Targets

| Metric | Target |
|--------|--------|
| API Response Time p95 | < 500ms (see [Key Metrics](01-system-overview.md#16-key-metrics)) |
| API Response Time p99 | < 1000ms |
| System Availability | 99.9% |
| Review Moderation Latency | < 5000ms (DIAL timeout) |

---

## Horizontal Scaling

### Kubernetes HPA

- Managed via Helm chart (`deploy-templates/hpa.yaml`)
- Stateless pods — any pod can handle any request (no sticky sessions)
- Scale-out trigger: CPU utilisation or custom metrics (configured in HPA)

### Scaling Constraints

| Component | Scalable | Notes |
|-----------|----------|-------|
| api-handler pods | ✅ Yes | Stateless, horizontally scalable |
| DynamoDB | ✅ Yes | AWS-managed, on-demand or provisioned capacity |
| SQS | ✅ Yes | AWS-managed, effectively unlimited throughput |
| S3 | ✅ Yes | AWS-managed, unlimited storage |
| EPAM DIAL moderation | ⚠️ Rate-limited | Subject to DIAL API quotas |
| SendGrid email | ⚠️ Rate-limited | Subject to SendGrid plan limits |
| Rate limiter | ⚠️ Per-pod | In-memory; not cluster-wide (see [Security Architecture](07-security-architecture.md#rate-limiting)) |

---

## Performance Considerations

### DynamoDB Access Patterns

- All repositories use the DynamoDB Enhanced Client (Spring Cloud AWS) with `@DynamoDbBean` annotated entities
- `JitteredBackoff` utility provides exponential backoff with jitter on transient DynamoDB errors
- `DynamoDbUtils` provides shared scan/query helpers
- Table scans are avoided where possible; queries use partition key access

### AWS Credential Refresh

- Credentials are obtained via STS role assumption (`org/DeveloperAccessRoleTeam7`)
- The AWS SDK handles credential refresh automatically before expiry
- `AwsConfig` configures the STS-backed credentials provider

### AI Moderation Latency Impact

- Review submissions are synchronous and include a DIAL API call (up to 5000ms)
- This increases p99 latency for `POST /bookings/{id}/review` above the general target
- Timeout and retry settings: `dial.moderation.timeout-ms=5000`, `dial.moderation.max-attempts=2`

### Async Event Publishing

- `EventPublisherService` publishes SQS messages asynchronously (non-blocking from the booking/review request path)
- SQS publish latency does not contribute to API response time per [P4](02-architecture-principles.md#p4--asynchronous-event-publishing-for-side-effects)

---

## Capacity Planning

| Dimension | Current Approach | Notes |
|-----------|-----------------|-------|
| Pod count | HPA-managed | Min/max replica counts in `deploy-templates/hpa.yaml` |
| DynamoDB capacity | [To be defined — on-demand vs provisioned] | |
| SQS throughput | Standard queue | At-least-once, no ordering guarantee |
| S3 storage | Bucket: `tm7-documents-prod` | No current storage lifecycle policy defined |

---

## Resilience Patterns

| Pattern | Implementation |
|---------|----------------|
| Retry on transient errors | `JitteredBackoff` for DynamoDB; DIAL retry (`max-attempts`) |
| Circuit breaker | Not currently implemented — [TODO for future] |
| Timeout | DIAL moderation: 5000ms; HTTP clients: [To be defined in RestTemplate config] |
| Graceful degradation | DIAL failure → review rejected (fail-safe, not fail-open) |
| Pod restart recovery | Stateless design — no warm-up state required |

---

## Load Testing Baseline

- No formal load test results documented at this time
- Recommended tool: Apache JMeter or k6 targeting the Kubernetes Ingress
- Key scenarios to test: login → browse tours → create booking → upload document → submit review

