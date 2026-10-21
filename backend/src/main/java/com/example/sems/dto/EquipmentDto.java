package com.example.sems.dto;

import com.example.sems.domain.Equipment;

public class EquipmentDto {
    public Long id;
    public String name;
    public Long venueId;
    public int total;
    public int available;

    public static EquipmentDto from(Equipment e) {
        EquipmentDto d = new EquipmentDto();
        d.id = e.getId();
        d.name = e.getName();
        d.venueId = e.getVenueId();
        d.total = e.getTotal();
        d.available = e.getAvailable();
        return d;
    }
}
