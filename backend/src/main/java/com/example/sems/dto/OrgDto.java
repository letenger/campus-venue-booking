package com.example.sems.dto;

import com.example.sems.domain.Club;
import com.example.sems.domain.SchoolClass;

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
