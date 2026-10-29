<!-- PO_SPEC_GATE: SKIPPED (user override at 2026-07-10) -->

# Section 1 — Executive Summary & Section 2 — System Overview

[Architecture](../ARCHITECTURE.md)

---

## 1. Executive Summary

### 1.1 System Name
**Travel Agency Backend — api-handler**

### 1.2 Version
`2.5.x` (as of 2026-07-10)

### 1.3 Problem Statement
Travel agencies require a reliable, secure backend platform to manage the full customer journey — from account registration and tour browsing through booking management, document submission, review submission with content moderation, and administrative reporting. Without a unified backend, these workflows are fragmented across manual processes, increasing operational overhead and customer friction.

### 1.4 Solution
A single Spring Boot REST API service deployed as a container in an AWS-hosted Kubernetes cluster. The service owns the complete business logic for authentication, tour catalog, bookings, documents, reviews, and reporting. Asynchronous event publishing to AWS SQS decouples the reporting pipeline.

### 1.5 Business Value
- **End-to-end booking workflow** — customers can register, discover tours, book, upload documents, and leave reviews through a single coherent API.
- **Automated weekly reporting** — booking and review lifecycle events flow to a dedicated `reports-handler` service via SQS, enabling automated aggregate reporting and manager email summaries.
- **AI-powered review moderation** — review content is screened by EPAM DIAL (GPT-4.1) before publication, reducing manual moderation overhead.
- **Role-based access** — CUSTOMER, TRAVEL_AGENT, and ADMIN roles enforce appropriate access boundaries without bespoke middleware.

### 1.6 Key Metrics

| Metric | Value |
|--------|-------|
| API Response Time (p95) | < 500ms |
| API Response Time (p99) | < 1000ms |
| System Availability SLA | 99.9% |
| Concurrent Users (target) | [To be defined] |
| DynamoDB Tables | 8 |
| REST Endpoints | ~30 |
| Test Coverage | [See JaCoCo report] |

---

## 2. System Overview

### 2.1 Context

The `api-handler` is part of a multi-service platform. It is the primary backend service consumed directly by web/mobile frontends. A separate `reports-handler` service (outside this repository) consumes SQS events produced by `api-handler`.

```
  [Frontend / Mobile Client]
            |  HTTPS/REST
            ▼
  ┌─────────────────────┐
  │   api-handler       │  ← this service
  │  (Spring Boot 3.x)  │
  └─────────────────────┘
       |           |
  DynamoDB        SQS
  S3              |
  SendGrid    reports-handler
  DIAL (AI)
  Google OAuth2
```

### 2.2 Deployment Context
- **Platform**: AWS Kubernetes (ap-southeast-2)
- **Delivery**: KubeRocketCI pipelines + ArgoCD GitOps
- **Helm chart**: `deploy-templates/`
- **Container**: Single Docker image per release

### 2.3 Use Cases

| ID | Actor | Use Case | Notes |
|----|-------|----------|-------|
| UC-001 | Customer | Register account with email verification | Two-step: sign-up → verify OTP |
| UC-002 | Customer | Sign in (email/password or Google OAuth2) | JWT tokens issued |
| UC-003 | Customer | Browse and search tours | Public endpoint, no auth required |
| UC-004 | Customer | Create and manage bookings | Availability checks, pricing |
| UC-005 | Customer | Upload booking documents | S3 presigned URLs |
| UC-006 | Customer | Submit and update tour reviews | AI moderation gate |
| UC-007 | Customer | Manage account profile and password | Email change with OTP verification |
| UC-008 | Travel Agent | View assigned bookings | Role-gated |
| UC-009 | Admin | View and export aggregate reports | CSV/PDF export |
| UC-010 | System | Publish booking/review events for reporting | Async SQS events |
| UC-011 | System | Weekly summary email to manager | Automated via reports-handler |

### 2.4 Stakeholders

| Role | Concern |
|------|---------|
| Customer (End User) | Smooth booking experience, reliable notifications |
| Travel Agent | Accurate booking state visibility |
| Admin / Manager | Reporting accuracy, system health |
| Development Team | Maintainability, test coverage, deployment velocity |
| Operations | Uptime, incident response, cost |

### 2.5 Out of Scope
- Frontend / mobile application (separate repository)
- `reports-handler` service internals (separate service)
- DynamoDB table provisioning (managed externally via IaC)
- DNS / TLS certificate management (managed by Kubernetes ingress / cert-manager)

