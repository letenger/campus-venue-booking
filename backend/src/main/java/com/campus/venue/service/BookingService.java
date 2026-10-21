package com.campus.venue.service;

import com.campus.venue.common.BadRequestException;
import com.campus.venue.common.ConflictException;
import com.campus.venue.common.ForbiddenException;
import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.common.Role;
import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingPurpose;
import com.campus.venue.domain.BookingStatus;
import com.campus.venue.domain.EquipmentBorrow;
import com.campus.venue.domain.NotificationType;
import com.campus.venue.domain.User;
import com.campus.venue.domain.Venue;
import com.campus.venue.domain.VenueStatus;
import com.campus.venue.dto.BookingDto;
import com.campus.venue.dto.BookingRequest;
import com.campus.venue.repository.BookingRepository;
import com.campus.venue.repository.EquipmentBorrowRepository;
import com.campus.venue.repository.EquipmentRepository;
import com.campus.venue.repository.UserRepository;
import com.campus.venue.repository.VenueRepository;
import com.campus.venue.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final VenueRepository venues;
    private final UserRepository users;
    private final EquipmentRepository equipment;
    private final EquipmentBorrowRepository borrows;
    private final NotificationService notifications;
    private final AuditService audit;

    public BookingService(BookingRepository bookings, VenueRepository venues, UserRepository users,
                          EquipmentRepository equipment, EquipmentBorrowRepository borrows,
                          NotificationService notifications, AuditService audit) {
        this.bookings = bookings;
        this.venues = venues;
        this.users = users;
        this.equipment = equipment;
        this.borrows = borrows;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional
    public BookingDto create(BookingRequest req, Long userId) {
        Venue venue = venues.findById(req.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        if (venue.getStatus() != VenueStatus.OPEN) {
            throw new BadRequestException("This venue is not open for booking");
        }
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        if (req.getStartTime().isBefore(venue.getOpenTime()) || req.getEndTime().isAfter(venue.getCloseTime())) {
            throw new BadRequestException("Requested time is outside the opening hours");
        }
        if (req.getAttendeesCount() > venue.getCapacity()) {
            throw new BadRequestException("That many people do not fit in this venue");
        }
        if (!bookings.findOverlapping(venue.getId(), req.getBookingDate(),
                req.getStartTime(), req.getEndTime()).isEmpty()) {
            throw new ConflictException("That slot is already booked");
        }

        // Small free play is trusted; anything that takes the venue from other people waits.
        boolean needsApproval = needsApproval(req);
        Booking booking = bookings.save(Booking.builder()
                .userId(userId)
                .venueId(venue.getId())
                .classId(req.getClassId())
                .clubId(req.getClubId())
                .bookingDate(req.getBookingDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .purpose(req.getPurpose())
                .attendeesCount(req.getAttendeesCount())
                .status(needsApproval ? BookingStatus.PENDING : BookingStatus.APPROVED)
                .approvedBy(needsApproval ? null : userId)
                .approvedAt(needsApproval ? null : Instant.now())
                .build());

        if (needsApproval) {
            alertTeachers(booking, venue);
            notifications.notify(userId, NotificationType.BOOKING_CREATED,
                    "Booking submitted", venue.getName() + " is waiting for a teacher", booking.getId());
        } else {
            notifications.notify(userId, NotificationType.APPROVED,
                    "Booking confirmed", venue.getName() + " is yours", booking.getId());
        }

        audit.record(userId, "CREATE_BOOKING", "BOOKING", booking.getId(),
                venue.getName() + " " + req.getBookingDate());
        return dto(booking, venue.getName());
    }

    /**
     * A request needs approval when it takes the venue away from other people:
     * a class, a club, an event or exam slot, or a group of ten and more.
     */
    private boolean needsApproval(BookingRequest req) {
        if (req.getClassId() != null || req.getClubId() != null || req.getAttendeesCount() >= 10) {
            return true;
        }
        return req.getPurpose() == BookingPurpose.PE_CLASS
                || req.getPurpose() == BookingPurpose.CLUB_TRAINING
                || req.getPurpose() == BookingPurpose.EVENT
                || req.getPurpose() == BookingPurpose.EXAM;
    }

    private void alertTeachers(Booking booking, Venue venue) {
        for (User teacher : users.findByRole(Role.TEACHER)) {
            notifications.notify(teacher.getId(), NotificationType.APPROVAL_REQUIRED,
                    "Booking needs a decision", venue.getName() + " has a new request", booking.getId());
        }
    }

    public BookingDto get(Long id) {
        Booking booking = load(id);
        checkCanRead(booking);
        return dto(booking, venueName(booking));
    }

    public Page<BookingDto> list(Long venueId, BookingStatus status, LocalDate from, LocalDate to,
                                 Boolean mine, Pageable pageable) {
        Long userId = SecurityUtils.currentUserId();
        // Students only ever see their own rows, whatever the query asks for.
        Boolean onlyMine = SecurityUtils.currentRole() == Role.STUDENT ? Boolean.TRUE : mine;
        return bookings.search(venueId, status, from, to, onlyMine, userId, pageable)
                .map(b -> dto(b, venueName(b)));
    }

    public BookingDto update(Long id, BookingRequest req) {
        Booking booking = load(id);
        if (!booking.getUserId().equals(SecurityUtils.currentUserId())) {
            throw new ForbiddenException("Only the owner can edit this booking");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be edited");
        }
        booking.setBookingDate(req.getBookingDate());
        booking.setStartTime(req.getStartTime());
        booking.setEndTime(req.getEndTime());
        booking.setPurpose(req.getPurpose());
        booking.setAttendeesCount(req.getAttendeesCount());
        booking.setClassId(req.getClassId());
        booking.setClubId(req.getClubId());
        bookings.save(booking);
        audit.record(SecurityUtils.currentUserId(), "UPDATE_BOOKING", "BOOKING", id, null);
        return dto(booking, venueName(booking));
    }

    @Transactional
    public BookingDto cancel(Long id) {
        Booking booking = load(id);
        if (!booking.getUserId().equals(SecurityUtils.currentUserId())) {
            throw new ForbiddenException("Only the owner can cancel this booking");
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("This booking can no longer be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        bookings.save(booking);
        giveBackEquipment(booking);
        notifications.notify(booking.getUserId(), NotificationType.CANCELLED,
                "Booking cancelled", "The slot has been released", booking.getId());
        audit.record(SecurityUtils.currentUserId(), "CANCEL_BOOKING", "BOOKING", id, null);
        return dto(booking, venueName(booking));
    }

    public BookingDto approve(Long id) {
        Booking booking = requirePending(id);
        booking.setStatus(BookingStatus.APPROVED);
        booking.setApprovedBy(SecurityUtils.currentUserId());
        booking.setApprovedAt(Instant.now());
        bookings.save(booking);
        notifications.notify(booking.getUserId(), NotificationType.APPROVED,
                "Booking approved", "Your request was approved", booking.getId());
        audit.record(SecurityUtils.currentUserId(), "APPROVE_BOOKING", "BOOKING", id, null);
        return dto(booking, venueName(booking));
    }

    public BookingDto reject(Long id, String reason) {
        Booking booking = requirePending(id);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectReason(reason);
        bookings.save(booking);
        notifications.notify(booking.getUserId(), NotificationType.REJECTED,
                "Booking rejected",
                "Reason: " + (reason == null || reason.isBlank() ? "not given" : reason), booking.getId());
        audit.record(SecurityUtils.currentUserId(), "REJECT_BOOKING", "BOOKING", id, reason);
        return dto(booking, venueName(booking));
    }

    public BookingDto checkIn(Long id) {
        Booking booking = load(id);
        requireStaff();
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("Only approved bookings can be checked in");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCheckInAt(Instant.now());
        bookings.save(booking);
        audit.record(SecurityUtils.currentUserId(), "CHECKIN_BOOKING", "BOOKING", id, null);
        return dto(booking, venueName(booking));
    }

    public BookingDto markNoShow(Long id) {
        Booking booking = load(id);
        requireStaff();
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("Only approved bookings can be marked no-show");
        }
        markNoShow(booking);
        return dto(booking, venueName(booking));
    }

    /**
     * Runs every minute from SchedulerService. An approved booking that ran past its end
     * time without a check-in becomes a no-show, and the owner is told.
     */
    @Transactional
    public void sweepNoShow() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> overdue = bookings.findByStatus(BookingStatus.APPROVED).stream()
                .filter(b -> LocalDateTime.of(b.getBookingDate(), b.getEndTime()).isBefore(now))
                .toList();
        for (Booking booking : overdue) {
            markNoShow(booking);
            audit.record(null, "AUTO_NOSHOW", "BOOKING", booking.getId(), null);
        }
    }

    private void markNoShow(Booking booking) {
        booking.setStatus(BookingStatus.NO_SHOW);
        bookings.save(booking);
        notifications.notify(booking.getUserId(), NotificationType.NO_SHOW,
                "Marked as no-show", "The venue was not used during the booked time", booking.getId());
    }

    private void giveBackEquipment(Booking booking) {
        for (EquipmentBorrow borrow : borrows.findByBookingId(booking.getId())) {
            equipment.findById(borrow.getEquipmentId()).ifPresent(item -> {
                item.setAvailable(Math.min(item.getTotal(), item.getAvailable() + borrow.getQty()));
                equipment.save(item);
            });
            borrow.setReturned(true);
            borrows.save(borrow);
        }
    }

    private Booking requirePending(Long id) {
        Booking booking = load(id);
        requireStaff();
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be decided");
        }
        return booking;
    }

    private Booking load(Long id) {
        return bookings.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private void checkCanRead(Booking booking) {
        if (SecurityUtils.currentRole() == Role.STUDENT
                && !booking.getUserId().equals(SecurityUtils.currentUserId())) {
            throw new ForbiddenException("You can only read your own bookings");
        }
    }

    private void requireStaff() {
        if (!SecurityUtils.isTeacherOrAdmin()) {
            throw new ForbiddenException("Only a teacher or an admin can do this");
        }
    }

    private String venueName(Booking booking) {
        return venues.findById(booking.getVenueId()).map(Venue::getName).orElse("");
    }

    private BookingDto dto(Booking booking, String venueName) {
        return BookingDto.from(booking, venueName, requesterName(booking), SecurityUtils.currentUserId());
    }

    /** Approvals read better with a name than with an id. */
    private String requesterName(Booking booking) {
        return users.findById(booking.getUserId())
                .map(User::getRealName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(null);
    }
}
