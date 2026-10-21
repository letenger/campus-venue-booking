package com.campus.venue.controller;

import com.campus.venue.common.ApiResponse;
import com.campus.venue.common.ForbiddenException;
import com.campus.venue.common.PageResponse;
import com.campus.venue.domain.VenueStatus;
import com.campus.venue.dto.AvailabilitySlot;
import com.campus.venue.dto.VenueDto;
import com.campus.venue.dto.VenueRequest;
import com.campus.venue.security.SecurityUtils;
import com.campus.venue.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public ApiResponse<PageResponse<VenueDto>> list(@RequestParam(required = false) VenueStatus status,
                                                   @RequestParam(required = false) String keyword,
                                                   Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(venueService.list(status, keyword, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<VenueDto> get(@PathVariable Long id) {
        return ApiResponse.ok(venueService.get(id));
    }

    @GetMapping("/{id}/availability")
    public ApiResponse<List<AvailabilitySlot>> availability(@PathVariable Long id,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(venueService.availability(id, from, to));
    }

    @PostMapping
    public ApiResponse<VenueDto> create(@Valid @RequestBody VenueRequest req) {
        requireAdmin();
        return ApiResponse.ok(venueService.create(req, SecurityUtils.currentUserId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<VenueDto> update(@PathVariable Long id, @Valid @RequestBody VenueRequest req) {
        requireAdmin();
        return ApiResponse.ok(venueService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        requireAdmin();
        venueService.delete(id);
        return ApiResponse.ok(null, "venue closed");
    }

    private void requireAdmin() {
        if (!SecurityUtils.isAdmin()) {
            throw new ForbiddenException("Admin only");
        }
    }
}
