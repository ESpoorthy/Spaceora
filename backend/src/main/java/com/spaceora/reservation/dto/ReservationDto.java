package com.spaceora.reservation.dto;

import com.spaceora.reservation.entity.Reservation;

import java.math.BigDecimal;

/**
 * Response DTO matching the shape in api-contract.md.
 */
public record ReservationDto(
        Long id,
        Long spaceId,
        String spaceName,
        Long customerId,
        String startDatetime,
        String endDatetime,
        BigDecimal totalAmount,
        String status,
        String notes,
        String createdAt
) {
    public static ReservationDto from(Reservation r, String spaceName) {
        return new ReservationDto(
                r.getId(),
                r.getSpaceId(),
                spaceName,
                r.getCustomerId(),
                r.getStartDatetime().toString(),
                r.getEndDatetime().toString(),
                r.getTotalAmount(),
                r.getStatus().name(),
                r.getNotes(),
                r.getCreatedAt() != null ? r.getCreatedAt().toString() : null
        );
    }
}
