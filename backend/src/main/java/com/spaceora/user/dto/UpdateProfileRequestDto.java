package com.spaceora.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDto(
        @Size(min = 1) String firstName,
        @Size(min = 1) String lastName,
        String phone
) {}
