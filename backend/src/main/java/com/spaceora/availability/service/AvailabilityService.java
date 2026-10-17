package com.spaceora.availability.service;

import com.spaceora.availability.dto.*;
import com.spaceora.availability.entity.AvailabilitySchedule;
import com.spaceora.availability.entity.BlockedPeriod;
import com.spaceora.availability.repository.AvailabilityScheduleRepository;
import com.spaceora.availability.repository.BlockedPeriodRepository;
import com.spaceora.exception.AuthorizationException;
import com.spaceora.exception.BookingConflictException;
import com.spaceora.exception.BookingValidationException;
import com.spaceora.exception.ResourceNotFoundException;
import com.spaceora.space.repository.SpaceRepository;
import com.spaceora.user.entity.Role;
import com.spaceora.user.entity.User;
import com.spaceora.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class AvailabilityService {

    private final AvailabilityScheduleRepository scheduleRepo;
    private final BlockedPeriodRepository blockedPeriodRepo;
    private final SpaceRepository spaceRepo;
    private final UserRepository userRepo;

    public AvailabilityService(AvailabilityScheduleRepository scheduleRepo,
                                BlockedPeriodRepository blockedPeriodRepo,
                                SpaceRepository spaceRepo,
                                UserRepository userRepo) {
        this.scheduleRepo = scheduleRepo;
        this.blockedPeriodRepo = blockedPeriodRepo;
        this.spaceRepo = spaceRepo;
        this.userRepo = userRepo;
    }

    // -----------------------------------------------------------------------
    // Schedule management (OWNER only)
    // -----------------------------------------------------------------------

    /**
     * Replaces the entire weekly schedule for a space.
     * Only the owner of the space (or ADMIN) may call this.
     */
    public List<ScheduleEntryDto> setSchedule(Long spaceId,
                                               ScheduleSetRequestDto dto,
                                               String callerEmail) {
        verifyOwnership(spaceId, callerEmail);

        // Validate each entry
        for (ScheduleEntryDto entry : dto.schedule()) {
            LocalTime open = parseTime(entry.openTime(), "openTime");
            LocalTime close = parseTime(entry.closeTime(), "closeTime");
            if (!close.isAfter(open)) {
                throw new BookingValidationException(
                        "closeTime must be after openTime for dayOfWeek " + entry.dayOfWeek());
            }
        }

        // Check for duplicate days in the request
        long distinctDays = dto.schedule().stream()
                .map(ScheduleEntryDto::dayOfWeek)
                .distinct()
                .count();
        if (distinctDays < dto.schedule().size()) {
            throw new BookingValidationException("Duplicate dayOfWeek entries in schedule");
        }

        // Replace: delete existing, save new
        scheduleRepo.deleteBySpaceId(spaceId);

        List<AvailabilitySchedule> saved = dto.schedule().stream()
                .map(entry -> AvailabilitySchedule.builder()
                        .spaceId(spaceId)
                        .dayOfWeek(entry.dayOfWeek())
                        .openTime(parseTime(entry.openTime(), "openTime"))
                        .closeTime(parseTime(entry.closeTime(), "closeTime"))
                        .build())
                .map(scheduleRepo::save)
                .toList();

        return saved.stream().map(ScheduleEntryDto::from).toList();
    }

    /**
     * Returns the current schedule for a space (public read).
     */
    @Transactional(readOnly = true)
    public List<ScheduleEntryDto> getSchedule(Long spaceId) {
        requireSpaceExists(spaceId);
        return scheduleRepo.findBySpaceId(spaceId)
                .stream()
                .map(ScheduleEntryDto::from)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Blocked period management (OWNER only)
    // -----------------------------------------------------------------------

    public BlockedPeriodDto addBlockedPeriod(Long spaceId,
                                              BlockedPeriodRequestDto dto,
                                              String callerEmail) {
        verifyOwnership(spaceId, callerEmail);

        Instant start = parseInstant(dto.startDatetime(), "startDatetime");
        Instant end = parseInstant(dto.endDatetime(), "endDatetime");

        if (!end.isAfter(start)) {
            throw new BookingValidationException("endDatetime must be after startDatetime");
        }

        List<BlockedPeriod> overlapping = blockedPeriodRepo.findOverlapping(spaceId, start, end);
        if (!overlapping.isEmpty()) {
            throw new BookingConflictException(
                    "New blocked period overlaps an existing blocked period");
        }

        BlockedPeriod bp = BlockedPeriod.builder()
                .spaceId(spaceId)
                .startDatetime(start)
                .endDatetime(end)
                .reason(dto.reason())
                .build();

        return BlockedPeriodDto.from(blockedPeriodRepo.save(bp));
    }

    public void removeBlockedPeriod(Long spaceId, Long bpId, String callerEmail) {
        verifyOwnership(spaceId, callerEmail);

        BlockedPeriod bp = blockedPeriodRepo.findById(bpId)
                .orElseThrow(() -> new ResourceNotFoundException("Blocked period not found: " + bpId));

        if (!bp.getSpaceId().equals(spaceId)) {
            throw new ResourceNotFoundException("Blocked period " + bpId + " does not belong to space " + spaceId);
        }

        blockedPeriodRepo.delete(bp);
    }

    public List<BlockedPeriodDto> listBlockedPeriods(Long spaceId, String callerEmail) {
        verifyOwnership(spaceId, callerEmail);
        return blockedPeriodRepo.findBySpaceId(spaceId)
                .stream()
                .map(BlockedPeriodDto::from)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Merged availability view (public)
    // -----------------------------------------------------------------------

    /**
     * Computes the merged availability view for a space over a date range.
     * Days without a schedule entry are omitted (space is closed that day).
     *
     * @param spaceId the space
     * @param startDate start date (inclusive)
     * @param weeks number of weeks (1-4)
     */
    @Transactional(readOnly = true)
    public AvailabilityResponseDto computeAvailability(Long spaceId,
                                                        LocalDate startDate,
                                                        int weeks) {
        requireSpaceExists(spaceId);

        if (weeks < 1 || weeks > 4) {
            throw new BookingValidationException("weeks must be between 1 and 4");
        }

        // Build a map: dayOfWeek -> schedule for fast lookup
        Map<Integer, AvailabilitySchedule> scheduleMap = scheduleRepo.findBySpaceId(spaceId)
                .stream()
                .collect(Collectors.toMap(AvailabilitySchedule::getDayOfWeek, Function.identity()));

        LocalDate endDate = startDate.plusWeeks(weeks);

        // Fetch all blocked periods that touch the date range
        Instant rangeStart = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant rangeEnd = endDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        List<BlockedPeriod> allBlockedPeriods =
                blockedPeriodRepo.findBySpaceIdAndRange(spaceId, rangeStart, rangeEnd);

        List<AvailabilitySlotResponseDto> slots = new ArrayList<>();
        LocalDate date = startDate;

        while (date.isBefore(endDate)) {
            // ISO DayOfWeek: MONDAY=1…SUNDAY=7; our convention: 0=Mon…6=Sun
            int isoDay = date.getDayOfWeek().getValue(); // 1..7
            int domDay = isoDay - 1;                     // 0..6

            AvailabilitySchedule schedule = scheduleMap.get(domDay);
            if (schedule != null) {
                // Find blocked periods that overlap this calendar day
                Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
                Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

                List<BlockedPeriodDto> dayBlocked = allBlockedPeriods.stream()
                        .filter(bp -> bp.getStartDatetime().isBefore(dayEnd)
                                && bp.getEndDatetime().isAfter(dayStart))
                        .map(BlockedPeriodDto::from)
                        .toList();

                slots.add(new AvailabilitySlotResponseDto(
                        date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        domDay,
                        schedule.getOpenTime().toString(),
                        schedule.getCloseTime().toString(),
                        dayBlocked
                ));
            }
            date = date.plusDays(1);
        }

        return new AvailabilityResponseDto(spaceId, slots);
    }

    // -----------------------------------------------------------------------
    // Package-visible helpers used by ReservationService
    // -----------------------------------------------------------------------

    /**
     * Returns the schedule entry for a specific space and date, or empty if
     * the space has no schedule for that day (closed).
     */
    @Transactional(readOnly = true)
    public java.util.Optional<AvailabilitySchedule> findScheduleForDate(Long spaceId, LocalDate date) {
        int isoDay = date.getDayOfWeek().getValue();
        int domDay = isoDay - 1;
        return scheduleRepo.findBySpaceIdAndDayOfWeek(spaceId, domDay);
    }

    /**
     * Returns all blocked periods for the space that overlap the given window.
     */
    @Transactional(readOnly = true)
    public List<BlockedPeriod> findOverlappingBlockedPeriods(Long spaceId,
                                                              Instant start,
                                                              Instant end) {
        return blockedPeriodRepo.findOverlapping(spaceId, start, end);
    }

    /**
     * Returns the full schedule for a space (used by SpaceService to populate SpaceDto).
     */
    @Transactional(readOnly = true)
    public List<AvailabilitySchedule> getScheduleEntities(Long spaceId) {
        return scheduleRepo.findBySpaceId(spaceId);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void requireSpaceExists(Long spaceId) {
        if (!spaceRepo.existsById(spaceId)) {
            throw new ResourceNotFoundException("Space not found: " + spaceId);
        }
    }

    private void verifyOwnership(Long spaceId, String callerEmail) {
        var space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + spaceId));

        User caller = userRepo.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (caller.getRole() == Role.ADMIN) {
            return; // ADMIN may manage any space's availability
        }

        if (!space.getOwnerId().equals(caller.getId())) {
            throw new AuthorizationException("You do not own this space");
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            throw new BookingValidationException(
                    fieldName + " must be in HH:mm format, got: " + value);
        }
    }

    private Instant parseInstant(String value, String fieldName) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new BookingValidationException(
                    fieldName + " must be ISO 8601 UTC datetime (e.g. 2025-06-16T12:00:00Z), got: " + value);
        }
    }
}
