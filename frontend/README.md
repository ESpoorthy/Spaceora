# Spaceora frontend

Responsive React + Vite app for the Spaceora workspace marketplace. UI styling follows the official Figma file (`jmILFgP2XqbqcjRCrXiA6W`) using Bootstrap 5 and custom design tokens.

## Run locally

```sh
npm ci
cp .env.example .env.local
npm run dev
```

Set `VITE_API_BASE_URL` to the Spring Boot server origin (defaults to `http://localhost:8080`).

## Checks

```sh
npm run lint
npm run build
```

There is no frontend test script in this scaffold.

## Backend status

Authentication/profile calls use the documented `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/users/me` and `PUT /api/users/me` DTOs. JWTs are sent as Bearer tokens; “Remember me” chooses local storage or session-only storage. A 401 clears the session.

The current backend has no space, availability or reservation controllers. `src/api/marketplaceService.js` defines replaceable interfaces for the documented routes. The Explore and detail screens use `src/data/spaces.js` development fixtures; availability and booking pages explicitly show that no backend request was saved. Owner screens use an empty local fixture and do not claim listing or booking persistence. Saved-space IDs are a non-sensitive local preference only.
