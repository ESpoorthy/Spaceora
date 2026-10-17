package com.spaceora.space.dto;

import com.spaceora.availability.entity.AvailabilitySchedule;
import com.spaceora.space.entity.Space;

import java.math.BigDecimal;
import java.util.List;

public record SpaceDto(
        Long id,
        Long ownerId,
        String name,
        String description,
        String type,
        String city,
        String address,
        BigDecimal hourlyRate,
        BigDecimal dailyRate,
        int capacity,
        boolean isActive,
        List<SpaceImageDto> images,
        List<AvailabilitySlotDto> availabilitySchedule,
        String createdAt
) {

    /**
     * Build a SpaceDto with an empty availability schedule.
     * Use {@link #from(Space, List)} when the schedule is available.
     */
    public static SpaceDto from(Space s) {
        return from(s, List.of());
    }

    /**
     * Build a SpaceDto and populate the availability schedule from the provided entities.
     */
    public static SpaceDto from(Space s, List<AvailabilitySchedule> schedules) {
        List<SpaceImageDto> imageDtos = s.getImages().stream()
                .map(SpaceImageDto::from)
                .toList();

        List<AvailabilitySlotDto> scheduleDtos = schedules.stream()
                .map(sc -> new AvailabilitySlotDto(
                        sc.getDayOfWeek(),
                        sc.getOpenTime().toString(),
                        sc.getCloseTime().toString()))
                .toList();

        return new SpaceDto(
                s.getId(),
                s.getOwnerId(),
                s.getName(),
                s.getDescription(),
                s.getType().name(),
                s.getCity(),
                s.getAddress(),
                s.getHourlyRate(),
                s.getDailyRate(),
                s.getCapacity(),
                s.isActive(),
                imageDtos,
                scheduleDtos,
                s.getCreatedAt().toString()
        );
    }
}
