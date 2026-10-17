package com.spaceora.availability.dto;

import com.spaceora.availability.entity.AvailabilitySchedule;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * One entry in a weekly schedule set/replace request, and also used in responses.
 * dayOfWeek: 0 = Monday … 6 = Sunday.
 * openTime / closeTime: "HH:mm" strings (e.g. "08:00", "20:00").
 */
public record ScheduleEntryDto(
        @NotNull @Min(0) @Max(6) Integer dayOfWeek,
        @NotNull String openTime,
        @NotNull String closeTime
) {
    public static ScheduleEntryDto from(AvailabilitySchedule s) {
        return new ScheduleEntryDto(
                s.getDayOfWeek(),
                s.getOpenTime().toString(),
                s.getCloseTime().toString()
        );
    }
}
