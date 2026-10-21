package com.campus.venue.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long venueId;

    private Long classId;

    private Long clubId;

    private LocalDate bookingDate;

    private LocalTime startTime;

    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private BookingPurpose purpose;

    private int attendeesCount;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private String rejectReason;

    private Long approvedBy;

    private Instant approvedAt;

    private Instant checkInAt;

    private Instant cancelledAt;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = BookingStatus.PENDING;
        }
    }
}
