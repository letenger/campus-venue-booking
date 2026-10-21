package com.example.sems.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A candidate time slot for a venue on a given date, with availability verdict.
 */
public class AvailabilitySlot {
    public LocalDate date;
    public LocalTime startTime;
    public LocalTime endTime;
    public boolean available;
    public String reason;

    public AvailabilitySlot(LocalDate date, LocalTime startTime, LocalTime endTime, boolean available, String reason) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
        this.reason = reason;
    }
}
