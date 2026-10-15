# Spaceora

**Spaceora** is a full-stack workspace booking marketplace that lets individuals and teams discover, compare and reserve coworking spaces, study rooms, meeting rooms and training halls in real time.

## Purpose

Spaceora connects workspace owners who want to monetise their venues with customers who need flexible, bookable spaces. Owners list their spaces with pricing, capacity, opening hours and blocked-off periods. Customers search, filter, view availability and make reservations — all through a clean, responsive interface.

## Planned Features

| Area | Features |
|---|---|
| Authentication | JWT-based register/login, role-based access (CUSTOMER, OWNER, ADMIN) |
| Space Management | CRUD for spaces, photos, pricing, capacity, weekly schedule |
| Search & Discovery | Filter by city, type, capacity, price, availability |
| Availability | Weekly opening hours, blocked periods, real-time slot checking |
| Reservations | Create, view, cancel bookings with status lifecycle |
| Owner Dashboard | Manage spaces, view incoming bookings, accept/reject |
| Admin Panel | User management, space moderation |
| Notifications | Booking confirmations and reminders (email, planned) |
| API Documentation | OpenAPI / Swagger UI |

## Technology Stack

### Frontend
- React (JavaScript) + Vite
- Bootstrap 5
- React Router
- Axios

### Backend
- Java 21
- Spring Boot 3
- Spring MVC · Spring Security · Spring Data JPA (Hibernate)
- PostgreSQL
- Flyway (database migrations)
- Maven

### Dev & Tooling
- OpenAPI / Swagger UI
- JUnit 5 · Mockito · Spring Boot Test
- Docker Compose (planned for local dev DB)

## UI/UX Reference

Figma design: [Spaceora — Official Design](https://www.figma.com/design/jmILFgP2XqbqcjRCrXiA6W/Untitled)

## Project Structure

```
Spaceora/
├── frontend/       # React + Vite application
├── backend/        # Spring Boot application (Maven)
├── docs/           # Architecture, API contract, requirements
└── README.md
```

## Team

**OrbitX**

| Member | Role |
|---|---|
| OrbitX Team | Full-stack development |

## Status

🚧 In active development — MVP in progress.

## Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| Java (JDK) | 21+ |
| Maven | 3.9+ (or use `./mvnw` in `backend/`) |
| Node.js | 20+ |
| npm | 9+ |
| Docker + Docker Compose | 24+ (for local PostgreSQL) |
| PostgreSQL | 16 (via Docker Compose) |

### Environment Setup

1. Copy `.env.example` to `.env` at the repo root and fill in your values:
   ```bash
   cp .env.example .env
   ```
2. Copy `frontend/.env.example` to `frontend/.env.local`:
   ```bash
   cp frontend/.env.example frontend/.env.local
   ```

### Start local PostgreSQL

```bash
docker compose up -d
```

### Run database migrations

Flyway runs automatically on Spring Boot startup. To run manually:
```bash
cd backend
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/spaceora_dev \
  -Dflyway.user=spaceora -Dflyway.password=changeme
```

### Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui.html  
Health check: http://localhost:8080/actuator/health

### Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: http://localhost:5173

### Run backend tests

```bash
cd backend
./mvnw test
```

### Environment Variables

| Variable | Default (dev) | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/spaceora_dev` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `spaceora` | DB username |
| `DB_PASSWORD` | — | DB password (required) |
| `JWT_SECRET` | dev-placeholder | HS256 signing secret (min 32 chars) |
| `JWT_EXPIRY_SECONDS` | `86400` | JWT validity (seconds) |
| `VITE_API_URL` | `http://localhost:8080` | Backend API base URL for React app |

### Seed Accounts (development only)

| Email | Password | Role |
|---|---|---|
| `customer@spaceora.dev` | `Customer1!` | CUSTOMER |
| `owner@spaceora.dev` | `Owner1!` | OWNER |
| `admin@spaceora.dev` | `Admin1!` | ADMIN |
