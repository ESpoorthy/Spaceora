package com.spaceora.space.dto;

import com.spaceora.space.entity.SpaceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SpaceUpdateRequestDto(
        @Size(min = 1) String name,
        String description,
        SpaceType type,
        @Size(min = 1) String city,
        @Size(min = 1) String address,
        @DecimalMin("0.0") BigDecimal hourlyRate,
        @DecimalMin("0.0") BigDecimal dailyRate,
        @Min(1) Integer capacity,
        Boolean isActive
) {
}
