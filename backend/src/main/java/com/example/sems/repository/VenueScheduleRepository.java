package com.example.sems.repository;

import com.example.sems.domain.VenueSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueScheduleRepository extends JpaRepository<VenueSchedule, Long> {
    List<VenueSchedule> findByVenueId(Long venueId);

    void deleteByVenueId(Long venueId);
}
