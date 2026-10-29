[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > Booking Service

# Booking Service

**Type**: Business Service (Tier 2 — Application/Business Logic Layer)

---

## Purpose

Manages the full booking lifecycle for customers: creation with availability and pricing validation, status tracking, updates, cancellations, and guest management. Also dispatches SQS events for reporting.

---

## Classes

| Class | Role |
|-------|------|
| `BookingServiceImpl` | Core booking business logic |
| `TourBookingValidation` | Validates booking creation parameters (date, guests, package options) |
| `BookingCalculationsUtils` | Pricing calculations, guest count utilities |
| `BookingCreationValidationResponseData` | Holds validation result for booking creation checks |

---

## Responsibilities

- **Create booking**: Validate tour availability, validate request data, calculate pricing, persist `Booking` in DynamoDB, publish `BookingStatusChangedEvent` to SQS
- **List bookings**: Return all bookings for the authenticated user (with booking details and linked tour snapshot)
- **Get booking detail**: Return full booking including guest details, pricing, personal information, associated review
- **Update booking**: Modify booking details (dates, guests, package) — validate new availability
- **Cancel booking**: Transition booking to `CANCELLED` state, publish `BookingStatusChangedEvent` to SQS
- **State management**: Enforce valid booking status transitions (`BookingStateTransitionException` if invalid)
- **Access control**: Users can only access their own bookings (travel agents/admins can view assigned bookings)

---

## Booking Status State Machine

```
PENDING → CONFIRMED → COMPLETED
    │
    └──► CANCELLED  (from PENDING or CONFIRMED)
```

| Status | Description |
|--------|-------------|
| `PENDING` | Booking created, awaiting confirmation |
| `CONFIRMED` | Booking confirmed by travel agent |
| `COMPLETED` | Travel completed |
| `CANCELLED` | Booking cancelled by customer or agent |

---

## Booking Entity Key Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| bookingId | String (PK) | Unique booking identifier |
| userId | String | Owning customer |
| tourId | String | Tour reference |
| status | `BookingStatus` | Current state |
| guests | `Guests` | Adult/child counts |
| personalDetails | `PersonalDetails` | Contact information |
| travelAgentSnapshot | `TravelAgentSnapshot` | Assigned agent at booking time |
| packageOption | `TourPackageOption` | Selected pricing package |
| totalPrice | BigDecimal | Calculated total |

---

## Technologies
- **Framework**: Spring Boot 3
- **Validation**: `TourBookingValidation`, Jakarta Validation
- **Pricing**: `BookingCalculationsUtils`
- **Events**: Delegates to [EventPublisherService](08-report-event-service.md)
- **Location**: `src/main/java/com/epam/edp/demo/service/booking/`, `validation/booking/`, `entity/booking/`

---

## Dependencies
- **Depends on (Tier 3)**: [DynamoDB Repositories](09-dynamodb-repositories.md) — `BookingRepositoryImpl`, `TourRepositoryImpl`, `TravelAgentRepositoryImpl`
- **Depends on (Tier 2)**: [EventPublisherService](08-report-event-service.md)
- **Depended by (Tier 1)**: [BookingController](01-rest-api-controllers.md)
- **Depended by (Tier 2)**: [ReviewService](06-review-service.md) (eligibility check)

---

## Business Rules

- A booking can only be created if `tour.availableSlots >= requestedGuests`
- Tour available slots are decremented on booking creation and restored on cancellation
- Pricing is calculated based on `TourPackageOption` and guest counts (`BookingCalculationsUtils`)
- A travel agent is auto-assigned from `TravelAgentRepository` if none specified
- Only booking owner (or ADMIN/TRAVEL_AGENT) can view/modify a booking (`BookingAccessDeniedException`)
- Status transitions must follow the defined state machine (`BookingStateTransitionException`)

---

## Failure Modes

| Failure | Response |
|---------|---------|
| Tour unavailable (no slots) | HTTP 409 (`NoAvailableSpotsException`) |
| No travel agent available | HTTP 409 (`NoAvailableTravelAgentException`) |
| Booking not found | HTTP 404 (`BookingNotFoundException`) |
| Access denied (wrong user) | HTTP 403 (`BookingAccessDeniedException`) |
| Invalid state transition | HTTP 409 (`BookingStateTransitionException`) |
| Invalid booking data | HTTP 400 (`BookingValidationException`) |
| SQS publish failure | Logged as error; does not fail the booking operation (see [P4](../02-architecture-principles.md#p4--asynchronous-event-publishing-for-side-effects)) |

---

## Monitoring
- **Key metrics**: Bookings created per hour, cancellation rate, available slots utilisation
- **Alerts**: Spike in cancellations, tour slot exhaustion
- **Logs**: BookingId, tourId, userId logged at INFO on create/update/cancel

