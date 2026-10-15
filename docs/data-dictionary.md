# Spaceora — Database Schema (Data Dictionary)

## Conventions

- All timestamps: `TIMESTAMPTZ` (UTC).
- All monetary values: `NUMERIC(10,2)`.
- Enum values stored as `VARCHAR` with a `CHECK` constraint (rather than a PostgreSQL `ENUM` type — simpler to manage with Flyway; adding values requires only an `ALTER TABLE … CHECK` update rather than `ALTER TYPE`).
- Surrogate primary keys: `BIGSERIAL` (auto-increment 64-bit integer).
- All tables include `created_at` and (where mutable) `updated_at` columns managed by the application layer.
- Flyway migration naming: `V{n}__{description}.sql`, e.g. `V1__create_users.sql`.

---

## Table: users

Stores all platform users regardless of role.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | Auto-increment surrogate key |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Login identifier; stored lowercase |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt hash — never returned in API responses |
| first_name | VARCHAR(100) | NOT NULL | |
| last_name | VARCHAR(100) | NOT NULL | |
| phone | VARCHAR(30) | | Optional contact number |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'CUSTOMER', CHECK(role IN ('CUSTOMER','OWNER','ADMIN')) | Role assignment; only ADMIN can set ADMIN |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Set on insert |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Updated on every write by application |

---

## Table: spaces

Represents a bookable workspace listing.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| owner_id | BIGINT | NOT NULL, FK → users(id) ON DELETE RESTRICT | The OWNER who created this space |
| name | VARCHAR(200) | NOT NULL | Display name |
| description | TEXT | | Long-form description |
| type | VARCHAR(30) | NOT NULL, CHECK(type IN ('COWORKING','STUDY_ROOM','MEETING_ROOM','TRAINING_HALL')) | Space category |
| city | VARCHAR(100) | NOT NULL | Primary search dimension — indexed |
| address | VARCHAR(500) | NOT NULL | Full street address |
| hourly_rate | NUMERIC(10,2) | NOT NULL, CHECK(hourly_rate >= 0) | Rate per hour in GBP |
| daily_rate | NUMERIC(10,2) | CHECK(daily_rate >= 0) | Optional full-day rate |
| capacity | INT | NOT NULL, CHECK(capacity > 0) | Maximum number of occupants |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | FALSE = hidden from search; not deleted |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |

---

## Table: space_images

Stores URLs for photos associated with a space.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| space_id | BIGINT | NOT NULL, FK → spaces(id) ON DELETE CASCADE | Cascades when space is deleted |
| image_url | VARCHAR(1000) | NOT NULL | External CDN or storage URL |
| display_order | INT | NOT NULL, DEFAULT 0 | Lower number = shown first |

---

## Table: availability_schedules

Defines the weekly opening hours for a space. One row per day-of-week per space.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| space_id | BIGINT | NOT NULL, FK → spaces(id) ON DELETE CASCADE | |
| day_of_week | SMALLINT | NOT NULL, CHECK(day_of_week BETWEEN 0 AND 6) | 0 = Monday, 6 = Sunday (ISO day-of-week convention) |
| open_time | TIME | NOT NULL | e.g. `08:00` |
| close_time | TIME | NOT NULL, CHECK(close_time > open_time) | e.g. `20:00` |

**Unique constraint:** `(space_id, day_of_week)` — only one schedule entry per day per space.

---

## Table: blocked_periods

Represents a period when a space is unavailable (maintenance, holiday, etc.).

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| space_id | BIGINT | NOT NULL, FK → spaces(id) ON DELETE CASCADE | |
| start_datetime | TIMESTAMPTZ | NOT NULL | Start of blocked window |
| end_datetime | TIMESTAMPTZ | NOT NULL, CHECK(end_datetime > start_datetime) | End of blocked window |
| reason | VARCHAR(500) | | Optional description |

---

## Table: reservations

Records a customer's booking of a space for a specific time window.

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| space_id | BIGINT | NOT NULL, FK → spaces(id) ON DELETE RESTRICT | Retain reservation history even if space is deactivated |
| customer_id | BIGINT | NOT NULL, FK → users(id) ON DELETE RESTRICT | The CUSTOMER who made the booking |
| start_datetime | TIMESTAMPTZ | NOT NULL | Inclusive start of booking |
| end_datetime | TIMESTAMPTZ | NOT NULL, CHECK(end_datetime > start_datetime) | Exclusive end of booking |
| total_amount | NUMERIC(10,2) | NOT NULL, CHECK(total_amount >= 0) | Server-calculated; never client-supplied |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING', CHECK(status IN ('PENDING','CONFIRMED','CANCELLED','REJECTED')) | State machine described below |
| notes | TEXT | | Customer-supplied notes for the owner |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | |

### Reservation Status Transitions

```
         ┌──────────► CONFIRMED ──► CANCELLED (by customer, before start)
PENDING ──┤
         └──────────► REJECTED
```

Any other transition is rejected with HTTP 422.

---

## Recommended Indexes

```sql
-- Space search filters
CREATE INDEX idx_spaces_city        ON spaces(city);
CREATE INDEX idx_spaces_type        ON spaces(type);
CREATE INDEX idx_spaces_capacity    ON spaces(capacity);
CREATE INDEX idx_spaces_hourly_rate ON spaces(hourly_rate);
CREATE INDEX idx_spaces_is_active   ON spaces(is_active);

-- Reservation lookups
CREATE INDEX idx_reservations_space_status ON reservations(space_id, status);
CREATE INDEX idx_reservations_customer     ON reservations(customer_id);

-- Blocked period lookups
CREATE INDEX idx_blocked_periods_space ON blocked_periods(space_id);
```

---

## Flyway Migration Order

All migration files live in `backend/src/main/resources/db/migration/`.

| Version | File | Description |
|---|---|---|
| V1 | `V1__create_users.sql` | `users` table |
| V2 | `V2__create_spaces.sql` | `spaces` and `space_images` tables |
| V3 | `V3__create_availability.sql` | `availability_schedules` and `blocked_periods` tables |
| V4 | `V4__create_reservations.sql` | `reservations` table |
| V5 | `V5__add_indexes.sql` | All recommended indexes |
| V6 | `V6__seed_dev_data.sql` | Dev seed: 3 users (customer, owner, admin), 2 spaces, 1 reservation |

### Seed Data (V6)

| Entity | Details |
|---|---|
| User 1 | email: `customer@spaceora.dev`, role: `CUSTOMER`, password: `Customer1!` (BCrypt hash in migration) |
| User 2 | email: `owner@spaceora.dev`, role: `OWNER`, password: `Owner1!` (BCrypt hash in migration) |
| User 3 | email: `admin@spaceora.dev`, role: `ADMIN`, password: `Admin1!` (BCrypt hash in migration) |
| Space 1 | owner: User 2, name: `Canary Wharf Focus Room`, type: `MEETING_ROOM`, city: `London`, hourlyRate: `15.00`, capacity: `10`, isActive: `true` |
| Space 2 | owner: User 2, name: `Shoreditch Hot Desk`, type: `COWORKING`, city: `London`, hourlyRate: `8.00`, capacity: `4`, isActive: `true` |
| Reservation 1 | customer: User 1, space: Space 1, status: `PENDING`, future date (e.g. 30 days from migration run date) |

> **Note:** Password hashes in V6 must be generated once and hard-coded as static BCrypt strings. They should never be generated dynamically in a migration script.
