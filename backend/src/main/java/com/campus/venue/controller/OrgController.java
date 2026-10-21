package com.campus.venue.controller;

import com.campus.venue.common.ApiResponse;
import com.campus.venue.domain.Club;
import com.campus.venue.domain.SchoolClass;
import com.campus.venue.dto.OrgDto;
import com.campus.venue.repository.ClubRepository;
import com.campus.venue.repository.SchoolClassRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrgController {

    private final SchoolClassRepository schoolClassRepository;
    private final ClubRepository clubRepository;

    public OrgController(SchoolClassRepository schoolClassRepository, ClubRepository clubRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.clubRepository = clubRepository;
    }

    @GetMapping("/classes")
    public ApiResponse<List<OrgDto>> classes() {
        return ApiResponse.ok(schoolClassRepository.findAll().stream().map(OrgDto::fromClass).toList());
    }

    @GetMapping("/clubs")
    public ApiResponse<List<OrgDto>> clubs() {
        return ApiResponse.ok(clubRepository.findAll().stream().map(OrgDto::fromClub).toList());
    }
}
