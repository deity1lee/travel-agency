[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > S3 File Storage

# S3 File Storage

**Type**: File Storage (Tier 3 — Data Layer)

---

## Purpose

Provides secure, scalable object storage for booking documents (passports, visa copies, travel insurance, etc.) using AWS S3 with presigned URL access so files are uploaded directly by the browser without proxying through the API server.

---

## Configuration

| Attribute | Value |
|-----------|-------|
| **Bucket Name** | `tm7-documents-prod` |
| **Region** | `ap-southeast-2` |
| **SDK** | `software.amazon.awssdk:s3` v2.25.60 |
| **STS SDK** | `software.amazon.awssdk:sts` v2.25.60 |
| **Auth** | STS role assumption (`org/DeveloperAccessRoleTeam7` on account `317356818777`) |
| **Access Pattern** | Presigned PUT URLs (upload) + Presigned GET URLs (retrieval) |

---

## Access Pattern: Presigned URLs

**Upload (PUT)**:
1. `DocumentService` calls `S3PresignedUrlService.generateUploadUrl(key, contentType)`
2. SDK uses `S3Presigner` (configured in `AwsConfig`) to generate a time-limited PUT URL
3. URL returned to client; client uploads directly to S3
4. Client confirms upload via `/bookings/documents/complete`

**Retrieval (GET)**:
1. `S3PresignedUrlService.generateDownloadUrl(key)` generates a time-limited GET URL
2. URL returned to client for direct download from S3

**Benefits**:
- Eliminates file data flowing through the API server (bandwidth and latency)
- S3 enforces content-type, size limits via presigned URL parameters
- Files are never stored on the application server

---

## Technologies
- **Spring Config**: `AwsConfig` — builds `S3Presigner` bean using STS credentials
- **Service Class**: `S3PresignedUrlService` — encapsulates presign logic
- **Location**: `src/main/java/com/epam/edp/demo/service/document/S3PresignedUrlService.java`, `config/AwsConfig.java`

---

## Dependencies
- **Depends on (External)**: AWS S3 service (`ap-southeast-2`), AWS STS (credential assumption)
- **Depended by (Tier 2)**: [DocumentService](07-document-service.md)

---

## Security

- Access is scoped to a specific S3 key per presigned URL — no bucket-level open access
- STS role assumption limits credentials to the specific IAM role permissions
- Presigned URLs have a time-based expiry — expired URLs are rejected by S3
- S3 bucket access policy: [To be defined — should restrict public access, enforce encryption at rest]

**ADR**: [ADR-004](../../adr/ADR-004-s3-document-storage.md)

---

## Scaling
- **Horizontal**: S3 is AWS-managed; scales to unlimited object count and storage
- **Throughput**: S3 supports thousands of requests per second per prefix
- **Storage cost**: [To be defined — no lifecycle policy currently configured]

---

## Backup & Recovery

| Aspect | Approach |
|--------|---------|
| Durability | S3 Standard: 99.999999999% (11 nines) |
| Versioning | [To be defined — enable for document audit trail] |
| Replication | [To be defined — cross-region replication not configured] |
| Lifecycle policy | [To be defined — transition to S3-IA or Glacier after N days] |

---

## Failure Modes

| Failure | Response |
|---------|---------|
| S3 presigner error (STS failure) | HTTP 500 — `S3PresignedUrlService` throws exception |
| S3 unavailable during upload | Client receives S3 error directly (not proxied) |
| STS credential expiry | SDK auto-refreshes; failure triggers HTTP 500 |

---

## Monitoring
- **Key metrics**: Presigned URL generation rate, S3 PUT success/failure rate (from CloudWatch S3 metrics)
- **Alerts**: STS credential refresh failures, S3 error rate spike
- **Logs**: S3 key, documentId, presign expiry logged at INFO

