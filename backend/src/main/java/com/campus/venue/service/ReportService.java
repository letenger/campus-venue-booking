package com.campus.venue.service;

import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingStatus;
import com.campus.venue.domain.Venue;
import com.campus.venue.dto.BookingSummaryReport;
import com.campus.venue.dto.UtilizationReport;
import com.campus.venue.repository.BookingRepository;
import com.campus.venue.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final BookingRepository bookings;
    private final VenueRepository venues;

    public ReportService(BookingRepository bookings, VenueRepository venues) {
        this.bookings = bookings;
        this.venues = venues;
    }

    /**
     * One query for the whole range, then counted in memory. The earlier version ran
     * six count queries per venue, which is 48 round trips for a six-venue school.
     */
    public List<UtilizationReport> utilization(Long venueId, LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now() : to;

        List<Venue> targets = venueId == null
                ? venues.findAll()
                : List.of(venues.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found")));

        Map<Long, List<Booking>> byVenue = bookings.findByBookingDateBetween(start, end).stream()
                .collect(Collectors.groupingBy(Booking::getVenueId));

        List<UtilizationReport> report = new ArrayList<>();
        for (Venue venue : targets) {
            List<Booking> rows = byVenue.getOrDefault(venue.getId(), List.of());
            report.add(new UtilizationReport(
                    venue.getId(), venue.getName(), venue.getType(),
                    rows.size(),
                    count(rows, BookingStatus.COMPLETED),
                    count(rows, BookingStatus.CANCELLED),
                    count(rows, BookingStatus.NO_SHOW)));
        }
        return report;
    }

    public BookingSummaryReport summary(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now() : to;

        List<Booking> rows = bookings.findByBookingDateBetween(start, end);

        Map<String, Long> byStatus = rows.stream()
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));

        Map<Long, String> names = venues.findAll().stream()
                .collect(Collectors.toMap(Venue::getId, Venue::getName, (a, b) -> a));

        Map<String, Long> topVenues = rows.stream()
                .collect(Collectors.groupingBy(b -> names.getOrDefault(b.getVenueId(), "Unknown"), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        return new BookingSummaryReport(rows.size(), byStatus, topVenues);
    }

    private long count(List<Booking> rows, BookingStatus status) {
        return rows.stream().filter(b -> b.getStatus() == status).count();
    }
}
