# travel-agency-backend-reports-handler-app

### Notice
All technical details below are aligned with release `2.0.0`.

## Project goal
- The app integrates asynchronous processing for booking events, review event, and scheduled report emails.
- In release `2.0.0`, the reports handler was migrated from an AWS Lambda implementation to a standalone Spring Boot service.

## Solution overview
- The application runs as a container in a Kubernetes cluster hosted on AWS.
- It is deployed as a Spring Boot service and managed with Helm templates in `deploy-templates/`.
- Delivery is automated through KubeRocketCI pipelines and ArgoCD synchronization.

## Deployment
- Deployed as a containerized Spring Boot service on an AWS-hosted Kubernetes cluster.
- Kubernetes manifests are managed through Helm chart templates in `deploy-templates/`.
- The chart includes `deployment.yaml`, `service.yaml`, `ingress.yaml`, and `hpa.yaml`.
- Delivery is automated through KubeRocketCI pipelines and ArgoCD synchronization.

## Local development

### Prerequisites
- Java `17`
- Maven `3.9+`

### Required environment variables
- `AWS_REGION` - AWS region used for DynamoDB, SES, and other AWS integrations.
- `REPORTS_TABLE_NAME` - DynamoDB table name that stores report data.
- `S3_BUCKET_NAME` - bucket used for storing generated report files.
- `SES_FROM_EMAIL` - verified sender address used by Amazon SES.
- `SERVER_PORT` - local server port for the Spring Boot application.

### Run and test

```bash
mvn clean install
mvn test
mvn spring-boot:run
```

To generate JaCoCo coverage report:

```bash
mvn verify
```

## Main capabilities
- `GET /report` — retrieves aggregate reports in JSON format from the reports service for admin users.
- `GET /report/export?format={CSV|PDF}` — exports reports in the requested format for admin users.
- Data seeding is available for the `reports` DynamoDB table to simplify testing and development.
- The service keeps weekly reporting workflows available for storage and email delivery.

### Notes on data seeding
- `GET /seed?date=YYYY-MM-DD` — seeds test report data for the specified date and returns `204 No Content`; this endpoint is available without API key authentication.
  - Seeding for a specific date is intended for testing scheduled weekly reports for the week ending in that date. For example, seeding for `2026-06-21` will add data for the week ending on that date (i.e., from `2026-06-15` to `2026-06-21`).
  - The seed endpoint populates the report table with DAILY entries for the specified week (i.e., from `2026-06-15` to `2026-06-21`), and WEEKLY entries for the previous week (i.e., `2026-06-15` in the example above).
  - Since filtering endpoints use only DAILY entries to generate weekly reports, be mindful of the date range when seeding data for testing.

For release details, see [CHANGELOG.md](CHANGELOG.md).
