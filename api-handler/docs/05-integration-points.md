# Section 6 — Integration Points

[Architecture](../ARCHITECTURE.md)

---

## Overview

The `api-handler` integrates with the following external systems. All integrations use HTTPS. Credentials are injected via environment variables per [P5](02-architecture-principles.md#p5--external-configuration-via-environment-variables).

---

## INT-001: AWS DynamoDB

| Attribute | Value |
|-----------|-------|
| **Type** | AWS Managed NoSQL Database |
| **Region** | `ap-southeast-2` |
| **SDK** | Spring Cloud AWS DynamoDB (`io.awspring.cloud:spring-cloud-aws-starter-dynamodb 3.0.3`) |
| **Auth** | STS role assumption — `org/DeveloperAccessRoleTeam7` on account `317356818777` |
| **Tables** | 8 (see below) |

**DynamoDB Tables**:

| Table Name | Entities Stored | Key Notes |
|------------|-----------------|-----------|
| `tm7-Users-prod` | `User` — user accounts, roles, profile | Partition key: userId |
| `tm7-Tours-prod` | `Tour` — tour catalog, availability, ratings | |
| `tm7-Bookings-prod` | `Booking` — booking state, guests, pricing | |
| `tm7-Reviews-prod` | `Review` — review text, rating, moderation status | |
| `tm7-Documents-prod` | `Document` — upload metadata, S3 key, status | |
| `tm7-TravelAgents-prod` | `TravelAgent` — agent profiles, assignments | |
| `tm7-JwtBlacklist-prod` | `JwtBlacklist` — revoked tokens (logout) | TTL-based expiry |
| `tm7-Tokens-prod` | `VerificationToken` — OTP tokens, email change tokens, password reset tokens | TTL-based expiry |

**Failure Handling**: Jittered backoff retry (`JitteredBackoff`) on DynamoDB SDK exceptions. `DynamoDbUtils` provides shared query/scan helpers.

---

## INT-002: AWS S3

| Attribute | Value |
|-----------|-------|
| **Type** | AWS Object Storage |
| **Region** | `ap-southeast-2` |
| **Bucket** | `tm7-documents-prod` |
| **SDK** | `software.amazon.awssdk:s3` (AWS SDK v2) |
| **Auth** | STS role assumption |
| **Usage** | Presigned PUT URLs for direct browser uploads; presigned GET URLs for downloads |

**Integration Flow**: See [Data Flow Patterns — Flow 4](04-data-flow-patterns.md#flow-4-document-upload-via-s3-presigned-url).

**ADR**: [ADR-004](../adr/ADR-004-s3-document-storage.md)

---

## INT-003: AWS SQS

| Attribute | Value |
|-----------|-------|
| **Type** | AWS Managed Message Queue |
| **Region** | `ap-southeast-2` |
| **Queue** | `tm7-Queue-prod` (standard queue) |
| **SDK** | Spring Cloud AWS SQS (`io.awspring.cloud:spring-cloud-aws-starter-sqs 3.0.3`) |
| **Auth** | STS role assumption |
| **Direction** | Send only (api-handler is producer; reports-handler is consumer) |

**Events published**:
- `BookingStatusChangedEvent` — payload: bookingId, status, customerId, timestamp
- `FeedbackEvent` — payload: reviewId, tourId, rating, timestamp

**ADR**: [ADR-003](../adr/ADR-003-sqs-event-driven-reporting.md)

---

## INT-004: SendGrid Email API

| Attribute | Value |
|-----------|-------|
| **Type** | Transactional Email SaaS |
| **Protocol** | REST (HTTPS) |
| **Auth** | `SENDGRID_API_KEY` environment variable |
| **Sender** | `SENDGRID_EMAIL` environment variable |
| **Usage** | OTP verification emails, password reset emails, email change confirmation |

**Component**: `EmailService` — wraps SendGrid REST API calls.

**Failure Handling**: `EmailSendException` thrown on non-2xx response; surfaced as HTTP 503 to caller.

---

## INT-005: EPAM DIAL (Azure OpenAI Gateway)

| Attribute | Value |
|-----------|-------|
| **Type** | AI Moderation API (LLM Gateway) |
| **Endpoint** | `https://ai-proxy.lab.epam.com` |
| **Model** | `gpt-4.1-mini-2025-04-14` |
| **API Version** | `2024-02-01` |
| **SDK** | `com.azure:azure-ai-openai 1.0.0-beta.16` |
| **Auth** | `DIAL_API_KEY` environment variable |
| **Timeout** | 5000ms (configurable via `dial.moderation.timeout-ms`) |
| **Max Retries** | 2 (configurable via `dial.moderation.max-attempts`) |
| **Usage** | Review text moderation before persistence |

**Component**: `ModerationService` — sends review text to DIAL, receives `APPROVED` or `REJECTED` verdict.

**Failure Handling**: On timeout or error, a `ModerationFeedback` exception is raised, and the review is rejected to prevent unmoderated content from being stored. Per [P6](02-architecture-principles.md#p6--defence-in-depth-for-user-generated-content).

---

## INT-006: Google OAuth2

| Attribute | Value |
|-----------|-------|
| **Type** | OAuth2 Identity Provider |
| **Protocol** | OAuth2 Authorization Code Flow |
| **Auth** | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` environment variables |
| **Scopes** | `profile`, `email` |
| **SDK** | Spring Security OAuth2 Client (`spring-boot-starter-oauth2-client`) |
| **Usage** | Social login — alternative to email/password registration |

**Flow**: See [Data Flow Patterns — Flow 5](04-data-flow-patterns.md#flow-5-google-oauth2-login).

---

## INT-007: Google reCAPTCHA v3

| Attribute | Value |
|-----------|-------|
| **Type** | Bot / Abuse Protection |
| **Protocol** | REST (HTTPS) |
| **Auth** | `APP_CAPTCHA_API_KEY` environment variable |
| **Min Score** | `0.5` (configurable via `app.captcha.min-score`) |
| **Toggle** | `APP_CAPTCHA_ENABLE` (enable/disable per environment) |
| **Usage** | Protects high-risk unauthenticated endpoints (sign-up, forgot password) |

**Component**: `CaptchaService` — validates reCAPTCHA response token against Google's API.

---

## INT-008: reports-handler (Internal Kubernetes Service)

| Attribute | Value |
|-----------|-------|
| **Type** | Internal Kubernetes microservice |
| **Protocol** | REST (HTTP, internal cluster) |
| **Base URL** | `${REPORTS_HANDLER_URL}` environment variable |
| **Auth** | Static API key `secretKey` (header) — [to be rotated to secrets management] |
| **Usage** | Admin report retrieval (`GET /admin/report/**`) |

**Component**: `Service2Client` — `RestTemplate`-based HTTP client.

**Note**: The `reports-handler` also consumes SQS events (INT-003) independently. The REST integration is only for synchronous report query from the admin panel.

