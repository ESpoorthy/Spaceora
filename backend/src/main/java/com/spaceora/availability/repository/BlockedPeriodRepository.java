package com.spaceora.availability.repository;

import com.spaceora.availability.entity.BlockedPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface BlockedPeriodRepository extends JpaRepository<BlockedPeriod, Long> {

    List<BlockedPeriod> findBySpaceId(Long spaceId);

    /**
     * Returns any blocked periods for the space whose window overlaps [start, end).
     * Two intervals overlap when: existingStart < end AND existingEnd > start.
     */
    @Query("SELECT bp FROM BlockedPeriod bp " +
           "WHERE bp.spaceId = :spaceId " +
           "  AND bp.startDatetime < :end " +
           "  AND bp.endDatetime > :start")
    List<BlockedPeriod> findOverlapping(@Param("spaceId") Long spaceId,
                                        @Param("start") Instant start,
                                        @Param("end") Instant end);

    /**
     * Returns blocked periods within or overlapping a date range for availability view.
     */
    @Query("SELECT bp FROM BlockedPeriod bp " +
           "WHERE bp.spaceId = :spaceId " +
           "  AND bp.startDatetime < :rangeEnd " +
           "  AND bp.endDatetime > :rangeStart")
    List<BlockedPeriod> findBySpaceIdAndRange(@Param("spaceId") Long spaceId,
                                               @Param("rangeStart") Instant rangeStart,
                                               @Param("rangeEnd") Instant rangeEnd);
}
