package com.example.sems.controller;

import com.example.sems.common.ApiResponse;
import com.example.sems.domain.Club;
import com.example.sems.domain.SchoolClass;
import com.example.sems.dto.OrgDto;
import com.example.sems.repository.ClubRepository;
import com.example.sems.repository.SchoolClassRepository;
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
