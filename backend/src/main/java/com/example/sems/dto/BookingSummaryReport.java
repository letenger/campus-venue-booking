package com.example.sems.dto;

import java.util.Map;

public class BookingSummaryReport {
    public long total;
    public Map<String, Long> byStatus;
    public Map<String, Long> topVenues;

    public BookingSummaryReport(long total, Map<String, Long> byStatus, Map<String, Long> topVenues) {
        this.total = total;
        this.byStatus = byStatus;
        this.topVenues = topVenues;
    }
}
