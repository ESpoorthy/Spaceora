package com.spaceora.availability.repository;

import com.spaceora.availability.entity.AvailabilitySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvailabilityScheduleRepository extends JpaRepository<AvailabilitySchedule, Long> {

    List<AvailabilitySchedule> findBySpaceId(Long spaceId);

    Optional<AvailabilitySchedule> findBySpaceIdAndDayOfWeek(Long spaceId, int dayOfWeek);

    void deleteBySpaceId(Long spaceId);
}
