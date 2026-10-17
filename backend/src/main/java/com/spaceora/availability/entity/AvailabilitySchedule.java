package com.spaceora.availability.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * One row per (space, day-of-week) representing weekly opening hours.
 * day_of_week: 0 = Monday … 6 = Sunday (ISO convention per data-dictionary.md).
 */
@Entity
@Table(name = "availability_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_availability_space_day",
                columnNames = {"space_id", "day_of_week"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilitySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;   // 0=Mon … 6=Sun

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;
}
