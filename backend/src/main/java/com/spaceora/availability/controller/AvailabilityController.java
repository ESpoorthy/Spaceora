package com.spaceora.availability.controller;

import com.spaceora.availability.dto.*;
import com.spaceora.availability.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Handles availability and blocked-period endpoints.
 *
 * Public:
 *   GET  /api/spaces/{id}/availability
 *
 * Owner-only:
 *   PUT  /api/spaces/{id}/availability/schedule
 *   POST /api/spaces/{id}/blocked-periods
 *   DELETE /api/spaces/{id}/blocked-periods/{bpId}
 */
@RestController
@RequestMapping("/api/spaces/{id}")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    // ------------------------------------------------------------------
    // Public availability view
    // ------------------------------------------------------------------

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponseDto> getAvailability(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int weeks) {

        LocalDate startDate = (date != null) ? date : LocalDate.now();
        AvailabilityResponseDto response = availabilityService.computeAvailability(id, startDate, weeks);
        return ResponseEntity.ok(response);
    }

    // ------------------------------------------------------------------
    // Owner: manage weekly schedule
    // ------------------------------------------------------------------

    @PutMapping("/availability/schedule")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<ScheduleEntryDto>> setSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleSetRequestDto dto,
            Authentication auth) {

        List<ScheduleEntryDto> result = availabilityService.setSchedule(id, dto, auth.getName());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/availability/schedule")
    public ResponseEntity<List<ScheduleEntryDto>> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(availabilityService.getSchedule(id));
    }

    // ------------------------------------------------------------------
    // Owner: manage blocked periods
    // ------------------------------------------------------------------

    @PostMapping("/blocked-periods")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<BlockedPeriodDto> addBlockedPeriod(
            @PathVariable Long id,
            @Valid @RequestBody BlockedPeriodRequestDto dto,
            Authentication auth) {

        BlockedPeriodDto created = availabilityService.addBlockedPeriod(id, dto, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/blocked-periods/{bpId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> removeBlockedPeriod(
            @PathVariable Long id,
            @PathVariable Long bpId,
            Authentication auth) {

        availabilityService.removeBlockedPeriod(id, bpId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocked-periods")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<BlockedPeriodDto>> listBlockedPeriods(
            @PathVariable Long id,
            Authentication auth) {

        return ResponseEntity.ok(availabilityService.listBlockedPeriods(id, auth.getName()));
    }
}
