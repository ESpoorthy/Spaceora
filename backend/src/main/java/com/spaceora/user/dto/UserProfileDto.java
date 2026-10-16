package com.spaceora.user.dto;

import com.spaceora.user.entity.User;

public record UserProfileDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String role,
        String createdAt
) {
    public static UserProfileDto from(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole().name(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }
}
