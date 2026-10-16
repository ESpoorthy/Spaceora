package com.spaceora.auth.dto;

public record AuthResponseDto(
        String token,
        long expiresIn,
        Long userId,
        String role
) {}
