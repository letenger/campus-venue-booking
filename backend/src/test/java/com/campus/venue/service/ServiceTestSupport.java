package com.campus.venue.service;

import com.campus.venue.common.Role;
import com.campus.venue.security.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared scaffolding for the service-layer unit tests. Services read the acting user
 * straight from the SecurityContext, so tests install an AuthUser principal instead of
 * booting a Spring context.
 */
abstract class ServiceTestSupport {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Puts an authenticated principal with the given id and role into the SecurityContext. */
    protected void givenCurrentUser(Long userId, Role role) {
        AuthUser principal = new AuthUser(userId, role);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "", principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }
}
