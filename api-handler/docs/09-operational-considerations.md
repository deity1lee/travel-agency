# Section 10 — Operational Considerations

[Architecture](../ARCHITECTURE.md)

---

## Deployment

### Container Build

- The service is packaged as a Docker image via `Dockerfile` in the project root
- Artifact: `java-maven-springboot-2.x.x.jar` produced by `mvn clean install`
- Java version: 17

### Kubernetes Deployment

- Managed via Helm chart in `deploy-templates/`
- Templates: `deployment.yaml`, `service.yaml`, `ingress.yaml`, `hpa.yaml`, `serviceaccount.yaml`
- CI/CD: KubeRocketCI pipelines trigger build + push + Helm release on merge to main
- GitOps sync: ArgoCD watches the Helm chart and applies changes to the AWS Kubernetes cluster

### Environment Promotion

| Environment | Configuration Source | Notes |
|-------------|----------------------|-------|
| Development | Local `.env` / IDE run config | `APP_CAPTCHA_ENABLE=false` recommended |
| Staging | Kubernetes Secrets + ConfigMap | Separate DynamoDB tables recommended |
| Production | Kubernetes Secrets + ConfigMap | `tm7-*-prod` tables |

---

## Environment Variables

All required environment variables (see [Security Architecture](07-security-architecture.md#credential-management) and [README](../README.md)):

| Variable | Purpose | Required |
|----------|---------|---------|
| `AWS_ACCESS_KEY_ID` | AWS credential for DynamoDB/S3/SQS | ✅ |
| `AWS_SECRET_ACCESS_KEY` | AWS credential | ✅ |
| `AWS_SESSION_TOKEN` | AWS temporary session token | ✅ |
| `REPORTS_HANDLER_URL` | Base URL of reports-handler service | ✅ |
| `SENDGRID_EMAIL` | Sender address for emails | ✅ |
| `SENDGRID_API_KEY` | SendGrid REST API key | ✅ |
| `APP_CAPTCHA_API_KEY` | Google reCAPTCHA secret key | ✅ |
| `APP_CAPTCHA_ENABLE` | Toggle CAPTCHA validation | ✅ |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | ✅ |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret | ✅ |
| `DIAL_API_KEY` | EPAM DIAL API key | ✅ |
| `ALLOWED_ORIGINS` | CORS allowed origin list | ✅ |

---

## Monitoring & Logging

### Application Logs

- Spring Boot default logging via SLF4J + Logback
- All controllers log incoming requests and responses at `INFO` level
- Exceptions logged at `ERROR` level with stack traces
- Log aggregation: [To be defined — integrate with cluster logging stack, e.g., ELK/EFK or CloudWatch]

### Health Checks

- Kubernetes liveness/readiness probes configured in `deployment.yaml`
- Spring Boot Actuator endpoints: [To be defined — add `spring-boot-starter-actuator` if not present]
- Recommended: `/actuator/health` for readiness, `/actuator/info` for version

### Metrics

- JaCoCo test coverage report: `target/site/jacoco/jacoco.xml` (CI artifact)
- Application metrics: [To be defined — consider Micrometer + Prometheus integration]
- Key business metrics to track:
  - Booking creation rate
  - Review submission + rejection rate
  - Authentication success/failure rate
  - Email delivery success rate

---

## CI Pipeline (KubeRocketCI)

| Stage | Action |
|-------|--------|
| Build | `mvn clean install` |
| Test | `mvn test` (JUnit 5 + Mockito) |
| Coverage Gate | `mvn verify` — JaCoCo report generated; coverage threshold: [To be defined] |
| Docker Build | Build image, tag with commit SHA |
| Push | Push to container registry |
| Helm Release | Update Helm chart values with new image tag |
| ArgoCD Sync | ArgoCD detects change, applies to Kubernetes cluster |

---

## Disaster Recovery

| Concern | Current Approach |
|---------|-----------------|
| DynamoDB backup | AWS Point-In-Time Recovery (PITR) — [confirm enabled per table] |
| S3 data | AWS S3 durability 99.999999999%; versioning: [To be defined] |
| SQS message durability | Standard queue — AWS-managed durability; messages retained for [default: 4 days] |
| Pod failure | Kubernetes restarts pods automatically (stateless — no data loss) |
| Region failure | Single region (`ap-southeast-2`) — no multi-region DR currently |

**RTO / RPO**: [To be defined based on business requirements]

---

## Runbook: Common Operations

### Local Development

```bash
# Install dependencies and run tests
mvn clean install

# Run tests only
mvn test

# Run with coverage
mvn verify

# Start locally (requires env vars set)
mvn spring-boot:run
```

### Access Swagger UI

- Local: `http://localhost:8080/swagger-ui.html`
- Kubernetes: `https://<ingress-host>/swagger-ui.html`

### Seed Development Data

```
POST /dev/seed   (public endpoint — development only)
GET  /dev/code   (retrieve OTP for dev user — development only)
```

### Deploy New Version

1. Merge to main branch
2. KubeRocketCI pipeline runs automatically
3. ArgoCD syncs Helm release to Kubernetes
4. Monitor pod rollout: `kubectl rollout status deployment/api-handler`

---

## Known Operational Issues

| # | Issue | Impact | Resolution |
|---|-------|--------|------------|
| 1 | Rate limiter is per-pod, not cluster-wide | Bypass possible under multi-pod deployment | Implement Redis-backed distributed rate limiter |
| 2 | JWT secret is a static string in `application.properties` | Secret rotation requires redeployment | Move to environment variable |
| 3 | reports-handler API key is hard-coded | Key rotation requires code change | Move to Kubernetes Secret + environment variable |
| 4 | No circuit breaker for DIAL / SendGrid / reCAPTCHA | Cascading failure risk | Add Resilience4j circuit breaker |

