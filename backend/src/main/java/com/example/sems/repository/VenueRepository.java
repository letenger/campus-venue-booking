package com.example.sems.repository;

import com.example.sems.domain.Venue;
import com.example.sems.domain.VenueStatus;
import com.example.sems.domain.VenueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    Page<Venue> findByStatus(VenueStatus status, Pageable pageable);

    List<Venue> findByType(VenueType type);

    Page<Venue> findByNameContainingIgnoreCaseOrBuildingContainingIgnoreCase(String name, String building, Pageable pageable);
}
