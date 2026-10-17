# Spaceora — Developer Handover Document

**Team:** OrbitX  
**Date:** October 2026  
**Prepared by:** Kiro (AI development environment)

---

## 1. Repository

| Item | Value |
|---|---|
| Repository URL | https://github.com/ESpoorthy/Spaceora.git |
| Branch | `main` |
| Latest commit | `a5a4d5f` — fix: address M2 security review findings |
| Remote status | Local and remote are fully synchronised |

### Commit history (milestones)

| Commit | Description |
|---|---|
| `a5a4d5f` | fix(m2): address security review findings (sort whitelist, owner listing, email leak) |
| `fc16ce7` | feat(m2): Space entity, SpaceController CRUD, search/filter, M2 tests |
| `e50b430` | Merge pull request #1 — Codex frontend implementation |
| `1ac0987` | fix(m1): remove email from ResourceNotFoundException messages |
| `db918b1` | feat(m1): JWT authentication, User entity, AuthController, security config |
| `8162670` | fix(m0): Hibernate dialect, open-in-view, compose version |
| `d63f4c4` | feat(m0): React/Vite frontend scaffold, README |
| `70993fa` | feat(M0): Spring Boot backend foundation, Flyway migrations V1–V6 |
| `64c216f` | docs: architecture specifications and API contract |
| `103042b` | chore: repository structure, README, .gitignore |

---

## 2. Completed functionality

### M0 — Project Foundation ✅
- Spring Boot 3.2.5 Maven project (`backend/`)
- PostgreSQL via Docker Compose (`docker-compose.yml`)
- Flyway migrations V1–V6 (schema + indexes + seed data)
- React + Vite frontend scaffold (`frontend/`)
- Environment variable configuration (`.env.example`)

### M1 — Authentication ✅
- `POST /api/auth/register` — register as CUSTOMER or OWNER; ADMIN self-assignment blocked
- `POST /api/auth/login` — returns signed HS256 JWT (userId, email, role claims)
- `GET /api/users/me` — authenticated user profile (no password hash returned)
- `PUT /api/users/me` — update first name, last name, phone
- JWT filter validates token from claims only (zero DB hit per request)
- BCrypt password hashing; no secret fallback in production config

### M2 — Space Management ✅
- `GET /api/spaces` — public, paginated, filterable (city, type, capacity, price), sort whitelist enforced
- `GET /api/spaces/{id}` — public space detail; 404 on missing
- `POST /api/spaces` — OWNER only; ownerId set from JWT, never from client
- `PUT /api/spaces/{id}` — OWNER of that space only (ownership checked server-side)
- `DELETE /api/spaces/{id}` — OWNER of own space or ADMIN
- `GET /api/spaces/owner/me` — OWNER lists their own spaces

### Frontend (Codex implementation, merged at `e50b430`) ✅
Full React/Vite SPA with Bootstrap 5:
- Home page with hero section and featured spaces
- Explore page — search, filter, sort, workspace cards
- Space detail page
- Login and Register pages (wired to real backend via `axiosInstance.js`)
- My Bookings page (fixture-labelled — M4 not yet implemented)
- Owner Dashboard page (partial — space listing wired to backend; booking management pending M6)
- Profile page
- Saved spaces page
- 404 page
- `developmentFixtures.js` — labelled fixture data used where backend endpoints are not yet available

---

## 3. What is NOT yet implemented

| Feature | Milestone | Notes |
|---|---|---|
| Availability schedules (weekly hours) | M3 | `availabilitySchedule` field in `SpaceDto` returns empty list |
| Blocked periods | M3 | Entities scaffolded in DB (V3 migration applied) but no endpoints |
| Reservations | M4 | `bookingService` in frontend calls correct API paths but endpoints don't exist yet |
| Owner booking management (accept/reject) | M6 | Pending M4 |
| Admin panel | M7 | Pending |
| Real-time notifications | Post-MVP | Out of scope |
| Image upload | Post-MVP | Image URLs accepted as strings; no file upload endpoint |
| Payment processing | Post-MVP | `totalAmount` calculated and stored but no gateway |

---

## 4. Prerequisites

| Tool | Required version | Check command |
|---|---|---|
| Java (JDK) | 21+ | `java -version` |
| Maven | via `./mvnw` wrapper | `cd backend && bash mvnw -version` |
| Node.js | 20+ | `node -v` |
| npm | 9+ | `npm -v` |
| PostgreSQL | 16 (via Docker Compose) | `docker compose up -d` |
| Docker Desktop | 24+ | `docker --version` |

> **Docker note:** The Docker CLI binary on macOS may not be on `PATH` in non-interactive shells.  
> Use the full path: `/Applications/Docker.app/Contents/Resources/bin/docker` or add it to your PATH.

---

## 5. Fresh-clone setup

```bash
# 1. Clone the repository
git clone https://github.com/ESpoorthy/Spaceora.git
cd Spaceora

# 2. Configure environment variables
cp .env.example .env
# Edit .env and set:
#   DB_URL=jdbc:postgresql://localhost:5432/spaceora_dev
#   DB_USERNAME=spaceora
#   DB_PASSWORD=your_secure_password
#   JWT_SECRET=your_minimum_32_character_random_secret
#   JWT_EXPIRY_SECONDS=86400

cp frontend/.env.example frontend/.env.local
# Edit frontend/.env.local and set:
#   VITE_API_BASE_URL=http://localhost:8080

# 3. Start PostgreSQL (requires Docker Desktop running)
docker compose up -d

# Wait for PostgreSQL to be ready:
docker exec spaceora_postgres pg_isready -U spaceora -d spaceora_dev

# 4. Start the backend (Flyway migrations run automatically on startup)
cd backend
./mvnw spring-boot:run
# Backend: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# Health check: http://localhost:8080/actuator/health

# 5. Start the frontend (in a separate terminal)
cd frontend
npm install
npm run dev
# Frontend: http://localhost:5173

# 6. Run backend tests
cd backend
./mvnw test
```

---

## 6. Environment variables

### Backend (`backend/` — set in `.env` at repo root or export before running)

| Variable | Example value | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/spaceora_dev` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `spaceora` | Database username |
| `DB_PASSWORD` | *(set securely)* | Database password |
| `JWT_SECRET` | *(min 32 random chars)* | HS256 JWT signing secret — **no default in production** |
| `JWT_EXPIRY_SECONDS` | `86400` | JWT validity in seconds (24 h) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allowed frontend origins |

### Frontend (`frontend/.env.local`)

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Backend API base URL |

---

## 7. Seed accounts (development only)

Loaded by Flyway migration V6 — available immediately after first startup.

| Email | Password | Role |
|---|---|---|
| `customer@spaceora.dev` | `Customer1!` | CUSTOMER |
| `owner@spaceora.dev` | `Owner1!` | OWNER |
| `admin@spaceora.dev` | `Admin1!` | ADMIN |

---

## 8. Backend URLs

| URL | Description |
|---|---|
| `http://localhost:8080/actuator/health` | Health check — `{"status":"UP"}` |
| `http://localhost:8080/swagger-ui.html` | Interactive API documentation |
| `http://localhost:8080/api-docs` | OpenAPI JSON spec |
| `http://localhost:8080/api/auth/register` | Registration endpoint |
| `http://localhost:8080/api/auth/login` | Login endpoint |
| `http://localhost:8080/api/spaces` | Space search (public) |

---

## 9. Test results (verified at handover)

| Suite | Tests | Pass | Fail | Skip |
|---|---|---|---|---|
| `AuthControllerIntegrationTest` | 13 | 13 | 0 | 0 |
| `UserServiceTest` | 12 | 12 | 0 | 0 |
| `SpaceControllerIntegrationTest` | 18 | 18 | 0 | 0 |
| `SpaceServiceTest` | 9 | 9 | 0 | 0 |
| `SpaceoraApplicationTests` | 1 | 1 | 0 | 0 |
| `FlywayMigrationIntegrationTest` | 0 | 0 | 0 | 0 (skipped — Docker socket) |
| **Total** | **53** | **53** | **0** | **0** |

**Frontend:** `npm run build` — 107 modules, BUILD SUCCESS.  
**Frontend lint:** `npm run lint` (oxlint) — no errors.

> **FlywayMigrationIntegrationTest note:** This test uses Testcontainers and requires Docker daemon access via `/var/run/docker.sock`. On macOS, Docker Desktop uses a non-standard socket path and Testcontainers cannot locate it automatically. The test skips gracefully. Migration correctness was verified separately by running the live application against real PostgreSQL (all V1–V6 migrations applied cleanly — confirmed in the PostgreSQL `flyway_schema_history` table).

---

## 10. Database migration state

All 6 Flyway migrations applied to `spaceora_dev` (PostgreSQL 16):

| Version | Description | Status |
|---|---|---|
| V1 | create users | ✅ applied |
| V2 | create spaces | ✅ applied |
| V3 | create availability (schedules + blocked_periods) | ✅ applied |
| V4 | create reservations | ✅ applied |
| V5 | add indexes | ✅ applied |
| V6 | seed dev data | ✅ applied |

V3 and V4 tables exist in the database but have no corresponding API endpoints yet (pending M3 and M4).

---

## 11. Known limitations and issues

| Issue | Severity | Notes |
|---|---|---|
| `availabilitySchedule` always returns `[]` in `SpaceDto` | Low | Populated in M3 |
| Reservation endpoints return 404 — not implemented | Expected | M4 work |
| `FlywayMigrationIntegrationTest` skips on macOS (Testcontainers socket) | Low | Use `bash mvnw test -Dtest=FlywayMigrationIntegrationTest` with Docker running manually |
| `dead ADMIN branch in SpaceService.update()` | Cosmetic | Unreachable code; cleanup deferred to M10 |
| Frontend `name` in `package.json` is `frontend_temp` | Cosmetic | Rename to `spaceora-frontend` before release |
| No HTTPS in development | Expected | Production deployment should terminate TLS at a reverse proxy |

---

## 12. Remaining milestones for teammate

### M3 — Availability and Blocked Periods
- `AvailabilitySchedule` entity (table `availability_schedules` already exists via V3)
- `BlockedPeriod` entity (table `blocked_periods` already exists via V3)
- `GET /api/spaces/{id}/availability` — merged availability view
- `POST /api/spaces/{id}/blocked-periods` — owner adds blocked period
- `DELETE /api/spaces/{id}/blocked-periods/{bpId}` — owner removes blocked period
- Populate `availabilitySchedule` field in `SpaceDto`

### M4 — Reservations
- `Reservation` entity (table `reservations` already exists via V4)
- `POST /api/reservations` — booking creation with full validation:
  - Past-date check, opening-hours check, blocked-period overlap, reservation overlap
  - Pessimistic locking (`SELECT … FOR UPDATE`) to prevent double-booking
  - Server-side `totalAmount` calculation
- `GET /api/reservations/{id}`, `GET /api/reservations/my`
- `DELETE /api/reservations/{id}` — cancel (CUSTOMER, own reservation)
- `PUT /api/reservations/{id}/status` — OWNER confirms or rejects
- `GET /api/spaces/{id}/reservations` — OWNER views reservations for their space
- Status state machine: PENDING → CONFIRMED | REJECTED, CONFIRMED → CANCELLED

> **Note to teammate:** The M3+M4 combined implementation prompt will be supplied separately by the project owner. The database schema for both milestones is already deployed. Do not run new schema migrations that conflict with V3 or V4.

---

## 13. Architecture reference

All architecture documents are in `docs/`:

| File | Description |
|---|---|
| `docs/requirements.md` | Functional and non-functional requirements |
| `docs/design.md` | System architecture, entity relationships, security design |
| `docs/api-contract.md` | Full REST API contract with schemas and status codes |
| `docs/data-dictionary.md` | PostgreSQL schema, column types, constraints, indexes |
| `docs/testing-plan.md` | Unit/integration test strategy, coverage targets |
| `docs/tasks.md` | M0–M10 implementation checklist with acceptance criteria |

Figma design (official UI/UX reference):  
https://www.figma.com/design/jmILFgP2XqbqcjRCrXiA6W/Untitled

---

## 14. Fresh-clone verification procedure

Run these commands after cloning to confirm the environment is correctly set up:

```bash
# 1. Backend compiles
cd backend && bash mvnw clean package -DskipTests
# Expected: BUILD SUCCESS

# 2. Backend tests pass
bash mvnw test
# Expected: Tests run: 53, Failures: 0, Errors: 0

# 3. Frontend builds
cd ../frontend && npm install && npm run build
# Expected: ✓ built in Xms

# 4. Frontend lint passes
npm run lint
# Expected: no output / exit 0

# 5. Application starts against PostgreSQL
# (requires Docker Desktop running and .env configured)
docker compose up -d
cd ../backend && bash mvnw spring-boot:run &
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# 6. Seed accounts work
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@spaceora.dev","password":"Customer1!"}'
# Expected: {"token":"eyJ...","userId":1,"role":"CUSTOMER",...}
```
