package com.example.sems.service;

import com.example.sems.common.BadRequestException;
import com.example.sems.common.ResourceNotFoundException;
import com.example.sems.domain.Venue;
import com.example.sems.domain.VenueSchedule;
import com.example.sems.domain.VenueStatus;
import com.example.sems.dto.AvailabilitySlot;
import com.example.sems.dto.VenueDto;
import com.example.sems.dto.VenueRequest;
import com.example.sems.dto.VenueScheduleDto;
import com.example.sems.repository.BookingRepository;
import com.example.sems.repository.VenueRepository;
import com.example.sems.repository.VenueScheduleRepository;
import com.example.sems.security.SecurityUtils;
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

    public List<AvailabilitySlot> availability(Long venueId, LocalDate from, LocalDate to) {
        Venue v = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));
        if (from == null) {
            from = LocalDate.now();
        }
        if (to == null) {
            to = from.plusDays(7);
        }
        List<VenueSchedule> scheds = venueScheduleRepository.findByVenueId(venueId);
        List<AvailabilitySlot> slots = new ArrayList<>();
        LocalDate d = from;
        while (!d.isAfter(to)) {
            int wd = d.getDayOfWeek().getValue();
            for (VenueSchedule s : scheds) {
                if (s.getWeekday() == wd) {
                    LocalTime t = s.getStartTime();
                    while (t.isBefore(s.getEndTime())) {
                        LocalTime slotEnd = t.plusHours(1);
                        if (slotEnd.isAfter(s.getEndTime())) {
                            slotEnd = s.getEndTime();
                        }
                        boolean conflict = bookingRepository.findOverlapping(venueId, d, t, slotEnd).stream()
                                .anyMatch(b -> b.getStatus() == com.example.sems.domain.BookingStatus.APPROVED
                                        || b.getStatus() == com.example.sems.domain.BookingStatus.PENDING);
                        String reason = null;
                        boolean available = true;
                        if (v.getStatus() != VenueStatus.OPEN) {
                            available = false;
                            reason = v.getStatus() == VenueStatus.MAINTENANCE ? "场馆维护中" : "场馆已关闭";
                        } else if (conflict) {
                            available = false;
                            reason = "已被预订";
                        }
                        slots.add(new AvailabilitySlot(d, t, slotEnd, available, reason));
                        t = slotEnd;
                    }
                }
            }
            d = d.plusDays(1);
        }
        return slots;
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
