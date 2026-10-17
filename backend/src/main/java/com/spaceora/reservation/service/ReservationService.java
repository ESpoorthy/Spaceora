package com.spaceora.reservation.service;

import com.spaceora.availability.entity.AvailabilitySchedule;
import com.spaceora.availability.service.AvailabilityService;
import com.spaceora.exception.AuthorizationException;
import com.spaceora.exception.BookingConflictException;
import com.spaceora.exception.BookingValidationException;
import com.spaceora.exception.InvalidStatusTransitionException;
import com.spaceora.exception.ResourceNotFoundException;
import com.spaceora.reservation.dto.ReservationDto;
import com.spaceora.reservation.dto.ReservationRequestDto;
import com.spaceora.reservation.entity.Reservation;
import com.spaceora.reservation.entity.ReservationStatus;
import com.spaceora.reservation.repository.ReservationRepository;
import com.spaceora.space.entity.Space;
import com.spaceora.space.repository.SpaceRepository;
import com.spaceora.user.entity.Role;
import com.spaceora.user.entity.User;
import com.spaceora.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepo;
    private final SpaceRepository spaceRepo;
    private final UserRepository userRepo;
    private final AvailabilityService availabilityService;

    public ReservationService(ReservationRepository reservationRepo,
                               SpaceRepository spaceRepo,
                               UserRepository userRepo,
                               AvailabilityService availabilityService) {
        this.reservationRepo = reservationRepo;
        this.spaceRepo = spaceRepo;
        this.userRepo = userRepo;
        this.availabilityService = availabilityService;
    }

    // -----------------------------------------------------------------------
    // Customer: create reservation
    // -----------------------------------------------------------------------

    public ReservationDto create(ReservationRequestDto dto, String customerEmail) {
        User customer = loadUser(customerEmail);
        if (customer.getRole() != Role.CUSTOMER) {
            throw new AuthorizationException("Only customers can create reservations");
        }

        Space space = spaceRepo.findById(dto.spaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + dto.spaceId()));

        if (!space.isActive()) {
            throw new BookingValidationException("Space is not active");
        }

        Instant start = parseInstant(dto.startDatetime(), "startDatetime");
        Instant end = parseInstant(dto.endDatetime(), "endDatetime");

        if (!end.isAfter(start)) {
            throw new BookingValidationException("endDatetime must be after startDatetime");
        }

        if (!start.isAfter(Instant.now())) {
            throw new BookingValidationException("startDatetime must be in the future");
        }

        // --- Day has a schedule? ---
        LocalDate bookingDate = start.atZone(ZoneOffset.UTC).toLocalDate();
        AvailabilitySchedule schedule = availabilityService.findScheduleForDate(space.getId(), bookingDate)
                .orElseThrow(() -> new BookingValidationException(
                        "Space is closed on " + bookingDate));

        // --- Within opening hours? ---
        LocalTime startTime = start.atZone(ZoneOffset.UTC).toLocalTime();
        LocalTime endTime = end.atZone(ZoneOffset.UTC).toLocalTime();

        if (startTime.isBefore(schedule.getOpenTime()) || endTime.isAfter(schedule.getCloseTime())) {
            throw new BookingValidationException(
                    "Booking is outside opening hours (" + schedule.getOpenTime()
                    + " – " + schedule.getCloseTime() + ")");
        }

        // Ensure start and end are on the same calendar day (no overnight bookings in MVP)
        LocalDate endDate = end.atZone(ZoneOffset.UTC).toLocalDate();
        if (!bookingDate.equals(endDate)) {
            throw new BookingValidationException(
                    "Reservation must start and end on the same calendar day");
        }

        // --- Blocked period overlap? ---
        List<?> blocked = availabilityService.findOverlappingBlockedPeriods(space.getId(), start, end);
        if (!blocked.isEmpty()) {
            throw new BookingValidationException(
                    "Requested time overlaps a blocked period");
        }

        // --- Concurrent-safe overlap check with pessimistic lock ---
        List<Reservation> overlapping = reservationRepo.findOverlappingActiveWithLock(
                space.getId(), start, end);
        if (!overlapping.isEmpty()) {
            throw new BookingConflictException(
                    "The requested time slot is already booked");
        }

        // --- Server-calculated total ---
        BigDecimal totalAmount = calculateTotal(space, start, end);

        Reservation reservation = Reservation.builder()
                .spaceId(space.getId())
                .customerId(customer.getId())
                .startDatetime(start)
                .endDatetime(end)
                .totalAmount(totalAmount)
                .status(ReservationStatus.PENDING)
                .notes(dto.notes())
                .build();

        Reservation saved = reservationRepo.save(reservation);
        return ReservationDto.from(saved, space.getName());
    }

    // -----------------------------------------------------------------------
    // Customer: get single reservation (own, or owner of space, or admin)
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReservationDto getById(Long reservationId, String callerEmail) {
        Reservation r = loadReservation(reservationId);
        User caller = loadUser(callerEmail);
        Space space = loadSpace(r.getSpaceId());

        boolean isCustomerOwner = caller.getRole() == Role.CUSTOMER && r.getCustomerId().equals(caller.getId());
        boolean isSpaceOwner = caller.getRole() == Role.OWNER && space.getOwnerId().equals(caller.getId());
        boolean isAdmin = caller.getRole() == Role.ADMIN;

        if (!isCustomerOwner && !isSpaceOwner && !isAdmin) {
            throw new AuthorizationException("Access denied");
        }

        return ReservationDto.from(r, space.getName());
    }

    // -----------------------------------------------------------------------
    // Customer: cancel reservation
    // -----------------------------------------------------------------------

    public ReservationDto cancel(Long reservationId, String customerEmail) {
        User customer = loadUser(customerEmail);
        Reservation r = loadReservation(reservationId);

        if (!r.getCustomerId().equals(customer.getId())) {
            throw new AuthorizationException("You do not own this reservation");
        }

        // Terminal states cannot be cancelled
        if (r.getStatus() == ReservationStatus.CANCELLED || r.getStatus() == ReservationStatus.REJECTED) {
            throw new InvalidStatusTransitionException(
                    "Cannot cancel a reservation in state: " + r.getStatus());
        }

        // Cannot cancel if already started
        if (!r.getStartDatetime().isAfter(Instant.now())) {
            throw new BookingValidationException(
                    "Cannot cancel a reservation that has already started");
        }

        r.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepo.save(r);
        return ReservationDto.from(saved, loadSpace(r.getSpaceId()).getName());
    }

    // -----------------------------------------------------------------------
    // Customer: list own reservations
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ReservationDto> listMine(String customerEmail,
                                          ReservationStatus statusFilter,
                                          Pageable pageable) {
        User customer = loadUser(customerEmail);
        Page<Reservation> page = (statusFilter != null)
                ? reservationRepo.findByCustomerIdAndStatus(customer.getId(), statusFilter, pageable)
                : reservationRepo.findByCustomerId(customer.getId(), pageable);

        return page.map(r -> ReservationDto.from(r, spaceNameFor(r.getSpaceId())));
    }

    // -----------------------------------------------------------------------
    // Owner: list reservations for a space
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ReservationDto> listForSpace(Long spaceId,
                                              String ownerEmail,
                                              ReservationStatus statusFilter,
                                              Pageable pageable) {
        User caller = loadUser(ownerEmail);
        Space space = loadSpace(spaceId);

        if (caller.getRole() != Role.ADMIN && !space.getOwnerId().equals(caller.getId())) {
            throw new AuthorizationException("You do not own this space");
        }

        Page<Reservation> page = (statusFilter != null)
                ? reservationRepo.findBySpaceIdAndStatus(spaceId, statusFilter, pageable)
                : reservationRepo.findBySpaceId(spaceId, pageable);

        return page.map(r -> ReservationDto.from(r, space.getName()));
    }

    // -----------------------------------------------------------------------
    // Owner: confirm or reject a pending reservation
    // -----------------------------------------------------------------------

    public ReservationDto updateStatus(Long reservationId,
                                        String newStatusStr,
                                        String ownerEmail) {
        User owner = loadUser(ownerEmail);
        Reservation r = loadReservation(reservationId);
        Space space = loadSpace(r.getSpaceId());

        if (!space.getOwnerId().equals(owner.getId())) {
            throw new AuthorizationException("You do not own the space for this reservation");
        }

        ReservationStatus newStatus;
        try {
            newStatus = ReservationStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BookingValidationException("Invalid status: " + newStatusStr);
        }

        // Only CONFIRMED and REJECTED are owner-settable
        if (newStatus != ReservationStatus.CONFIRMED && newStatus != ReservationStatus.REJECTED) {
            throw new InvalidStatusTransitionException(
                    "Owner can only set status to CONFIRMED or REJECTED");
        }

        // State machine: only PENDING can be confirmed/rejected
        if (r.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                    "Can only confirm/reject a PENDING reservation; current status: " + r.getStatus());
        }

        r.setStatus(newStatus);
        Reservation saved = reservationRepo.save(r);
        return ReservationDto.from(saved, space.getName());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Server-calculated total: hourlyRate × durationInHours.
     * If dailyRate is set and booking spans the full opening window, use dailyRate.
     */
    private BigDecimal calculateTotal(Space space, Instant start, Instant end) {
        long seconds = end.getEpochSecond() - start.getEpochSecond();
        BigDecimal hours = BigDecimal.valueOf(seconds)
                .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
        return space.getHourlyRate().multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private Reservation loadReservation(Long id) {
        return reservationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    private User loadUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Space loadSpace(Long id) {
        return spaceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + id));
    }

    private String spaceNameFor(Long spaceId) {
        return spaceRepo.findById(spaceId).map(Space::getName).orElse("Unknown");
    }

    private Instant parseInstant(String value, String fieldName) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new BookingValidationException(
                    fieldName + " must be ISO 8601 UTC (e.g. 2025-06-17T09:00:00Z), got: " + value);
        }
    }
}
