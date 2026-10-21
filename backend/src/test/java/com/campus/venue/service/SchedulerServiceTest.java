package com.campus.venue.service;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link SchedulerService}: the scheduled sweep must delegate to the
 * booking service without adding its own behaviour.
 */
@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private SchedulerService service;

    @Test
    void shouldDelegateSweepToBookingService() {
        service.sweepNoShow();

        verify(bookingService).sweepNoShow();
    }
}
