[Architecture](../../ARCHITECTURE.md) > [Components](README.md) > Tour Service

# Tour Service

**Type**: Business Service (Tier 2 — Application/Business Logic Layer)

---

## Purpose

Manages the tour catalog — providing listing, filtering, sorting, pagination, and individual tour details to customers. Also exposes tour-level review aggregates.

---

## Classes

| Class | Role |
|-------|------|
| `TourServiceImpl` | Core tour business logic |
| `TourMapper` | Maps `Tour` DynamoDB entities to `TourListItemDTO`, `TourDetailsDTO`, etc. |
| `TourSearchValidator` | Validates search parameters (date ranges, guest quantities, filters) |
| `DateRangeParser` | Parses and validates date range strings for tour search |
| `TourDurationParser` | Parses duration bucket values |

---

## Responsibilities

- Return paginated, filtered list of available tours (`GET /tours/available`)
- Return full tour details including package options, pricing, availability (`GET /tours/{id}`)
- Return distinct destinations for filter UI (`GET /tours/destinations`)
- Return paginated reviews for a specific tour (`GET /tours/{id}/reviews`)
- Validate search/filter parameters before querying DynamoDB
- Map tour entities to response DTOs (`TourListResponseDTO`, `TourDetailsDTO`)

---

## Tour Entity Key Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| tourId | String (PK) | Unique tour identifier |
| title | String | Tour name |
| destination | String | Geographic destination |
| type | `TourType` | Enum: tour categories |
| duration | `DurationBucket` | Duration range |
| packageOptions | List\<TourPackageOption\> | Pricing tiers with meal plans |
| availableSlots | int | Remaining capacity |
| averageRating | double | Aggregate rating from reviews |

---

## Technologies
- **Framework**: Spring Boot 3, Spring MVC
- **Mapper**: `TourMapper` (manual mapping, Lombok `@Builder`)
- **Validation**: `TourSearchValidator`, `DateRangeParser`, `TourDurationParser`
- **Location**: `src/main/java/com/epam/edp/demo/service/tour/`, `mapper/`, `validation/tour/`, `entity/tourModel/`

---

## Dependencies
- **Depends on (Tier 3)**: [DynamoDB Repositories](09-dynamodb-repositories.md) — `TourRepositoryImpl`
- **Depended by (Tier 1)**: [TourController](01-rest-api-controllers.md)
- **Consumed by**: [BookingService](05-booking-service.md) (availability checks)

---

## Business Rules

- Tours with `availableSlots <= 0` are not returned in available listing
- Search filters: destination, type, duration bucket, date range, guest count, price range, meal plan
- Sorting options: price, rating, duration (`TourSortBy` enum)
- Reviews are sorted by `ReviewSortBy` (e.g., most recent, highest rating)

---

## Failure Modes

| Failure | Response |
|---------|---------|
| Tour not found | HTTP 404 (`TourNotFoundException`) |
| Invalid search parameters | HTTP 400 (`TourValidationException`) |
| DynamoDB unavailable | HTTP 500 (propagated) |

---

## Monitoring
- **Key metrics**: Tour list request rate, average response time, filter usage distribution
- **Alerts**: High latency on tour list scan (>500ms p95)
- **Logs**: Search parameters logged at DEBUG; errors at ERROR

