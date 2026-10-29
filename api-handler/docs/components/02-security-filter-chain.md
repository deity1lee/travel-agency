[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > Security Filter Chain

# Security Filter Chain

**Type**: Security Filter (Tier 1 — Presentation Layer)

---

## Purpose

Enforce authentication, authorisation, rate limiting, and CORS policy on every inbound HTTP request before it reaches any controller.

---

## Components

| Class | Role |
|-------|------|
| `SecurityConfig` | Spring `@Configuration` — assembles the `SecurityFilterChain` bean |
| `JwtAuthenticationFilter` | Validates Bearer JWT tokens on every request |
| `VerifyRateLimitFilter` | Rate-limits verification/OTP endpoints per IP |
| `VerifyRateLimiter` | In-memory rate limit bucket logic |
| `OAuth2LoginSuccessHandler` | Issues JWT tokens after successful Google OAuth2 callback |
| `OAuth2LoginFailureHandler` | Handles Google OAuth2 errors |

---

## Filter Execution Order

```
Inbound Request
      │
      ▼
VerifyRateLimitFilter          (runs BEFORE JwtAuthenticationFilter)
      │  Check rate-limit bucket per client IP for OTP paths
      │  → 429 Too Many Requests if exceeded
      ▼
JwtAuthenticationFilter        (runs BEFORE UsernamePasswordAuthenticationFilter)
      │  Extract Bearer token from Authorization header
      │  Validate JWT signature (HMAC-SHA256)
      │  Check token expiry
      │  Query DynamoDB JwtBlacklist (revocation check)
      │  Populate SecurityContext with CustomUserDetails
      ▼
Spring Security AuthorizationFilter
      │  Enforce route-level access rules (SecurityFilterChain)
      │  Enforce method-level rules (@PreAuthorize)
      ▼
Controller
```

---

## Route Access Rules (`SecurityConfig`)

| Pattern | Access |
|---------|--------|
| `OPTIONS /**` | Permit all (CORS preflight) |
| `/v3/api-docs/**`, `/swagger-ui/**` | Permit all |
| `/auth/sign-in`, `/auth/sign-up`, `/auth/sign-up/verify`, `/auth/sign-up/resend` | Permit all |
| `/auth/non/resend`, `/auth/forgot-password/**`, `/auth/reset-password` | Permit all |
| `/tours/**` | Permit all |
| `/dev/seed`, `/dev/code` | Permit all |
| `/api/hello` | Permit all |
| `/oauth2/**`, `/login/**` | Permit all |
| `/auth/logout` | Authenticated only |
| `/**` (all other) | Authenticated only |

---

## CORS Configuration

- **Allowed Origins**: `${ALLOWED_ORIGINS}` environment variable (comma-separated)
- **Allowed Methods**: `GET, POST, PUT, DELETE, PATCH, OPTIONS`
- **Allowed Headers**: `Authorization, Content-Type`
- **Exposed Headers**: `Authorization`
- **Credentials**: `allowCredentials = true`

---

## JWT Validation (`JwtAuthenticationFilter`)

1. Extract `Authorization: Bearer <token>` header
2. Parse and verify JWT signature using HMAC-SHA256 and `jwt.secret`
3. Check token expiry claim
4. Load `CustomUserDetails` via `CustomUserDetailsService`
5. Check `JwtBlacklist` DynamoDB table — if found → reject with 401
6. Set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`

---

## Rate Limiting (`VerifyRateLimitFilter`)

- Applied to OTP verification endpoints (rate-limit bucket keyed by client IP)
- In-memory buckets — **per-pod, not cluster-wide** (see [Security Gaps](../07-security-architecture.md#security-gaps--known-issues))
- Returns HTTP 429 when limit exceeded

---

## Technologies
- **Framework**: Spring Security 6 (`SecurityFilterChain`, `@EnableMethodSecurity`)
- **JWT**: JJWT 0.12.5
- **Location**: `src/main/java/com/epam/edp/demo/config/SecurityConfig.java`, `filter/`, `ratelimit/`, `handler/`

---

## Dependencies
- **Depends on (Tier 2)**: [Auth Service](03-auth-service.md) (via `CustomUserDetailsService`, `JwtService`)
- **Depends on (Tier 3)**: [DynamoDB Repositories](09-dynamodb-repositories.md) (JWT blacklist check)
- **Depended by**: All controllers ([REST API Controllers](01-rest-api-controllers.md))

---

## Failure Modes
- **JWT signature invalid**: HTTP 401 Unauthorized
- **JWT expired**: HTTP 401 Unauthorized
- **Token blacklisted (logged out)**: HTTP 401 Unauthorized
- **DynamoDB blacklist unavailable**: Request treated as invalid (fail-safe)
- **Rate limit exceeded**: HTTP 429 Too Many Requests

