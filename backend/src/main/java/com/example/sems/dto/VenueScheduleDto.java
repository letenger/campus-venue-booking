package com.example.sems.dto;

import com.example.sems.domain.VenueSchedule;

import java.time.LocalTime;

public class VenueScheduleDto {
    public int weekday;
    public LocalTime startTime;
    public LocalTime endTime;

    public static VenueScheduleDto from(VenueSchedule s) {
        VenueScheduleDto d = new VenueScheduleDto();
        d.weekday = s.getWeekday();
        d.startTime = s.getStartTime();
        d.endTime = s.getEndTime();
        return d;
    }
}
