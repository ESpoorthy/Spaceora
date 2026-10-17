package com.spaceora.space.dto;

import com.spaceora.space.entity.Space;

import java.math.BigDecimal;
import java.util.Comparator;

public record SpaceSummaryDto(
        Long id,
        String name,
        String type,
        String city,
        BigDecimal hourlyRate,
        int capacity,
        boolean isActive,
        String thumbnailUrl
) {

    public static SpaceSummaryDto from(Space s) {
        String thumbnailUrl = s.getImages().stream()
                .sorted(Comparator.comparingInt(img -> img.getDisplayOrder()))
                .findFirst()
                .map(img -> img.getImageUrl())
                .orElse(null);

        return new SpaceSummaryDto(
                s.getId(),
                s.getName(),
                s.getType().name(),
                s.getCity(),
                s.getHourlyRate(),
                s.getCapacity(),
                s.isActive(),
                thumbnailUrl
        );
    }
}
