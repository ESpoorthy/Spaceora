package com.spaceora.availability.dto;

import com.spaceora.availability.entity.BlockedPeriod;

/**
 * Response DTO for a blocked period, as specified in api-contract.md.
 */
public record BlockedPeriodDto(
        Long id,
        Long spaceId,
        String startDatetime,
        String endDatetime,
        String reason
) {
    public static BlockedPeriodDto from(BlockedPeriod bp) {
        return new BlockedPeriodDto(
                bp.getId(),
                bp.getSpaceId(),
                bp.getStartDatetime().toString(),
                bp.getEndDatetime().toString(),
                bp.getReason()
        );
    }
}
