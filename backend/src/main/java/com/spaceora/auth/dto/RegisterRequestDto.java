package com.spaceora.auth.dto;

import com.spaceora.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequestDto(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be a valid email address") String email,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Password must be at least 8 characters with uppercase, lowercase and digit"
        )
        String password,
        Role role
) {}
