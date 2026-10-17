package com.spaceora.availability.dto;

import java.util.List;

/**
 * Top-level response for GET /api/spaces/{id}/availability.
 */
public record AvailabilityResponseDto(
        Long spaceId,
        List<AvailabilitySlotResponseDto> slots
) {}
