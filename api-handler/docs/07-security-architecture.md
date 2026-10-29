# Section 8 — Security Architecture

[Architecture](../ARCHITECTURE.md)

---

## Overview

Security is layered across all three tiers. The guiding principle is [P2 — Security by Default](02-architecture-principles.md#p2--security-by-default): every endpoint requires authentication unless explicitly whitelisted.

---

## Authentication

### JWT Token Authentication

- **Standard**: Bearer JWT in `Authorization` header
- **Library**: JJWT 0.12.5 (see [Technology Stack](06-technology-stack.md))
- **Validation**: `JwtAuthenticationFilter` runs before every request
  - Verifies signature using HMAC-SHA256 (`jwt.secret` env var)
  - Checks token expiry
  - Queries `JwtBlacklist` DynamoDB table to detect revoked tokens (logout)
  - Populates `SecurityContext` with `CustomUserDetails`
- **Stateless**: No HTTP session is created per [P1](02-architecture-principles.md#p1--stateless-application-layer). Session policy is `IF_REQUIRED` (OAuth2 only).

### Google OAuth2 (Social Login)

- **Protocol**: OAuth2 Authorization Code Flow
- **Provider**: Google (`profile`, `email` scopes)
- **Handler**: `OAuth2LoginSuccessHandler` — issues JWT tokens on successful Google authentication
- **Failure**: `OAuth2LoginFailureHandler` — redirects to error URL
- **New users**: Auto-provisioned with `CUSTOMER` role on first Google login

### ADR reference: [ADR-002](../adr/ADR-002-jwt-stateless-authentication.md)

---

## Authorization

### Role-Based Access Control (RBAC)

| Role | Capabilities |
|------|-------------|
| `CUSTOMER` | Browse tours, create/manage own bookings, upload documents, submit reviews, manage own profile |
| `TRAVEL_AGENT` | View assigned bookings (role-gated endpoints) |
| `ADMIN` | Access aggregate reports, export data (`/admin/report/**`) |

- Method-level security: `@EnableMethodSecurity` + `@PreAuthorize` annotations
- Route-level security: `SecurityFilterChain` in `SecurityConfig`

### Public Endpoints (No Authentication Required)

```
GET  /tours/**
POST /auth/sign-in
POST /auth/sign-up
POST /auth/sign-up/verify
POST /auth/sign-up/resend
POST /auth/non/resend
POST /auth/forgot-password
POST /auth/forgot-password/verify
POST /auth/reset-password
GET  /v3/api-docs/**
GET  /swagger-ui/**
GET  /api/hello
POST /dev/seed  (development only)
GET  /dev/code  (development only)
```

All other endpoints require a valid JWT.

---

## JWT Token Management

### Token Blacklist (Logout Invalidation)

- On `POST /auth/logout`, the token is written to `tm7-JwtBlacklist-prod` DynamoDB table
- TTL is set to the token's expiry time — DynamoDB auto-expires the record
- `JwtAuthenticationFilter` checks blacklist on every authenticated request
- **Component**: [JwtBlacklist Repository](components/09-dynamodb-repositories.md)

### Verification Tokens (OTP)

- 6-digit numeric codes stored in `tm7-Tokens-prod` with TTL-based expiry
- Used for: sign-up verification, email change, password reset
- Token types: `EmailVerificationToken`, `EmailChangeToken`, `PasswordResetToken`
- Rate-limited via `VerifyRateLimitFilter`

---

## Rate Limiting

| Target | Mechanism | Limit |
|--------|-----------|-------|
| OTP / Verification endpoints | `VerifyRateLimitFilter` + `VerifyRateLimiter` | In-memory per-IP bucket (limit configurable) |

The rate limiter is in-memory (per pod). Under multi-replica deployments, rate limiting is not cluster-wide. For distributed rate limiting, a Redis-backed solution should be considered in future work.

---

## Content Moderation (User-Generated Content)

- **What**: Review text submitted via `POST /bookings/{id}/review`
- **How**: `ModerationService` sends text to EPAM DIAL (GPT-4.1-mini) via Azure OpenAI SDK
- **Result**: `APPROVED` → review persisted; `REJECTED` → HTTP 422 with moderation feedback
- **Timeout**: 5000ms; max 2 retries; on error → review rejected (fail-safe)
- **Principle**: [P6 — Defence-in-Depth for User-Generated Content](02-architecture-principles.md#p6--defence-in-depth-for-user-generated-content)

---

## CAPTCHA Protection

- **Provider**: Google reCAPTCHA v3
- **Scope**: High-risk unauthenticated endpoints (sign-up, forgot password)
- **Min Score**: 0.5 (configurable via `app.captcha.min-score`)
- **Toggle**: `APP_CAPTCHA_ENABLE` — can be disabled in non-production environments

---

## CORS Configuration

- Allowed origins: `${ALLOWED_ORIGINS}` (comma-separated list, injected at runtime)
- Allowed methods: `GET, POST, PUT, DELETE, PATCH, OPTIONS`
- Allowed headers: `Authorization, Content-Type`
- Exposed headers: `Authorization`
- Credentials: allowed (`allowCredentials = true`)
- All OPTIONS preflight requests are permitted

---

## Transport Security

- All external-facing communication is HTTPS (enforced at the Kubernetes Ingress / Load Balancer level)
- Internal cluster service-to-service communication uses HTTP (within the cluster network)
- All AWS SDK calls use HTTPS (SDK default)

---

## Credential Management

All secrets are injected via environment variables — never stored in source code or container images. Per [P5](02-architecture-principles.md#p5--external-configuration-via-environment-variables).

| Secret | Variable | Used By |
|--------|----------|---------|
| AWS access key | `AWS_ACCESS_KEY_ID` | DynamoDB, S3, SQS |
| AWS secret key | `AWS_SECRET_ACCESS_KEY` | DynamoDB, S3, SQS |
| AWS session token | `AWS_SESSION_TOKEN` | STS role assumption |
| JWT signing secret | `jwt.secret` (hard-coded in dev) | JwtService — **MUST be rotated for production** |
| Google OAuth2 client ID | `GOOGLE_CLIENT_ID` | OAuth2 flow |
| Google OAuth2 client secret | `GOOGLE_CLIENT_SECRET` | OAuth2 flow |
| DIAL API key | `DIAL_API_KEY` | ModerationService |
| SendGrid API key | `SENDGRID_API_KEY` | EmailService |
| reCAPTCHA secret key | `APP_CAPTCHA_API_KEY` | CaptchaService |
| reports-handler API key | `secretKey` (hard-coded) | Service2Client — **TODO: move to env var** |

---

## Security Gaps & Known Issues

| # | Issue | Severity | Recommendation |
|---|-------|----------|----------------|
| 1 | JWT secret `jwt.secret` is a static value in `application.properties` | High | Inject via `JWT_SECRET` environment variable; rotate periodically |
| 2 | `reports-handler` API key is hard-coded as `secretKey` | Medium | Move to environment variable / Kubernetes Secret |
| 3 | Rate limiting is per-pod (not cluster-wide) | Medium | Consider Redis-backed distributed rate limiter |
| 4 | DynamoDB tables accessible by any pod with the AWS credentials | Low | Apply DynamoDB resource-based policies per table |

---

## Threat Model (Summary)

| Threat | Mitigation |
|--------|------------|
| Unauthorized API access | JWT authentication on all non-public endpoints |
| Token replay after logout | JWT blacklist in DynamoDB |
| Brute-force OTP attacks | Rate limiting (`VerifyRateLimitFilter`) |
| Account enumeration | Uniform error messages on auth failures |
| Malicious review content | EPAM DIAL moderation gate |
| Bot registration / abuse | Google reCAPTCHA v3 |
| CSRF | CSRF disabled (stateless JWT — no cookie-based sessions for API calls) |
| XSS via CORS | Explicit CORS allowlist |
| Credential leakage | Secrets via environment variables only |

