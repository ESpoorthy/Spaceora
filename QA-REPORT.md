# Spaceora End-to-End QA Report

**Run date:** 2026-10-18
**Scope:** Top-level workspace (`Spaceora-main`) served at `http://localhost:5173`, with the top-level Spring backend and local PostgreSQL database. The top-level folder is not a Git repository; `Spaceora/` is a separate nested Git checkout and is not the code served by the browser.

## Release summary

**Release readiness: CONDITIONAL — do not give full sign-off yet.** The previously reported negative-pagination API defect is fixed and verified by an integration regression test and live OWNER API requests; invalid page/size values now return the standard HTTP 422 response, while normal pagination and the 100-row cap remain intact. Some browser error/state combinations remain unverified. Binary image upload is explicitly post-MVP in the requirements and is not an MVP blocker; owner-managed image URL entry remains a scope clarification because create/update request documentation omits image fields.

The top-level backend’s reservation, availability, space-management, authentication and access-control endpoints passed live PostgreSQL/API checks. Explore/search, space details, customer booking creation, My Bookings, and owner management use real backend data and endpoints. The concurrent booking fix locks the stable space row inside the create transaction before checking for overlaps, so requests for the same space serialize even when no reservation row exists yet. The full backend suite and frontend lint/build pass, and a live PostgreSQL API race probe returned one HTTP 201 and one HTTP 409.

The distinction matters: a backend feature being implemented and passing an API check does not mean it is wired into the frontend or verified as an end-to-end UI journey.

## Test results

| # | Journey/check | Result | Evidence |
|---|---|---|---|
| 1 | Guest searches, filters and opens a detail page | **PASS — browser + API** | In the browser, applying City=London submitted to the real search API and returned persisted listing ID 1; its card opened `/spaces/1`, where real backend name, address, description, type, capacity and rate rendered. Default Hyderabad search showed the API-backed empty state. At 390px, Explore and detail had no horizontal overflow. |
| 2 | Customer registers, logs in and creates a booking | **PASS — live UI and API** | Registered and logged in a disposable CUSTOMER through the browser. From backend space ID 1, selected 2026-10-19 at 10:00 UTC for one hour and submitted. UI showed “Your booking is pending”; `GET /api/reservations/my` returned persisted reservation ID 4, `PENDING`, space ID 1, 10:00–11:00 UTC, server-calculated total 15.00. Test reservation/account were removed afterward. |
| 3 | My Bookings displays the persisted reservation | **PASS — live UI and API** | Registered and logged in a disposable CUSTOMER, created a real reservation through the booking UI (ID 5, space ID 1, PENDING, 2026-10-19 10:00–11:00 UTC, ₹15.00), then opened My Bookings. The page displayed the backend space name, reservation ID, timestamps, amount, status and cancellation action. A second newly registered CUSTOMER with no reservations saw the actual empty state. Temporary QA rows were removed after verification. |
| 4 | Overlapping booking for the same space/time is rejected | **PASS — browser sequential conflict + API concurrency** | A sequential browser booking conflict displayed HTTP 409 with a clear conflict message. After the backend fix, the API integration regression fired eight simultaneous authenticated requests for the same slot: one HTTP 201, seven HTTP 409, one reservation row; its `@AfterEach` cleanup removes test data. A live PostgreSQL probe of two simultaneous requests for space ID 1 at 2030-05-15 10:00–11:00 UTC returned one HTTP 201 (temporary ID 10) and one HTTP 409. The exact successful QA row was subsequently deleted. |
| 5 | Owner creates/edits a space and configures availability | **PASS — live UI and API** | Registered/logged in a disposable OWNER. Created real space ID 4 through the owner UI; `/api/spaces/owner/me` displayed it after navigation and reload. Edited its name through `/api/spaces/{id}` and confirmed the changed form value after reload. Saved Mon–Fri schedule (Monday 08:30–18:00, other weekdays 09:00–17:00) via `/availability/schedule`; after reload the times remained and the weekly view reflected them. Created a future blocked period and confirmed its UTC time/reason remained after reload. |
| 6 | Owner sees and confirms/rejects a pending reservation | **PASS — live UI and API** | A disposable CUSTOMER created reservations 6 and 7 for the owner’s real space. Owner Bookings loaded them by querying each owned space’s reservation endpoint. The owner confirmed reservation 6 and rejected reservation 7; the UI showed `CONFIRMED` and `REJECTED`, and both statuses remained after reload. |
| 7 | Customer cancels and the slot becomes available again | **PASS — browser cancellation; API slot-release check** | The customer cancelled reservation ID 5 through My Bookings; the real DELETE response updated the row to `CANCELLED`, moved it to Past bookings, and reload showed the persisted status. A prior PostgreSQL-backed API smoke flow confirmed the cancelled interval became bookable again. The latter was not separately repeated in the UI during the final review. |
| 8 | Customer cannot read another customer’s private booking | **PASS — backend API** | A different customer’s `GET /api/reservations/{id}` returned HTTP 403. |
| 9 | Non-owner cannot modify another owner’s space/reservation | **PASS — backend API** | Using a different OWNER JWT, attempted `PUT /api/spaces/4` and `PUT /api/reservations/6/status`; both returned HTTP 403 and did not mutate records. |
| 10 | Invalid input, unavailable slots and backend errors are understandable | **PARTIAL PASS** | Browser submission for a closed date produced HTTP 422 and UI message “This space is unavailable for the selected time: Space is closed on 2026-10-18”. The repeated overlapping request produced HTTP 409 with a clear conflict message. After stopping the backend, a real submission attempt displayed “The booking service could not be reached. Check your connection and try again.” Other HTTP statuses are handled in code but were not individually browser-tested here. |
| 11 | Desktop/mobile loading, empty, error and success states | **PARTIAL PASS — final pass** | Owner availability loaded successfully at desktop and 390px mobile widths; the mobile document width was 375px, with no horizontal overflow. A temporary blocked period was added, removed from the UI, and remained absent after reload. In this pass, simulated HTTP 503 failures on `GET /api/spaces/owner/me` displayed a clear error with Try again at desktop (1440px viewport, 1425px document) and mobile (390px viewport, 375px document); retry then loaded the actual backend listings at each width, with no horizontal overflow. Owner/customer role guards were also browser-checked at both widths. These specific checks pass; other state combinations were not comprehensively repeated at both widths. |

## Automated checks

| Command | Result |
|---|---|
| `frontend: npm run lint` | **PASS, no warnings** after owner integration. |
| `frontend: npm run build` | **PASS** after owner integration (Vite production build completed). |
| `backend: .\mvnw.cmd test` | **PASS after fix**: 79 tests, 0 failures, 0 errors. The baseline run had 7 `UnnecessaryStubbingException` errors in `AvailabilityServiceTest`; seven unused test stubs were removed. |
| `backend: .\mvnw.cmd -Dtest=SpaceControllerIntegrationTest,SpaceServiceTest test` | **PASS** after this frontend integration: 27 tests, 0 failures, 0 errors. |
| `backend: .\mvnw.cmd -Dtest=ReservationServiceTest test` | **PASS** for this task: 16 tests, 0 failures, 0 errors. |
| `backend: .\mvnw.cmd '-Dtest=SpaceControllerIntegrationTest,SpaceServiceTest,AvailabilityServiceTest,ReservationServiceTest' test` | **PASS** for owner integration: 53 tests, 0 failures, 0 errors. |
| Final release review, from `backend`: `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd '-Dapi.version=1.41' '-Dtest=FlywayMigrationIntegrationTest' test` | **PASS**: 2 tests, 0 failures, 0 errors, 0 skipped. Testcontainers found the local Npipe socket; Flyway applied all 7 migrations (V1–V7) to a fresh PostgreSQL 16 container and both migration checks completed. |
| Final release review, from `backend`: `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd '-Dapi.version=1.41' test` | **PASS**: 82 tests, 0 failures, 0 errors, 0 skipped. This includes 2 migration tests and the concurrent reservation integration test. |
| Final release review, from `frontend`: `npm run lint` | **PASS**, no diagnostics. |
| Final release review, from `frontend`: `npm run build` | **PASS**, Vite production build completed (107 modules). |
| Reservation race task, from `backend`: `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd '-Dtest=ReservationServiceTest,ReservationConcurrencyIntegrationTest' test` | **PASS**: 17 tests, 0 failures, 0 errors (16 reservation service tests and 1 eight-request API concurrency integration test). |
| Reservation race task, from `backend`: `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd test` | **PASS**: 80 tests, 0 failures, 0 errors. `FlywayMigrationIntegrationTest` reports 0 tests because Testcontainers cannot connect to Docker. |
| Reservation race task, from `frontend`: `npm run lint` | **PASS**, no diagnostics. |
| Reservation race task, from `frontend`: `npm run build` | **PASS**, Vite production build completed. |
| Final pass, from `backend`: `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd test` | **PASS**: 79 tests, 0 failures, 0 errors; the migration class reports zero tests because Testcontainers aborts on Docker discovery. |
| Final pass, from `backend`: `$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'; .\mvnw.cmd '-Dtest=FlywayMigrationIntegrationTest' test` | **NOT EXECUTED**: `BUILD SUCCESS`, but 0 tests. Testcontainers could not find a valid Docker environment; its Npipe probe returned HTTP 400 with an empty Docker server identity and zero containers. |
| Final pass: `npm run lint` | **PASS**, no diagnostics. |
| Final pass: `npm run build` | **PASS**, Vite production build completed. |
| Frontend automated tests | **NOT AVAILABLE**: the frontend package has no test script. |

The environment had Java 21 installed but no persistent `JAVA_HOME`; each Maven invocation set `JAVA_HOME=C:\Program Files\Java\jdk-21` in its process.

## Final verification pass — 2026-10-18

### Reproduced defects

#### F-08 — Resolved (High): Owner dashboard crashed on entry

**Affected file:** [OwnerPages.jsx](./frontend/src/pages/OwnerPages.jsx#L94).

**Baseline reproduction:** With the backend available, sign in as `owner@spaceora.dev` and open `/owner/dashboard` (also the destination after owner login). Browser runtime reported `ReferenceError: creating is not defined` in `OwnerDashboardPage`; `creating` was declared only in the separate listings component.

**Fix and verification (2026-10-18):** Removed the single stray `if (creating) return undefined` guard from `OwnerDashboardPage`’s effect. The dashboard already performs its owner-data load unconditionally and does not need the create-form guard. No error was hidden and no data or styling changed. After logging in through the browser as the seeded OWNER and opening `/owner/dashboard`, the dashboard rendered “A clearer view of your space.”, finished “Loading your owner data…”, and displayed backend-backed metrics (2 active spaces, 1 pending booking), owned spaces, and recent bookings. Playwright confirmed the heading and metrics were visible, loading cleared, and no runtime page error occurred during this verification. `npm run lint` passed with no diagnostics; `npm run build` passed with Vite producing the production bundle.

#### F-09 — Resolved (High): Simultaneous overlapping reservations both succeeded

**Affected files:** [ReservationService.java](./backend/src/main/java/com/spaceora/reservation/service/ReservationService.java#L57), [SpaceRepository.java](./backend/src/main/java/com/spaceora/space/repository/SpaceRepository.java#L20), [ReservationRepository.java](./backend/src/main/java/com/spaceora/reservation/repository/ReservationRepository.java), [ReservationServiceTest.java](./backend/src/test/java/com/spaceora/reservation/service/ReservationServiceTest.java), [ReservationConcurrencyIntegrationTest.java](./backend/src/test/java/com/spaceora/reservation/controller/ReservationConcurrencyIntegrationTest.java).

**Confirmed cause:** The original `findOverlappingActiveWithLock` only locks matching reservation rows. When there is no matching reservation yet, neither concurrent transaction locks anything, so both can observe an empty result and insert.

**Fix:** Added `SpaceRepository.findByIdForUpdate` using JPA `PESSIMISTIC_WRITE`, and use it as the first space load in `ReservationService.create`. The database row for the space always exists and is locked until the service transaction commits, including the overlap check and insert. Thus requests for the same space serialize even on an empty slot; different spaces can still be booked independently. Existing overlap validation, roles, statuses and interval semantics remain unchanged. No migration or frontend change was needed.

**Regression and live verification:** `ReservationConcurrencyIntegrationTest` creates unique OWNER/CUSTOMER accounts and a temporary space, runs eight simultaneous authenticated requests for the same slot, requires one HTTP 201 and seven HTTP 409, and checks there is only one reservation. `@AfterEach` removes the reservations, schedules, space and users in a transaction. The targeted reservation tests passed 17/17. Against the live PostgreSQL-backed backend, two simultaneous identical requests for seed space ID 1 at 2030-05-15 10:00–11:00 UTC returned HTTP 201 (ID 10) and HTTP 409 (“The requested time slot is already booked”); the created reservation was cancelled with HTTP 200. No probe reservation remains.

### Final-pass verification evidence

- **Browser — blocked-period removal:** Signed in as the seeded OWNER, opened `/owner/availability`, added a one-hour period labeled `temporary final QA removal test` on space ID 1, clicked Remove, and confirmed “No blocked periods for this space.” Reloaded and confirmed it was still absent. The temporary period was removed; no QA period remains.
- **Browser — owner error/retry:** Used Playwright failure injection to abort `GET /api/spaces/owner/me`. The page displayed the connection error and Try again control. After removing the injected failure, clicked Try again; the retried owner-space GET returned HTTP 200 and the weekly availability loaded. This verifies the UI error/retry behavior under a simulated network failure, not a server-generated 5xx response. When the backend was stopped and the page fully reloaded, the separate `/api/users/me` authentication check failed and redirected to `/login?next=%2Fowner%2Favailability`; therefore the owner-page retry check used targeted failure injection.
- **API — authorization:** Seeded CUSTOMER login returned HTTP 200; CUSTOMER `GET /api/spaces/owner/me`, `PUT /api/spaces/1`, and `PUT /api/reservations/1/status` each returned HTTP 403. Seeded OWNER login and `GET /api/spaces/owner/me` returned HTTP 200. Prior owner-to-other-owner mutation checks also returned HTTP 403 (see Journey 9).
- **Automated status transitions:** The final full suite passed, including the 16 `ReservationServiceTest` cases for pending-to-confirmed, pending-to-rejected, invalid terminal transitions and wrong-space owner checks. Earlier live browser owner verification confirmed/rejected pending reservations and observed both statuses persist after reload.
- **Automated checks:** Full `.\mvnw.cmd test` completed with 79 tests, 0 failures and 0 errors. `FlywayMigrationIntegrationTest` was selected separately and reported 0 tests because Testcontainers could not connect to Docker; the emitted probe error was `BadRequestException (Status 400)` from `NpipeSocketClientProviderStrategy`, followed by “Could not find a valid Docker environment.” Frontend `npm run lint` and `npm run build` both passed.
- **Responsive check:** Owner availability page had no horizontal overflow at 390×844 (document/body width 375px) or 1440×900 (document/body width 1425px). This does not verify every loading/error/empty state at both viewport sizes.
- **OWNER dashboard fix:** In the top-level frontend, changed only `frontend/src/pages/OwnerPages.jsx` by removing the undefined `creating` guard from `OwnerDashboardPage`. `npm run lint` passed; `npm run build` passed (107 modules transformed). With the backend health endpoint UP and the seeded OWNER signed in, `/owner/dashboard` loaded persisted owner spaces/reservations and rendered its dashboard without the ReferenceError. Browser assertion: heading visible, metrics visible, loading state cleared, runtime errors `[]`.
- **Concurrent booking fix:** In the top-level backend, changed the reservation create path to lock the stable space row before overlap checks, clarified the overlap-query locking comment, updated reservation service mocks, and added an API-level concurrency regression test with transactional cleanup. The targeted selector passed 17/17 and the full suite passed 80/80. The live PostgreSQL probe passed with one HTTP 201 and one HTTP 409; temporary reservation ID 10 was cancelled (HTTP 200). The backend process used for the probe was stopped afterward.
- **Working tree:** The top-level served workspace has no Git root. This task changed only `frontend/src/pages/OwnerPages.jsx` and this QA report. The separate nested `Spaceora/` checkout still has the same five pre-existing modifications (157 insertions, 32 deletions) in the availability test and four search/detail frontend files; it is not the live application. No existing modifications were discarded or reset.
- **Evidence limitation:** Browser accessibility snapshots and network/runtime errors were captured through the browser tools; no new screenshot files were saved during this final pass.

## Findings and reproduction

### F-01 — Resolved (High): Customer booking creation was preview-only

**Affected files:** [BookingPages.jsx](./frontend/src/pages/BookingPages.jsx), [SpaceDetailsPage.jsx](./frontend/src/pages/SpaceDetailsPage.jsx), [marketplaceService.js](./frontend/src/api/marketplaceService.js).

**Baseline reproduction:** Before integration, the booking-review screen used mock fallback data and navigated to a preview confirmation without a request.

**Implementation and verification:** The review route loads the real space by route ID. The authenticated CUSTOMER submits `{spaceId, startDatetime, endDatetime, notes?}` through the existing Axios client and token interceptor; date/time inputs are labeled UTC to match the API and schedule contract. Duplicate submits are guarded and the button is disabled while pending. HTTP 201 must return a reservation ID and matching space ID before showing confirmation. Browser verification created reservation ID 4 for real space ID 1, and `GET /api/reservations/my` independently confirmed the persisted PENDING record and server-calculated amount. A repeat request returned 409 with a clear conflict message; a closed day returned 422 with its reason; after backend shutdown the UI showed a network-unavailable message. Temporary QA account/reservation were removed.

The customer's role is enforced by the existing route guard and backend `@PreAuthorize`; no authorization bypass or client-supplied customer/amount was introduced. Booking styles and routes were preserved.

### F-02 — Resolved (High): My Bookings did not load persisted reservations

**Affected file:** [MyBookingsPage.jsx](./frontend/src/pages/MyBookingsPage.jsx)

**Baseline reproduction:** Create a reservation and open `/my-bookings`; the page always rendered a fixed empty-state notice.

**Implementation and verification:** The customer-only page now calls the existing authenticated `GET /api/reservations/my` service, validates and renders its paginated `Page<ReservationDto>` response, separates upcoming and past records, and shows reservation ID, real space name/ID, UTC start/end, server total and actual status. It has loading, retryable error, true empty and populated states. The backend’s customer cancellation endpoint is also wired: a confirmation-protected `DELETE /api/reservations/{id}` updates the local row only from the returned DTO and surfaces server errors. Cancellation is available only for future PENDING/CONFIRMED records.

Live browser verification registered a disposable CUSTOMER, created reservation ID 5 for backend space ID 1, and saw it in Upcoming as PENDING. Cancelling it through the page changed its status to CANCELLED; after reloading, it remained visible in Past bookings with the cancelled status. A second disposable CUSTOMER with no reservations saw the real empty state. At 390px mobile width the persisted-reservation view had no horizontal overflow. Both temporary accounts and the one test reservation were deleted by exact identity after verification.

### F-03 — Resolved (High): Owner screens could not persist listings, availability or booking decisions

**Affected files:** [OwnerPages.jsx](./frontend/src/pages/OwnerPages.jsx), [marketplaceService.js](./frontend/src/api/marketplaceService.js), and [App.jsx](./frontend/src/App.jsx).

**Implementation:** Connected owner listings to authenticated `GET /api/spaces/owner/me`, `POST /api/spaces`, and `PUT /api/spaces/{id}` with backend DTO fields; added a protected edit route. Connected weekly schedules to `GET/PUT /api/spaces/{id}/availability/schedule`, blocked periods to list/create/delete endpoints, and owner reservation views/status actions to paginated `GET /api/spaces/{id}/reservations` and `PUT /api/reservations/{id}/status`. All requests use the existing Axios JWT interceptor and OWNER route guard. Submissions are guarded against duplicate requests, and server errors/successes are surfaced.

**Live verification:** Disposable owner created space ID 4, edited it, reloaded and confirmed the new name persisted. Saved and reloaded a weekday schedule and a blocked time/reason. A disposable customer then created two pending reservations for that space; owner confirmation and rejection changed their statuses and the outcomes survived reload. A different OWNER account’s attempts to edit the space and set reservation status each returned 403. Temporary owner, customer, listing, availability records and reservations were removed by verified exact-identity cleanup after testing.

**Limitation:** The backend space request DTO has no photo URL/image field or image-upload endpoint. The existing photo input is now explicitly marked unsupported rather than implying that a photo is saved. Owner dashboard aggregate figures are computed from owner-space reservations returned by the backend.

### F-04 — Resolved (Medium): Backend suite initially failed on unused test stubs

**Affected file:** [AvailabilityServiceTest.java](./backend/src/test/java/com/spaceora/availability/service/AvailabilityServiceTest.java)

**Reproduction:** From the top-level `backend` folder, set `JAVA_HOME` to the installed JDK 21 and run `.\mvnw.cmd test`.

**Baseline observed:** Seven tests errored with `UnnecessaryStubbingException`: `addBlockedPeriod_noOverlap_succeeds`, `addBlockedPeriod_overlapsExisting_throws`, `addBlockedPeriod_nonOwner_throws`, `addBlockedPeriod_endBeforeStart_throws`, `setSchedule_closeBeforeOpen_throws`, `setSchedule_duplicateDays_throws`, and `removeBlockedPeriod_wrongSpace_throws`. Those test cases stubbed `spaceRepo.existsById(1L)`, while the ownership-check path uses `findById`; the unused stubs caused Mockito’s strict test extension to fail.

**Fix and verification:** Removed only those seven unused stubs; validation and assertions were unchanged. `.\mvnw.cmd -Dtest=AvailabilityServiceTest test` passed 10/10. The subsequent full `.\mvnw.cmd test` passed with 79 tests, 0 failures and 0 errors. `FlywayMigrationIntegrationTest` still reports 0 tests because Testcontainers could not detect Docker.

### F-05 — Resolved (Medium): Documented seed credentials initially failed

**Affected files:** [V6 seed migration](./backend/src/main/resources/db/migration/V6__seed_dev_data.sql), [V7 correction](./backend/src/main/resources/db/migration/V7__correct_dev_seed_password_hashes.sql), and [README.md](./README.md).

**Baseline reproduction:** POST `/api/auth/login` with `customer@spaceora.dev` / `Customer1!`, as listed in the README, against the running local database.

**Baseline observed:** HTTP 401, although the seed user row existed. The V6 hashes did not match their documented passwords.

**Fix and verification:** Added V7 to replace only each seed user’s unchanged V6 hash, preserving accounts whose passwords have already been changed. Applied V7 to PostgreSQL; health remained `UP`, and the documented CUSTOMER, OWNER and ADMIN credentials each returned HTTP 200 with the expected role.

### F-06 — Resolved (Medium): PostgreSQL migration integration test initially did not run

**Affected areas:** Testcontainers/Docker test configuration and `FlywayMigrationIntegrationTest`.

**Initial observed error:** Docker CLI reported Docker Desktop 4.95.0 / Engine 29.8.2, but Testcontainers 1.19.8 (bundling docker-java 3.3.6) initially failed its Npipe discovery probe with `BadRequestException (Status 400)` and an `/info` response whose server identity/version fields were empty; the response label referenced `docker_cli`. The assumption in `FlywayMigrationIntegrationTest` then caused JUnit to abort before tests ran, so Maven reported 0 tests. Setting `DOCKER_HOST` to the Docker Desktop context pipe alone did not fix that client negotiation.

**Environment correction and verification:** Docker’s reported minimum API version is 1.40. Running the wrapper with `-Dapi.version=1.41` made Testcontainers discover `npipe:////./pipe/docker_engine`. The targeted selector then ran 2/2 tests successfully. Logs show a fresh PostgreSQL 16 container and successful application of migrations V1 through V7. The final full suite with the same setting ran 82 tests, 0 failures, 0 errors and 0 skipped. Project README documents `./mvnw test`; on this Windows/Docker combination use `$env:JAVA_HOME='C:\Program Files\Java\jdk-21'; .\mvnw.cmd '-Dapi.version=1.41' test`. No manual migration-test rerun is required in this environment. The live application’s PostgreSQL 16.15 startup still emits Flyway 9.22.3’s compatibility advisory that PostgreSQL 16 is newer than its tested maximum (15); the migration integration itself passed against PostgreSQL 16.

### F-07 — Resolved (Low): Frontend backend-status documentation was stale

**Affected files:** [frontend README](./frontend/README.md), [OwnerPages.jsx](./frontend/src/pages/OwnerPages.jsx)

**Baseline observed:** Documentation incorrectly said the backend controllers were absent. The README now documents that customer and owner screens use the real APIs, notes that photo URLs/uploads are not supported by the space API, and distinguishes local saved-space preferences from backend data. The owner pages no longer display a preview-only notice. Frontend lint and production build passed after integration.

## Browser and backend evidence

- Screenshots were captured in the earlier QA thread: desktop Explore with filter controls; mobile Explore empty state; mobile My Bookings empty state; mobile booking-preview confirmation.
- The browser showed expected HTTP 409, HTTP 422 and connection-refused console entries for deliberately rejected/error cases; successful booking generated no console error. A temporary customer-to-owner navigation attempt in earlier QA was correctly redirected to `/forbidden`.
- Follow-up browser check: changing City to London and submitting the Explore form returned the persisted “Canary Wharf Focus Room” (ID 1); opening `/spaces/1` rendered the API DTO fields. An unknown ID showed the not-found state and generated the expected HTTP 404 console entry. At 390px mobile width, Explore and detail had no horizontal overflow. Initial loading and backend-unavailable error states were also observed.
- For the booking follow-up, backend was started temporarily against local PostgreSQL. It returned health `UP`; booking ID 4 was visible from the authenticated customer's `/api/reservations/my`. The test reservation and disposable customer account were then removed with exact identity checks, and the backend was stopped.
- For My Bookings, the backend was started against the same local PostgreSQL. A browser-created reservation (ID 5) appeared with its actual DTO fields; the browser cancellation updated it to CANCELLED and a fresh load confirmed persistence. A second customer with no reservations saw the empty state. The disposable accounts/reservation were removed by exact identity after verification. `npm run lint` passed without warnings; `npm run build` passed; `.\mvnw.cmd -Dtest=ReservationServiceTest test` passed 16/16.
- For owner integration, live UI calls created/edited a disposable owner space, persisted weekday hours and a blocked period, and loaded two customer reservations. Confirmation/rejection returned successful backend DTOs and persisted after reload. A different OWNER’s attempted space update and reservation status update both returned 403. Exact QA database rows were removed transactionally. The backend test selector covering space controller/service, availability service and reservation service passed 53/53; frontend lint/build passed.
- Backend health returned `UP`. The PostgreSQL-backed smoke flow registered and logged in disposable customer/owner accounts, created and edited a disposable space, configured availability, created/listed/confirmed/cancelled reservations, verified overlap and authorization failures, checked invalid/past/blocked slots, and then removed all QA rows.
- Before V7, the documented seed customer login returned HTTP 401. After V7, all three documented seed accounts returned HTTP 200.
- The top-level workspace has no `.git` root. A separate nested `Spaceora/` checkout is on branch `main` and has existing modifications in `backend/src/test/java/com/spaceora/availability/service/AvailabilityServiceTest.java` and four frontend files. Those nested files are not the live top-level frontend/backend tested here.

## Final browser verification addendum — 2026-10-18

No application code was changed. From `backend`, started the documented service with `.\mvnw.cmd spring-boot:run`; `GET /actuator/health` returned `{"status":"UP"}`. The shared browser initially had an expired owner session because the backend was stopped; logging in again with the documented development OWNER credential restored the session. Browser checks below used the real backend except where HTTP 503 is explicitly identified as injected. Afterward, stopped the QA-started Java process (PID 32092) and verified no listener remained on port 8080. This pass performed only logins and GET requests against seeded data; it created no QA database records.

| Check | Result | Evidence |
|---|---|---|
| Owner API error display and retry — desktop | **PASS** | At 1440×900, browser route interception made both development-mode GET calls to `/api/spaces/owner/me` return simulated HTTP 503. The alert displayed the injected message `QA simulated owner service outage` and `Try again`. Disabling the injected failure and clicking the action fetched actual backend data and rendered “Canary Wharf Focus Room”. Document width 1425px; no horizontal overflow. The 503 console entries were expected test injection, not backend logs. |
| Owner API error display and retry — mobile | **PASS** | Repeated at 390×844. The injected error and retry action appeared; retry loaded the actual owner listing. Document width 375px; no horizontal overflow. |
| OWNER blocked from customer-only route — desktop | **PASS** | Authenticated as documented OWNER `owner@spaceora.dev`; navigating to `/my-bookings` redirected to `/forbidden` and displayed “That page isn’t available to this account.” |
| OWNER blocked from customer-only route — mobile | **PASS** | Same authenticated role-guard journey at 390×844; `/forbidden` rendered without observed horizontal overflow. |
| CUSTOMER blocked from owner-only route — desktop | **PASS** | Logged in as documented CUSTOMER `customer@spaceora.dev`; navigating to `/owner/dashboard` redirected to `/forbidden` and displayed the role-denial message. |
| CUSTOMER blocked from owner-only route — mobile | **PASS** | Repeated at 390×844; `/forbidden` rendered at 390px document width, with no horizontal overflow. |
| Owner binary photo upload requirement | **PASS — not an MVP requirement** | `docs/requirements.md` explicitly classifies file/image upload as out of scope and says actual upload service is post-MVP. Binary upload should not block MVP under the current requirements. The same requirements call for customers to view photos and describe image URLs stored as strings; however, the documented POST/PUT space request shape does not include image fields, so owner-managed image-URL persistence is **NOT VERIFIED** and requires scope clarification only if URL entry is expected for MVP. |
| Actual backend-generated owner 5xx | **NOT VERIFIED** | The UI’s handling was checked using browser-injected 503s, then recovery against the real API. No server-generated 5xx was induced. |

### Previous-pass remaining verification (superseded where updated below)

| Check | Result | Reason / evidence status |
|---|---|---|
| Rapid duplicate-click handling in booking UI | **PASS — updated in this pass** | See final targeted verification below. |
| Booking responses for HTTP 400, 401, 403, 404, 500, or malformed/unexpected 201 | **PASS — simulated browser responses; updated in this pass** | All named cases were triggered through browser-intercepted responses; see below for status-specific UI results and 401 redirect. These were simulated responses, not server-generated booking errors. |
| Server-generated owner 5xx response | **NOT VERIFIED end-to-end — updated in this pass** | The API can be made to return an actual generic 500 using invalid page input, revealing a 500-vs-client-error defect. Owner UI 500/error-retry behavior passed with injected responses, but the exact server-generated 500 was not verified end-to-end in the page. |
| Concurrent/repeated owner status actions | **PASS — updated in this pass** | Competing real API updates returned one 200 and one 422; the owner UI also disabled both actions during an in-flight request and sent one PUT after a second attempted action. |
| Full loading/empty/success/error matrix across every page at desktop and mobile | **NOT VERIFIED** | Selected owner listing error/retry and role denial states were tested at both widths in this pass. Prior checks cover selected search, booking, empty, owner availability and dashboard states, but not every combination/page-size pair. |
| Frontend automated test suite | **NOT VERIFIED / unavailable** | No full frontend test suite is present. Lint and production build passed in the final review. |
| Owner entry/persistence of image URLs | **NOT VERIFIED — scope-dependent** | Binary upload is expressly post-MVP. The functional requirements mention URL storage, while the current documented space create/update request body omits images. Clarify whether owner URL entry is an MVP acceptance criterion; do not conflate it with binary upload. |

The ten original functional journeys are the first ten rows in the test-results table. Journeys 8 and 9 have prior backend API 403 evidence and browser route-guard evidence in both desktop and mobile layouts; this does not replace the earlier API-level ownership checks.

## Final targeted checks — 2026-10-18

No application code was changed. The documented backend was run locally and stopped after the checks. Browser-intercepted HTTP responses below were simulations unless expressly identified as a real API response.

| Check | Result | Evidence |
|---|---|---|
| Booking double-submit protection | **PASS** | Signed-in CUSTOMER opened `/spaces/1/book`, selected future date 2026-10-20, submitted once, then immediately dispatched a second form-submit event while the first POST was held. The UI disabled the submit button (`Submitting booking request…`); exactly one `POST /api/reservations` was observed. Releasing the held request with a simulated 409 showed the expected conflict message. No reservation was intended by this test; a real row from an interrupted earlier route attempt was identified and cleaned (see cleanup). |
| Booking POST HTTP 400 | **PASS — simulated browser response** | Simulated response body `{message:"QA invalid booking"}` produced `Check your booking details: QA invalid booking`. |
| Booking POST HTTP 401 | **PASS — session-expiry behavior** | Simulated 401 caused the shared Axios interceptor to clear the customer session and redirect to `/login?next=%2Fspaces%2F1%2Fbook`. One POST was observed. The booking-page-specific 401 alert was not retained because the protected route redirected immediately; user is returned to sign-in. |
| Booking POST HTTP 403 | **PASS — simulated browser response** | Produced `A customer account is required to request a booking.` |
| Booking POST HTTP 404 | **PASS — simulated browser response** | Produced `This space is no longer available. Return to Explore to choose another.` |
| Booking POST HTTP 500 | **PASS — simulated browser response** | Simulated `{message:"QA server failure"}` was surfaced verbatim. |
| Booking POST malformed HTTP 201 | **PASS — simulated browser response** | A 201 response with `{}` did not show success or navigate to confirmation; it displayed `The server did not return a valid reservation confirmation.` |
| Repeated owner status submission in UI | **PASS — browser with intercepted response** | On a QA-only pending reservation, held the first `PUT /api/reservations/14/status`, attempted Reject while Confirm was pending, and observed both buttons disabled and exactly one PUT. A simulated successful confirmation updated the visible status. This UI simulation did not mutate the backend row. |
| Competing status changes against backend | **PASS — live API** | Two near-simultaneous owner requests for QA reservation 13, one `CONFIRMED` and one `REJECTED`, returned HTTP 200 / `CONFIRMED` and HTTP 422 / `Can only confirm/reject a PENDING reservation; current status: CONFIRMED`. A subsequent GET returned `CONFIRMED`. Thus at most one competing terminal decision succeeded in this probe. |
| Owner error/retry UX at desktop and mobile | **PASS — simulated browser 500** | Injected HTTP 500 responses for both dev-mode GET calls to `/api/spaces/owner/me`. At 1440×900 and 390×844 the UI showed the injected server message and Try again; retry loaded actual backend listings. Document widths were 1425px and 375px respectively; no horizontal overflow. |
| Booking layout/error state — desktop | **PASS** | At 1440×900, `/spaces/1/book` loaded the real space and booking form. Simulated HTTP 400 displayed the validation message. Document width 1425px; no horizontal overflow. |
| Booking layout/error state — mobile | **PASS** | At 390×844, the real booking form loaded and a simulated HTTP 500 displayed its error. Document width 375px; no horizontal overflow. |
| Customer My Bookings layout with persisted data — desktop/mobile | **PASS** | Signed-in CUSTOMER’s persisted backend reservations rendered at both 1440×900 (1425px document) and 390×844 (375px document), with no horizontal overflow. |
| Owner bookings layout with backend data — desktop/mobile | **PASS** | Real owner reservations and statuses loaded at both widths (1440/1425 and 390/375 viewport/document widths); no horizontal overflow. |
| Actual owner API 500 and UI handling of that exact response | **NOT VERIFIED end-to-end; API failure reproduced** | An OWNER-authenticated `GET /api/spaces/1/reservations?page=-1&size=100` returned an actual server HTTP 500 with `{"status":500,"error":"Internal Server Error","message":"An unexpected error occurred",...}`. UI 500 handling/retry passed with injected HTTP 500 at `/api/spaces/owner/me`, but browser routing attempts to substitute the actual invalid-page request did not reliably deliver that response to the active React request (development StrictMode issues duplicate loads). Therefore the exact real 500 → owner UI path remains unverified. |

### Reproducible defect found — negative owner reservation page returns HTTP 500

- **Result: FAIL** for invalid-page API error handling.
- **Reproduction:** Authenticate as the documented OWNER, then send `GET /api/spaces/1/reservations?page=-1&size=100`. The server returns HTTP 500 and the generic `An unexpected error occurred` response. The owner controller passes the negative value to `PageRequest.of`, whose `IllegalArgumentException` reaches the catch-all exception handler.
- **Impact:** Malformed pagination input is treated as an internal server error rather than a client validation error; it can also produce a backend 500 log/alert. Normal frontend requests use page 0 and were unaffected in these checks.
- **Smallest safe fix proposal (not applied):** In `SpaceReservationController.listForSpace`, reject `page < 0` explicitly with the project’s client-validation exception before calling `PageRequest.of` (or add equivalent parameter validation and a specific handler). Return the repository-standard 400/422 validation response. Add a controller integration test asserting the negative-page request does not return 500.
- No other reproducible application defect was found in the targeted UI checks. No fix was applied, consistent with the instruction to report/propose first.

### QA data/process cleanup

Four temporary reservation rows were created while exercising the flows: IDs 11–14. ID 11 was an untagged row from an interrupted intercepted booking submission; exact owner API/DB inspection matched its owner space, customer, interval (2026-10-20 10:00–11:00 UTC), pending status and creation timestamp to that test. IDs 12–14 carried unique QA notes and were used for owner-status tests. All four were deleted with predicates including the exact IDs, space/customer IDs, intervals, statuses, notes and creation timestamps. Follow-up SQL returned zero rows for IDs 11–14; seeded demo reservation ID 1 remained `PENDING`. No test owner/customer/space was created. The QA backend process was stopped; verification found no listener on port 8080.

### Still NOT VERIFIED

- The exact actual server-generated owner 500 response has not been displayed by the owner React page; only direct API 500 plus injected UI 500/retry were separately verified.
- HTTP booking responses above used injected browser responses; production-generated booking 400/403/404/500 and malformed-201 paths were not caused by the backend. The 401 check verified the session-expiry redirect.
- All page states across every desktop/mobile viewport combination are not covered. This pass checked booking form, customer bookings with persisted data, owner bookings with data, and owner listing error/retry at the stated sizes. Real empty-state behavior exists from the previous verified pass but was not repeated at both widths here.
- No full frontend automated test suite exists.

## Remaining risks and limitations

- The negative-page owner reservation API defect is reproducible and remains unfixed pending approval; no application code was changed in this QA task.
- Binary photo upload is post-MVP and is not a release blocker. Image URL authoring may be an MVP scope gap and needs product clarification only if required.
- Browser and API findings remain distinctly labeled above; unverified cases are not counted as passes.
- Earlier Docker discovery failure is resolved for this environment using Docker API version 1.41. On another host, follow the README test command and ensure Docker is running; if Testcontainers reports zero tests, treat the migration check as unexecuted, not passed.

## Fix follow-up

Post-report fixes include removing only unused availability-test stubs, adding a guarded development-seed hash correction migration, correcting backend-status documentation, and integrating Explore/search/detail, customer booking creation, My Bookings/cancellation, and owner management. The dashboard’s undefined guard was removed and browser-verified. The concurrent booking race was fixed by locking the existing space row, covered by an eight-request integration test, and rechecked against live PostgreSQL. The Docker discovery issue was addressed for this environment with API version 1.41; all seven migrations were applied by the two migration tests. Prior QA probe rows 8–10 were deleted. In this latest targeted pass, exact temporary reservation rows 11–14 were also removed after verifying each row’s identifying fields. Seeded demo reservation ID 1 remains. No backend validation was weakened and no application code was changed in this pass. The temporary backend is stopped. The negative-page HTTP 500 defect and the remaining unverified checks are documented above; binary image upload is explicitly post-MVP and is not a blocker.

## Pricing correction — 2026-10-18

### Changes

- Added Flyway `V8__correct_dev_seed_hourly_rates.sql`; V6 and all previously applied migrations remain unchanged.
- The V8 updates are guarded by the seeded OWNER email/role, exact seeded space name, description, type, city, address, original hourly and daily rates, capacity, and active state. Canary Wharf is changed from `15.00` to `1200.00`; Shoreditch is changed from `8.00` to `250.00`. No daily-rate values are written.
- Updated currency and hourly-rate examples in `docs/requirements.md`, `docs/data-dictionary.md`, `docs/api-contract.md`, and `docs/testing-plan.md` to INR (₹).
- Kept the homepage design fixtures’ existing `price` property and content and added `hourlyRate` as a compatible alias, so the card’s required field is present without breaking booking fixture consumers that still read `price`.
- No stylesheet, layout, booking calculation, owner form, API contract shape, or authorization code was changed.

### Verification evidence

| Check | Result | Evidence |
|---|---|---|
| Fresh database migration chain | **PASS — Testcontainers/PostgreSQL** | `FlywayMigrationIntegrationTest.allMigrationsApplyCleanly_schemaMatchesV1Spec` executed the full V1–V8 chain. The resulting seeded hourly values were `1200.00` and `250.00`. |
| Upgrade database with V6 already applied | **PASS — Testcontainers/PostgreSQL** | The migration test applied migrations through V7, then ran V8. A Shoreditch seed row with its original V6 data changed to `250.00`; a Canary Wharf seed row whose hourly price had been owner-modified to `1555.55` remained `1555.55`. A separate owner-configured listing at `777.77` also remained unchanged. |
| Existing development database | **PASS — live PostgreSQL** | Applied with `.\mvnw.cmd '-Dapi.version=1.41' spring-boot:run` from `backend` (temporary `JWT_SECRET` environment value set for startup). Flyway history records V8 successful. Read-only follow-up query returned exactly two spaces: Canary Wharf Focus Room `1200.00` hourly / `120.00` daily and Shoreditch Hot Desk `250.00` hourly / `60.00` daily. There were no non-seed owner spaces in this database to compare; custom-price preservation is verified by the isolated migration test above. |
| Space API decimal create/update | **PASS — H2 integration test** | Added `createAndUpdateSpace_preservesDecimalHourlyRates`: create response returned `1234.56`, update response and subsequent GET returned `1357.89`. |
| Whole-hour booking calculation | **PASS — service test** | `createReservation_valid_calculatesTotal` uses `1200.00` hourly for the existing two-hour scenario and asserts `2400.00`. The existing server calculation and two-decimal `HALF_UP` rounding were not changed. Booking confirmation displays the returned `reservation.totalAmount`; the pre-submit review remains an estimate. |
| Homepage preview cards | **PASS — browser** | At `http://localhost:5173/`, the three rendered preview rates were `₹800 / hour`, `₹250 / hour`, and `₹300 / hour`; none rendered `NaN`. Existing fixture prices/content remain unchanged. |
| Explore listing | **PASS — browser + live API** | With the backend running, `/explore?city=London` loaded the real Canary Wharf record and displayed `₹1,200 / hour`. Live `GET /api/spaces?city=London&page=0&size=10` returned hourly rates `1200.00` and `250.00`; `GET /api/spaces/1` returned hourly `1200.00` and unchanged daily `120.00`. |
| Detail, owner listing, and booking review price display | **PASS — source/API inspection; browser NOT VERIFIED in this run** | The detail and owner listing components format backend `hourlyRate` with the rupee symbol and per-hour label; the owner form still submits `Number(form.hourlyRate)`. Booking review reads the backend rate for its estimate and confirmation uses the server response. Their UI code/styles were not changed. Browser checks for these three pages were not completed in this pricing task. |
| Frontend lint and production build | **PASS** | `npm run lint` completed without diagnostics; `npm run build` completed with Vite production output. |
| Full backend suite | **PASS — 84 executed** | `.\mvnw.cmd '-Dapi.version=1.41' test`: 84 tests, 0 failures, 0 errors, 0 skipped. This includes 3 executed Flyway/Testcontainers tests. |
| Focused backend suite | **PASS — 38 executed** | `.\mvnw.cmd '-Dapi.version=1.41' '-Dtest=FlywayMigrationIntegrationTest,SpaceControllerIntegrationTest,ReservationServiceTest' test`: 38 tests, 0 failures, 0 errors, 0 skipped. |

The temporary backend was stopped after the live API/browser check; port 8080 had no listening process afterward. The persistent development database was not dropped or recreated. The remaining pre-existing negative-pagination HTTP 500 defect documented above is outside this pricing change and remains unresolved.

## Space photo fix — 2026-10-18

### Root cause and changes

- Homepage fixtures supplied their local photo URL in `image`, but `WorkspaceCard` rendered only `thumbnailUrl`; this made all featured fixture cards show “Photo unavailable” despite the asset files being present.
- Backend Explore listings used `thumbnailUrl` from the first `space_images` row. The two seeded backend spaces had no rows in `space_images`, so their thumbnail URLs were null as well.
- `frontend/src/data/spaces.js` retains existing fixture fields and adds `thumbnailUrl: space.image` for compatible consumers.
- `frontend/src/components/WorkspaceCard.jsx` now renders `thumbnailUrl` or, when absent, `image`. Failed image requests switch to a mint-tinted “Workspace photo unavailable” panel instead of leaving a broken-image icon. Card dimensions/layout/navigation and stylesheet are unchanged.
- Added `V9__add_demo_space_images.sql` to assign existing local public assets to the unchanged seeded meeting room and hot-desk records. Inserts require the exact seeded owner, identifying space details, current approved hourly/daily rates, capacity and active state, and run only where that space has no existing image. No existing owner-added image is overwritten.

### Browser and migration evidence

| Check | Result | Evidence |
|---|---|---|
| Homepage preview photos | **PASS — browser, actual image decode** | `http://localhost:5173/` loaded all three featured fixture photos: Atrium `/images/space-13.jpeg` (natural width 1376), Daylight `/images/space-02.png` (512), and Quiet Chapter `/images/space-09.png` (512). |
| All demo fixture image URLs | **PASS — browser, actual image decode** | Verified all nine primary fixture URLs returned decodable images: `/images/space-13.jpeg`, `/images/space-02.png`, `/images/space-09.png`, `/images/space-08.png`, `/images/space-04.png`, `/images/space-10.jpeg`, `/images/space-15.png`, `/images/space-16.png`, and `/images/space-18.png`. |
| Explore photos from backend records | **PASS — browser + API** | The backend API returned the seeded records’ persisted image URLs. Explore with city=London and “Any group size” showed Canary Wharf `/images/space-13.jpeg` (1376) and Shoreditch `/images/space-02.png` (512); both decoded successfully. `GET /api/spaces` returned the same URLs in `thumbnailUrl`. |
| Space detail photo | **PASS — browser** | `/spaces/1` displayed and decoded `/images/space-13.jpeg` in the detail gallery (natural width 1376). |
| Failed image fallback | **PASS — browser, forced request failure** | Aborted the homepage card’s `/images/space-13.jpeg` request. The card rendered `Workspace photo unavailable` in the fallback panel, with zero broken `<img>` elements. Removed the interception and reloaded; the photo decoded normally. |
| V9 fresh and upgrade migration checks | **PASS — Testcontainers/PostgreSQL** | `.\mvnw.cmd '-Dapi.version=1.41' '-Dtest=FlywayMigrationIntegrationTest' test`: 3 executed, 0 failures/errors/skips. Fresh V1–V9 applies and asserts both image URLs. Upgrade test verifies a modified-rate seed row and custom owner listing receive no seeded images; the unchanged Shoreditch record receives its image. |
| Frontend lint/build | **PASS** | `npm run lint` and `npm run build` both passed after the card/fixture changes. |

The temporary backend was shut down after browser verification. No upload functionality or image-processing behavior was added. No styling files were modified.

## Owner reservation pagination validation — 2026-10-18

### Fix

`SpaceReservationController.listForSpace` now rejects `page < 0` and `size < 1` with `BookingValidationException` before constructing a `PageRequest`. The existing exception handler returns HTTP 422 with the project's standard JSON error shape. Existing owner/admin authorization and the maximum page-size cap of 100 are unchanged.

### Verification

| Check | Result | Evidence |
|---|---|---|
| Negative page regression | **PASS — controller integration test + live API** | `SpaceControllerIntegrationTest.listReservations_invalidPagination_returnsClientValidationResponse` checks the 422 response and message. Live authenticated `GET /api/spaces/1/reservations?page=-1&size=100` returned `422` with `{"status":422,"error":"Unprocessable Entity","message":"page must be greater than or equal to 0",...}`; it no longer returns HTTP 500. |
| Invalid page sizes | **PASS — controller integration test + live API** | Both `size=0` and `size=-1` returned HTTP 422 with `size must be greater than 0`. |
| Normal pagination and upper cap | **PASS — controller integration test + live API** | `page=0&size=20` returned HTTP 200 and page size 20. `page=0&size=101` returned HTTP 200 with effective page size 100. |
| Focused backend tests | **PASS — 20 executed** | `.\mvnw.cmd '-Dapi.version=1.41' '-Dtest=SpaceControllerIntegrationTest' test`: 20 tests, 0 failures, 0 errors, 0 skipped. |
| Full backend suite | **PASS — 85 executed** | `.\mvnw.cmd '-Dapi.version=1.41' test`: 85 tests, 0 failures, 0 errors, 0 skipped. |
| Frontend lint and production build | **PASS** | `npm run lint` and `npm run build` both passed. |

The live requests were made against the top-level backend with the seeded OWNER account. The test backend was temporary; the development database was not dropped or recreated. The pagination issue is resolved. This targeted fix does not close the other unverified browser/error-state items identified elsewhere in this report, so release readiness remains conditional.
