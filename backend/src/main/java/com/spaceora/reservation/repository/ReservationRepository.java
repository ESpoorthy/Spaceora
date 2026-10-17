package com.spaceora.reservation.repository;

import com.spaceora.reservation.entity.Reservation;
import com.spaceora.reservation.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByCustomerId(Long customerId, Pageable pageable);

    Page<Reservation> findByCustomerIdAndStatus(Long customerId, ReservationStatus status, Pageable pageable);

    Page<Reservation> findBySpaceId(Long spaceId, Pageable pageable);

    Page<Reservation> findBySpaceIdAndStatus(Long spaceId, ReservationStatus status, Pageable pageable);

    /**
     * Check for overlapping ACTIVE (PENDING or CONFIRMED) reservations for the space.
     * Uses pessimistic write lock to prevent concurrent double-booking.
     * Two windows overlap when: existingStart < requestEnd AND existingEnd > requestStart.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r " +
           "WHERE r.spaceId = :spaceId " +
           "  AND r.status IN ('PENDING', 'CONFIRMED') " +
           "  AND r.startDatetime < :end " +
           "  AND r.endDatetime > :start")
    List<Reservation> findOverlappingActiveWithLock(@Param("spaceId") Long spaceId,
                                                     @Param("start") Instant start,
                                                     @Param("end") Instant end);

    /**
     * Same overlap query without locking — for read-only checks (reporting, etc.).
     */
    @Query("SELECT r FROM Reservation r " +
           "WHERE r.spaceId = :spaceId " +
           "  AND r.status IN ('PENDING', 'CONFIRMED') " +
           "  AND r.startDatetime < :end " +
           "  AND r.endDatetime > :start")
    List<Reservation> findOverlappingActive(@Param("spaceId") Long spaceId,
                                             @Param("start") Instant start,
                                             @Param("end") Instant end);
}
