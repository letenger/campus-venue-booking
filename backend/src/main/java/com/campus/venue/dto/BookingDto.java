package com.campus.venue.dto;

import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public class BookingDto {
    public Long id;
    public Long userId;
    public Long venueId;
    public String venueName;
    public Long classId;
    public Long clubId;
    public LocalDate bookingDate;
    public LocalTime startTime;
    public LocalTime endTime;
    public String purpose;
    public int attendeesCount;
    public BookingStatus status;
    public String rejectReason;
    public Long approvedBy;
    public Instant approvedAt;
    public Instant checkInAt;
    public Instant cancelledAt;
    public Instant createdAt;
    public boolean canCancel;
    public boolean canApprove;

    public static BookingDto from(Booking b, String venueName, Long currentUserId) {
        BookingDto d = new BookingDto();
        d.id = b.getId();
        d.userId = b.getUserId();
        d.venueId = b.getVenueId();
        d.venueName = venueName;
        d.classId = b.getClassId();
        d.clubId = b.getClubId();
        d.bookingDate = b.getBookingDate();
        d.startTime = b.getStartTime();
        d.endTime = b.getEndTime();
        d.purpose = b.getPurpose() == null ? null : b.getPurpose().name();
        d.attendeesCount = b.getAttendeesCount();
        d.status = b.getStatus();
        d.rejectReason = b.getRejectReason();
        d.approvedBy = b.getApprovedBy();
        d.approvedAt = b.getApprovedAt();
        d.checkInAt = b.getCheckInAt();
        d.cancelledAt = b.getCancelledAt();
        d.createdAt = b.getCreatedAt();
        d.canCancel = b.getStatus() == BookingStatus.PENDING
                || b.getStatus() == BookingStatus.APPROVED;
        d.canApprove = currentUserId != null
                && (b.getStatus() == BookingStatus.PENDING);
        return d;
    }
}
