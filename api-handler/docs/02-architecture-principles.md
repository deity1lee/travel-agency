# Section 2 — Architecture Principles

[Architecture](../ARCHITECTURE.md)

---

## Overview

These principles govern all architectural decisions in the `api-handler` service. They are listed in priority order; when principles conflict, higher-ranked principles take precedence.

---

## P1 — Stateless Application Layer

**Statement**: The application tier must not hold user session state in memory. All state is externalised to DynamoDB (JWT blacklist, verification tokens) or the client (JWT).

**Rationale**: Stateless services scale horizontally without sticky sessions. Kubernetes can freely restart pods without session loss.

**Trade-off**: Each request requires a JWT validation step and potentially a DynamoDB read (blacklist check), adding marginal latency.

**Applies to**: [Architecture Layers](03-architecture-layers.md) Tier 2, [Security Architecture](07-security-architecture.md#jwt-token-management)

---

## P2 — Security by Default

**Statement**: All API endpoints require authentication except explicitly public routes. Authorization is enforced at both the route level (Spring Security `SecurityFilterChain`) and the method level (`@PreAuthorize`).

**Rationale**: Reduces attack surface. A missed annotation causes a compile-time or test failure rather than a silent security gap.

**Trade-off**: Developer overhead for correctly annotating new endpoints.

**Applies to**: [Security Architecture](07-security-architecture.md)

---

## P3 — Separation of Concerns Across Tiers

**Statement**: Controllers handle only HTTP concerns (request parsing, response formatting, routing). Services own business logic. Repositories own data access. No business logic in controllers or repositories.

**Rationale**: Enables independent testing of each layer. Business rule changes are isolated to the service layer.

**Trade-off**: More classes and interfaces per feature; slight boilerplate overhead.

**Applies to**: [Architecture Layers](03-architecture-layers.md), [Component Details](components/README.md)

---

## P4 — Asynchronous Event Publishing for Side Effects

**Statement**: Booking and review lifecycle events (status changes, cancellations, new reviews) are published to SQS asynchronously rather than processed inline. The main request flow does not wait for downstream report processing.

**Rationale**: Decouples booking throughput from reporting latency. A failure in the `reports-handler` does not fail a customer's booking operation.

**Trade-off**: Eventual consistency in reporting data. Events may be processed out of order if the queue delivers duplicates.

**Applies to**: [Data Flow Patterns](04-data-flow-patterns.md), [Integration Points](05-integration-points.md#aws-sqs)

---

## P5 — External Configuration via Environment Variables

**Statement**: All secrets, credentials, and environment-specific values (AWS keys, API keys, URLs, JWT secret) are injected via environment variables. No secrets are hard-coded in source files.

**Rationale**: Enables the same Docker image to run across dev/staging/prod. Secrets never appear in version control.

**Trade-off**: Deployment complexity — environment variables must be managed and rotated in the cluster secret store.

**Applies to**: [Operational Considerations](09-operational-considerations.md#environment-variables)

---

## P6 — Defence-in-Depth for User-Generated Content

**Statement**: User-submitted review text passes through EPAM DIAL (AI moderation) before becoming visible. Captcha validation protects high-risk unauthenticated endpoints.

**Rationale**: Prevents abuse of public review and registration flows without purely manual moderation.

**Trade-off**: Moderation calls add latency (~500ms–5s depending on AI response time) and incur external API cost.

**Applies to**: [Security Architecture](07-security-architecture.md#moderation), [Component Details](components/06-review-service.md)

---

## P7 — API-First Design

**Statement**: All service endpoints are documented via OpenAPI 3 (SpringDoc). The Swagger UI is available in all environments at `/swagger-ui.html`.

**Rationale**: Frontend teams and QA can consume and test the API contract without relying on source code.

**Trade-off**: Documentation must be kept in sync with implementation; annotations add code verbosity.

**Applies to**: [Component Details](components/01-rest-api-controllers.md)

---

## P8 — Observable by Design

**Statement**: All controller methods log incoming requests and outgoing responses at INFO level. Errors are logged at ERROR level with stack traces. JaCoCo coverage is enforced in CI.

**Rationale**: Enables rapid incident diagnosis. Coverage gates prevent regressions.

**Trade-off**: Log volume increases with traffic; log aggregation pipeline must scale accordingly.

**Applies to**: [Operational Considerations](09-operational-considerations.md#monitoring-and-logging)

