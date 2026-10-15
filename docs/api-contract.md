# Spaceora — REST API Contract

## Conventions

- **Base URL:** `/api`
- **Content-Type:** `application/json`
- **Authentication:** `Authorization: Bearer <jwt>`
- **Pagination query params:** `page` (0-indexed), `size` (default 20), `sort` (e.g. `hourlyRate,asc`)
- **Error response body:**
  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed for field 'email'",
    "timestamp": "2025-01-01T10:00:00Z"
  }
  ```
- All timestamps in ISO 8601 UTC format (e.g. `2025-06-15T09:00:00Z`).
- All monetary values as decimal strings (e.g. `"15.00"`).

---

## Auth

### POST /api/auth/register

Register a new user account.

- **Auth required:** No
- **Request body:**
  ```json
  {
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane@example.com",
    "password": "SecurePass1!",
    "role": "CUSTOMER"
  }
  ```
  - `role` is optional; defaults to `CUSTOMER`. Accepted values: `CUSTOMER`, `OWNER`.
  - ADMIN role cannot be self-assigned; it must be granted by an existing ADMIN.

- **Response 201:**
  ```json
  {
    "id": 1,
    "email": "jane@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "role": "CUSTOMER",
    "createdAt": "2025-01-01T10:00:00Z"
  }
  ```

- **Errors:**
  - `400` — validation failure (missing required field, invalid email format, weak password)
  - `409` — email already registered

---

### POST /api/auth/login

Authenticate and receive a JWT.

- **Auth required:** No
- **Request body:**
  ```json
  { "email": "jane@example.com", "password": "SecurePass1!" }
  ```

- **Response 200:**
  ```json
  {
    "token": "eyJhbGci...",
    "expiresIn": 86400,
    "userId": 1,
    "role": "CUSTOMER"
  }
  ```
  - `expiresIn` is in seconds.

- **Errors:**
  - `400` — missing email or password
  - `401` — invalid credentials

---

## Users

### GET /api/users/me

Retrieve the authenticated user's profile.

- **Auth required:** Yes (any role)
- **Response 200:**
  ```json
  {
    "id": 1,
    "email": "jane@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "phone": "+44 7700 900123",
    "role": "CUSTOMER",
    "createdAt": "2025-01-01T10:00:00Z"
  }
  ```
- **Errors:** `401`

---

### PUT /api/users/me

Update the authenticated user's profile.

- **Auth required:** Yes (any role)
- **Request body (all fields optional):**
  ```json
  { "firstName": "Jane", "lastName": "Doe", "phone": "+44 7700 900456" }
  ```
- **Response 200:** Updated user profile (same shape as GET /api/users/me)
- **Errors:** `400`, `401`

---

## Spaces

### GET /api/spaces

Search and list active spaces.

- **Auth required:** No
- **Query params:**

  | Param | Type | Description |
  |---|---|---|
  | `city` | string | Filter by city (case-insensitive, partial match) |
  | `type` | string | `COWORKING`, `STUDY_ROOM`, `MEETING_ROOM`, `TRAINING_HALL` |
  | `minCapacity` | integer | Minimum capacity |
  | `maxCapacity` | integer | Maximum capacity |
  | `minPrice` | decimal | Minimum hourly rate |
  | `maxPrice` | decimal | Maximum hourly rate |
  | `date` | ISO date | Filter spaces available on this date |
  | `page` | integer | 0-indexed page number (default 0) |
  | `size` | integer | Page size (default 20, max 100) |
  | `sort` | string | e.g. `hourlyRate,asc` or `name,desc` |

- **Response 200:**
  ```json
  {
    "content": [
      {
        "id": 1,
        "name": "Canary Wharf Focus Room",
        "type": "MEETING_ROOM",
        "city": "London",
        "hourlyRate": "15.00",
        "capacity": 10,
        "isActive": true,
        "thumbnailUrl": "https://cdn.example.com/space1.jpg"
      }
    ],
    "totalElements": 42,
    "totalPages": 3,
    "page": 0,
    "size": 20
  }
  ```
- Only `isActive = true` spaces are returned.

---

### POST /api/spaces

Create a new space.

- **Auth required:** Yes (OWNER)
- **Request body:**
  ```json
  {
    "name": "Canary Wharf Focus Room",
    "description": "Quiet meeting room with projector.",
    "type": "MEETING_ROOM",
    "city": "London",
    "address": "1 Canada Square, London E14 5AB",
    "hourlyRate": "15.00",
    "dailyRate": "100.00",
    "capacity": 10
  }
  ```
  - `dailyRate` is optional.
  - `ownerId` is set from the authenticated user; any client-supplied value is ignored.

- **Response 201:** Full SpaceDto (see GET /api/spaces/{id})
- **Errors:** `400`, `401`, `403`

---

### GET /api/spaces/{id}

Get full details for a single space.

- **Auth required:** No
- **Response 200:**
  ```json
  {
    "id": 1,
    "ownerId": 2,
    "name": "Canary Wharf Focus Room",
    "description": "Quiet meeting room with projector.",
    "type": "MEETING_ROOM",
    "city": "London",
    "address": "1 Canada Square, London E14 5AB",
    "hourlyRate": "15.00",
    "dailyRate": "100.00",
    "capacity": 10,
    "isActive": true,
    "images": [
      { "id": 1, "imageUrl": "https://cdn.example.com/space1.jpg", "displayOrder": 0 }
    ],
    "availabilitySchedule": [
      { "dayOfWeek": 0, "openTime": "08:00", "closeTime": "20:00" }
    ],
    "createdAt": "2025-01-01T10:00:00Z"
  }
  ```
- **Errors:** `404`

---

### PUT /api/spaces/{id}

Update a space.

- **Auth required:** Yes (OWNER — must own the space)
- **Request body:** Partial or full space fields (same shape as POST, all optional)
- **Response 200:** Updated SpaceDto (same shape as GET /api/spaces/{id})
- **Errors:** `400`, `401`, `403` (not owner), `404`

---

### DELETE /api/spaces/{id}

Delete a space. Any associated images, schedules and blocked periods are cascade-deleted. Reservations are NOT deleted to preserve history.

- **Auth required:** Yes (OWNER — must own the space, or ADMIN)
- **Response 204:** No body
- **Errors:** `401`, `403`, `404`

---

## Space Availability

### GET /api/spaces/{id}/availability

Retrieve the merged availability view for a space over a date range.

- **Auth required:** No
- **Query params:**

  | Param | Type | Default | Description |
  |---|---|---|---|
  | `date` | ISO date | today | Start date |
  | `weeks` | integer | 1 | Number of weeks to return (1–4) |

- **Response 200:**
  ```json
  {
    "spaceId": 1,
    "slots": [
      {
        "date": "2025-06-16",
        "dayOfWeek": 0,
        "openTime": "08:00",
        "closeTime": "20:00",
        "blockedPeriods": [
          { "id": 3, "startDatetime": "2025-06-16T12:00:00Z", "endDatetime": "2025-06-16T14:00:00Z", "reason": "Maintenance" }
        ]
      }
    ]
  }
  ```
  - Days with no AvailabilitySchedule entry are omitted (space is closed that day).

- **Errors:** `404`

---

### POST /api/spaces/{id}/blocked-periods

Add a blocked period to a space.

- **Auth required:** Yes (OWNER — must own the space)
- **Request body:**
  ```json
  {
    "startDatetime": "2025-06-16T12:00:00Z",
    "endDatetime": "2025-06-16T14:00:00Z",
    "reason": "Deep clean"
  }
  ```
  - `reason` is optional.

- **Response 201:** BlockedPeriodDto
  ```json
  { "id": 3, "spaceId": 1, "startDatetime": "2025-06-16T12:00:00Z", "endDatetime": "2025-06-16T14:00:00Z", "reason": "Deep clean" }
  ```
- **Errors:** `400`, `401`, `403`, `404`, `409` (overlaps an existing blocked period)

---

### DELETE /api/spaces/{id}/blocked-periods/{bpId}

Remove a blocked period.

- **Auth required:** Yes (OWNER — must own the space)
- **Response 204:** No body
- **Errors:** `401`, `403`, `404`

---

## Reservations

### POST /api/reservations

Create a new reservation.

- **Auth required:** Yes (CUSTOMER)
- **Request body:**
  ```json
  {
    "spaceId": 1,
    "startDatetime": "2025-06-17T09:00:00Z",
    "endDatetime": "2025-06-17T11:00:00Z",
    "notes": "Need the projector set up, please."
  }
  ```
  - `notes` is optional.
  - `totalAmount` is always server-calculated; any client-supplied value is ignored.
  - `customerId` is always taken from the JWT; any client-supplied value is ignored.

- **Response 201:** ReservationDto
  ```json
  {
    "id": 10,
    "spaceId": 1,
    "spaceName": "Canary Wharf Focus Room",
    "customerId": 1,
    "startDatetime": "2025-06-17T09:00:00Z",
    "endDatetime": "2025-06-17T11:00:00Z",
    "totalAmount": "30.00",
    "status": "PENDING",
    "notes": "Need the projector set up, please.",
    "createdAt": "2025-06-10T15:00:00Z"
  }
  ```

- **Errors:**
  - `400` — validation failure (missing fields, end before start)
  - `401` — not authenticated
  - `403` — caller is not a CUSTOMER
  - `404` — space not found
  - `409` — booking conflict (overlap with existing PENDING or CONFIRMED reservation)
  - `422` — past start datetime, outside opening hours, or within a blocked period

---

### GET /api/reservations/{id}

Get a single reservation.

- **Auth required:** Yes (CUSTOMER who owns it, OWNER of the space, or ADMIN)
- **Response 200:** ReservationDto (same shape as POST response)
- **Errors:** `401`, `403`, `404`

---

### DELETE /api/reservations/{id}

Cancel a reservation.

- **Auth required:** Yes (CUSTOMER who owns it)
- **Response 200:** Updated ReservationDto with `"status": "CANCELLED"`
- **Errors:**
  - `401` — not authenticated
  - `403` — caller does not own this reservation
  - `404` — reservation not found
  - `422` — reservation has already started, or is in a terminal state (CANCELLED / REJECTED)

---

### GET /api/reservations/my

List the authenticated customer's reservations.

- **Auth required:** Yes (CUSTOMER)
- **Query params:** `status` (optional, filter by status), `page`, `size`
- **Response 200:** Paginated list of ReservationDto
- **Errors:** `401`, `403`

---

### GET /api/spaces/{id}/reservations

List reservations for a specific space.

- **Auth required:** Yes (OWNER — must own the space, or ADMIN)
- **Query params:** `status` (optional), `page`, `size`
- **Response 200:** Paginated list of ReservationDto
- **Errors:** `401`, `403`, `404`

---

### PUT /api/reservations/{id}/status

Owner accepts or rejects a pending reservation.

- **Auth required:** Yes (OWNER — must own the space the reservation is for)
- **Request body:**
  ```json
  { "status": "CONFIRMED" }
  ```
  - Accepted values: `CONFIRMED`, `REJECTED`
- **Response 200:** Updated ReservationDto
- **Errors:** `400`, `401`, `403`, `404`, `422` (invalid status transition)

---

## Admin

### GET /api/admin/users

List all users (paginated).

- **Auth required:** Yes (ADMIN)
- **Query params:** `role` (optional filter), `page`, `size`
- **Response 200:** Paginated list of user profiles
- **Errors:** `401`, `403`

---

### PUT /api/admin/users/{id}/role

Change a user's role.

- **Auth required:** Yes (ADMIN)
- **Request body:**
  ```json
  { "role": "OWNER" }
  ```
- **Response 200:** Updated user profile
- **Errors:** `400`, `401`, `403`, `404`

---

## Health

### GET /actuator/health

Spring Boot Actuator health check.

- **Auth required:** No
- **Response 200:**
  ```json
  { "status": "UP" }
  ```

---

## HTTP Status Code Summary

| Code | Meaning |
|---|---|
| 200 | OK |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request — validation failure |
| 401 | Unauthorized — missing or invalid JWT |
| 403 | Forbidden — authenticated but insufficient permissions |
| 404 | Not Found |
| 409 | Conflict — duplicate resource or booking overlap |
| 422 | Unprocessable Entity — business rule violation |
| 500 | Internal Server Error — safe message only, no stack trace |
