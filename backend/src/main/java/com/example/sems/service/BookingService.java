package com.example.sems.service;

import com.example.sems.common.BadRequestException;
import com.example.sems.common.ConflictException;
import com.example.sems.common.ForbiddenException;
import com.example.sems.common.ResourceNotFoundException;
import com.example.sems.common.Role;
import com.example.sems.domain.*;
import com.example.sems.dto.BookingDto;
import com.example.sems.dto.BookingRequest;
import com.example.sems.dto.VenueDto;
import com.example.sems.repository.*;
import com.example.sems.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentBorrowRepository equipmentBorrowRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public BookingService(BookingRepository bookingRepository, VenueRepository venueRepository,
                          UserRepository userRepository, EquipmentRepository equipmentRepository,
                          EquipmentBorrowRepository equipmentBorrowRepository,
                          NotificationService notificationService, AuditService auditService) {
        this.bookingRepository = bookingRepository;
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.equipmentRepository = equipmentRepository;
        this.equipmentBorrowRepository = equipmentBorrowRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public BookingDto create(BookingRequest req, Long userId) {
        Venue v = venueRepository.findById(req.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        if (v.getStatus() != VenueStatus.OPEN) {
            throw new BadRequestException("Venue is not open for booking");
        }
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        if (req.getStartTime().isBefore(v.getOpenTime()) || req.getEndTime().isAfter(v.getCloseTime())) {
            throw new BadRequestException("Booking is outside the venue's open hours");
        }
        if (!bookingRepository.findOverlapping(req.getVenueId(), req.getBookingDate(),
                req.getStartTime(), req.getEndTime()).isEmpty()) {
            throw new ConflictException("This time slot is already booked");
        }

        Booking b = Booking.builder()
                .userId(userId)
                .venueId(req.getVenueId())
                .classId(req.getClassId())
                .clubId(req.getClubId())
                .bookingDate(req.getBookingDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .purpose(req.getPurpose())
                .attendeesCount(req.getAttendeesCount())
                .status(BookingStatus.PENDING)
                .build();
        b = bookingRepository.save(b);

        boolean needsApproval = needsApproval(req);
        if (needsApproval) {
            notifyTeachersApprovalRequired(b);
        } else {
            b.setStatus(BookingStatus.APPROVED);
            b.setApprovedAt(java.time.Instant.now());
            b.setApprovedBy(userId);
            b = bookingRepository.save(b);
            notificationService.notify(userId, NotificationType.APPROVED,
                    "预订已通过", "您预订的" + v.getName() + "已自动通过", b.getId());
        }
        notificationService.notify(userId, NotificationType.BOOKING_CREATED,
                "预订已提交", "您对" + v.getName() + "的预订已提交", b.getId());
        auditService.record(userId, "CREATE_BOOKING", "BOOKING", b.getId(),
                v.getName() + " " + req.getBookingDate());
        return toDto(b, v.getName());
    }

    private boolean needsApproval(BookingRequest req) {
        return req.getPurpose() == BookingPurpose.CLUB_TRAINING
                || req.getPurpose() == BookingPurpose.EVENT
                || req.getPurpose() == BookingPurpose.EXAM
                || req.getClassId() != null
                || req.getClubId() != null
                || req.getAttendeesCount() >= 10;
    }

    private void notifyTeachersApprovalRequired(Booking b) {
        String venueName = venueRepository.findById(b.getVenueId()).map(Venue::getName).orElse("");
        for (User teacher : userRepository.findByRole(Role.TEACHER)) {
            notificationService.notify(teacher.getId(), NotificationType.APPROVAL_REQUIRED,
                    "待审批预订", venueName + " 有新的预订待审批", b.getId());
        }
    }

    public BookingDto get(Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        authorizeOwnerOrStaff(b);
        return toDto(b, venueName(b));
    }

    public Page<BookingDto> list(Long venueId, BookingStatus status, LocalDate from, LocalDate to,
                                Boolean mine, Pageable pageable) {
        Long uid = SecurityUtils.currentUserId();
        boolean forceMine = SecurityUtils.currentRole() == Role.STUDENT;
        Boolean effectiveMine = forceMine ? Boolean.TRUE : mine;
        return bookingRepository.search(venueId, status, from, to, effectiveMine, uid, pageable)
                .map(b -> toDto(b, venueName(b)));
    }

    public BookingDto update(Long id, BookingRequest req) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!b.getUserId().equals(SecurityUtils.currentUserId())) {
            throw new ForbiddenException("Only the owner can edit this booking");
        }
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be edited");
        }
        b.setBookingDate(req.getBookingDate());
        b.setStartTime(req.getStartTime());
        b.setEndTime(req.getEndTime());
        b.setPurpose(req.getPurpose());
        b.setAttendeesCount(req.getAttendeesCount());
        b.setClassId(req.getClassId());
        b.setClubId(req.getClubId());
        b = bookingRepository.save(b);
        auditService.record(SecurityUtils.currentUserId(), "UPDATE_BOOKING", "BOOKING", id, null);
        return toDto(b, venueName(b));
    }

    public BookingDto cancel(Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!b.getUserId().equals(SecurityUtils.currentUserId())) {
            throw new ForbiddenException("Only the owner can cancel this booking");
        }
        if (b.getStatus() != BookingStatus.PENDING && b.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("This booking cannot be cancelled");
        }
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelledAt(java.time.Instant.now());
        b = bookingRepository.save(b);
        returnEquipment(b);
        notificationService.notify(b.getUserId(), NotificationType.CANCELLED,
                "预订已取消", "您的预订已取消", b.getId());
        auditService.record(SecurityUtils.currentUserId(), "CANCEL_BOOKING", "BOOKING", id, null);
        return toDto(b, venueName(b));
    }

    public BookingDto approve(Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        requireStaff();
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be approved");
        }
        b.setStatus(BookingStatus.APPROVED);
        b.setApprovedBy(SecurityUtils.currentUserId());
        b.setApprovedAt(java.time.Instant.now());
        b = bookingRepository.save(b);
        notificationService.notify(b.getUserId(), NotificationType.APPROVED,
                "预订已通过", "您的预订已通过审批", b.getId());
        auditService.record(SecurityUtils.currentUserId(), "APPROVE_BOOKING", "BOOKING", id, null);
        return toDto(b, venueName(b));
    }

    public BookingDto reject(Long id, String reason) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        requireStaff();
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be rejected");
        }
        b.setStatus(BookingStatus.REJECTED);
        b.setRejectReason(reason);
        b = bookingRepository.save(b);
        notificationService.notify(b.getUserId(), NotificationType.REJECTED,
                "预订被拒绝", "原因：" + (reason == null ? "未说明" : reason), b.getId());
        auditService.record(SecurityUtils.currentUserId(), "REJECT_BOOKING", "BOOKING", id, reason);
        return toDto(b, venueName(b));
    }

    public BookingDto checkIn(Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        requireStaff();
        if (b.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("Only approved bookings can be checked in");
        }
        b.setStatus(BookingStatus.COMPLETED);
        b.setCheckInAt(java.time.Instant.now());
        b = bookingRepository.save(b);
        auditService.record(SecurityUtils.currentUserId(), "CHECKIN_BOOKING", "BOOKING", id, null);
        return toDto(b, venueName(b));
    }

    public BookingDto markNoShow(Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        requireStaff();
        if (b.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("Only approved bookings can be marked no-show");
        }
        b.setStatus(BookingStatus.NO_SHOW);
        b = bookingRepository.save(b);
        notificationService.notify(b.getUserId(), NotificationType.NO_SHOW,
                "爽约记录", "您未按时使用场馆，已记录爽约", b.getId());
        auditService.record(SecurityUtils.currentUserId(), "NOSHOW_BOOKING", "BOOKING", id, null);
        return toDto(b, venueName(b));
    }

    void sweepNoShow() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> due = bookingRepository.findByStatus(BookingStatus.APPROVED).stream()
                .filter(b -> LocalDateTime.of(b.getBookingDate(), b.getEndTime()).isBefore(now))
                .toList();
        for (Booking b : due) {
            b.setStatus(BookingStatus.NO_SHOW);
            bookingRepository.save(b);
            notificationService.notify(b.getUserId(), NotificationType.NO_SHOW,
                    "爽约记录", "您未按时使用场馆，已记录爽约", b.getId());
            auditService.record(null, "AUTO_NOSHOW", "BOOKING", b.getId(), null);
        }
    }

    private void returnEquipment(Booking b) {
        equipmentBorrowRepository.findByBookingId(b.getId()).forEach(eb -> {
            Equipment e = equipmentRepository.findById(eb.getEquipmentId()).orElse(null);
            if (e != null) {
                e.setAvailable(e.getAvailable() + eb.getQty());
                equipmentRepository.save(e);
            }
            eb.setReturned(true);
            equipmentBorrowRepository.save(eb);
        });
    }

    private void authorizeOwnerOrStaff(Booking b) {
        if (SecurityUtils.currentRole() == Role.STUDENT
                && !b.getUserId().equals(SecurityUtils.currentUserId())) {
            throw new ForbiddenException("You can only view your own bookings");
        }
    }

    private void requireStaff() {
        if (!SecurityUtils.isTeacherOrAdmin()) {
            throw new ForbiddenException("Only teachers or admins can perform this action");
        }
    }

    private String venueName(Booking b) {
        return venueRepository.findById(b.getVenueId()).map(Venue::getName).orElse("");
    }

    private BookingDto toDto(Booking b, String venueName) {
        return BookingDto.from(b, venueName, SecurityUtils.currentUserId());
    }
}
