# ADR-001: DynamoDB as Primary Data Store

**Status**: Accepted
**Date**: 2026-07-10
**Authors**: Architecture Team
**Related**: [ADR-002](ADR-002-jwt-stateless-authentication.md), [ADR-003](ADR-003-sqs-event-driven-reporting.md)

---

## Context

The `api-handler` service requires persistent storage for users, tours, bookings, reviews, documents, travel agents, JWT blacklists, and verification tokens. The service is deployed on AWS in `ap-southeast-2` and the team has existing AWS infrastructure and expertise.

Key requirements:
- Schemaless or flexible schema (domain entities evolve)
- Simple, low-operational-overhead managed service on AWS
- Support for TTL-based automatic expiry (JWT blacklist, OTP tokens)
- Sufficient throughput for the expected booking/review workload
- No need for complex multi-table JOINs (domain entities are largely independent)

## Decision

Use **AWS DynamoDB** (via Spring Cloud AWS DynamoDB Enhanced Client 3.0.3) as the sole persistent data store, with one table per entity type.

## Rationale

- **Fully managed**: No DBA overhead for patching, backups, or failover — AWS handles availability and replication
- **TTL support**: Native DynamoDB TTL supports automatic expiry for JWT blacklist and OTP token tables
- **AWS-native**: Integrates cleanly with the existing STS credential management and IAM roles
- **Flexible schema**: `@DynamoDbBean` annotated entities can evolve without DDL migrations
- **Spring Cloud AWS**: First-class Spring Boot integration via `spring-cloud-aws-starter-dynamodb 3.0.3`
- **Cost**: On-demand pricing scales with actual usage — no idle capacity costs

## Consequences

**Positive**:
- Zero-ops database — no server management
- TTL-based cleanup eliminates manual token/blacklist pruning
- On-demand capacity mode avoids over-provisioning

**Negative**:
- No ACID transactions across multiple tables (DynamoDB transactions have cost and complexity)
- Query patterns must be designed upfront with GSIs — ad-hoc queries require table scans
- No SQL — developers must learn DynamoDB access patterns and Enhanced Client API
- Limited query expressiveness compared to relational databases

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|----------------|
| PostgreSQL (RDS) | More operational overhead; relational model not required for this domain |
| MongoDB Atlas | Not AWS-native; additional integration complexity |
| MySQL (RDS) | Relational overkill; schema migrations add friction to entity evolution |

