# Section 7 — Technology Stack

[Architecture](../ARCHITECTURE.md)

---

## Runtime Platform

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 17 (LTS) |
| Build Tool | Apache Maven | 3.9+ |
| Application Framework | Spring Boot | 3.0.3 |
| Web Layer | Spring MVC (embedded Tomcat) | 3.0.3 |
| Reactive Client | Spring WebFlux (`WebClient`) | 3.0.3 |
| Container Runtime | Docker | [CI-managed] |
| Orchestration | Kubernetes (AWS) | [cluster-managed] |

---

## Security

| Component | Technology | Version |
|-----------|------------|---------|
| Security Framework | Spring Security | 6.x (via Boot 3.0.3) |
| OAuth2 Client | Spring Security OAuth2 Client | 6.x |
| JWT Library | JJWT | 0.12.5 |
| Password Hashing | BCrypt (Spring Security) | built-in |
| CAPTCHA | Google reCAPTCHA v3 | REST API |

---

## AWS Cloud Services

| Service | SDK / Integration | Version |
|---------|-------------------|---------|
| DynamoDB | Spring Cloud AWS DynamoDB | 3.0.3 |
| S3 | AWS SDK v2 (`software.amazon.awssdk:s3`) | 2.25.60 |
| SQS | Spring Cloud AWS SQS | 3.0.3 |
| STS | AWS SDK v2 (`software.amazon.awssdk:sts`) | 2.25.60 |

---

## External API Integrations

| Service | Client Library | Version |
|---------|---------------|---------|
| EPAM DIAL (Azure OpenAI) | Azure OpenAI SDK for Java | `1.0.0-beta.16` |
| SendGrid (Email) | REST via Spring `RestTemplate` | HTTP REST |
| Google OAuth2 | Spring Security OAuth2 Client | 6.x |
| Google reCAPTCHA | REST via Spring `WebClient` | HTTP REST |
| reports-handler | Spring `RestTemplate` | HTTP REST |

---

## Data Validation & Mapping

| Component | Technology | Version |
|-----------|------------|---------|
| Bean Validation | Jakarta Validation (Hibernate Validator) | Boot-managed |
| Email Validation | Apache Commons Validator | 1.10.0 |
| DTO Mapping | Manual (`UserMapper`, `TourMapper`, `ReviewMapper`) | — |
| Boilerplate Reduction | Lombok | 1.18.30 |

---

## API Documentation

| Component | Technology | Version |
|-----------|------------|---------|
| OpenAPI Spec | SpringDoc OpenAPI Starter WebMVC UI | 2.3.0 |
| UI | Swagger UI (embedded) | via SpringDoc |

---

## Testing

| Component | Technology | Version |
|-----------|------------|---------|
| Unit / Integration Tests | JUnit 5 (Jupiter) | Boot-managed |
| Legacy Tests | JUnit 4 | 4.13.2 |
| Mocking | Mockito | Boot-managed |
| Mockito Final Classes | Mockito Inline | Boot-managed |
| Spring MVC Tests | Spring Security Test | Boot-managed |
| Test Suite | JUnit Platform Suite | Boot-managed |
| Coverage | JaCoCo | 0.8.14 |
| JVM Instrumentation | Byte Buddy | 1.14.19 |

---

## Deployment & CI/CD

| Component | Technology |
|-----------|------------|
| Helm Chart | Helm 3 (templates in `deploy-templates/`) |
| CI/CD Platform | KubeRocketCI |
| GitOps | ArgoCD |
| Container Registry | [To be defined — configured in pipeline] |

---

## Dependency Versions Summary (pom.xml)

| Artifact | Version |
|----------|---------|
| `spring-boot-starter-parent` | `3.0.3` |
| `spring-cloud-aws-dependencies` | `3.0.3` |
| `aws.sdk.version` (BOM) | `2.25.60` |
| `azure-ai-openai` | `1.0.0-beta.16` |
| `jjwt-api` | `0.12.5` |
| `springdoc-openapi-starter-webmvc-ui` | `2.3.0` |
| `lombok` | `1.18.30` |
| `commons-validator` | `1.10.0` |
| `junit` (4) | `4.13.2` |
| `jacoco-maven-plugin` | `0.8.14` |
| `byte-buddy.version` (override) | `1.14.19` |

