# Spaceora — Functional and Non-Functional Requirements

## Actors

| Actor | Description |
|---|---|
| **CUSTOMER** | Discovers and books workspaces |
| **OWNER** | Lists and manages spaces, reviews bookings |
| **ADMIN** | Moderates users and spaces platform-wide |

---

## User Stories

### CUSTOMER

- As a customer I can register and log in with email and password.
- As a customer I can search spaces by city, type, capacity, price range and date.
- As a customer I can view a space's details, photos, pricing and availability.
- As a customer I can create a reservation for an available slot.
- As a customer I can view my own reservations.
- As a customer I can cancel a reservation I own (if not already started).

### OWNER

- As an owner I can register and log in.
- As an owner I can create, update and delete my spaces.
- As an owner I can set weekly opening hours (per day of week) for each space.
- As an owner I can add and remove blocked periods for maintenance or holidays.
- As an owner I can view all reservations for my spaces.
- As an owner I can accept or reject a PENDING reservation.
- As an owner I can deactivate a space (hide from search without deleting).

### ADMIN

- As an admin I can list all users.
- As an admin I can change a user's role.
- As an admin I can activate or deactivate any space.

---

## Non-Functional Requirements

### Security
- OWASP Top 10 awareness throughout the implementation.
- Passwords hashed with BCrypt; plaintext passwords never logged or returned.
- JWT-based stateless authentication (HS256, configurable secret and expiry).
- Server-side validation on all inputs; client-supplied role, ownerId and totalAmount are never trusted.
- No stack traces or internal details in API error responses.

### Performance
- All list endpoints paginated (default page size 20).
- Database indexes on all search and filter columns (city, type, capacity, hourly_rate, is_active).
- Pagination metadata (totalElements, totalPages, page, size) returned in every paginated response.

### Scalability
- Stateless REST API — no server-side HTTP session state.
- JWT tokens carry all authentication state needed per request.

### Testability
- Service layer covered at ≥ 80% by unit tests.
- Booking business rules (ReservationService) covered at 100%.
- Controller layer covered for all key happy-path and error-path scenarios.

### Maintainability
- All schema changes managed through Flyway versioned migration scripts.
- `spring.jpa.hibernate.ddl-auto=validate` in all environments; never `create` or `create-drop` in production.
- Secrets kept in environment variables; `.env.example` provided with safe placeholder values only.

### Reliability
- Pessimistic locking (SELECT … FOR UPDATE) used when checking space availability before creating a reservation to prevent double-booking under concurrent requests.
- HTTP 409 returned for any booking conflict.
- Explicit status transition rules enforced; invalid transitions return HTTP 422.

---

## Out of Scope (MVP)

| Feature | Notes |
|---|---|
| Real payment processing | No Stripe, PayPal or similar integrations in MVP |
| Real-time chat or messaging | Post-MVP feature |
| Mobile applications (iOS / Android) | Post-MVP; API-first design allows future addition |
| Email notifications | Planned post-MVP |
| Multi-currency pricing | Single currency (GBP) for MVP |
| Reviews and ratings | Post-MVP feature |
| File / image upload | Image URLs stored as strings; actual upload service is post-MVP |
