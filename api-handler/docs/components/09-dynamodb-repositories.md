[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > DynamoDB Repositories

# DynamoDB Repositories

**Type**: Repository / Data Access (Tier 3 — Data Layer)

---

## Purpose

Provide the data access layer for all domain entities persisted in AWS DynamoDB. Each repository encapsulates DynamoDB read/write operations for a specific entity type.

---

## Repositories

| Repository Class | Entity | DynamoDB Table |
|-----------------|--------|----------------|
| `UserRepositoryImpl` | `User` | `tm7-Users-prod` |
| `TourRepositoryImpl` | `Tour` | `tm7-Tours-prod` |
| `BookingRepositoryImpl` | `Booking` | `tm7-Bookings-prod` |
| `ReviewRepositoryImpl` | `Review` | `tm7-Reviews-prod` |
| `DocumentRepositoryImpl` | `Document` | `tm7-Documents-prod` |
| `TravelAgentRepositoryImpl` | `TravelAgent` | `tm7-TravelAgents-prod` |
| `JwtBlacklistRepository` | `JwtBlacklist` | `tm7-JwtBlacklist-prod` |
| `VerificationTokenRepository` | `VerificationToken`, `EmailChangeToken`, `PasswordResetToken` | `tm7-Tokens-prod` |
| `TokenRepository` | Generic token operations | `tm7-Tokens-prod` |

---

## Technologies

| Component | Details |
|-----------|---------|
| **SDK** | Spring Cloud AWS DynamoDB Enhanced Client (`io.awspring.cloud:spring-cloud-aws-starter-dynamodb 3.0.3`) |
| **Entity Mapping** | `@DynamoDbBean` annotated entity classes |
| **Config** | `AwsConfig` — configures `DynamoDbEnhancedClient` with STS-assumed credentials |
| **Utilities** | `DynamoDbUtils` — shared scan/query helpers |
| **Error Handling** | `JitteredBackoff` — exponential backoff with jitter on transient errors |
| **Location** | `src/main/java/com/epam/edp/demo/repository/`, `entity/`, `util/` |

---

## DynamoDB Tables

| Table | Primary Key | GSI / Sort Key | Key Notes |
|-------|-------------|----------------|-----------|
| `tm7-Users-prod` | userId (PK) | email GSI | Unique email index for sign-in |
| `tm7-Tours-prod` | tourId (PK) | | Aggregate rating updated on review |
| `tm7-Bookings-prod` | bookingId (PK) | userId GSI | Query by user for booking list |
| `tm7-Reviews-prod` | reviewId (PK) | tourId GSI, bookingId | Query by tour for review list |
| `tm7-Documents-prod` | documentId (PK) | bookingId GSI | Query by booking for document list |
| `tm7-TravelAgents-prod` | agentId (PK) | | Agent assignment lookup |
| `tm7-JwtBlacklist-prod` | tokenHash (PK) | | TTL attribute for automatic expiry |
| `tm7-Tokens-prod` | tokenId (PK) | email+type GSI | TTL attribute for OTP expiry |

**Note**: Table schema and indexes are provisioned externally (IaC). The application assumes tables exist.

**ADR**: [ADR-001](../../adr/ADR-001-dynamodb-as-primary-database.md)

---

## Access Patterns

### JWT Blacklist
- Write: `save(JwtBlacklist)` with TTL = token expiry on logout
- Read: `existsById(tokenHash)` on every authenticated request (via `JwtAuthenticationFilter`)
- TTL auto-expires records — no manual cleanup required

### Verification Tokens (OTP)
- Write: `save(VerificationToken)` with short TTL on OTP generation
- Read: `findByEmailAndType(email, type)` on OTP verification
- Delete: `delete(token)` on successful verification
- TTL handles expiry cleanup automatically

### Tour Ratings
- `ReviewRepositoryImpl` returns `RatingAggregates` (sum, count)
- `TourRepositoryImpl` applies `RatingAggregateUpdate` to increment tour rating aggregate on review submission

---

## Dependencies
- **Depends on (External)**: AWS DynamoDB (`ap-southeast-2`), AWS STS (credential provider)
- **Depended by (Tier 2)**: [AuthService](03-auth-service.md), [TourService](04-tour-service.md), [BookingService](05-booking-service.md), [ReviewService](06-review-service.md), [DocumentService](07-document-service.md), [EventPublisherService](08-report-event-service.md), [Security Filter Chain](02-security-filter-chain.md)

---

## Error Handling

- `JitteredBackoff` utility wraps DynamoDB SDK calls with exponential backoff + jitter
- Transient errors (throttling, network) are retried
- Persistent errors propagate up to the service layer → eventually HTTP 500

---

## Scaling
- **Horizontal**: Multiple pods share DynamoDB access; no connection pool limitations (DynamoDB is serverless)
- **DynamoDB capacity**: [To be defined — on-demand vs provisioned per table]
- **Throughput**: DynamoDB on-demand mode scales automatically; provisioned mode requires capacity planning

---

## Backup & Recovery

| Aspect | Approach |
|--------|---------|
| Backup | AWS DynamoDB PITR (Point-In-Time Recovery) — [confirm enabled per table] |
| Retention | DynamoDB PITR: 35 days |
| RTO | < 1 hour (DynamoDB table restore) |
| RPO | < 5 minutes (DynamoDB PITR granularity) |

---

## Monitoring
- **Key metrics**: DynamoDB read/write capacity consumed, throttling rate, latency p95
- **Alerts**: DynamoDB throttling events, latency > 100ms p95
- **Logs**: DynamoDB errors logged at ERROR; slow queries (via `JitteredBackoff` debug logging)

