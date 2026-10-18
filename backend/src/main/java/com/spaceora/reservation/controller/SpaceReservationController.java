package com.spaceora.reservation.controller;

import com.spaceora.exception.BookingValidationException;
import com.spaceora.reservation.dto.ReservationDto;
import com.spaceora.reservation.entity.ReservationStatus;
import com.spaceora.reservation.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * GET /api/spaces/{id}/reservations — owner-only endpoint to list reservations for a space.
 * Kept separate from AvailabilityController to preserve clean separation of concerns.
 */
@RestController
@RequestMapping("/api/spaces/{id}/reservations")
public class SpaceReservationController {

    private final ReservationService reservationService;

    public SpaceReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Page<ReservationDto>> listForSpace(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        if (page < 0) {
            throw new BookingValidationException("page must be greater than or equal to 0");
        }
        if (size < 1) {
            throw new BookingValidationException("size must be greater than 0");
        }

        ReservationStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = ReservationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Unknown status filter → return all
            }
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(reservationService.listForSpace(id, auth.getName(), statusFilter, pageable));
    }
}
