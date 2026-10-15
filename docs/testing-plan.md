# Spaceora — Testing Strategy

## Philosophy

- Test behaviour, not implementation details.
- Unit tests use Mockito to isolate service logic from infrastructure.
- Integration tests use `@SpringBootTest` with a real database (TestContainers PostgreSQL).
- Every booking business rule has a dedicated test case.
- Tests are named using the pattern `methodUnderTest_scenario_expectedOutcome`.

---

## Coverage Targets

| Layer | Target |
|---|---|
| Service layer overall | ≥ 80% line coverage |
| ReservationService (booking rules) | 100% branch coverage |
| Controller layer | Happy-path + at least one error-path per endpoint |

---

## Unit Tests (JUnit 5 + Mockito)

All unit tests mock repositories and collaborators; no Spring context is loaded.

### ReservationService

| Test | Scenario | Expected Outcome |
|---|---|---|
| `createReservation_pastDate_throws` | `startDatetime` is in the past | Throws `BookingValidationException` |
| `createReservation_outsideOpeningHours_throws` | Start or end falls outside the space's `AvailabilitySchedule` | Throws `BookingValidationException` |
| `createReservation_dayWithNoSchedule_throws` | Requested day has no `AvailabilitySchedule` entry (space closed) | Throws `BookingValidationException` |
| `createReservation_inBlockedPeriod_throws` | Requested range overlaps a `BlockedPeriod` | Throws `BookingValidationException` |
| `createReservation_overlapsExistingReservation_throws` | A `PENDING` or `CONFIRMED` reservation exists for the same space and overlapping time | Throws `BookingConflictException` |
| `createReservation_valid_calculatesTotal` | All validations pass | `totalAmount` = `hourlyRate × durationInHours`; reservation saved with status `PENDING` |
| `createReservation_valid_withDailyRate_calculatesTotal` | Full-day booking uses dailyRate | `totalAmount` = `dailyRate` |
| `cancelReservation_ownedByCustomer_succeeds` | Customer cancels their own `PENDING` reservation before start | Status transitions to `CANCELLED` |
| `cancelReservation_notOwner_throws` | A different customer attempts to cancel | Throws `AuthorizationException` |
| `cancelReservation_alreadyStarted_throws` | `startDatetime` is in the past | Throws `BookingValidationException` |
| `cancelReservation_terminalState_throws` | Reservation is already `CANCELLED` or `REJECTED` | Throws `InvalidStatusTransitionException` |
| `transitionStatus_pendingToConfirmed_succeeds` | Owner confirms a `PENDING` reservation on their space | Status transitions to `CONFIRMED` |
| `transitionStatus_pendingToRejected_succeeds` | Owner rejects a `PENDING` reservation | Status transitions to `REJECTED` |
| `transitionStatus_cancelledToConfirmed_throws` | Attempt to confirm an already `CANCELLED` reservation | Throws `InvalidStatusTransitionException` |
| `transitionStatus_confirmedToPending_throws` | Attempt to revert `CONFIRMED` to `PENDING` | Throws `InvalidStatusTransitionException` |
| `transitionStatus_ownerOnWrongSpace_throws` | Owner tries to confirm a reservation on another owner's space | Throws `AuthorizationException` |

### SpaceService

| Test | Scenario | Expected Outcome |
|---|---|---|
| `createSpace_notOwnerRole_throws` | User with role `CUSTOMER` calls create | Throws `AuthorizationException` |
| `createSpace_ownerRole_succeeds` | User with role `OWNER` calls create | Space saved; `ownerId` set from authenticated user |
| `updateSpace_notOwner_throws` | An `OWNER` attempts to update a space they do not own | Throws `AuthorizationException` |
| `updateSpace_ownerOfSpace_succeeds` | The owning OWNER updates their space | Space updated; returned DTO reflects changes |
| `deleteSpace_asAdmin_succeeds` | `ADMIN` deletes any space | Space deleted regardless of ownership |
| `deleteSpace_nonOwner_throws` | An OWNER deletes another OWNER's space | Throws `AuthorizationException` |
| `deactivateSpace_togglesIsActive` | Owner deactivates an active space | `isActive` set to `false`; not returned in public search |

### AvailabilityService

| Test | Scenario | Expected Outcome |
|---|---|---|
| `addBlockedPeriod_noOverlap_succeeds` | New blocked period does not overlap existing ones | Saved successfully |
| `addBlockedPeriod_overlapsExisting_throws` | New blocked period overlaps an existing one | Throws `BookingConflictException` |
| `computeAvailability_closedDay_omitted` | Date has no schedule entry | Day omitted from response |
| `computeAvailability_includesBlockedPeriods` | Date has both a schedule and a blocked period | Both returned in merged view |

---

## Integration Tests (@SpringBootTest + TestContainers)

Integration tests spin up a real PostgreSQL container via TestContainers. Flyway migrations run automatically before tests. Seed data from `V6__seed_dev_data.sql` is available in each test; any mutations are rolled back or the container is reset between test classes.

### Auth Controller

| Test | Scenario | Expected HTTP |
|---|---|---|
| `register_valid_returns201` | Valid registration payload | 201, body contains `id`, `email`, `role` |
| `register_duplicateEmail_returns409` | Email already registered | 409 |
| `register_missingEmail_returns400` | `email` field absent | 400 |
| `register_weakPassword_returns400` | Password too short | 400 |
| `login_validCredentials_returnsJwt` | Correct email + password | 200, body contains `token`, `userId`, `role` |
| `login_wrongPassword_returns401` | Correct email, wrong password | 401 |
| `login_unknownEmail_returns401` | Email not registered | 401 |

### User Controller

| Test | Scenario | Expected HTTP |
|---|---|---|
| `getMe_authenticated_returns200` | Valid JWT | 200, body matches seed user |
| `getMe_noToken_returns401` | No Authorization header | 401 |
| `updateMe_valid_returns200` | Valid update payload | 200, updated fields reflected |

### Space Controller

| Test | Scenario | Expected HTTP |
|---|---|---|
| `listSpaces_noAuth_returns200` | Public request, no filter | 200, paginated list |
| `listSpaces_filterByCity_returnsFiltered` | `?city=London` | 200, only London spaces |
| `createSpace_asOwner_returns201` | OWNER JWT, valid payload | 201, `ownerId` = authenticated user |
| `createSpace_asCustomer_returns403` | CUSTOMER JWT | 403 |
| `createSpace_noAuth_returns401` | No token | 401 |
| `getSpace_existing_returns200` | Valid id | 200, full SpaceDto |
| `getSpace_notFound_returns404` | Unknown id | 404 |
| `updateSpace_ownOwner_returns200` | Owning OWNER JWT | 200 |
| `updateSpace_nonOwner_returns403` | Different OWNER JWT | 403 |
| `deleteSpace_asAdmin_returns204` | ADMIN JWT | 204 |

### Reservation Controller

| Test | Scenario | Expected HTTP |
|---|---|---|
| `createReservation_valid_returns201` | CUSTOMER JWT, available slot | 201, `totalAmount` correct, `status` = PENDING |
| `createReservation_overlap_returns409` | Slot already PENDING/CONFIRMED | 409 |
| `createReservation_pastDate_returns422` | Start datetime in the past | 422 |
| `createReservation_outsideHours_returns422` | Start before open time | 422 |
| `createReservation_asOwner_returns403` | OWNER JWT | 403 |
| `cancelReservation_ownCustomer_returns200` | Customer cancels their own reservation | 200, `status` = CANCELLED |
| `cancelReservation_wrongCustomer_returns403` | Different customer's JWT | 403 |
| `cancelReservation_alreadyStarted_returns422` | Reservation start is in the past | 422 |
| `listMyReservations_returns200` | CUSTOMER JWT | 200, paginated |
| `listSpaceReservations_asOwner_returns200` | Owning OWNER JWT | 200, paginated |
| `listSpaceReservations_asNonOwner_returns403` | Different OWNER JWT | 403 |
| `ownerConfirm_pendingReservation_returns200` | OWNER JWT, own space | 200, status CONFIRMED |
| `ownerConfirm_wrongOwner_returns403` | OWNER JWT, different space | 403 |

### Security Tests

| Test | Scenario | Expected HTTP |
|---|---|---|
| `protectedEndpoint_noToken_returns401` | Any protected endpoint, no header | 401 |
| `adminEndpoint_calledByCustomer_returns403` | CUSTOMER JWT on `/api/admin/**` | 403 |
| `adminEndpoint_calledByOwner_returns403` | OWNER JWT on `/api/admin/**` | 403 |
| `ownerEndpoint_calledByCustomer_returns403` | CUSTOMER JWT on `POST /api/spaces` | 403 |

### Admin Controller

| Test | Scenario | Expected HTTP |
|---|---|---|
| `listUsers_asAdmin_returns200` | ADMIN JWT | 200, paginated |
| `changeRole_asAdmin_returns200` | ADMIN JWT, valid role | 200, updated role |
| `changeRole_invalidRole_returns400` | Unknown role string | 400 |

---

## Concurrency Test

**Goal:** Verify that pessimistic locking prevents double-booking.

**Setup:**
- Use seed Space 1 (MEETING_ROOM, capacity 10) with an open slot.
- Two threads simultaneously POST to `POST /api/reservations` for the same space and identical time window using two different CUSTOMER JWTs.

**Assertion:**
- Exactly one thread receives HTTP 201.
- The other thread receives HTTP 409.
- Database contains exactly one reservation for that slot.

**Implementation note:** Use a `CountDownLatch` or `ExecutorService` with two tasks; assert results after both complete. This test is marked `@Tag("concurrency")` and may be excluded from standard CI if it requires a longer timeout.

---

## Test Utilities and Helpers

| Utility | Purpose |
|---|---|
| `TestDataFactory` | Static factory methods for creating test entities and DTOs |
| `JwtTestHelper` | Generates signed JWTs for seed users in integration tests |
| `AbstractIntegrationTest` | Base class with `@SpringBootTest`, `@Testcontainers`, shared container config |

---

## Seed Data Reference (used in integration tests)

| Entity | Details |
|---|---|
| User 1 | email: `customer@spaceora.dev`, role: `CUSTOMER` |
| User 2 | email: `owner@spaceora.dev`, role: `OWNER` |
| User 3 | email: `admin@spaceora.dev`, role: `ADMIN` |
| Space 1 | owner: User 2, type: `MEETING_ROOM`, city: `London`, hourlyRate: `15.00`, capacity: `10`, isActive: `true` |
| Space 2 | owner: User 2, type: `COWORKING`, city: `London`, hourlyRate: `8.00`, capacity: `4`, isActive: `true` |
| Reservation 1 | customer: User 1, space: Space 1, status: `PENDING`, future date |
