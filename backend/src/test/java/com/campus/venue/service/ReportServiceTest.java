package com.campus.venue.service;

import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingPurpose;
import com.campus.venue.domain.BookingStatus;
import com.campus.venue.domain.Venue;
import com.campus.venue.domain.VenueStatus;
import com.campus.venue.domain.VenueType;
import com.campus.venue.dto.BookingSummaryReport;
import com.campus.venue.dto.UtilizationReport;
import com.campus.venue.repository.BookingRepository;
import com.campus.venue.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReportService}: utilisation aggregation, default date range,
 * single-venue lookup and the booking summary with its top-5 venue ranking.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 31);

    @Mock
    private BookingRepository bookings;

    @Mock
    private VenueRepository venues;

    @InjectMocks
    private ReportService service;

    private Venue venue(long id, String name) {
        return Venue.builder()
                .id(id)
                .name(name)
                .type(VenueType.BADMINTON)
                .capacity(10)
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(21, 0))
                .status(VenueStatus.OPEN)
                .build();
    }

    private Booking booking(long id, long venueId, BookingStatus status) {
        return Booking.builder()
                .id(id)
                .userId(1L)
                .venueId(venueId)
                .bookingDate(FROM.plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .purpose(BookingPurpose.FREE_PLAY)
                .attendeesCount(2)
                .status(status)
                .build();
    }

    @Test
    void shouldCountEachStatusPerVenue() {
        when(venues.findAll()).thenReturn(List.of(venue(1L, "Gym"), venue(2L, "Pool")));
        when(bookings.findByBookingDateBetween(FROM, TO)).thenReturn(List.of(
                booking(1L, 1L, BookingStatus.COMPLETED),
                booking(2L, 1L, BookingStatus.CANCELLED),
                booking(3L, 1L, BookingStatus.NO_SHOW),
                booking(4L, 1L, BookingStatus.APPROVED),
                booking(5L, 2L, BookingStatus.COMPLETED)));

        List<UtilizationReport> report = service.utilization(null, FROM, TO);

        assertThat(report).hasSize(2);
        UtilizationReport gym = report.get(0);
        assertThat(gym.venueId).isEqualTo(1L);
        assertThat(gym.venueName).isEqualTo("Gym");
        assertThat(gym.total).isEqualTo(4);
        assertThat(gym.completed).isEqualTo(1);
        assertThat(gym.cancelled).isEqualTo(1);
        assertThat(gym.noShow).isEqualTo(1);
        assertThat(gym.utilizationRate).isEqualTo(25.0);
        assertThat(report.get(1).total).isEqualTo(1);
    }

    @Test
    void shouldReportZeroRateForVenueWithoutBookings() {
        when(venues.findAll()).thenReturn(List.of(venue(1L, "Gym")));
        when(bookings.findByBookingDateBetween(FROM, TO)).thenReturn(List.of());

        List<UtilizationReport> report = service.utilization(null, FROM, TO);

        assertThat(report).hasSize(1);
        assertThat(report.get(0).total).isZero();
        assertThat(report.get(0).utilizationRate).isZero();
    }

    @Test
    void shouldDefaultToTheLastThirtyDaysWhenNoRangeGiven() {
        when(venues.findAll()).thenReturn(List.of());

        service.utilization(null, null, null);

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(bookings).findByBookingDateBetween(from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(LocalDate.now().minusDays(30));
        assertThat(to.getValue()).isEqualTo(LocalDate.now());
    }

    @Test
    void shouldReportOnlyTheRequestedVenue() {
        when(venues.findById(1L)).thenReturn(Optional.of(venue(1L, "Gym")));
        when(bookings.findByBookingDateBetween(FROM, TO))
                .thenReturn(List.of(booking(1L, 1L, BookingStatus.COMPLETED)));

        List<UtilizationReport> report = service.utilization(1L, FROM, TO);

        assertThat(report).hasSize(1);
        assertThat(report.get(0).completed).isEqualTo(1);
        assertThat(report.get(0).utilizationRate).isEqualTo(100.0);
    }

    @Test
    void throwsResourceNotFoundExceptionWhenReportedVenueMissing() {
        when(venues.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.utilization(1L, FROM, TO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found");
    }

    @Test
    void shouldSummariseStatusCountsAndTopVenues() {
        when(venues.findAll()).thenReturn(List.of(venue(1L, "Gym"), venue(2L, "Pool")));
        when(bookings.findByBookingDateBetween(FROM, TO)).thenReturn(List.of(
                booking(1L, 1L, BookingStatus.COMPLETED),
                booking(2L, 1L, BookingStatus.CANCELLED),
                booking(3L, 2L, BookingStatus.APPROVED),
                booking(4L, 2L, BookingStatus.APPROVED),
                booking(5L, 99L, BookingStatus.COMPLETED)));

        BookingSummaryReport summary = service.summary(FROM, TO);

        assertThat(summary.total).isEqualTo(5);
        assertThat(summary.byStatus)
                .containsEntry("COMPLETED", 2L)
                .containsEntry("CANCELLED", 1L)
                .containsEntry("APPROVED", 2L);
        // venues with no matching Venue row are grouped under "Unknown"
        assertThat(summary.topVenues)
                .containsEntry("Pool", 2L)
                .containsEntry("Gym", 2L)
                .containsEntry("Unknown", 1L);
    }

    @Test
    void shouldLimitTopVenuesToFiveSortedByCountDescending() {
        // distinct counts keep the expected order deterministic regardless of map iteration order
        when(venues.findAll()).thenReturn(List.of(
                venue(1L, "V1"), venue(2L, "V2"), venue(3L, "V3"),
                venue(4L, "V4"), venue(5L, "V5"), venue(6L, "V6")));
        when(bookings.findByBookingDateBetween(FROM, TO)).thenReturn(List.of(
                booking(1L, 1L, BookingStatus.COMPLETED),
                booking(2L, 1L, BookingStatus.COMPLETED),
                booking(3L, 1L, BookingStatus.COMPLETED),
                booking(4L, 1L, BookingStatus.COMPLETED),
                booking(5L, 1L, BookingStatus.COMPLETED),
                booking(6L, 1L, BookingStatus.COMPLETED),
                booking(7L, 2L, BookingStatus.COMPLETED),
                booking(8L, 2L, BookingStatus.COMPLETED),
                booking(9L, 2L, BookingStatus.COMPLETED),
                booking(10L, 2L, BookingStatus.COMPLETED),
                booking(11L, 2L, BookingStatus.COMPLETED),
                booking(12L, 3L, BookingStatus.COMPLETED),
                booking(13L, 3L, BookingStatus.COMPLETED),
                booking(14L, 3L, BookingStatus.COMPLETED),
                booking(15L, 3L, BookingStatus.COMPLETED),
                booking(16L, 4L, BookingStatus.COMPLETED),
                booking(17L, 4L, BookingStatus.COMPLETED),
                booking(18L, 4L, BookingStatus.COMPLETED),
                booking(19L, 5L, BookingStatus.COMPLETED),
                booking(20L, 5L, BookingStatus.COMPLETED),
                booking(21L, 6L, BookingStatus.COMPLETED)));

        BookingSummaryReport summary = service.summary(FROM, TO);

        assertThat(summary.topVenues).hasSize(5);
        assertThat(summary.topVenues.keySet())
                .containsExactly("V1", "V2", "V3", "V4", "V5");
        assertThat(summary.topVenues.get("V1")).isEqualTo(6L);
        assertThat(summary.topVenues.get("V5")).isEqualTo(2L);
    }
}
