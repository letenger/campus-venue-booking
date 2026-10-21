package com.campus.venue.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Background jobs. Currently sweeps approved bookings whose end time has passed without
 * a check-in and marks them NO_SHOW. A reminder job can be added the same way.
 */
@Service
public class SchedulerService {

    private final BookingService bookingService;

    public SchedulerService(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void sweepNoShow() {
        bookingService.sweepNoShow();
    }
}
