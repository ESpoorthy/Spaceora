# Spaceora — System Design

## Architecture Overview

```
Browser
  │
  ▼
React SPA (Vite)
  │  HTTP/HTTPS (Axios + JWT Bearer header)
  ▼
Spring Boot REST API  (port 8080)
  │
  ├─► Spring Security Filter Chain (JWT validation, role checks)
  │
  ├─► Controllers  (request/response mapping, input validation @Valid)
  ├─► Services     (business logic, transaction boundaries)
  ├─► Repositories (Spring Data JPA interfaces)
  └─► PostgreSQL   (port 5432, managed by Docker Compose locally)
```

---

## Backend Layers

| Layer | Responsibility |
|---|---|
| Controller | HTTP routing, request deserialization, response serialization, `@Valid` |
| Service | Business rules, transaction management, authorization checks |
| Repository | JPA queries, custom JPQL/native queries |
| Entity | JPA-mapped domain objects — no business logic lives here |
| DTO | Request and response shapes — entities are never serialised directly |
| Security Config | JWT filter, `SecurityFilterChain`, CORS configuration, role-based access |
| Exception Handler | `@ControllerAdvice` — safe, consistent error responses (no stack traces) |

### Package Layout (backend/src/main/java/…)

```
com.spaceora
├── auth/
│   ├── controller/   AuthController
│   ├── dto/          RegisterRequestDto, LoginRequestDto, AuthResponseDto
│   ├── service/      UserService, JwtService
│   └── filter/       JwtAuthenticationFilter
├── user/
│   ├── controller/   UserController
│   ├── dto/          UserProfileDto, UpdateProfileRequestDto
│   ├── entity/       User
│   └── repository/   UserRepository
├── space/
│   ├── controller/   SpaceController
│   ├── dto/          SpaceDto, SpaceRequestDto, SpaceSummaryDto
│   ├── entity/       Space, SpaceImage
│   └── repository/   SpaceRepository, SpaceImageRepository
├── availability/
│   ├── controller/   AvailabilityController
│   ├── dto/          AvailabilityResponseDto, BlockedPeriodDto, …
│   ├── entity/       AvailabilitySchedule, BlockedPeriod
│   ├── repository/   AvailabilityScheduleRepository, BlockedPeriodRepository
│   └── service/      AvailabilityService
├── reservation/
│   ├── controller/   ReservationController
│   ├── dto/          ReservationDto, ReservationRequestDto
│   ├── entity/       Reservation
│   ├── repository/   ReservationRepository
│   └── service/      ReservationService
├── admin/
│   ├── controller/   AdminController
│   └── dto/          RoleUpdateRequestDto
├── config/
│   │   SecurityConfig, CorsConfig, OpenApiConfig
│   └── JwtProperties
└── exception/
        GlobalExceptionHandler, BookingValidationException,
        InvalidStatusTransitionException, ResourceNotFoundException,
        BookingConflictException, AuthorizationException
```

---

## Entity Relationship Overview

```
User (1) ─────────────────────────────────────────── (N) Space
                                                            │
                          ┌─────────────────────────────────┼──────────────────────┐
                          │                                  │                      │
                     (N) SpaceImage           (N) AvailabilitySchedule    (N) BlockedPeriod

User (1) ─────── (N) Reservation ──────── (1) Space
```

### Entities Summary

| Entity | Table | Key Relationships |
|---|---|---|
| User | users | Owns many Spaces; places many Reservations |
| Space | spaces | Belongs to one User (owner); has many SpaceImages, AvailabilitySchedules, BlockedPeriods, Reservations |
| SpaceImage | space_images | Belongs to one Space |
| AvailabilitySchedule | availability_schedules | Belongs to one Space; one row per day-of-week |
| BlockedPeriod | blocked_periods | Belongs to one Space |
| Reservation | reservations | Links one Customer (User) to one Space |

---

## Security Design

### Authentication Flow

1. Client POSTs credentials to `POST /api/auth/login`.
2. `UserService` loads the user, verifies the BCrypt hash, throws 401 if invalid.
3. `JwtService` generates a signed HS256 JWT containing `userId`, `email` and `role`.
4. Token is returned to the client; the client stores it in memory or localStorage.
5. Every subsequent request includes `Authorization: Bearer <token>`.
6. `JwtAuthenticationFilter` (OncePerRequestFilter) validates the token, extracts claims and populates `SecurityContextHolder`.

### Authorization Rules

| Endpoint Group | Allowed Roles |
|---|---|
| `POST /api/auth/**` | Public |
| `GET /api/spaces`, `GET /api/spaces/{id}`, `GET /api/spaces/{id}/availability` | Public |
| `POST /api/spaces`, `PUT /api/spaces/{id}`, `DELETE /api/spaces/{id}` | OWNER (own spaces), ADMIN |
| `POST /api/reservations`, `GET /api/reservations/my` | CUSTOMER |
| `GET /api/reservations/{id}` | CUSTOMER (own), OWNER (their space), ADMIN |
| `DELETE /api/reservations/{id}` | CUSTOMER (own, not started) |
| `PUT /api/reservations/{id}/status` | OWNER (their space) |
| `GET /api/spaces/{id}/reservations` | OWNER (their space), ADMIN |
| `GET /api/admin/**`, `PUT /api/admin/**` | ADMIN |

### Security Invariants

- Registration stores BCrypt-hashed passwords; plaintext passwords never logged or returned.
- Login returns a signed JWT (HS256); secret and expiry configured via environment variables.
- `@PreAuthorize` annotations enforce role and ownership checks in the service layer.
- CORS configured to permit only the intended frontend origin (e.g. `http://localhost:5173` in dev).
- Server-side validation with `@Valid` + Jakarta Constraints on all request bodies.
- Client-supplied `role`, `ownerId` and `totalAmount` fields are always ignored / overridden on the server.

---

## Key Design Decisions

### 1. Pessimistic Locking for Concurrent Bookings

MVP uses `SELECT … FOR UPDATE` (`LockModeType.PESSIMISTIC_WRITE`) when checking space availability before creating a reservation.

**Rationale:** Simpler to reason about correctness under concurrent load than optimistic locking with retry loops. At MVP scale (single-node API, PostgreSQL) the serialization cost is acceptable.

### 2. Server-Calculated Booking Totals

`totalAmount` is never accepted from the client. The `ReservationService` computes it from `hourlyRate × duration` (hours) or `dailyRate` for full-day bookings.

**Rationale:** Prevents clients from manipulating prices.

### 3. Explicit Status Transitions

Reservation status follows a strict state machine:

```
PENDING ──► CONFIRMED
PENDING ──► REJECTED
CONFIRMED ──► CANCELLED   (by customer, before start time)
```

Any other transition returns HTTP 422 Unprocessable Entity.

### 4. Flyway Migrations Only

All DDL is in versioned Flyway migration scripts (`V1__…` through `V6__…`).  
`spring.jpa.hibernate.ddl-auto=validate` in all environments — Hibernate validates the schema but never modifies it.

**Rationale:** Reproducible, auditable schema history; safe to run on shared and production databases.

### 5. DTOs at the API Boundary

Entities are never serialised directly. Manual mappers or MapStruct convert between entity and DTO.

**Rationale:** Decouples the API contract from the internal data model; prevents accidental exposure of sensitive fields (e.g. `passwordHash`).

### 6. No Payment Processing in MVP

The `totalAmount` field is calculated and stored, but no payment gateway is integrated. A `CONFIRMED` status represents owner acceptance, not payment. A payment milestone is planned post-MVP.

---

## Frontend Architecture

- **Build tool:** Vite + React (JavaScript) — bootstrapped with `npm create vite@latest`.
- **Routing:** React Router v6 with protected route wrappers (redirect to `/login` if no token).
- **HTTP client:** Axios instance with:
  - Request interceptor — attaches `Authorization: Bearer <token>` from storage.
  - Response interceptor — handles 401 (clears token, redirects to login) and shows error toasts.
- **UI framework:** Bootstrap 5 — layout, components and utilities.
- **State management:** React Context API (AuthContext for auth state; no Redux for MVP).

### Page Map

| Route | Page | Auth Required | Roles |
|---|---|---|---|
| `/` | Home / Search | No | All |
| `/spaces/:id` | Space Detail | No | All |
| `/login` | Login | No | — |
| `/register` | Register | No | — |
| `/my-bookings` | My Bookings | Yes | CUSTOMER |
| `/owner/dashboard` | Owner Dashboard | Yes | OWNER |
| `/admin` | Admin Panel | Yes | ADMIN |
| `*` | 404 Not Found | No | — |

### UI / UX Reference

Official Figma design: [https://www.figma.com/design/jmILFgP2XqbqcjRCrXiA6W/Untitled](https://www.figma.com/design/jmILFgP2XqbqcjRCrXiA6W/Untitled)

All UI work must reference this design as the authoritative visual specification.
