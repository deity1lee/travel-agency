# Section 5 — Data Flow Patterns

[Architecture](../ARCHITECTURE.md)

---

## Overview

This section documents the primary request/response and event-driven data flows through the `api-handler` service.

---

## Flow 1: Authenticated API Request (Happy Path)

**Description**: Standard flow for any authenticated REST operation (e.g., creating a booking).

```
Client
  │  HTTPS POST /bookings  {Authorization: Bearer <JWT>}
  ▼
VerifyRateLimitFilter         [Tier 1]
  │  Check rate-limit bucket (in-memory, keyed by IP)
  ▼
JwtAuthenticationFilter       [Tier 1]
  │  Validate JWT signature, expiry, issuer
  │  Query DynamoDB JwtBlacklist — confirm token not revoked
  │  Populate SecurityContext with CustomUserDetails
  ▼
BookingController             [Tier 1]
  │  @Valid @RequestBody — Jakarta validation
  │  Extract @CurrentUser via CurrentUserArgumentResolver
  ▼
BookingService                [Tier 2]
  │  Check tour availability (TourRepositoryImpl → DynamoDB)
  │  Validate booking rules (TourBookingValidation)
  │  Persist Booking entity (BookingRepositoryImpl → DynamoDB)
  │  Publish SQS BookingStatusChangedEvent (async, non-blocking)
  ▼
BookingController             [Tier 1]
  │  Map result to BookingDetailsResponse DTO
  ▼
HTTP 201 Created  → Client
```

**Async side-effect**:
```
SQS Queue (tm7-Queue-prod)
  └─► reports-handler service  [external]
        └─► Aggregate reporting / manager email
```

---

## Flow 2: Two-Step User Registration

**Description**: Account creation requires email OTP verification.

```
Client  POST /auth/sign-up  {email, password, firstName, lastName}
  ▼
VerifyRateLimitFilter  [Tier 1]  ← rate-limited
  ▼
AuthController → AuthService
  │  Validate password complexity (PasswordValidator)
  │  Check email uniqueness (UserRepositoryImpl → DynamoDB)
  │  Hash password (BCrypt)
  │  Store VerificationToken in DynamoDB (Tokens table, TTL-based)
  │  Send 6-digit OTP via EmailService → SendGrid REST API
  ▼
HTTP 200 OK  →  "Verification code sent"

--- (customer reads email, gets 6-digit code) ---

Client  POST /auth/sign-up/verify  {email, code}
  ▼
AuthController → AuthService
  │  Load VerificationToken from DynamoDB
  │  Validate code match and non-expiry
  │  Create User record in DynamoDB (Users table)
  │  Delete VerificationToken
  ▼
HTTP 201 Created  →  "Account created successfully"
```

---

## Flow 3: Review Submission with AI Moderation

**Description**: Review text passes through the EPAM DIAL moderation gate before persisting.

```
Client  POST /bookings/{id}/review  {rating, comment}
  ▼
JwtAuthenticationFilter  [Tier 1]
  ▼
ReviewController → ReviewService
  │  Validate booking ownership + eligibility (BookingRepositoryImpl)
  │  Check no duplicate review exists (ReviewRepositoryImpl)
  │
  ├─► ModerationService → Azure OpenAI (EPAM DIAL)
  │       model: gpt-4.1-mini
  │       timeout: 5000ms, max retries: 2
  │       Returns: APPROVED | REJECTED + feedback
  │
  ├── [REJECTED] → Throw ReviewModerationRejectedException → HTTP 422
  │
  └── [APPROVED]
        │  Persist Review in DynamoDB (Reviews table)
        │  Update Tour rating aggregate (Tours table)
        │  Publish FeedbackEvent to SQS
        ▼
HTTP 201 Created  →  ReviewResponseDTO
```

---

## Flow 4: Document Upload via S3 Presigned URL

**Description**: Documents are uploaded directly from the browser to S3; the backend only generates the upload URL.

```
Client  POST /bookings/{id}/documents/upload-url
  ▼
DocumentController → DocumentService → S3PresignedUrlService
  │  Validate booking ownership
  │  Generate S3 presigned PUT URL (AWS SDK v2, STS role assumption)
  ▼
HTTP 200 OK  →  {uploadUrl, documentId}

--- (client uploads file directly to S3) ---

Client  POST /bookings/documents/complete  {documentId, bookingId, ...}
  ▼
DocumentController → DocumentService
  │  Validate document metadata
  │  Persist Document entity in DynamoDB (Documents table)
  ▼
HTTP 200 OK  →  EditProfileResponse
```

---

## Flow 5: Google OAuth2 Login

**Description**: Users can authenticate via Google. On first login, a local account is auto-provisioned.

```
Client  GET /oauth2/authorization/google
  ▼
Spring Security OAuth2 Client
  │  Redirect to Google OAuth2 consent page
  ▼
Google → Callback: GET /login/oauth2/code/google?code=...
  ▼
OAuth2UserService
  │  Exchange code for Google user profile
  │  Lookup or create User in DynamoDB by email
  │  Assign CUSTOMER role if new user
  ▼
OAuth2LoginSuccessHandler
  │  Issue JWT access token + refresh token
  ▼
Redirect to frontend with tokens in query params / cookie
```

---

## Flow 6: Admin Report Export

**Description**: Admin users can request aggregate reports and export them as CSV or PDF.

```
Admin Client  GET /admin/report?type=SALES&...
  ▼
JwtAuthenticationFilter + @PreAuthorize("hasRole('ADMIN')")
  ▼
ReportController → ReportService → Service2Client
  │  HTTP GET to reports-handler service (${REPORTS_HANDLER_URL})
  │  Deserialize AggregateReport / SalesAggregateReport
  ▼
HTTP 200 OK  →  ReportResponse

Admin Client  GET /admin/report/export?format=CSV
  ▼
ReportController → ReportService
  │  Build report data
  │  Format as CSV or PDF (Apache POI / manual CSV serialisation)
  ▼
HTTP 200 OK  Content-Type: text/csv | application/pdf
```

---

## Async Event Flow (SQS)

**Description**: Booking and review lifecycle events are published to SQS and consumed by `reports-handler`.

```
BookingService / ReviewService                [Tier 2]
  │  EventPublisherService.publish(event)
  │  Serialise to JSON
  ▼
AWS SQS Queue: tm7-Queue-prod                 [Tier 3]
  │  Standard queue, at-least-once delivery
  ▼
reports-handler (separate service)            [External]
  │  Deserialise event
  │  Update aggregate report tables
  │  Schedule weekly summary email (SendGrid)
```

**Event types published**:
- `BookingStatusChangedEvent` — booking created, updated, cancelled
- `FeedbackEvent` — review submitted

