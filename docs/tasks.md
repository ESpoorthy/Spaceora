# Spaceora — Implementation Checklist

## Milestone Overview

| Milestone | Name | Complexity | Depends On |
|---|---|---|---|
| M0 | Repo & Tooling Setup | S | — |
| M1 | Authentication | M | M0 |
| M2 | Space Management | M | M1 |
| M3 | Availability & Blocked Periods | M | M2 |
| M4 | Reservations | L | M3 |
| M5 | Search & Filter | S | M2 |
| M6 | Owner Dashboard APIs | S | M4 |
| M7 | Admin APIs | S | M1 |
| M8 | Frontend Scaffold | M | M1 |
| M9 | Frontend Core Pages | L | M8, M4 |
| M10 | Polish, Docs, Testing | M | M9 |

---

## M0 — Repo & Tooling Setup

**Acceptance criteria:** Maven project compiles; Flyway connects to PostgreSQL; seed data loads; Docker Compose starts the DB.

- [ ] Initialise Maven project with Spring Boot 3 parent POM inside `backend/`
- [ ] Add dependencies: Spring Web, Spring Security, Spring Data JPA, PostgreSQL driver, Flyway, springdoc-openapi, Lombok, MapStruct, JUnit 5, Mockito, Testcontainers
- [ ] Configure `application.properties` / `application.yml` with env-var-driven DB URL, JWT secret and JWT expiry
- [ ] Create `.env.example` at repo root with placeholder values for all secrets
- [ ] Create `docker-compose.yml` at repo root for local PostgreSQL (port 5432, named database `spaceora_dev`)
- [ ] Write Flyway migrations V1–V6 (schema + indexes + seed data) in `backend/src/main/resources/db/migration/`
- [ ] Verify: `mvn clean package -DskipTests` passes; Flyway migrations run cleanly on Docker Compose PostgreSQL

---

## M1 — Authentication

**Acceptance criteria:** Register and login return JWTs; protected endpoints reject requests without a valid token; roles are enforced.

- [ ] `User` entity + `UserRepository`
- [ ] `RegisterRequestDto`, `LoginRequestDto`, `AuthResponseDto`
- [ ] `UserService`: register (BCrypt hash, reject duplicate email), `loadUserByUsername` for Spring Security
- [ ] `JwtService`: generate token, validate token, extract claims
- [ ] `JwtAuthenticationFilter` (`OncePerRequestFilter`) — validates Bearer token, populates `SecurityContextHolder`
- [ ] `SecurityFilterChain` config: permit `/api/auth/**` publicly; require authentication elsewhere; configure CORS to `http://localhost:5173` in dev
- [ ] `AuthController`: `POST /api/auth/register`, `POST /api/auth/login`
- [ ] `UserController`: `GET /api/users/me`, `PUT /api/users/me`
- [ ] `GlobalExceptionHandler` (`@ControllerAdvice`) for safe, consistent error responses
- [ ] Unit tests: duplicate email → 409, wrong password → 401
- [ ] Integration tests: register → login → access `GET /api/users/me`

---

## M2 — Space Management

**Acceptance criteria:** Owners can CRUD their spaces; non-owners cannot mutate; CUSTOMERs can only read active spaces.

- [ ] `Space` entity, `SpaceImage` entity + repositories
- [ ] `SpaceDto`, `SpaceRequestDto`, `SpaceSummaryDto`
- [ ] `SpaceService`: create (OWNER only, set ownerId from JWT), update (ownership check), delete (owner or ADMIN), toggle active
- [ ] `SpaceController`: CRUD endpoints per `api-contract.md`
- [ ] `@PreAuthorize` ownership checks in service layer (not only controller)
- [ ] Unit tests: create as CUSTOMER → 403; update as non-owner OWNER → 403
- [ ] Integration tests: `POST /api/spaces` as OWNER → 201; `GET /api/spaces/{id}` → 200

---

## M3 — Availability & Blocked Periods

**Acceptance criteria:** Weekly schedules can be set; blocked periods can be added/removed; the availability endpoint returns a merged view.

- [ ] `AvailabilitySchedule` entity + repository
- [ ] `BlockedPeriod` entity + repository
- [ ] `AvailabilityService`:
  - Set/replace weekly schedule for a space
  - Add blocked period (overlap check against existing blocked periods)
  - Remove blocked period
  - Compute merged availability for a date range
- [ ] `AvailabilityController`: endpoints per `api-contract.md`
- [ ] Unit tests: adding overlapping blocked period → 409
- [ ] Integration tests: `GET /api/spaces/{id}/availability` returns correct slots and blocked periods

---

## M4 — Reservations

**Acceptance criteria:** Booking creation validates all rules; conflicts return 409; server calculates total; status transitions are enforced; pessimistic lock prevents double-booking.

- [ ] `Reservation` entity + `ReservationRepository`
- [ ] `ReservationDto`, `ReservationRequestDto`
- [ ] `ReservationService`:
  - Past-date check
  - Day-has-schedule check (space closed → 422)
  - Outside opening-hours check (→ 422)
  - Blocked-period overlap check (→ 422)
  - Pending/confirmed reservation overlap check with pessimistic lock (`SELECT … FOR UPDATE`) → 409
  - Server-side `totalAmount` calculation (`hourlyRate × durationInHours` or `dailyRate`)
  - Status transition validation (state machine enforcement → 422 on invalid)
  - Ownership check for cancel (customer must own reservation)
- [ ] `ReservationController`: `POST`, `GET /{id}`, `DELETE /{id}` (cancel), `GET /my`, `GET` via `/spaces/{id}/reservations`, `PUT /{id}/status`
- [ ] Unit tests: all `ReservationService` scenarios from `testing-plan.md` (100% branch coverage target)
- [ ] Integration tests: overlap → 409; cancel own → 200 with `CANCELLED`; cancel other's → 403
- [ ] Concurrency test: two simultaneous requests for same slot → exactly one 201, one 409

---

## M5 — Search & Filter

**Acceptance criteria:** `GET /api/spaces` filters by all supported params and returns paginated results.

- [ ] Add JPA `Specification` (or dynamic JPQL) to `SpaceRepository` for multi-param filtering
- [ ] `SpaceSearchService` or extend `SpaceService` with `search(SpaceSearchCriteria, Pageable)` method
- [ ] Validate filter param types at controller level (`@RequestParam` with defaults)
- [ ] Integration tests: filter by city, type, minCapacity, maxPrice; verify only active spaces returned

---

## M6 — Owner Dashboard APIs

**Acceptance criteria:** An owner can view and action reservations for their spaces.

- [ ] `GET /api/spaces/{id}/reservations` — owner-only, paginated, optional status filter
- [ ] `PUT /api/reservations/{id}/status` — OWNER confirms or rejects a PENDING reservation on their space
- [ ] Ownership check: owner must own the space the reservation belongs to
- [ ] Unit tests: OWNER confirms reservation on own space → 200; on different owner's space → 403
- [ ] Integration tests: OWNER actions PENDING reservation → status updated

---

## M7 — Admin APIs

**Acceptance criteria:** ADMIN can list all users, change roles, and activate/deactivate spaces.

- [ ] `AdminController`: `GET /api/admin/users` (paginated, role filter), `PUT /api/admin/users/{id}/role`
- [ ] Extend `SpaceController` / `SpaceService` with ADMIN override for DELETE and `isActive` toggle
- [ ] Integration tests: role change by ADMIN → 200, updated role; space deactivation by ADMIN → `isActive = false`

---

## M8 — Frontend Scaffold

**Acceptance criteria:** Vite + React project runs; routing works; Axios instance configured; Login/Register pages work against the live backend.

- [ ] Run `npm create vite@latest frontend -- --template react` (executed inside repo root or `frontend/` created manually)
- [ ] Add dependencies: `react-router-dom`, `axios`, `bootstrap`
- [ ] Bootstrap CSS imported in `main.jsx`
- [ ] `AuthContext` with `login`, `logout`, `register` actions using Axios
- [ ] Axios instance at `src/api/axiosInstance.js` with:
  - Request interceptor: attach `Authorization: Bearer <token>` from storage
  - Response interceptor: handle 401 (clear token, redirect to `/login`)
- [ ] Protected route component (`<RequireAuth>`) wrapping authenticated pages
- [ ] Pages scaffolded: Login, Register, 404 placeholder
- [ ] Smoke test: login with seed user `customer@spaceora.dev`, receive JWT, call `GET /api/users/me` successfully

---

## M9 — Frontend Core Pages

**Acceptance criteria:** Users can search, view and book spaces through the UI; customers can view their bookings; owners can manage their listings.

All UI work must reference the Figma design: [https://www.figma.com/design/jmILFgP2XqbqcjRCrXiA6W/Untitled](https://www.figma.com/design/jmILFgP2XqbqcjRCrXiA6W/Untitled)

- [ ] **Home / Search page**: search form (city, type, capacity, price), results grid using `SpaceSummaryDto`
- [ ] **Space Detail page**: images, description, pricing, availability calendar or slot-picker, "Book Now" button
- [ ] **Booking form**: date/time selection, notes field, price preview, confirmation modal
- [ ] **My Bookings page** (CUSTOMER): paginated list of reservations; cancel button for PENDING reservations
- [ ] **Owner Dashboard page** (OWNER):
  - My spaces list with edit/deactivate actions
  - Per-space reservation list with Accept / Reject actions
  - Add / edit / delete blocked periods
- [ ] **Admin Panel page** (ADMIN): user list with role-change controls; space activate/deactivate controls
- [ ] Accessibility: all interactive elements keyboard-accessible; appropriate ARIA labels on forms

---

## M10 — Polish, Docs, Testing, Final Review

**Acceptance criteria:** API docs accessible at `/swagger-ui.html`; all tests pass; coverage targets met; CORS and security headers verified.

- [ ] `springdoc-openapi` configured; `@Operation` and `@ApiResponse` annotations added to all controllers
- [ ] CORS configuration reviewed and locked to intended origins; tested from the React dev server
- [ ] All unit and integration tests pass (`mvn verify`)
- [ ] JaCoCo coverage report generated; service-layer ≥ 80%, `ReservationService` 100% branches
- [ ] `README.md` updated with local dev setup instructions (Docker Compose, env vars, `mvn`, `npm run dev`)
- [ ] Secrets audit: no credentials in source control; `.env.example` contains only placeholders
- [ ] Docker Compose final test: clean start → migrations run → seed loads → all APIs respond correctly
- [ ] Security headers review: check `X-Content-Type-Options`, `X-Frame-Options`, no stack traces in error responses
- [ ] Final walkthrough against `requirements.md` user stories: each story manually verified end-to-end

---

## Known Risks and Ambiguities

| Item | Decision / Default |
|---|---|
| Image upload | MVP stores image URLs only; no file upload endpoint. Post-MVP: S3 or similar. |
| Timezone handling | All datetimes stored as `TIMESTAMPTZ` (UTC). Frontend converts to local time for display. |
| Booking duration minimum | MVP: no enforced minimum; owner sets opening hours. Post-MVP: configurable per space. |
| Password strength policy | Minimum 8 characters, at least one uppercase, one lowercase, one digit. |
| JWT expiry default | 24 hours (86 400 seconds); configurable via `JWT_EXPIRY_SECONDS` env var. |
| Concurrent booking locking scope | Pessimistic lock per `space_id` + overlapping datetime range. |
| Pagination max page size | 100 rows. Requests for larger pages are clamped to 100. |
| Email notifications | Out of scope for MVP. Placeholder `NotificationService` interface may be added but not implemented. |
