package com.campus.venue.service;

import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.common.Role;
import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingPurpose;
import com.campus.venue.domain.BookingStatus;
import com.campus.venue.domain.Venue;
import com.campus.venue.domain.VenueSchedule;
import com.campus.venue.domain.VenueStatus;
import com.campus.venue.domain.VenueType;
import com.campus.venue.dto.AvailabilitySlot;
import com.campus.venue.repository.BookingRepository;
import com.campus.venue.repository.VenueRepository;
import com.campus.venue.repository.VenueScheduleRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VenueService}: the availability grid built from weekly schedule
 * windows, and the soft delete that closes a venue.
 */
@ExtendWith(MockitoExtension.class)
class VenueServiceTest extends ServiceTestSupport {

    private static final long VENUE_ID = 1L;
    private static final long ADMIN_ID = 1L;
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);
    private static final LocalDate SUNDAY = MONDAY.plusDays(6);

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueScheduleRepository venueScheduleRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private VenueService service;

    private Venue venue(VenueStatus status) {
        return Venue.builder()
                .id(VENUE_ID)
                .name("Badminton Court")
                .type(VenueType.BADMINTON)
                .capacity(4)
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(21, 0))
                .status(status)
                .build();
    }

    private VenueSchedule window(LocalTime start, LocalTime end) {
        return VenueSchedule.builder()
                .venueId(VENUE_ID)
                .weekday(MONDAY.getDayOfWeek().getValue())
                .startTime(start)
                .endTime(end)
                .build();
    }

    private Booking booking(LocalDate date, LocalTime start, LocalTime end, BookingStatus status) {
        return Booking.builder()
                .id(10L)
                .userId(42L)
                .venueId(VENUE_ID)
                .bookingDate(date)
                .startTime(start)
                .endTime(end)
                .purpose(BookingPurpose.FREE_PLAY)
                .attendeesCount(2)
                .status(status)
                .build();
    }

    private void givenOpenVenue() {
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.of(venue(VenueStatus.OPEN)));
    }

    private void givenNoBookings() {
        when(bookingRepository.findByBookingDateBetween(any(), any())).thenReturn(List.of());
    }

    @Test
    void shouldSplitWeeklyWindowIntoOneHourSlots() {
        givenOpenVenue();
        when(venueScheduleRepository.findByVenueId(VENUE_ID))
                .thenReturn(List.of(window(LocalTime.of(8, 0), LocalTime.of(10, 0))));
        givenNoBookings();

        List<AvailabilitySlot> slots = service.availability(VENUE_ID, MONDAY, MONDAY);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).date).isEqualTo(MONDAY);
        assertThat(slots.get(0).startTime).isEqualTo(LocalTime.of(8, 0));
        assertThat(slots.get(0).endTime).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(0).available).isTrue();
        assertThat(slots.get(0).reason).isNull();
        assertThat(slots.get(1).startTime).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(1).endTime).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void shouldClipTheLastSlotToTheWindowEnd() {
        givenOpenVenue();
        when(venueScheduleRepository.findByVenueId(VENUE_ID))
                .thenReturn(List.of(window(LocalTime.of(8, 0), LocalTime.of(9, 30))));
        givenNoBookings();

        List<AvailabilitySlot> slots = service.availability(VENUE_ID, MONDAY, MONDAY);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(1).startTime).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(1).endTime).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void shouldBlockSlotsOverlappingAnActiveBooking() {
        givenOpenVenue();
        when(venueScheduleRepository.findByVenueId(VENUE_ID))
                .thenReturn(List.of(window(LocalTime.of(8, 0), LocalTime.of(10, 0))));
        // a pending booking contained inside the first hour must block that slot only
        when(bookingRepository.findByBookingDateBetween(any(), any()))
                .thenReturn(List.of(booking(MONDAY, LocalTime.of(8, 15), LocalTime.of(8, 45),
                        BookingStatus.PENDING)));

        List<AvailabilitySlot> slots = service.availability(VENUE_ID, MONDAY, MONDAY);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).available).isFalse();
        assertThat(slots.get(0).reason).isEqualTo("Already booked");
        assertThat(slots.get(1).available).isTrue();
    }

    @Test
    void shouldIgnoreCancelledAndRejectedBookings() {
        givenOpenVenue();
        when(venueScheduleRepository.findByVenueId(VENUE_ID))
                .thenReturn(List.of(window(LocalTime.of(8, 0), LocalTime.of(10, 0))));
        when(bookingRepository.findByBookingDateBetween(any(), any())).thenReturn(List.of(
                booking(MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), BookingStatus.CANCELLED),
                booking(MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), BookingStatus.REJECTED)));

        List<AvailabilitySlot> slots = service.availability(VENUE_ID, MONDAY, MONDAY);

        assertThat(slots).allMatch(s -> s.available);
    }

    @Test
    void shouldOnlyOpenSlotsOnScheduledWeekdays() {
        givenOpenVenue();
        when(venueScheduleRepository.findByVenueId(VENUE_ID))
                .thenReturn(List.of(window(LocalTime.of(8, 0), LocalTime.of(10, 0))));
        givenNoBookings();

        // a full week of range, but the venue only opens on Monday
        List<AvailabilitySlot> slots = service.availability(VENUE_ID, MONDAY, SUNDAY);

        assertThat(slots).hasSize(2);
        assertThat(slots).allMatch(s -> s.date.equals(MONDAY));
    }

    @Test
    void shouldBlockEverythingWhenVenueIsClosed() {
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.of(venue(VenueStatus.CLOSED)));
        when(venueScheduleRepository.findByVenueId(VENUE_ID))
                .thenReturn(List.of(window(LocalTime.of(8, 0), LocalTime.of(10, 0))));
        givenNoBookings();

        List<AvailabilitySlot> slots = service.availability(VENUE_ID, MONDAY, MONDAY);

        assertThat(slots).hasSize(2);
        assertThat(slots).noneMatch(s -> s.available);
        assertThat(slots.get(0).reason).isEqualTo("Venue closed");
    }

    @Test
    void shouldBlockEverythingWhenVenueIsUnderMaintenance() {
        when(venueRepository.findById(VENUE_ID))
                .thenReturn(Optional.of(venue(VenueStatus.MAINTENANCE)));
        when(venueScheduleRepository.findByVenueId(VENUE_ID))
                .thenReturn(List.of(window(LocalTime.of(8, 0), LocalTime.of(10, 0))));
        givenNoBookings();

        List<AvailabilitySlot> slots = service.availability(VENUE_ID, MONDAY, MONDAY);

        assertThat(slots).allMatch(s -> "Venue under maintenance".equals(s.reason));
    }

    @Test
    void throwsResourceNotFoundExceptionWhenVenueForAvailabilityMissing() {
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.availability(VENUE_ID, MONDAY, MONDAY))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found");
    }

    @Test
    void shouldCloseVenueInsteadOfHardDeletingIt() {
        givenCurrentUser(ADMIN_ID, Role.ADMIN);
        when(venueRepository.findById(VENUE_ID)).thenReturn(Optional.of(venue(VenueStatus.OPEN)));

        service.delete(VENUE_ID);

        ArgumentCaptor<Venue> saved = ArgumentCaptor.forClass(Venue.class);
        verify(venueRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(VenueStatus.CLOSED);
        verify(auditService).record(eq(ADMIN_ID), eq("CLOSE_VENUE"), eq("VENUE"),
                eq(VENUE_ID), any());
    }
}
