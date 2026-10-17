package com.spaceora.reservation.controller;

import com.spaceora.reservation.dto.ReservationDto;
import com.spaceora.reservation.dto.ReservationRequestDto;
import com.spaceora.reservation.dto.StatusUpdateRequestDto;
import com.spaceora.reservation.entity.ReservationStatus;
import com.spaceora.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Handles all reservation lifecycle endpoints:
 *
 *   POST   /api/reservations              — customer creates
 *   GET    /api/reservations/my           — customer lists own
 *   GET    /api/reservations/{id}         — customer/owner/admin reads single
 *   DELETE /api/reservations/{id}         — customer cancels
 *   PUT    /api/reservations/{id}/status  — owner confirms/rejects
 *
 * Reservations listed by space are handled in SpaceReservationController.
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReservationDto> create(
            @Valid @RequestBody ReservationRequestDto dto,
            Authentication auth) {
        ReservationDto created = reservationService.create(dto, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<ReservationDto>> listMine(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        ReservationStatus statusFilter = parseStatusFilter(status);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(reservationService.listMine(auth.getName(), statusFilter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationDto> getById(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(reservationService.getById(id, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReservationDto> cancel(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(reservationService.cancel(id, auth.getName()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReservationDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequestDto dto,
            Authentication auth) {
        return ResponseEntity.ok(reservationService.updateStatus(id, dto.status(), auth.getName()));
    }

    // ---- helper ----

    private ReservationStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return ReservationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // Silently ignore unknown status filter values
        }
    }
}
