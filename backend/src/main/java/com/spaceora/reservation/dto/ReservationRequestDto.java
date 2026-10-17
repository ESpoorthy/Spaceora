package com.spaceora.reservation.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/reservations.
 * totalAmount and customerId are NEVER accepted from the client.
 */
public record ReservationRequestDto(
        @NotNull Long spaceId,
        @NotNull String startDatetime,
        @NotNull String endDatetime,
        String notes
) {}
