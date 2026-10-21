package com.campus.venue.dto;

import com.campus.venue.domain.Venue;
import com.campus.venue.domain.VenueStatus;
import com.campus.venue.domain.VenueType;

import java.time.LocalTime;

public class VenueDto {
    public Long id;
    public String name;
    public VenueType type;
    public boolean indoor;
    public String building;
    public String location;
    public int capacity;
    public LocalTime openTime;
    public LocalTime closeTime;
    public VenueStatus status;
    public String rules;
    public String imageUrls;

    public static VenueDto from(Venue v) {
        VenueDto d = new VenueDto();
        d.id = v.getId();
        d.name = v.getName();
        d.type = v.getType();
        d.indoor = v.isIndoor();
        d.building = v.getBuilding();
        d.location = v.getLocation();
        d.capacity = v.getCapacity();
        d.openTime = v.getOpenTime();
        d.closeTime = v.getCloseTime();
        d.status = v.getStatus();
        d.rules = v.getRules();
        d.imageUrls = v.getImageUrls();
        return d;
    }
}
