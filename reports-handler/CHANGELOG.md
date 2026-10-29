# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.3] - 2026-07-17

### Changed
- reports are now stored in MongoDB instead of DynamoDB.

## [2.0.2] - 2026-07-16

### Changed
- seeding data endpoint now accepts startDate, endDate, tourCount, agentCount query parameters 
to seed data for a specific date range amount. Example usage:

```bash
/seed?startDate=2026-06-01&endDate=2026-07-16&tourCount=10&agentCount=10
```

## [2.0.1] - 2026-07-14

### Changed
- .csv file is now also attached to the email, in addition to the displayed table.

## [2.0.0] - 2026-07-06

### Added
- Unit test coverage is added to the `reports-handler` app, ensuring that the code is tested and reliable.
- Added 2 endpoints for retrieving filtered reports:
  - `GET /report` — retrieves aggregate reports (in json format) from the reports service (admin only).
  - `GET /report/export?format={CSV|PDF}` — exports reports in the requested format (admin only).
- Data seeding for the `reports` DynamoDB table is added to the `reports-handler` app, allowing for easy testing and development.


### Changed
- The `reports-handler` is now a Spring Boot application instead of an AWS Lambda function, which decreases costs of infrastructure significantly. 
It now runs as a standalone service in the Kubernetes cluster, deployed via Helm charts (`deploy-templates/`: `deployment.yaml`, `service.yaml`, `ingress.yaml`, `hpa.yaml`).


## [1.5.0] - 2026-06-22

### Added
- Added reviews data to weekly reports.
- Reports are now not only stored in S3 but also sent to travel agents via email using Amazon SES.


## [1.0.0] - 2026-06-12

### Added
- `reports-handler` Lambda triggered by SQS `booking-events` queue (batch size 10).
- Persists booking status change history to the `reports` DynamoDB table for audit and reporting purposes.