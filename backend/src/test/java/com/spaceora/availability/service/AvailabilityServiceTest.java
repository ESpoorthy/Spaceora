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
class AvailabilityServiceTest {

    @Mock AvailabilityScheduleRepository scheduleRepo;
    @Mock BlockedPeriodRepository blockedPeriodRepo;
    @Mock SpaceRepository spaceRepo;
    @Mock UserRepository userRepo;

    private AvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new AvailabilityService(scheduleRepo, blockedPeriodRepo, spaceRepo, userRepo);
    }

    // ---- helpers ----

    private User owner(Long id, String email) {
        return User.builder().id(id).email(email).passwordHash("x")
                .firstName("O").lastName("W").role(Role.OWNER).build();
    }

    private Space space(Long id, Long ownerId) {
        return Space.builder().id(id).ownerId(ownerId).name("S").type(SpaceType.COWORKING)
                .city("London").address("1 St").hourlyRate(BigDecimal.TEN).capacity(10)
                .isActive(true).images(new ArrayList<>()).build();
    }

    private AvailabilitySchedule schedule(Long id, Long spaceId, int day) {
        return AvailabilitySchedule.builder().id(id).spaceId(spaceId).dayOfWeek(day)
                .openTime(LocalTime.of(8, 0)).closeTime(LocalTime.of(20, 0)).build();
    }

    private BlockedPeriod blocked(Long id, Long spaceId, Instant start, Instant end) {
        return BlockedPeriod.builder().id(id).spaceId(spaceId)
                .startDatetime(start).endDatetime(end).reason("test").build();
    }

    // ---- addBlockedPeriod: no overlap succeeds ----

    @Test
    void addBlockedPeriod_noOverlap_succeeds() {
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(space(1L, 10L)));
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(10L, "owner@test.com")));
        when(blockedPeriodRepo.findOverlapping(eq(1L), any(), any())).thenReturn(List.of());

        Instant start = Instant.parse("2025-06-16T12:00:00Z");
        Instant end = Instant.parse("2025-06-16T14:00:00Z");
        BlockedPeriod saved = blocked(1L, 1L, start, end);
        when(blockedPeriodRepo.save(any())).thenReturn(saved);

        BlockedPeriodRequestDto dto = new BlockedPeriodRequestDto(
                "2025-06-16T12:00:00Z", "2025-06-16T14:00:00Z", "Deep clean");

        BlockedPeriodDto result = service.addBlockedPeriod(1L, dto, "owner@test.com");
        assertThat(result.id()).isEqualTo(1L);
    }

    // ---- addBlockedPeriod: overlapping existing → 409 ----

    @Test
    void addBlockedPeriod_overlapsExisting_throws() {
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(space(1L, 10L)));
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(10L, "owner@test.com")));

        Instant start = Instant.parse("2025-06-16T12:00:00Z");
        Instant end = Instant.parse("2025-06-16T14:00:00Z");
        when(blockedPeriodRepo.findOverlapping(eq(1L), any(), any()))
                .thenReturn(List.of(blocked(99L, 1L, start, end)));

        BlockedPeriodRequestDto dto = new BlockedPeriodRequestDto(
                "2025-06-16T12:00:00Z", "2025-06-16T14:00:00Z", "Overlap");

        assertThatThrownBy(() -> service.addBlockedPeriod(1L, dto, "owner@test.com"))
                .isInstanceOf(BookingConflictException.class);
    }

    // ---- addBlockedPeriod: non-owner → 403 ----

    @Test
    void addBlockedPeriod_nonOwner_throws() {
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(space(1L, 10L)));
        User other = User.builder().id(99L).email("other@test.com").passwordHash("x")
                .firstName("X").lastName("Y").role(Role.OWNER).build();
        when(userRepo.findByEmail("other@test.com")).thenReturn(Optional.of(other));

        BlockedPeriodRequestDto dto = new BlockedPeriodRequestDto(
                "2025-06-16T12:00:00Z", "2025-06-16T14:00:00Z", null);

        assertThatThrownBy(() -> service.addBlockedPeriod(1L, dto, "other@test.com"))
                .isInstanceOf(AuthorizationException.class);
    }

    // ---- addBlockedPeriod: end before start → 422 ----

    @Test
    void addBlockedPeriod_endBeforeStart_throws() {
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(space(1L, 10L)));
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(10L, "owner@test.com")));

        BlockedPeriodRequestDto dto = new BlockedPeriodRequestDto(
                "2025-06-16T14:00:00Z", "2025-06-16T12:00:00Z", null);

        assertThatThrownBy(() -> service.addBlockedPeriod(1L, dto, "owner@test.com"))
                .isInstanceOf(BookingValidationException.class);
    }

    // ---- computeAvailability: closed day omitted ----

    @Test
    void computeAvailability_closedDay_omitted() {
        when(spaceRepo.existsById(1L)).thenReturn(true);
        // Return schedule only for Monday (0)
        when(scheduleRepo.findBySpaceId(1L)).thenReturn(List.of(schedule(1L, 1L, 0)));
        when(blockedPeriodRepo.findBySpaceIdAndRange(eq(1L), any(), any())).thenReturn(List.of());

        // Use a Monday start so at least 1 slot returns; asking for 1 week
        LocalDate monday = LocalDate.of(2025, 6, 16); // That's a Monday
        AvailabilityResponseDto result = service.computeAvailability(1L, monday, 1);

        assertThat(result.slots()).hasSize(1);
        assertThat(result.slots().get(0).dayOfWeek()).isEqualTo(0); // Monday
    }

    // ---- computeAvailability: includes blocked periods ----

    @Test
    void computeAvailability_includesBlockedPeriods() {
        when(spaceRepo.existsById(1L)).thenReturn(true);
        when(scheduleRepo.findBySpaceId(1L)).thenReturn(List.of(schedule(1L, 1L, 0)));

        Instant bpStart = Instant.parse("2025-06-16T12:00:00Z");
        Instant bpEnd = Instant.parse("2025-06-16T14:00:00Z");
        when(blockedPeriodRepo.findBySpaceIdAndRange(eq(1L), any(), any()))
                .thenReturn(List.of(blocked(5L, 1L, bpStart, bpEnd)));

        LocalDate monday = LocalDate.of(2025, 6, 16);
        AvailabilityResponseDto result = service.computeAvailability(1L, monday, 1);

        assertThat(result.slots()).hasSize(1);
        assertThat(result.slots().get(0).blockedPeriods()).hasSize(1);
        assertThat(result.slots().get(0).blockedPeriods().get(0).id()).isEqualTo(5L);
    }

    // ---- setSchedule: closeTime before openTime → 422 ----

    @Test
    void setSchedule_closeBeforeOpen_throws() {
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(space(1L, 10L)));
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(10L, "owner@test.com")));

        ScheduleSetRequestDto dto = new ScheduleSetRequestDto(
                List.of(new ScheduleEntryDto(0, "20:00", "08:00")));

        assertThatThrownBy(() -> service.setSchedule(1L, dto, "owner@test.com"))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("closeTime must be after openTime");
    }

    // ---- setSchedule: duplicate dayOfWeek in request → 422 ----

    @Test
    void setSchedule_duplicateDays_throws() {
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(space(1L, 10L)));
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(10L, "owner@test.com")));

        ScheduleSetRequestDto dto = new ScheduleSetRequestDto(List.of(
                new ScheduleEntryDto(0, "08:00", "20:00"),
                new ScheduleEntryDto(0, "09:00", "18:00")));

        assertThatThrownBy(() -> service.setSchedule(1L, dto, "owner@test.com"))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("Duplicate");
    }

    // ---- removeBlockedPeriod: wrong space → 404 ----

    @Test
    void removeBlockedPeriod_wrongSpace_throws() {
        when(spaceRepo.findById(1L)).thenReturn(Optional.of(space(1L, 10L)));
        when(userRepo.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(10L, "owner@test.com")));

        // bp belongs to space 99, not 1
        BlockedPeriod bp = blocked(5L, 99L, Instant.now(), Instant.now().plusSeconds(3600));
        when(blockedPeriodRepo.findById(5L)).thenReturn(Optional.of(bp));

        assertThatThrownBy(() -> service.removeBlockedPeriod(1L, 5L, "owner@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- computeAvailability: invalid weeks → 422 ----

    @Test
    void computeAvailability_invalidWeeks_throws() {
        when(spaceRepo.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.computeAvailability(1L, LocalDate.now(), 5))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("weeks must be between");
    }
}
