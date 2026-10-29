[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > Review Service

# Review Service

**Type**: Business Service (Tier 2 — Application/Business Logic Layer)

---

## Purpose

Manages the review lifecycle for completed bookings. Review text is screened by EPAM DIAL AI moderation before persistence. Approved reviews update the tour's aggregate rating.

---

## Classes

| Class | Role |
|-------|------|
| `ReviewServiceImpl` | Core review business logic |
| `ModerationServiceImpl` | Sends review text to EPAM DIAL for AI moderation |
| `TourReviewValidator` | Validates review eligibility and data |
| `ReviewMapper` | Maps `Review` entities to DTOs |

---

## Responsibilities

- **Submit review**: Check booking ownership and eligibility → moderate via DIAL → persist `Review` in DynamoDB → update tour rating aggregate → publish `FeedbackEvent` to SQS
- **Update review**: Validate ownership → re-moderate updated text → persist update → recalculate rating
- **List reviews (per tour)**: Return paginated reviews for a given tour (public, no auth required)
- **AI moderation gate**: Every new or updated review text must pass EPAM DIAL moderation before storage per [P6](../02-architecture-principles.md#p6--defence-in-depth-for-user-generated-content)
- **Rating aggregation**: `ReviewRepository` maintains `RatingAggregates` (sum and count) on the `Tour` record; average is recalculated on each submission

---

## Moderation Flow

```
ReviewService.createReview(request)
  │
  ▼ ModerationService.moderate(text)
  │     → Azure OpenAI SDK → EPAM DIAL (gpt-4.1-mini)
  │     Timeout: 5000ms | Retries: 2
  │
  ├─ [REJECTED] → ReviewModerationRejectedException → HTTP 422 + feedback message
  │
  └─ [APPROVED]
        → Persist Review (DynamoDB)
        → Update Tour rating aggregate (DynamoDB)
        → Publish FeedbackEvent (SQS)
        → HTTP 201 Created
```

---

## Review Entity Key Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| reviewId | String (PK) | Unique review identifier |
| bookingId | String | Associated booking |
| userId | String | Reviewer |
| tourId | String | Reviewed tour |
| rating | int | 1–5 star rating |
| comment | String | Review text (moderated) |
| moderationStatus | `ModerationStatus` | APPROVED / REJECTED |
| createdAt | Instant | Submission timestamp |

---

## Technologies
- **Framework**: Spring Boot 3
- **AI Moderation**: Azure OpenAI SDK 1.0.0-beta.16 (EPAM DIAL endpoint)
- **Location**: `src/main/java/com/epam/edp/demo/service/review/`, `service/moderation/`, `validation/review/`, `mapper/`

---

## Dependencies
- **Depends on (Tier 3)**: [DynamoDB Repositories](09-dynamodb-repositories.md) — `ReviewRepositoryImpl`, `BookingRepositoryImpl`, `TourRepositoryImpl`
- **Depends on (Tier 2)**: [EventPublisherService](08-report-event-service.md)
- **Depends on (External)**: [EPAM DIAL](../05-integration-points.md#int-005-epam-dial-azure-openai-gateway)
- **Depended by (Tier 1)**: [ReviewController](01-rest-api-controllers.md)

---

## Business Rules

- A review can only be submitted for a booking the user owns
- Only one review per booking (duplicate check via `ReviewConflictException`)
- Review is only allowed after booking reaches a defined eligible state
- Moderation failure (timeout or API error) → review rejected (fail-safe)
- Rating (1–5) updates the tour's aggregate `averageRating`

---

## Failure Modes

| Failure | Response |
|---------|---------|
| Booking not owned by user | HTTP 403 |
| Duplicate review | HTTP 409 (`ReviewConflictException`) |
| Moderation rejected | HTTP 422 (`ReviewModerationRejectedException`) with feedback |
| Moderation timeout/error | HTTP 422 (fail-safe — no unmoderated reviews stored) |
| Review not found | HTTP 404 (`ReviewNotFoundException`) |
| DynamoDB unavailable | HTTP 500 |

---

## Monitoring
- **Key metrics**: Reviews submitted/hour, moderation rejection rate, DIAL API latency, DIAL error rate
- **Alerts**: DIAL rejection rate > 10% (potential abuse), DIAL API errors > 5%
- **Logs**: reviewId, tourId, moderationStatus logged at INFO; DIAL errors at ERROR

