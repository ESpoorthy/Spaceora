package com.spaceora.availability.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for PUT /api/spaces/{id}/availability/schedule.
 * Replaces the entire weekly schedule for the space.
 */
public record ScheduleSetRequestDto(
        @NotNull @Valid List<ScheduleEntryDto> schedule
) {}
