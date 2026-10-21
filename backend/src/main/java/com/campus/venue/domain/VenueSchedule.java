package com.campus.venue.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Recurring weekly availability template for a venue (e.g. "every Mon/Wed/Fri 08:00-18:00").
 * A booking is only valid inside an open schedule window and outside maintenance blocks.
 */
@Entity
@Table(name = "venue_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long venueId;

    /** 1=Monday ... 7=Sunday */
    private int weekday;

    private LocalTime startTime;

    private LocalTime endTime;
}
