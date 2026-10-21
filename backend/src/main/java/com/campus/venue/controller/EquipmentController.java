package com.campus.venue.controller;

import com.campus.venue.common.ApiResponse;
import com.campus.venue.dto.EquipmentBorrowRequest;
import com.campus.venue.dto.EquipmentDto;
import com.campus.venue.service.EquipmentService;
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
