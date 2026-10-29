# travel-agency-backend-app

Backend for a travel agency platform. The system supports customer authentication, tour discovery, booking management, document uploads, reviews, and automated weekly reporting.

### Notice
All technical details below are aligned with release `2.6.x`.

## Project goal
This project provides a production-oriented backend for travel booking workflows:
- Customers can sign up, sign in, explore tours, and manage bookings.
- Travel agency operations are supported through booking state tracking and reporting.
- The platform integrates asynchronous processing for booking events and scheduled summary emails.

## Solution overview
- The application runs as a container in a Kubernetes cluster hosted on AWS.
- Authentication and authorization are implemented using Spring Security and JWT tokens.
- Tours and reviews are persisted in MongoDB collections (`tours`, `reviews`); the remaining core entities are persisted in DynamoDB tables (`users`, `bookings`, `documents`, `travel_agents`, `reports`).
- Booking and review lifecycle events are published to SQS and processed by the `reports-handler` service in Kubernetes.
- Booking documents are uploaded through S3 presigned URLs.
- Application deployment is managed through KubeRocketCI and ArgoCD.

## Deployment
- Deployed as a containerized Spring Boot service on an AWS-hosted Kubernetes cluster.
- Kubernetes manifests are managed via Helm chart templates in `deploy-templates/`.
- Delivery is automated through KubeRocketCI pipelines and ArgoCD synchronization.

## Local development

### Prerequisites
- Java `17`
- Maven `3.9+`
- MongoDB `6+` (for the `tours` and `reviews` stores; a replica set is required for the transactional review write + tour rating-aggregate update)

### Required environment variables

| Variable | Purpose |
| --- | --- |
| `AWS_ACCESS_KEY_ID` | AWS access key for DynamoDB/S3/SQS access |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key |
| `AWS_SESSION_TOKEN` | AWS session token (when using temporary credentials) |
| `REPORTS_HANDLER_URL` | Base URL of the reports service |
| `SENDGRID_EMAIL` | Sender email for notification messages |
| `SENDGRID_API_KEY` | SendGrid API key |
| `APP_CAPTCHA_API_KEY` | Google reCAPTCHA secret key |
| `APP_CAPTCHA_ENABLE` | CAPTCHA toggle (`true`/`false`) |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret |
| `DIAL_API_KEY` | API key for moderation model access |
| `MONGODB_URI` | MongoDB connection string for the tours/reviews stores (default `mongodb://localhost:27017`) |
| `MONGODB_DB` | MongoDB database name (default `travel_agency`) |

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

### Authentication
- `POST /auth/sign-up` - start registration and send a verification code to email.
- `POST /auth/sign-up/verify` - verify sign-up with a code and complete account creation.
- `POST /auth/non/resend` - resend verification code for non-authenticated users.
- `POST /auth/resend` - resend verification code.
- `POST /auth/sign-in` - authenticate and obtain tokens.
- `POST /auth/logout` - revoke refresh token / terminate session.
- `PATCH /auth/me` - update profile details (first name, last name, profile photo).
- `POST /auth/me/email` - initiate email change with verification.
- `POST /auth/email/verify` - verify and confirm email change.
- `POST /auth/me/password` - change current user password.
- `POST /auth/forgot-password` - request a password recovery code.
- `POST /auth/forgot-password/verify` - verify password recovery code.
- `POST /auth/reset-password` - set a new password after successful recovery verification.
  Role-based access control (CUSTOMER, TRAVEL_AGENT, ADMIN).

### Tours
- `GET /tours/available` - list available tours with pagination, filters, and sorting.
- `GET /tours/{id}` - get full tour details.
- `GET /tours/destinations` - get supported destinations.
- `GET /tours/{id}/reviews` - get paginated tour reviews.

### Bookings and reviews
- `POST /bookings` - create booking with availability checks and pricing.
- `GET /bookings` - list current user bookings.
- `PUT|PATCH /bookings/{id}` - update booking.
- `DELETE /bookings/{id}` - cancel booking and trigger status event.
- `POST|PUT /bookings/{id}/review` - create or update review for eligible booking.

### Documents
- `POST /bookings/{id}/documents/upload-url` - generate presigned upload URL.
- `POST /bookings/documents/complete` - confirm upload and persist metadata.

### Reporting
- `GET /admin/report` - retrieve aggregate reports (admin only).
- `GET /admin/report/export?format={CSV|PDF}` - export reports in CSV or PDF format (admin only).
- Booking status/review events are stored for audit and reporting.
- Weekly summary report is generated and emailed to the manager.

For release details, see `CHANGELOG.md`.
