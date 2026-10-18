package com.spaceora.reservation.service;

import com.spaceora.availability.entity.AvailabilitySchedule;
import com.spaceora.availability.entity.BlockedPeriod;
import com.spaceora.availability.service.AvailabilityService;
import com.spaceora.exception.AuthorizationException;
import com.spaceora.exception.BookingConflictException;
import com.spaceora.exception.BookingValidationException;
import com.spaceora.exception.InvalidStatusTransitionException;
import com.spaceora.reservation.dto.ReservationDto;
import com.spaceora.reservation.dto.ReservationRequestDto;
import com.spaceora.reservation.entity.Reservation;
import com.spaceora.reservation.entity.ReservationStatus;
import com.spaceora.reservation.repository.ReservationRepository;
import com.spaceora.space.entity.Space;
import com.spaceora.space.entity.SpaceType;
import com.spaceora.space.repository.SpaceRepository;
import com.spaceora.user.entity.Role;
import com.spaceora.user.entity.User;
import com.spaceora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepo;
    @Mock SpaceRepository spaceRepo;
    @Mock UserRepository userRepo;
    @Mock AvailabilityService availabilityService;

    private ReservationService service;

    // Future datetimes for test reservations
    private static final Instant FUTURE_START = Instant.parse("2099-06-17T09:00:00Z"); // Tuesday ISO=2, domDay=1
    private static final Instant FUTURE_END   = Instant.parse("2099-06-17T11:00:00Z");

    @BeforeEach
    void setUp() {
        service = new ReservationService(reservationRepo, spaceRepo, userRepo, availabilityService);
    }

    // ---- helpers ----

    private User customer(Long id, String email) {
        return User.builder().id(id).email(email).passwordHash("x")
                .firstName("C").lastName("U").role(Role.CUSTOMER).build();
    }

    private User owner(Long id, String email) {
        return User.builder().id(id).email(email).passwordHash("x")
                .firstName("O").lastName("W").role(Role.OWNER).build();
    }

    private Space space(Long id, Long ownerId, BigDecimal rate) {
        return Space.builder().id(id).ownerId(ownerId).name("My Space")
                .type(SpaceType.MEETING_ROOM).city("London").address("1 St")
                .hourlyRate(rate).capacity(10).isActive(true).images(new ArrayList<>()).build();
    }

    private AvailabilitySchedule schedule(int domDay, LocalTime open, LocalTime close) {
        return AvailabilitySchedule.builder().id(1L).spaceId(1L)
                .dayOfWeek(domDay).openTime(open).closeTime(close).build();
    }

    private Reservation pendingReservation(Long id, Long spaceId, Long custId) {
        return Reservation.builder()
                .id(id).spaceId(spaceId).customerId(custId)
                .startDatetime(FUTURE_START).endDatetime(FUTURE_END)
                .totalAmount(new BigDecimal("30.00"))
                .status(ReservationStatus.PENDING).build();
    }

    private void mockValidSlot(Long spaceId, Space sp) {
        when(spaceRepo.findByIdForUpdate(spaceId)).thenReturn(Optional.of(sp));
        // 2099-06-17 is a Tuesday → ISO=2 → domDay=1
        when(availabilityService.findScheduleForDate(eq(spaceId), any(LocalDate.class)))
                .thenReturn(Optional.of(schedule(1, LocalTime.of(8, 0), LocalTime.of(20, 0))));
        when(availabilityService.findOverlappingBlockedPeriods(eq(spaceId), any(), any()))
                .thenReturn(List.of());
        when(reservationRepo.findOverlappingActiveWithLock(eq(spaceId), any(), any()))
                .thenReturn(List.of());
    }

    // ================================================================
    // createReservation tests
    // ================================================================

    @Test
    void createReservation_valid_calculatesTotal() {
        User cust = customer(1L, "cust@test.com");
        Space sp = space(1L, 10L, new BigDecimal("15.00"));
        when(userRepo.findByEmail("cust@test.com")).thenReturn(Optional.of(cust));
        mockValidSlot(1L, sp);

        Reservation saved = pendingReservation(100L, 1L, 1L);
        when(reservationRepo.save(any())).thenReturn(saved);
        when(spaceRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(sp));

        ReservationRequestDto dto = new ReservationRequestDto(1L,
                FUTURE_START.toString(), FUTURE_END.toString(), null);
        ReservationDto result = service.create(dto, "cust@test.com");

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.totalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void createReservation_pastDate_throws() {
        User cust = customer(1L, "cust@test.com");
        when(userRepo.findByEmail("cust@test.com")).thenReturn(Optional.of(cust));
        Space sp = space(1L, 10L, new BigDecimal("15.00"));
        when(spaceRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(sp));

        ReservationRequestDto dto = new ReservationRequestDto(1L,
                "2020-01-01T09:00:00Z", "2020-01-01T11:00:00Z", null);

        assertThatThrownBy(() -> service.create(dto, "cust@test.com"))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("future");
    }

    @Test
    void createReservation_dayWithNoSchedule_throws() {
        User cust = customer(1L, "cust@test.com");
        Space sp = space(1L, 10L, new BigDecimal("15.00"));
        when(userRepo.findByEmail("cust@test.com")).thenReturn(Optional.of(cust));
        when(spaceRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(sp));
        when(availabilityService.findScheduleForDate(eq(1L), any())).thenReturn(Optional.empty());

        ReservationRequestDto dto = new ReservationRequestDto(1L,
                FUTURE_START.toString(), FUTURE_END.toString(), null);

        assertThatThrownBy(() -> service.create(dto, "cust@test.com"))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void createReservation_outsideOpeningHours_throws() {
        User cust = customer(1L, "cust@test.com");
        Space sp = space(1L, 10L, new BigDecimal("15.00"));
        when(userRepo.findByEmail("cust@test.com")).thenReturn(Optional.of(cust));
        when(spaceRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(sp));
        // Schedule only 10:00-12:00 but booking is 09:00-11:00
        when(availabilityService.findScheduleForDate(eq(1L), any()))
                .thenReturn(Optional.of(schedule(1, LocalTime.of(10, 0), LocalTime.of(12, 0))));

        ReservationRequestDto dto = new ReservationRequestDto(1L,
                FUTURE_START.toString(), FUTURE_END.toString(), null);

        assertThatThrownBy(() -> service.create(dto, "cust@test.com"))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("opening hours");
    }

    @Test
    void createReservation_inBlockedPeriod_throws() {
        User cust = customer(1L, "cust@test.com");
        Space sp = space(1L, 10L, new BigDecimal("15.00"));
        when(userRepo.findByEmail("cust@test.com")).thenReturn(Optional.of(cust));
        when(spaceRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(sp));
        when(availabilityService.findScheduleForDate(eq(1L), any()))
                .thenReturn(Optional.of(schedule(1, LocalTime.of(8, 0), LocalTime.of(20, 0))));
        when(availabilityService.findOverlappingBlockedPeriods(eq(1L), any(), any()))
                .thenReturn(List.of(BlockedPeriod.builder().id(1L).spaceId(1L)
                        .startDatetime(FUTURE_START).endDatetime(FUTURE_END).build()));

        ReservationRequestDto dto = new ReservationRequestDto(1L,
                FUTURE_START.toString(), FUTURE_END.toString(), null);

        assertThatThrownBy(() -> service.create(dto, "cust@test.com"))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("blocked period");
    }

    @Test
    void createReservation_overlapsExistingReservation_throws() {
        User cust = customer(1L, "cust@test.com");
        Space sp = space(1L, 10L, new BigDecimal("15.00"));
        when(userRepo.findByEmail("cust@test.com")).thenReturn(Optional.of(cust));
        when(spaceRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(sp));
        when(availabilityService.findScheduleForDate(eq(1L), any()))
                .thenReturn(Optional.of(schedule(1, LocalTime.of(8, 0), LocalTime.of(20, 0))));
        when(availabilityService.findOverlappingBlockedPeriods(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(reservationRepo.findOverlappingActiveWithLock(eq(1L), any(), any()))
                .thenReturn(List.of(pendingReservation(5L, 1L, 2L)));

        ReservationRequestDto dto = new ReservationRequestDto(1L,
                FUTURE_START.toString(), FUTURE_END.toString(), null);

        assertThatThrownBy(() -> service.create(dto, "cust@test.com"))
                .isInstanceOf(BookingConflictException.class);
    }

    @Test
    void createReservation_notCustomer_throws() {
        User ownerUser = owner(10L, "owner@test.com");
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(ownerUser));

        ReservationRequestDto dto = new ReservationRequestDto(1L,
                FUTURE_START.toString(), FUTURE_END.toString(), null);

        assertThatThrownBy(() -> service.create(dto, "owner@test.com"))
                .isInstanceOf(AuthorizationException.class);
    }

    // ================================================================
    // cancelReservation tests
    // ================================================================

    @Test
    void cancelReservation_ownedByCustomer_succeeds() {
        User cust = customer(1L, "cust@test.com");
        when(userRepo.findByEmail("cust@test.com")).thenReturn(Optional.of(cust));

        Reservation r = pendingReservation(10L, 1L, 1L);
        when(reservationRepo.findById(10L)).thenReturn(Optional.of(r));
        when(reservationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Space sp = space(1L, 99L, BigDecimal.TEN);
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(sp));

        ReservationDto result = service.cancel(10L, "cust@test.com");

        assertThat(result.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelReservation_notOwner_throws() {
        User other = customer(2L, "other@test.com");
        when(userRepo.findByEmail("other@test.com")).thenReturn(Optional.of(other));

        Reservation r = pendingReservation(10L, 1L, 1L); // belongs to customer 1
        when(reservationRepo.findById(10L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancel(10L, "other@test.com"))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void cancelReservation_alreadyStarted_throws() {
        User cust = customer(1L, "cust@test.com");
        when(userRepo.findByEmail("cust@test.com")).thenReturn(Optional.of(cust));

        // start is in the past
        Reservation r = Reservation.builder()
                .id(10L).spaceId(1L).customerId(1L)
                .startDatetime(Instant.parse("2020-01-01T09:00:00Z"))
                .endDatetime(Instant.parse("2020-01-01T11:00:00Z"))
                .totalAmount(BigDecimal.TEN)
                .status(ReservationStatus.PENDING).build();
        when(reservationRepo.findById(10L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancel(10L, "cust@test.com"))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("already started");
    }

    @Test
    void cancelReservation_terminalState_throws() {
        User cust = customer(1L, "cust@test.com");
        when(userRepo.findByEmail("cust@test.com")).thenReturn(Optional.of(cust));

        Reservation r = pendingReservation(10L, 1L, 1L);
        r.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepo.findById(10L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancel(10L, "cust@test.com"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ================================================================
    // updateStatus (owner confirm/reject) tests
    // ================================================================

    @Test
    void transitionStatus_pendingToConfirmed_succeeds() {
        User ownerUser = owner(10L, "owner@test.com");
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(ownerUser));

        Space sp = space(1L, 10L, BigDecimal.TEN);
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(sp));

        Reservation r = pendingReservation(10L, 1L, 1L);
        when(reservationRepo.findById(10L)).thenReturn(Optional.of(r));
        when(reservationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationDto result = service.updateStatus(10L, "CONFIRMED", "owner@test.com");

        assertThat(result.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void transitionStatus_pendingToRejected_succeeds() {
        User ownerUser = owner(10L, "owner@test.com");
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(ownerUser));

        Space sp = space(1L, 10L, BigDecimal.TEN);
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(sp));

        Reservation r = pendingReservation(10L, 1L, 1L);
        when(reservationRepo.findById(10L)).thenReturn(Optional.of(r));
        when(reservationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReservationDto result = service.updateStatus(10L, "REJECTED", "owner@test.com");

        assertThat(result.status()).isEqualTo("REJECTED");
    }

    @Test
    void transitionStatus_cancelledToConfirmed_throws() {
        User ownerUser = owner(10L, "owner@test.com");
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(ownerUser));

        Space sp = space(1L, 10L, BigDecimal.TEN);
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(sp));

        Reservation r = pendingReservation(10L, 1L, 1L);
        r.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepo.findById(10L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.updateStatus(10L, "CONFIRMED", "owner@test.com"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void transitionStatus_confirmedToPending_throws() {
        User ownerUser = owner(10L, "owner@test.com");
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(ownerUser));

        Space sp = space(1L, 10L, BigDecimal.TEN);
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(sp));

        Reservation r = pendingReservation(10L, 1L, 1L);
        r.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepo.findById(10L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.updateStatus(10L, "PENDING", "owner@test.com"))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CONFIRMED or REJECTED");
    }

    @Test
    void transitionStatus_ownerOnWrongSpace_throws() {
        User ownerUser = owner(10L, "owner@test.com");
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(ownerUser));

        // Space owned by user 99, not 10
        Space sp = space(1L, 99L, BigDecimal.TEN);
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(sp));

        Reservation r = pendingReservation(10L, 1L, 1L);
        when(reservationRepo.findById(10L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.updateStatus(10L, "CONFIRMED", "owner@test.com"))
                .isInstanceOf(AuthorizationException.class);
    }
}
