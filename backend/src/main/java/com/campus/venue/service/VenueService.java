package com.campus.venue.service;

import com.campus.venue.common.BadRequestException;
import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.domain.Booking;
import com.campus.venue.domain.BookingStatus;
import com.campus.venue.domain.Venue;
import com.campus.venue.domain.VenueSchedule;
import com.campus.venue.domain.VenueStatus;
import com.campus.venue.dto.AvailabilitySlot;
import com.campus.venue.dto.VenueDto;
import com.campus.venue.dto.VenueRequest;
import com.campus.venue.dto.VenueScheduleDto;
import com.campus.venue.repository.BookingRepository;
import com.campus.venue.repository.VenueRepository;
import com.campus.venue.repository.VenueScheduleRepository;
import com.campus.venue.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final VenueScheduleRepository venueScheduleRepository;
    private final BookingRepository bookingRepository;
    private final AuditService auditService;

    public VenueService(VenueRepository venueRepository, VenueScheduleRepository venueScheduleRepository,
                        BookingRepository bookingRepository, AuditService auditService) {
        this.venueRepository = venueRepository;
        this.venueScheduleRepository = venueScheduleRepository;
        this.bookingRepository = bookingRepository;
        this.auditService = auditService;
    }

    public Page<VenueDto> list(VenueStatus status, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return venueRepository
                    .findByNameContainingIgnoreCaseOrBuildingContainingIgnoreCase(keyword, keyword, pageable)
                    .map(VenueDto::from);
        }
        if (status != null) {
            return venueRepository.findByStatus(status, pageable).map(VenueDto::from);
        }
        return venueRepository.findAll(pageable).map(VenueDto::from);
    }

    public VenueDto get(Long id) {
        return VenueDto.from(venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found")));
    }

    public VenueDto create(VenueRequest req, Long actorId) {
        Venue v = Venue.builder()
                .name(req.getName())
                .type(req.getType())
                .indoor(req.isIndoor())
                .building(req.getBuilding())
                .location(req.getLocation())
                .capacity(req.getCapacity())
                .openTime(req.getOpenTime())
                .closeTime(req.getCloseTime())
                .status(req.getStatus() == null ? VenueStatus.OPEN : req.getStatus())
                .rules(req.getRules())
                .imageUrls(req.getImageUrls())
                .build();
        v = venueRepository.save(v);
        saveSchedules(v.getId(), req.getSchedules());
        auditService.record(actorId, "CREATE_VENUE", "VENUE", v.getId(), v.getName());
        return VenueDto.from(v);
    }

    public VenueDto update(Long id, VenueRequest req) {
        Venue v = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        v.setName(req.getName());
        v.setType(req.getType());
        v.setIndoor(req.isIndoor());
        v.setBuilding(req.getBuilding());
        v.setLocation(req.getLocation());
        v.setCapacity(req.getCapacity());
        v.setOpenTime(req.getOpenTime());
        v.setCloseTime(req.getCloseTime());
        if (req.getStatus() != null) {
            v.setStatus(req.getStatus());
        }
        v.setRules(req.getRules());
        v.setImageUrls(req.getImageUrls());
        v = venueRepository.save(v);
        if (req.getSchedules() != null) {
            saveSchedules(id, req.getSchedules());
        }
        auditService.record(SecurityUtils.currentUserId(), "UPDATE_VENUE", "VENUE", id, v.getName());
        return VenueDto.from(v);
    }

    public void delete(Long id) {
        Venue v = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        v.setStatus(VenueStatus.CLOSED);
        venueRepository.save(v);
        auditService.record(SecurityUtils.currentUserId(), "CLOSE_VENUE", "VENUE", id, v.getName());
    }

    /**
     * Splits the venue's weekly windows into one-hour slots for the requested date range.
     * Bookings for the whole range are fetched once and matched in memory, so the loop
     * does not hit the database per slot.
     */
    public List<AvailabilitySlot> availability(Long venueId, LocalDate from, LocalDate to) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start.plusDays(7) : to;

        List<Booking> taken = bookingRepository.findByBookingDateBetween(start, end).stream()
                .filter(b -> b.getVenueId().equals(venueId))
                .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.APPROVED)
                .toList();

        List<VenueSchedule> windows = venueScheduleRepository.findByVenueId(venueId);
        List<AvailabilitySlot> slots = new ArrayList<>();

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            int weekday = day.getDayOfWeek().getValue();
            for (VenueSchedule window : windows) {
                if (window.getWeekday() != weekday) {
                    continue;
                }
                LocalTime cursor = window.getStartTime();
                while (cursor.isBefore(window.getEndTime())) {
                    LocalTime slotEnd = cursor.plusHours(1).isAfter(window.getEndTime())
                            ? window.getEndTime()
                            : cursor.plusHours(1);
                    String reason = blockReason(venue, taken, day, cursor, slotEnd);
                    slots.add(new AvailabilitySlot(day, cursor, slotEnd, reason == null, reason));
                    cursor = slotEnd;
                }
            }
        }
        return slots;
    }

    /** Returns why a slot cannot be booked, or null when it is free. */
    private String blockReason(Venue venue, List<Booking> taken, LocalDate day, LocalTime from, LocalTime to) {
        if (venue.getStatus() != VenueStatus.OPEN) {
            return venue.getStatus() == VenueStatus.MAINTENANCE ? "Venue under maintenance" : "Venue closed";
        }
        boolean busy = taken.stream().anyMatch(b -> b.getBookingDate().equals(day)
                && b.getStartTime().isBefore(to) && b.getEndTime().isAfter(from));
        return busy ? "Already booked" : null;
    }

    private void saveSchedules(Long venueId, List<VenueScheduleDto> dtos) {
        venueScheduleRepository.deleteByVenueId(venueId);
        if (dtos == null) {
            return;
        }
        for (VenueScheduleDto sd : dtos) {
            if (sd.startTime == null || sd.endTime == null) {
                continue;
            }
            venueScheduleRepository.save(VenueSchedule.builder()
                    .venueId(venueId)
                    .weekday(sd.weekday)
                    .startTime(sd.startTime)
                    .endTime(sd.endTime)
                    .build());
        }
    }
}
