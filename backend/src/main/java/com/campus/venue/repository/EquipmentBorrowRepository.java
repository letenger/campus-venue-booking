package com.campus.venue.repository;

import com.campus.venue.domain.EquipmentBorrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentBorrowRepository extends JpaRepository<EquipmentBorrow, Long> {
    List<EquipmentBorrow> findByBookingId(Long bookingId);
}
