package com.campus.venue.dto;

import com.campus.venue.domain.Club;
import com.campus.venue.domain.SchoolClass;

public class OrgDto {
    public Long id;
    public String name;
    public String meta;

    public static OrgDto fromClass(SchoolClass c) {
        OrgDto d = new OrgDto();
        d.id = c.getId();
        d.name = c.getName();
        d.meta = c.getGrade();
        return d;
    }

    public static OrgDto fromClub(Club c) {
        OrgDto d = new OrgDto();
        d.id = c.getId();
        d.name = c.getName();
        d.meta = c.getType();
        return d;
    }
}
