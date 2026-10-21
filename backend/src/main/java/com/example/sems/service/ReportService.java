package com.example.sems.service;

import com.example.sems.common.ResourceNotFoundException;
import com.example.sems.domain.Booking;
import com.example.sems.domain.BookingStatus;
import com.example.sems.domain.Venue;
import com.example.sems.dto.BookingSummaryReport;
import com.example.sems.dto.UtilizationReport;
import com.example.sems.repository.BookingRepository;
import com.example.sems.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;

    public ReportService(BookingRepository bookingRepository, VenueRepository venueRepository) {
        this.bookingRepository = bookingRepository;
        this.venueRepository = venueRepository;
    }

    public List<UtilizationReport> utilization(Long venueId, LocalDate from, LocalDate to) {
        if (from == null) {
            from = LocalDate.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now();
        }
        List<Venue> venues = venueId == null
                ? venueRepository.findAll()
                : List.of(venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found")));
        List<UtilizationReport> out = new ArrayList<>();
        for (Venue v : venues) {
            long total = 0;
            for (BookingStatus s : BookingStatus.values()) {
                total += bookingRepository.countByVenueAndStatusAndDateBetween(v.getId(), s, from, to);
            }
            long completed = bookingRepository.countCompletedByVenueAndDateBetween(v.getId(), from, to);
            long cancelled = bookingRepository.countByVenueAndStatusAndDateBetween(v.getId(), BookingStatus.CANCELLED, from, to);
            long noShow = bookingRepository.countByVenueAndStatusAndDateBetween(v.getId(), BookingStatus.NO_SHOW, from, to);
            out.add(new UtilizationReport(v.getId(), v.getName(), v.getType(), total, completed, cancelled, noShow));
        }
        return out;
    }

    public BookingSummaryReport summary(LocalDate from, LocalDate to) {
        if (from == null) {
            from = LocalDate.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now();
        }
        List<Booking> all = bookingRepository.findByBookingDateBetween(from, to);
        long total = all.size();
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));
        Map<String, Long> topVenues = all.stream()
                .collect(Collectors.groupingBy(b -> b.getVenueId().toString(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        return new BookingSummaryReport(total, byStatus, topVenues);
    }
}
