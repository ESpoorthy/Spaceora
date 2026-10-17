package com.spaceora.space.dto;

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

    public static SpaceDto from(Space s) {
        List<SpaceImageDto> imageDtos = s.getImages().stream()
                .map(SpaceImageDto::from)
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
                List.of(),
                s.getCreatedAt().toString()
        );
    }
}
