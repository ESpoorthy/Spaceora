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
