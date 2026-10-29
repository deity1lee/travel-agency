# ADR-004: S3 Presigned URLs for Document Storage

**Status**: Accepted
**Date**: 2026-07-10
**Authors**: Architecture Team
**Related**: [ADR-001](ADR-001-dynamodb-as-primary-database.md)

---

## Context

Customers need to upload identity documents (passport copies, visa documents, travel insurance) as part of the booking process. Files can be large (up to several MB per document). The service needs a scalable, cost-effective upload mechanism.

Options:
1. Proxy file upload through the API server
2. Generate S3 presigned URLs and have clients upload directly to S3

## Decision

Use **AWS S3** (`tm7-documents-prod`) for document storage. Generate **S3 presigned PUT URLs** via `S3PresignedUrlService` (using `S3Presigner` from AWS SDK v2). Clients upload files directly to S3. After upload, clients confirm completion via a separate API call and the `DocumentService` persists document metadata in DynamoDB.

## Rationale

- **No API server bandwidth cost**: Files bypass the API server entirely — uploaded directly from browser to S3
- **Scalability**: S3 can handle any number of concurrent uploads without pod-level bottlenecks
- **Security**: Presigned URLs are scoped to a specific S3 key and expire after a defined time — no bucket-level open access
- **AWS-native**: S3 integrates with existing IAM/STS credential management
- **Simplicity**: AWS SDK v2 `S3Presigner` makes presigned URL generation straightforward
- **Cost**: API server pods don't need large memory/network provisioning for file handling

## Consequences

**Positive**:
- API pods are not memory/bandwidth-constrained by document sizes
- S3 durability: 99.999999999% (11 nines) — documents are reliably stored
- Presigned URL expiry provides time-bounded upload windows
- S3 access logs provide document upload audit trail

**Negative**:
- Two-step API flow (generate URL → upload → confirm) is more complex than a single multipart upload
- Server-side content inspection / virus scanning is not possible without S3 event triggers + Lambda
- S3 bucket policy and lifecycle rules must be maintained separately from application code
- File size validation must be enforced via presigned URL `Content-Length` conditions or client-side

## Alternatives Considered

| Alternative | Reason Rejected |
|-------------|----------------|
| Multipart upload proxied through API | Memory/bandwidth overhead; pod scaling required for file sizes |
| Database BLOB storage (DynamoDB) | DynamoDB item size limit (400KB) — incompatible with document files |
| On-disk storage in Kubernetes pod | Not persistent across pod restarts; Kubernetes ephemeral storage is not appropriate |

