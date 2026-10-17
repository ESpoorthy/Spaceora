package com.spaceora.availability.dto;

import java.util.List;

/**
 * One day's slot in the merged availability view returned by
 * GET /api/spaces/{id}/availability.
 */
public record AvailabilitySlotResponseDto(
        String date,          // ISO date "yyyy-MM-dd"
        int dayOfWeek,        // 0=Mon … 6=Sun
        String openTime,      // "HH:mm"
        String closeTime,     // "HH:mm"
        List<BlockedPeriodDto> blockedPeriods
) {}
