[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > Document Service

# Document Service

**Type**: Business Service (Tier 2 — Application/Business Logic Layer)

---

## Purpose

Manages booking document uploads using a secure presigned URL pattern. The service generates time-limited S3 presigned PUT URLs so clients can upload files directly to S3 without proxying through the API server.

---

## Classes

| Class | Role |
|-------|------|
| `DocumentServiceImpl` | Orchestrates document validation, URL generation, and metadata persistence |
| `S3PresignedUrlService` | Generates S3 presigned PUT/GET URLs using AWS SDK v2 with STS role assumption |

---

## Upload Flow

```
1. Client → POST /bookings/{id}/documents/upload-url
   │  DocumentService validates booking ownership
   │  S3PresignedUrlService generates S3 presigned PUT URL
   └─ HTTP 200 → {uploadUrl, documentId, key}

2. Client → PUT <uploadUrl> (direct to S3, not through this service)
   │  Binary file uploaded directly to S3 bucket: tm7-documents-prod
   └─ S3 returns 200

3. Client → POST /bookings/documents/complete
   │  DocumentService validates document metadata
   │  DocumentRepositoryImpl persists Document entity in DynamoDB
   └─ HTTP 200 → EditProfileResponse
```

---

## Document Entity Key Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| documentId | String (PK) | Unique document identifier |
| bookingId | String | Associated booking |
| userId | String | Uploading user |
| s3Key | String | S3 object key |
| fileName | String | Original file name |
| fileFormat | `FileFormat` | Enum: PDF, JPG, PNG, etc. |
| documentType | `DocumentType` | Enum: PASSPORT, VISA, etc. |
| status | `DocumentStatus` | Enum: PENDING, UPLOADED, VERIFIED |
| uploadedAt | Instant | Upload timestamp |

---

## S3 Integration Details

| Attribute | Value |
|-----------|-------|
| Bucket | `tm7-documents-prod` |
| Region | `ap-southeast-2` |
| URL Type | Presigned PUT (for upload), Presigned GET (for retrieval) |
| Auth | STS role assumption (`org/DeveloperAccessRoleTeam7`) |
| URL Expiry | [To be defined in S3PresignedUrlService configuration] |

**ADR**: [ADR-004](../../adr/ADR-004-s3-document-storage.md)

---

## Technologies
- **AWS SDK**: `software.amazon.awssdk:s3` v2.25.60
- **STS**: `software.amazon.awssdk:sts` v2.25.60
- **AWS Config**: `AwsConfig` — configures `S3Presigner` with STS credentials
- **Location**: `src/main/java/com/epam/edp/demo/service/document/`

---

## Dependencies
- **Depends on (Tier 3)**: [DynamoDB Repositories](09-dynamodb-repositories.md) — `DocumentRepositoryImpl`
- **Depends on (Tier 3)**: [S3 File Storage](10-s3-file-storage.md)
- **Depended by (Tier 1)**: [DocumentController](01-rest-api-controllers.md)

---

## Business Rules

- Presigned URL can only be generated for a booking owned by the requesting user
- Document type and file format are validated before URL generation (`DocumentValidationException`)
- Document metadata is not persisted until the client confirms upload completion (2-step flow)
- Multiple documents can be uploaded per booking (see `GenerateUploadUrlsRequestDTO` for batch support)

---

## Failure Modes

| Failure | Response |
|---------|---------|
| Booking not found / access denied | HTTP 403 |
| Invalid document type or format | HTTP 400 (`DocumentValidationException`) |
| S3 presigner unavailable | HTTP 500 |
| DynamoDB unavailable (metadata persist) | HTTP 500 |

---

## Monitoring
- **Key metrics**: Documents uploaded per hour, S3 presign error rate, upload confirmation rate
- **Alerts**: High S3 error rate, low confirmation rate (client abandons after getting URL)
- **Logs**: documentId, bookingId, s3Key logged at INFO on URL generation and confirmation

