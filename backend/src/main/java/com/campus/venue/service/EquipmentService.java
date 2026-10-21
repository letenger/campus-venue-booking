package com.campus.venue.service;

import com.campus.venue.common.BadRequestException;
import com.campus.venue.common.ForbiddenException;
import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingStatus;
import com.campus.venue.domain.Equipment;
import com.campus.venue.domain.EquipmentBorrow;
import com.campus.venue.dto.EquipmentBorrowRequest;
import com.campus.venue.dto.EquipmentDto;
import com.campus.venue.repository.BookingRepository;
import com.campus.venue.repository.EquipmentBorrowRepository;
import com.campus.venue.repository.EquipmentRepository;
import com.campus.venue.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentBorrowRepository equipmentBorrowRepository;
    private final BookingRepository bookingRepository;

    public EquipmentService(EquipmentRepository equipmentRepository,
                           EquipmentBorrowRepository equipmentBorrowRepository,
                           BookingRepository bookingRepository) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentBorrowRepository = equipmentBorrowRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<EquipmentDto> list() {
        return equipmentRepository.findAll().stream().map(EquipmentDto::from).toList();
    }

    public EquipmentDto borrow(EquipmentBorrowRequest req) {
        Long uid = SecurityUtils.currentUserId();
        Booking b = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (b.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("Equipment can only be borrowed for approved bookings");
        }
        if (!b.getUserId().equals(uid) && !SecurityUtils.isTeacherOrAdmin()) {
            throw new ForbiddenException("Not allowed to borrow for this booking");
        }
        Equipment e = equipmentRepository.findById(req.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found"));
        if (e.getAvailable() < req.getQty()) {
            throw new BadRequestException("Not enough equipment available");
        }
        e.setAvailable(e.getAvailable() - req.getQty());
        equipmentRepository.save(e);
        equipmentBorrowRepository.save(EquipmentBorrow.builder()
                .bookingId(req.getBookingId())
                .equipmentId(req.getEquipmentId())
                .qty(req.getQty())
                .returned(false)
                .borrowedAt(Instant.now())
                .build());
        return EquipmentDto.from(e);
    }

    public EquipmentDto returnEquipment(Long borrowId) {
        EquipmentBorrow eb = equipmentBorrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow record not found"));
        if (eb.isReturned()) {
            throw new BadRequestException("Already returned");
        }
        Equipment e = equipmentRepository.findById(eb.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found"));
        e.setAvailable(e.getAvailable() + eb.getQty());
        equipmentRepository.save(e);
        eb.setReturned(true);
        eb.setReturnedAt(Instant.now());
        equipmentBorrowRepository.save(eb);
        return EquipmentDto.from(e);
    }
}
