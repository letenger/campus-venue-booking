package com.campus.venue.repository;

import com.campus.venue.domain.Venue;
import com.campus.venue.domain.VenueStatus;
import com.campus.venue.domain.VenueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    Page<Venue> findByStatus(VenueStatus status, Pageable pageable);

    List<Venue> findByType(VenueType type);

    Page<Venue> findByNameContainingIgnoreCaseOrBuildingContainingIgnoreCase(String name, String building, Pageable pageable);
}
