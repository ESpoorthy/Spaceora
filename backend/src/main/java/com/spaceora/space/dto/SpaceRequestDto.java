package com.spaceora.space.dto;

import com.spaceora.space.entity.SpaceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SpaceRequestDto(
        @NotBlank String name,
        String description,
        @NotNull SpaceType type,
        @NotBlank String city,
        @NotBlank String address,
        @NotNull @DecimalMin("0.0") BigDecimal hourlyRate,
        @DecimalMin("0.0") BigDecimal dailyRate,
        @NotNull @Min(1) Integer capacity
) {
}
