package com.example.sems.repository;

import com.example.sems.domain.EquipmentBorrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentBorrowRepository extends JpaRepository<EquipmentBorrow, Long> {
    List<EquipmentBorrow> findByBookingId(Long bookingId);
}
