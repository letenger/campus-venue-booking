package com.campus.venue.repository;

import com.campus.venue.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByVenueId(Long venueId);
}
