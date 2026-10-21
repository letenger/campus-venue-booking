package com.example.sems.controller;

import com.example.sems.common.ApiResponse;
import com.example.sems.dto.EquipmentBorrowRequest;
import com.example.sems.dto.EquipmentDto;
import com.example.sems.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public ApiResponse<List<EquipmentDto>> list() {
        return ApiResponse.ok(equipmentService.list());
    }

    @PostMapping("/borrow")
    public ApiResponse<EquipmentDto> borrow(@Valid @RequestBody EquipmentBorrowRequest req) {
        return ApiResponse.ok(equipmentService.borrow(req));
    }

    @PostMapping("/{id}/return")
    public ApiResponse<EquipmentDto> returnEquipment(@PathVariable Long id) {
        return ApiResponse.ok(equipmentService.returnEquipment(id));
    }
}
