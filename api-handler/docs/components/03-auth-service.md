[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > Auth Service

# Auth Service

**Type**: Business Service (Tier 2 — Application/Business Logic Layer)

---

## Purpose

Owns the complete user identity lifecycle: registration with email OTP verification, sign-in with JWT issuance, Google OAuth2 social login, password management, email change, and session revocation.

---

## Classes

| Class | Role |
|-------|------|
| `AuthServiceImpl` | Primary auth business logic (sign-up, sign-in, logout, profile, password, email) |
| `JwtService` | JWT generation, parsing, and validation |
| `VerificationTokenService` | OTP token creation, lookup, and expiry management |
| `CustomUserDetailsService` | Spring Security `UserDetailsService` — load user by email from DynamoDB |
| `OAuth2UserService` / `OAuth2UserServiceImpl` | Process Google OAuth2 profile; auto-provision new users |

---

## Responsibilities

- **Sign-up (2-step)**: Store `VerificationToken` in DynamoDB → send 6-digit OTP via SendGrid → on verify, create `User` in DynamoDB
- **Sign-in**: Validate credentials via BCrypt → issue JWT access token + refresh token
- **Logout**: Blacklist JWT in `tm7-JwtBlacklist-prod` DynamoDB table (TTL = token expiry)
- **Google OAuth2**: Process callback, lookup or create local `User` by Google email, issue JWT
- **Profile update**: Update user name / profile photo in DynamoDB
- **Email change**: Generate `EmailChangeToken` OTP → verify → update email in DynamoDB
- **Password change**: Validate current password (BCrypt) → hash new password → persist
- **Forgot password**: Generate `PasswordResetToken` → send OTP via SendGrid → verify → reset password
- **Captcha validation**: Delegate to `CaptchaService` for protected endpoints (when enabled)
- **OTP rate limiting**: `VerifyRateLimiter` enforced by `VerifyRateLimitFilter` (Tier 1)

---

## Verification Token Types

| Type | Table | TTL | Used For |
|------|-------|-----|---------|
| `EmailVerificationToken` | `tm7-Tokens-prod` | Short (minutes) | Sign-up OTP |
| `EmailChangeToken` | `tm7-Tokens-prod` | Short | Email change OTP |
| `PasswordResetToken` | `tm7-Tokens-prod` | Short | Password reset OTP |

---

## JWT Details

- **Algorithm**: HMAC-SHA256
- **Secret**: `jwt.secret` application property (should be injected via env var — see [Security Architecture](../07-security-architecture.md#credential-management))
- **Claims**: userId, email, role, expiry
- **Revocation**: Blacklist in DynamoDB (`JwtBlacklist`) — checked on every authenticated request

**ADR**: [ADR-002](../../adr/ADR-002-jwt-stateless-authentication.md)

---

## Technologies
- **Framework**: Spring Boot 3, Spring Security 6
- **JWT**: JJWT 0.12.5
- **Password**: BCrypt (Spring Security)
- **Email**: Delegates to `EmailService` → SendGrid (see [Integration Points](../05-integration-points.md#int-004-sendgrid-email-api))
- **Location**: `src/main/java/com/epam/edp/demo/service/auth/`

---

## Dependencies
- **Depends on (Tier 3)**: [DynamoDB Repositories](09-dynamodb-repositories.md) — `UserRepositoryImpl`, `VerificationTokenRepository`, `JwtBlacklistRepository`
- **Depends on (External)**: [SendGrid](../05-integration-points.md#int-004-sendgrid-email-api), [Google OAuth2](../05-integration-points.md#int-006-google-oauth2), [reCAPTCHA](../05-integration-points.md#int-007-google-recaptcha-v3)
- **Depended by (Tier 1)**: [AuthController](01-rest-api-controllers.md), [Security Filter Chain](02-security-filter-chain.md)

---

## Business Rules

- Email must be globally unique (checked in DynamoDB before registration)
- Passwords must meet complexity requirements (validated by `PasswordValidator`)
- OTP codes expire and can be resent; resend rate-limited by `VerifyRateLimitFilter`
- Sign-up is a 2-step flow: account is not created until OTP is verified
- Google OAuth2 users auto-provision with `CUSTOMER` role on first login
- Logout persists revocation until token expiry (TTL-based DynamoDB cleanup)

---

## Failure Modes

| Failure | Response |
|---------|---------|
| Email already registered | HTTP 409 Conflict |
| Invalid/expired OTP | HTTP 400 Bad Request |
| Wrong password on sign-in | HTTP 401 Unauthorized |
| Email verification not completed before sign-in | HTTP 403 (email not verified) |
| SendGrid unavailable | HTTP 503 (`EmailSendException`) |
| DynamoDB unavailable | HTTP 500 (propagated from repository) |
| Captcha validation failed | HTTP 400 (`CaptchaValidationException`) |

---

## Monitoring
- **Key metrics**: Sign-up rate, sign-in success/failure rate, OTP send rate, password reset rate
- **Alerts**: High auth failure rate (brute-force indicator), email send failure rate
- **Logs**: Email addresses logged at INFO on key operations (registration, sign-in, OTP resend)

