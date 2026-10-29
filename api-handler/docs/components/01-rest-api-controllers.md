[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > REST API Controllers

# REST API Controllers

**Type**: API Controller (Tier 1 — Presentation Layer)

---

## Purpose

Expose the HTTP REST API surface of the `api-handler` service. Each controller handles a specific domain area, delegating all business logic to the corresponding Tier 2 service.

---

## Controllers

| Controller | Base Path | Responsibility |
|------------|-----------|----------------|
| `AuthController` | `/auth/**` | User registration, authentication, profile management, password operations |
| `TourController` | `/tours/**` | Tour catalog browsing, destinations, tour reviews |
| `BookingController` | `/bookings/**` | Booking lifecycle (create, read, update, cancel) |
| `ReviewController` | `/bookings/{id}/review` | Submit and update booking reviews |
| `DocumentController` | `/bookings/{id}/documents/**` | Document upload URL generation and upload confirmation |
| `ReportController` | `/admin/report/**` | Aggregate report retrieval and CSV/PDF export (admin only) |
| `DevController` | `/dev/**` | Development seed data and OTP retrieval (not for production) |
| `HelloEdpController` | `/api/hello` | Health-check / smoke-test endpoint |
| `SwaggerRedirectController` | `/` | Redirect root to Swagger UI |

---

## Key Endpoints

### Auth (`/auth/**`)
| Method | Path | Auth Required | Role | Description |
|--------|------|---------------|------|-------------|
| POST | `/auth/sign-up` | ❌ | — | Start registration, send OTP email |
| POST | `/auth/sign-up/verify` | ❌ | — | Verify OTP, create account |
| POST | `/auth/non/resend` | ❌ | — | Resend OTP (unauthenticated) |
| POST | `/auth/resend` | ✅ | CUSTOMER | Resend OTP (authenticated) |
| POST | `/auth/sign-in` | ❌ | — | Authenticate, receive JWT |
| POST | `/auth/logout` | ✅ | ANY | Revoke token, blacklist JWT |
| PATCH | `/auth/me` | ✅ | CUSTOMER | Update profile name/photo |
| POST | `/auth/me/email` | ✅ | CUSTOMER | Initiate email change |
| POST | `/auth/email/verify` | ✅ | CUSTOMER | Confirm email change |
| POST | `/auth/me/password` | ✅ | CUSTOMER | Change password |
| POST | `/auth/forgot-password` | ❌ | — | Request password reset code |
| POST | `/auth/forgot-password/verify` | ❌ | — | Verify reset code |
| POST | `/auth/reset-password` | ❌ | — | Set new password |

### Tours (`/tours/**`)
| Method | Path | Auth Required | Description |
|--------|------|---------------|-------------|
| GET | `/tours/available` | ❌ | List tours with filters/pagination |
| GET | `/tours/{id}` | ❌ | Get tour details |
| GET | `/tours/destinations` | ❌ | Get available destinations |
| GET | `/tours/{id}/reviews` | ❌ | Get paginated tour reviews |

### Bookings (`/bookings/**`)
| Method | Path | Auth Required | Description |
|--------|------|---------------|-------------|
| POST | `/bookings` | ✅ | Create booking |
| GET | `/bookings` | ✅ | List user bookings |
| GET | `/bookings/{id}` | ✅ | Get booking details |
| PUT | `/bookings/{id}` | ✅ | Update booking |
| PATCH | `/bookings/{id}` | ✅ | Partial update booking |
| DELETE | `/bookings/{id}` | ✅ | Cancel booking |
| POST | `/bookings/{id}/review` | ✅ | Submit review |
| PUT | `/bookings/{id}/review` | ✅ | Update review |
| POST | `/bookings/{id}/documents/upload-url` | ✅ | Generate S3 upload URL |
| POST | `/bookings/documents/complete` | ✅ | Confirm document upload |

### Reports (`/admin/report/**`)
| Method | Path | Auth Required | Role |
|--------|------|---------------|------|
| GET | `/admin/report` | ✅ | ADMIN |
| GET | `/admin/report/export` | ✅ | ADMIN |

---

## Responsibilities

- Parse and validate `@RequestBody` with Jakarta Validation (`@Valid`)
- Authenticate caller via `@CurrentUser` (resolved by `CurrentUserArgumentResolver` using Spring Security context)
- Delegate to Tier 2 services — no business logic in controllers
- Map service exceptions to HTTP error responses via `@ControllerAdvice` handlers
- Log incoming requests and outgoing responses at `INFO` level
- Serve OpenAPI documentation via `@Tag`, `@Operation`, `@ApiResponse` annotations

---

## Technologies
- **Framework**: Spring Boot 3.0.3 (Spring MVC)
- **Location**: `src/main/java/com/epam/edp/demo/controller/`
- **API Docs**: SpringDoc OpenAPI 2.3.0 at `/swagger-ui.html`

---

## Dependencies
- **Depends on (Tier 2)**: [AuthService](03-auth-service.md), [TourService](04-tour-service.md), [BookingService](05-booking-service.md), [ReviewService](06-review-service.md), [DocumentService](07-document-service.md), [EventPublisherService](08-report-event-service.md)
- **Depended by**: Frontend clients, Admin UI, mobile apps

---

## Exception Handling

| Advice Class | Handles | Response |
|---|---|---|
| `GlobalExceptionHandler` | General exceptions (`MethodArgumentNotValidException`, etc.) | HTTP 400/500 |
| `AuthExceptionHandler` | Auth-specific exceptions | HTTP 400/401/409/503 |
| `ReviewControllerAdvice` | Review exceptions | HTTP 404/409/422 |
| `TourControllerAdvice` | Tour exceptions | HTTP 404/400 |
| `ReportControllerAdvice` | Report exceptions | HTTP 4xx/5xx |

---

## Failure Modes
- **Service unavailable (DynamoDB/SQS down)**: Returns HTTP 503 via global handler
- **Validation failure**: Returns HTTP 400 with field-level error details
- **Unauthorised**: Returns HTTP 401 (JWT missing/invalid) or HTTP 403 (insufficient role)

---

## Monitoring
- **Key metrics**: Request rate per endpoint, p95/p99 latency, 4xx/5xx error rate
- **Logs**: All controller methods log request entry and exit at INFO level; exceptions at ERROR level
- **Alerts**: High error rate on `/bookings`, `/auth/sign-in`

