package com.example.sems.controller;

import com.example.sems.common.ApiResponse;
import com.example.sems.common.ForbiddenException;
import com.example.sems.dto.BookingSummaryReport;
import com.example.sems.dto.UtilizationReport;
import com.example.sems.security.SecurityUtils;
import com.example.sems.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/utilization")
    public ApiResponse<List<UtilizationReport>> utilization(@RequestParam(required = false) Long venueId,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        requireStaff();
        return ApiResponse.ok(reportService.utilization(venueId, from, to));
    }

    @GetMapping("/bookings-summary")
    public ApiResponse<BookingSummaryReport> summary(@RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        requireStaff();
        return ApiResponse.ok(reportService.summary(from, to));
    }

    private void requireStaff() {
        if (!SecurityUtils.isTeacherOrAdmin()) {
            throw new ForbiddenException("Teachers and admins only");
        }
    }
}
