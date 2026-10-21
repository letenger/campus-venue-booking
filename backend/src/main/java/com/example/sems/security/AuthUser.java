package com.example.sems.security;

import com.example.sems.common.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * Authenticated principal carried in the SecurityContext. Built from JWT claims
 * (userId + role) so no DB hit is needed on every request.
 */
public class AuthUser implements UserDetails {

    private final Long userId;
    private final Role role;
    private final String username;

    public AuthUser(Long userId, Role role) {
        this.userId = userId;
        this.role = role;
        this.username = "u" + userId;
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
