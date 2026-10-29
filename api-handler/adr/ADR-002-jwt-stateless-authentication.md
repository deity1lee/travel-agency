# ADR-002: JWT Stateless Authentication

**Status**: Accepted
**Date**: 2026-07-10
**Authors**: Architecture Team
**Related**: [ADR-001](ADR-001-dynamodb-as-primary-database.md)

---

## Context

The service needs an authentication mechanism that:
- Supports stateless Kubernetes pods (no sticky sessions)
- Works with both email/password and Google OAuth2 login paths
- Allows session revocation (logout) without a central session store
- Integrates cleanly with Spring Security 6

## Decision

Use **JWT (JSON Web Token)** signed with HMAC-SHA256 via JJWT 0.12.5. Store a **JWT blacklist** in DynamoDB (`tm7-JwtBlacklist-prod`) for token revocation on logout. Use Spring Security's `SecurityFilterChain` with a custom `JwtAuthenticationFilter` for token validation on every request.

## Rationale

- **Stateless by default**: JWT carries all claims needed for authorisation — no server-side session state
- **Horizontal scalability**: Any pod can validate any JWT (shared secret) without coordination
- **Revocation support**: DynamoDB blacklist with TTL provides logout invalidation without a Redis dependency
- **OAuth2 compatibility**: After Google OAuth2 callback, a JWT is issued using the same mechanism — unified auth model
- **Spring Security integration**: `JwtAuthenticationFilter` + `SecurityFilterChain` is idiomatic Spring Security 6

## Consequences

**Positive**:
- Pods are fully stateless — no session affinity required in Kubernetes
- No additional infrastructure (Redis) needed for token management
- DynamoDB TTL auto-cleans expired blacklist entries
- Single auth model for both email/password and OAuth2 flows

**Negative**:
- Every authenticated request incurs a DynamoDB read (blacklist check)
- JWT secret rotation requires redeployment (current implementation has static secret in `application.properties` — **needs to be moved to env var**)
- Short-lived token refresh strategy not fully specified — token lifetime trade-offs (security vs UX)

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|----------------|
| Server-side sessions (HttpSession) | Requires sticky sessions or shared session store; breaks horizontal scaling |
| Redis for session storage | Additional infrastructure; DynamoDB already in stack |
| Opaque tokens (reference tokens) | Requires lookup on every request — same DynamoDB cost without JWT benefits |
| Spring Session | Session sharing between pods requires Redis or JDBC — additional dependency |

