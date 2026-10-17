package com.spaceora.reservation.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for PUT /api/reservations/{id}/status.
 * Only CONFIRMED or REJECTED are accepted from owners.
 */
public record StatusUpdateRequestDto(
        @NotNull String status
) {}
