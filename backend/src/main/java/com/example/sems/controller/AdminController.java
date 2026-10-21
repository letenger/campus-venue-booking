package com.example.sems.controller;

import com.example.sems.common.ApiResponse;
import com.example.sems.common.ForbiddenException;
import com.example.sems.common.PageResponse;
import com.example.sems.common.Role;
import com.example.sems.common.Status;
import com.example.sems.dto.UserDto;
import com.example.sems.security.SecurityUtils;
import com.example.sems.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserDto>> users(@RequestParam(required = false) Role role, Pageable pageable) {
        requireAdmin();
        return ApiResponse.ok(PageResponse.from(userService.list(role, pageable)));
    }

    @PatchMapping("/users/{id}")
    public ApiResponse<UserDto> setStatus(@PathVariable Long id, @RequestParam Status status) {
        requireAdmin();
        return ApiResponse.ok(userService.setStatus(id, status));
    }

    private void requireAdmin() {
        if (!SecurityUtils.isAdmin()) {
            throw new ForbiddenException("Admin only");
        }
    }
}
