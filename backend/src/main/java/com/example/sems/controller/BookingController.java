package com.example.sems.controller;

import com.example.sems.common.ApiResponse;
import com.example.sems.common.PageResponse;
import com.example.sems.domain.BookingStatus;
import com.example.sems.dto.BookingDto;
import com.example.sems.dto.BookingRequest;
import com.example.sems.security.SecurityUtils;
import com.example.sems.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ApiResponse<PageResponse<BookingDto>> list(@RequestParam(required = false) Long venueId,
                                                     @RequestParam(required = false) BookingStatus status,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @RequestParam(required = false) Boolean mine,
                                                     Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(bookingService.list(venueId, status, from, to, mine, pageable)));
    }

    @PostMapping
    public ApiResponse<BookingDto> create(@Valid @RequestBody BookingRequest req) {
        return ApiResponse.ok(bookingService.create(req, SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingDto> get(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.get(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<BookingDto> update(@PathVariable Long id, @Valid @RequestBody BookingRequest req) {
        return ApiResponse.ok(bookingService.update(id, req));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<BookingDto> cancel(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.cancel(id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<BookingDto> approve(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<BookingDto> reject(@PathVariable Long id,
                                         @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok(bookingService.reject(id, reason));
    }

    @PostMapping("/{id}/check-in")
    public ApiResponse<BookingDto> checkIn(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.checkIn(id));
    }

    @PostMapping("/{id}/no-show")
    public ApiResponse<BookingDto> noShow(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.markNoShow(id));
    }
}
