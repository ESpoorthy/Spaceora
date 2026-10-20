# Spaceora — Find Your Space. Make Room for Great Ideas.

**Spaceora** is a full-stack workspace discovery and booking platform that helps students, freelancers, professionals and businesses find suitable spaces for studying, coworking, meetings and training sessions.

Developed by **Team OrbitX**, Spaceora connects customers looking for productive spaces with owners who want to manage their listings, availability and reservations through a unified platform.

> Discover the right space. Manage your workspace. Make every hour count.

---

## Overview

Finding and booking a suitable workspace often involves searching across multiple platforms, comparing prices, checking availability and coordinating with space owners. Spaceora aims to simplify this process through a single, intuitive platform.

Customers can explore workspaces, compare details and manage their accounts, while owners can create and manage listings. The planned booking workflow will allow customers to request reservations and owners to confirm or reject them.

## Key Features

### Customer Experience
- **Workspace Discovery:** Browse coworking spaces, study rooms, meeting rooms and training halls.
- **Search and Filters:** Find spaces using available search criteria, sorting and filters.
- **Workspace Details:** Explore descriptions, locations, capacity, amenities and hourly pricing.
- **Secure Authentication:** Register, log in and manage personal profile information.
- **Saved Spaces:** Access the saved-space interface.
- **Booking Interface:** Review booking details and access booking-related screens.

### Space Owner Experience
- **Listing Management:** Create, view, update and delete eligible workspace listings.
- **Ownership Protection:** Owners can manage only their own spaces.
- **Owner Dashboard:** Access the workspace management interface.
- **Availability and Reservations:** Planned for the upcoming availability and booking implementation.

### Platform and Security
- RESTful APIs built with Spring Boot.
- JWT-based authentication and Spring Security authorisation.
- BCrypt password hashing.
- PostgreSQL persistence with Flyway database migrations.
- Server-side request validation and consistent error responses.
- Automated backend tests for authentication, workspace management and access control.

---

## Technology Stack

| Layer | Technologies |
|---|---|
| Frontend | HTML5, CSS3, JavaScript, React |
| UI and Styling | Bootstrap 5, reusable React components |
| Frontend Tooling | Vite, React Router, Axios |
| Backend | Java 21, Spring Boot, Spring MVC |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Database Migrations | Flyway |
| Build Tool | Maven |
| Testing | JUnit, Spring Boot Test, MockMvc, H2 for applicable tests |
| Design | Figma |
| Version Control | Git and GitHub |

---

## System Architecture

Spaceora follows a layered client-server architecture.

```text
                 CUSTOMER / SPACE OWNER
                           |
                           v
                React + Bootstrap Frontend
                           |
                     Axios / REST
                           |
                           v
                 Spring Boot Backend
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
         Controllers    Services     Security
             |             |        JWT / Roles
             +-------------+-------------+
                           |
                           v
                Spring Data JPA / Hibernate
                           |
                           v
                     PostgreSQL
                           |
                           v
                    Flyway Migrations
```

### Backend Design

The backend separates HTTP controllers, business services, repositories, entities, DTOs, security configuration and exception handling.

This separation supports maintainability, testing and future extension of the workspace availability and reservation features.

---

## Project Structure

```text
Spaceora/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/
│   │   └── test/
│   ├── pom.xml
│   └── mvnw
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── pages/
│   │   └── ...
│   ├── package.json
│   └── ...
├── docs/
│   ├── requirements.md
│   ├── design.md
│   ├── api-contract.md
│   ├── data-dictionary.md
│   ├── testing-plan.md
│   └── tasks.md
├── docker-compose.yml
├── .gitignore
└── README.md
```

*The directory structure above describes the intended organisation; consult the repository for the exact current file names.*

---

## Getting Started

### Prerequisites

Install the following software:

- Git
- Java Development Kit 21
- Node.js and npm, compatible with the frontend configuration
- PostgreSQL, or Docker Desktop with Docker Compose
- A code editor such as VS Code, Kiro

### 1. Clone the Repository

```bash
git clone https://github.com/ESpoorthy/Spaceora.git
cd Spaceora
```

### 2. Configure the Database

Use either a local PostgreSQL installation or the repository's Docker Compose configuration.

For Docker-based development, inspect `docker-compose.yml` and start the configured database service:

```bash
docker compose up -d
```

For a local PostgreSQL installation, create the development database and configure the database connection according to `backend/src/main/resources/application.yml`.

Configure all required environment variables locally. In particular, provide a secure `JWT_SECRET` for the backend. Never commit real credentials or secrets to GitHub.

### 3. Start the Backend

Open a terminal in the project root:

```bash
cd backend
./mvnw clean test
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

Use the database profile and environment variables specified by the application configuration. Flyway manages database schema migrations during application startup.

### 4. Start the Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

Configure `VITE_API_BASE_URL` to point to the running backend API, using the URL and port configured for your environment.

Open the local URL printed by Vite in your browser.

**Note:** These are the standard development commands. Confirm the database configuration and required environment variables in the current repository before starting the application.

---

## API Overview

The following endpoints are implemented in the documented M1 and M2 backend scope.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a customer or permitted owner |
| POST | `/api/auth/login` | Authenticate and receive a JWT |
| GET | `/api/users/me` | Retrieve the authenticated user's profile |
| PUT | `/api/users/me` | Update the authenticated user's profile |
| GET | `/api/spaces` | Search and retrieve paginated workspace listings |
| GET | `/api/spaces/{id}` | Retrieve workspace details |
| POST | `/api/spaces` | Create a workspace as an authorised owner |
| PUT | `/api/spaces/{id}` | Update an owned workspace |
| DELETE | `/api/spaces/{id}` | Delete a workspace according to authorisation rules |
| GET | `/api/spaces/owner/me` | Retrieve the authenticated owner's listings |

Availability and reservation endpoints will be documented here once their implementation is complete and verified. Refer to `docs/api-contract.md` for the authoritative request and response definitions.

---

## Development Roadmap

Spaceora is being developed incrementally to maintain code quality and protect existing functionality.

- **M0 — Project Foundation:** Project scaffolding, PostgreSQL configuration, Flyway migrations and health checks.
- **M1 — Authentication:** Registration, login, JWT authentication, profile management and security tests.
- **M2 — Space Management:** Workspace CRUD, search, filtering, pagination and owner access controls.
- **M3 — Availability and Blocked Periods:** Weekly schedules, unavailable periods and availability retrieval.
- **M4 — Reservation Management:** Booking requests, conflict prevention, owner approval or rejection, customer booking history and cancellation.

The target for the core MVP is completion of M4, followed by frontend integration, end-to-end verification and demonstration.

### Current Limitations

The frontend includes screens for features whose backend implementation may still be pending. Depending on the current integration state, some workspace or booking views may use development fixtures or display unavailable states.

Real payment processing, payment confirmation, production image uploads and external map integrations are not part of the current core MVP scope.

A feature should be considered complete only after its backend implementation, relevant tests and frontend integration have been verified.

---

## Testing and Quality Assurance

The project uses automated tests to validate business logic, API behaviour and security boundaries.

Backend verification includes:

- Authentication and registration tests.
- Password hashing and credential validation.
- JWT-protected endpoint tests.
- Role and workspace ownership checks.
- Workspace creation, updates and deletion.
- Request validation and error handling.
- Regression testing for existing functionality.

Run the backend tests with:

```bash
cd backend
./mvnw clean test
```

Run the frontend checks with:

```bash
cd frontend
npm run lint
npm run build
```

PostgreSQL-specific migration and concurrency behaviour should be verified against PostgreSQL. H2-based tests do not independently establish PostgreSQL integration correctness.

---

## UI/UX Design

The application follows the Spaceora designs created in Figma, with a responsive layout for desktop and mobile experiences.

**Figma design:** https://www.figma.com/design/jmILFgP2XqbqcjRCrXiA6W/Untitled

The design covers workspace discovery, workspace details, authentication, customer booking screens and owner management interfaces.

---

## Team OrbitX

Spaceora is developed collaboratively by **Team OrbitX**, combining frontend development, backend engineering, database design, API integration and testing.

Our goal is to deliver a secure and usable workspace marketplace with a polished interface and a reliable booking workflow.

---
