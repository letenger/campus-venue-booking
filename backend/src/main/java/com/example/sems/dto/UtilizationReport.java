package com.example.sems.dto;

import com.example.sems.domain.VenueType;

public class UtilizationReport {
    public Long venueId;
    public String venueName;
    public VenueType type;
    public long total;
    public long completed;
    public long cancelled;
    public long noShow;
    public double utilizationRate;

    public UtilizationReport(Long venueId, String venueName, VenueType type,
                             long total, long completed, long cancelled, long noShow) {
        this.venueId = venueId;
        this.venueName = venueName;
        this.type = type;
        this.total = total;
        this.completed = completed;
        this.cancelled = cancelled;
        this.noShow = noShow;
        this.utilizationRate = total == 0 ? 0.0 : Math.round((completed * 1000.0 / total)) / 10.0;
    }
}
