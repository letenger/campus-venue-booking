package com.campus.venue.security;

import com.campus.venue.common.BadRequestException;
import com.campus.venue.common.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthUser currentAuthUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser au)) {
            throw new BadRequestException("Not authenticated");
        }
        return au;
    }

    public static Long currentUserId() {
        return currentAuthUser().getUserId();
    }

    public static Role currentRole() {
        return currentAuthUser().getRole();
    }

    public static boolean isAdmin() {
        return currentRole() == Role.ADMIN;
    }

    public static boolean isTeacherOrAdmin() {
        Role r = currentRole();
        return r == Role.TEACHER || r == Role.ADMIN;
    }
}
