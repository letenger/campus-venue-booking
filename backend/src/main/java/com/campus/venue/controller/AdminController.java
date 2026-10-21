package com.campus.venue.controller;

import com.campus.venue.common.ApiResponse;
import com.campus.venue.common.ForbiddenException;
import com.campus.venue.common.PageResponse;
import com.campus.venue.common.Role;
import com.campus.venue.common.Status;
import com.campus.venue.dto.UserDto;
import com.campus.venue.security.SecurityUtils;
import com.campus.venue.service.UserService;
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
