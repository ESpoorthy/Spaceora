package com.spaceora.availability.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/spaces/{id}/blocked-periods.
 */
public record BlockedPeriodRequestDto(
        @NotNull String startDatetime,
        @NotNull String endDatetime,
        String reason
) {}
