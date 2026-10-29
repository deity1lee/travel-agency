# Changelog
All notable changes to this project will be documented in this file.
 
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.6.6] - 2026-07-15

### Added
- added functionality for role `ADMIN` to be automatically assigned to a user when they sign-up if their email is present in the staff table with role `ADMIN`

### Changed
- repurposed `travel_agents` table to a more general `staff` table to allow for admin related functionality.
  - names of packages, repo classes and test classes have been renamed `staff` and `StaffRepository` etc. to reflect this change.
- changed `TourReviewValidator` class to have static methods, and removed `@Component` annotation from it.
- changed tour/booking dates in seeding data json.

### Fixed
- Booking seeding: bookings were not being seeded correctly due to insufficient permissions to confirm bookings.

## [2.6.5] - 2026-07-14

### Changed
- Migrated the remaining persistence layer off DynamoDB to MongoDB.
  - Migrated the `User` and JWT blacklist auth repositories to MongoDB.
  - Migrated the verification token, booking, review, tour and travel-agent repositories to MongoDB.
  - Reworked auth token entities (`User`, `JwtBlacklist`, `VerificationToken` and subtypes) for MongoDB document mapping.

### Removed
- Removed DynamoDB support: dropped `DynamoDbUtils`, trimmed AWS DynamoDB wiring from `AwsConfig`, and removed related `application.properties` settings.

### Fixed
- Hardened data seeding: persist verified users directly and make booking seeding more robust.

## [2.6.4] - 2026-07-14

### Added
- Added config to start a dockker/podman SonarQube container

### Fixed
- Fixed sonar critical issues.

## [2.6.3] - 2026-07-14

### Added
- Added option for disabling email sending in the application configuration through environment variable

## [2.6.2] - 2026-07-14

### Changed
- Changed internal validation implementation to use Jakarta Bean Validation.
  - `BookingRequest`
    - Changed mealplan field in `BookingRequest` to enum type `MealPlan` instead of string.
    - Validating duration field using jakarta @Pattern annotation
  - `CompleteUploadCheckRequest`
    - changed document names to be given as a list of strings instead of a raw json string list.
  - `GenerateUploadUrlRequestDTO`, `GenerateUploadUrlsRequestDTO`
    - First/last name is required for PASSPORT documents only    
    - moved validations to dto classes
  - `CreateReviewRequestDTO`, `UpdateReviewRequestDTO`
    - moved validations to dto classes
  - `TourReviewQueryDTO`
    - default page, pageSize, sortBy is now set in dto level.
  - `Guests`
    - Moved guest validation counts, format to here
- Error message structures sent to the client have been updated.
  - Other affected classes:
    - `BookingController` - confirming booking
    - `Reviewcontroller` - creating reviews
- Changed status code for `/forgot-password/verify`. Returns 400 instead of 404 when the token is invalid, expired or if it doesn't exist.

## [2.6.1] - 2026-07-13

### Changed

#### Bookings, documents and TAs persistence migrated from DynamoDB to MongoDB
- `SeedService` now seeds tours and reviews into MongoDB (via `MongoTemplate`) instead of DynamoDB batch writes.

## [2.6.0] - 2026-07-10

### Changed

#### Tour and review persistence migrated from DynamoDB to MongoDB
- Tours and reviews are now stored in MongoDB collections (`tours`, `reviews`) via Spring Data MongoDB (`spring-boot-starter-data-mongodb`); the remaining entities (users, bookings, documents, travel_agents, reports) continue to use DynamoDB.
- MongoDB connection is configured through `spring.data.mongodb.uri` / `spring.data.mongodb.database` (env `MONGODB_URI`, `MONGODB_DB`), with automatic index creation enabled.
- `SeedService` now seeds tours and reviews into MongoDB (via `MongoTemplate`) instead of DynamoDB batch writes.

### Added
- `MongoConfig` provides a `MongoTransactionManager` and `TransactionTemplate` so a review write and its tour rating-aggregate update commit atomically. Multi-document transactions require the MongoDB deployment to be a replica set (or sharded cluster).
- Compound indexes on the `reviews` collection — `{tourId, createdAt}` and `{tourId, rate}` — backing the tour-review listing queries.
- Optimistic-locking on review rate updates (`OptimisticLockException`) and duplicate-review protection on insert (`DuplicateReviewException`).
- Deterministic, stable review pagination: `GET /tours/{tourId}/reviews` now applies a secondary sort on `_id` so reviews that tie on `rate` or `createdAt` keep a consistent order across pages (no duplicated or skipped reviews when paging).

## [2.5.4] - 2026-07-10

### Added
- added webp and jpeg as valid types for user profile picture upload

### Changed
- changed sign-in response dto field from pictureUrl to imageUrl to comply with frontend naming.

## [2.5.3] - 2026-07-10

### Fixed
- Fixed a bug where calling authenticated endpoints without a token resulted in redirection to google oauth login.

## [2.5.2] - 2026-07-10

### Added
- used [Thinking-Beast-Mode](.github/agents/Thinking-Beast-Mode.agent.md) agent to generate architecture documentation for the project. 
  - 71.8K tokens were used and it took 15 mins to generate the main [Architecture](ARCHITECTURE.md) document and documents in adr/ and docs/ directories.

## [2.5.1] - 2026-07-09

### Changed
- Moved allowed origins to environment variable to allow for easier configuration of the application.


## [2.5.0] - 2026-07-06

### Added

#### Reports migration to Kubernetes
- The `reports-handler` is no longer an AWS Lambda consuming SQS directly; it now runs as a standalone service in the Kubernetes cluster, deployed via Helm charts (`deploy-templates/`: `deployment.yaml`, `service.yaml`, `ingress.yaml`, `hpa.yaml`).
- The api-handler publishes `BookingStatusChangedEvent` messages to SQS (`EventPublisherService` using `SqsTemplate` from Spring Cloud AWS) for the reports service to consume.
- `GET /admin/report` — retrieves aggregate reports from the reports service (admin only).
- `GET /admin/report/export?format={CSV|PDF}` — exports reports in the requested format (admin only).
- Report endpoints are protected with `@PreAuthorize("hasRole('ADMIN')")` and call the reports service over HTTP via `Service2Client` (WebClient with `X-API-KEY` header).

#### Automated feedback moderation by AI
- Reviews submitted via `POST /bookings/{bookingId}/review` and `PUT /bookings/{bookingId}/review` are automatically moderated before publishing.
- `ModerationServiceImpl` sends review content to an LLM (`gpt-4.1-mini`) deployed on EPAM DIAL using the Azure OpenAI SDK (`com.azure:azure-ai-openai`), authenticated with a securely stored API key (`DIAL_API_KEY`).
- Moderation returns one of `APPROVED`, `NEEDS_EDIT`, or `REJECTED` (for hate speech, harassment, threats, sexual, self-harm, illegal, or private-information content); errors are handled gracefully with a timeout and retries so the flow fails safely.

#### Update profile information
- `PATCH /auth/me` — updates first name, last name, and profile photo.
- `POST /auth/me/email` — initiates an email change by sending a verification code to the new address.
- `POST /auth/email/verify` — confirms the email change with the verification code.
- `POST /auth/me/password` — changes the user's password.

#### User password recovery
- `POST /auth/forgot-password` — sends a secure verification code to the user's registered email.
- `POST /auth/forgot-password/verify` — validates the recovery code.
- `POST /auth/reset-password` — sets a new password once the code has been verified. Recovery codes expire after a predefined period for security.

#### CAPTCHA during registration
- Google reCAPTCHA v3 validation is enforced during `POST /auth/sign-up` via `CaptchaServiceImpl`, which verifies tokens against `https://www.google.com/recaptcha/api/siteverify`.
- Configurable via `app.captcha.enabled`, `app.captcha.secret-key`, and `app.captcha.min-score` (default `0.5`).

#### Social media login
- OAuth2 login support (Google) added via `spring-boot-starter-oauth2-client` with `profile,email` scope.
- `OAuth2UserServiceImpl` associates or creates a user by matching email, and `OAuth2LoginSuccessHandler` redirects back to the frontend with a JWT, role, username, and email. `OAuth2LoginFailureHandler` returns a clear error on failure.

#### Secure user profile registration with email verification
- `POST /auth/sign-up` — validates the request, stashes registration data, and sends a 6-digit verification code to the user's email instead of creating the account immediately.
- `POST /auth/sign-up/verify` — verifies the code and creates the account.
- `POST /auth/non/resend` and `POST /auth/resend` — resend the verification code.
- `VerificationTokenService` generates 6-digit OTPs that expire after 15 minutes and are invalidated after 5 failed attempts; codes are hashed (bcrypt) before storage in the tokens table.

#### Email delivery
- SendGrid integration for sending verification codes, email-change confirmations, and password-recovery codes.

### Changed
- Registration is now a two-step, verification-code-based flow: the account is only created after the emailed code is confirmed.
- Reviews now pass through AI moderation before being persisted/published.

## [2.0.0] - 2026-06-22

### Added

#### Migration
- The CodeMie AI tool is used to generate the backend api-handler project.
- The Syndicate CLI tool is used as a build and deploy tool for both the legacy components.
- KubeRocketCI is introduced as an additional build and deploy tool, specifically for Kubernetes deployments.
- A k8s (Kubernetes) cluster is set up on AWS using instances.
- Namespace, deployment, and pod resources are created for the team and the api-handler application.
- ArgoCD deploys the application to the Kubernetes cluster.
- The api-handler is now a containerized "backend api-handler application" running in a Kubernetes pod.
- Changes to the component's repository can trigger build and deploy pipelines automatically.
- Unit test coverage is added to the api-handler project, ensuring that the code is tested and reliable.

#### Added new features
- Added reviews data to weekly reports.
- Reports are now not only stored in S3 but also sent to travel agents via email using Amazon SES.

### Changed
- The api-handler is now a Spring Boot application instead of an AWS Lambda function, which decreases costs of infrastructure significantly.
- The api-handler is now built and deployed using KubeRocketCI for Kubernetes deployments, in addition to the existing Syndicate CLI for AWS services deployment.
- Spring annotations are used instead of openapi.json for Swagger documentation.
- Spring Security (JWT based) is used for authentication and authorization instead of AWS Cognito.
- Integrated Spring Cloud AWS for simplified AWS service configuration and credential management.

### Fixed
- CORS error was fixed by editing S3 bucket for documents policy to allow cross-origin requests from the frontend domain.

## [1.0.0] - 2026-06-12

### Added

#### Authentication
- `POST /auth/sign-up` — registers a new user in Amazon Cognito, assigns the default customer role, and persists the user profile in DynamoDB.
- `POST /auth/sign-in` — authenticates an existing user via Cognito and returns tokens.
- `POST /auth/logout` — invalidates the user session by revoking the Cognito refresh token.
- Role-based access control with `CUSTOMER` and `TRAVEL_AGENT` roles.
- Input validation service for all auth requests.

#### Tours
- `GET /tours/available` — returns a paginated and filterable list of available tours, supporting filters by destination, date range, duration bucket, meal plan, tour type, guest quantity, and sort options.
- `GET /tours/{id}` — returns full details for a single tour.
- `GET /tours/destinations` — returns the list of available destinations.
- `GET /tours/{id}/reviews` — returns paginated reviews for a tour, with sort support.

#### Bookings
- `POST /bookings` — creates a new booking, validates available spots, calculates pricing, and assigns a travel agent.
- `GET /bookings` — retrieves the current user's bookings list with tour and travel agent details.
- `DELETE /bookings/{id}` — cancels an existing booking, restores tour availability, and publishes a status-changed event to SQS.
- `PUT /bookings/{id}` — fully updates a booking (package options, personal details, guests).
- `PATCH /bookings/{id}` — partially updates a booking.
- `POST /bookings/{id}/reviews` — creates a review for a completed booking (authenticated customers only).
- `PUT /bookings/{id}/reviews` — updates an existing review.
- Booking state machine with validated transitions between statuses.
- Booking availability and ownership access control.

#### Documents
- `POST /bookings/{id}/documents/upload-url` — generates an S3 presigned URL for uploading booking documents.
- `POST /bookings/documents/complete` — marks a document upload as complete and persists document metadata in DynamoDB.
- Document status tracking (`PENDING`, `UPLOADED`, etc.) and type classification.

#### Reports
- `reports-handler` Lambda triggered by SQS booking events queue (batch size 10).
- Persists booking status change history to the reports DynamoDB table for audit and reporting purposes.

#### Infrastructure
- AWS Lambda function `travel-agency-api-handler` serving all API routes via Amazon API Gateway.
- AWS Lambda function `reports-handler` consuming the `booking-events` SQS queue.
- DynamoDB tables: `users`, `tours`, `reviews`, `travel_agents`, `tour_bookings`, `documents`, `reports`.
- Amazon Cognito user pool for authentication and token management.
- Amazon S3 bucket for booking document storage with presigned URL access.
- SQS queue for asynchronous booking status event processing.
- AWS Syndicate-based deployment configuration.
- Swagger UI for interactive API documentation (`/travel_agency_api_ui`).