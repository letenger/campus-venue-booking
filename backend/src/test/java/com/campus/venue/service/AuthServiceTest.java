package com.campus.venue.service;

import com.campus.venue.common.ConflictException;
import com.campus.venue.common.ForbiddenException;
import com.campus.venue.common.Role;
import com.campus.venue.common.Status;
import com.campus.venue.common.UnauthorizedException;
import com.campus.venue.domain.User;
import com.campus.venue.dto.AuthRequest;
import com.campus.venue.dto.AuthResponse;
import com.campus.venue.dto.RegisterRequest;
import com.campus.venue.repository.UserRepository;
import com.campus.venue.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}: registration conflicts, password verification
 * and the disabled-account rule.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService service;

    private RegisterRequest registerRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("sam");
        req.setPassword("secret123");
        req.setRealName("Sam Carter");
        req.setRole(Role.STUDENT);
        return req;
    }

    private AuthRequest loginRequest() {
        AuthRequest req = new AuthRequest();
        req.setUsername("sam");
        req.setPassword("secret123");
        return req;
    }

    @Test
    void shouldRegisterNewUserAndReturnToken() {
        when(userRepository.existsByUsername("sam")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });
        when(jwtService.generate(7L, Role.STUDENT)).thenReturn("jwt-token");

        AuthResponse response = service.register(registerRequest());

        assertThat(response.token).isEqualTo("jwt-token");
        assertThat(response.user.username).isEqualTo("sam");
        assertThat(response.user.role).isEqualTo(Role.STUDENT);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        // the raw password must never reach the database
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed-secret");
        assertThat(saved.getValue().getStatus()).isEqualTo(Status.ACTIVE);
        verify(auditService).record(eq(7L), eq("REGISTER"), eq("USER"), eq(7L), isNull());
    }

    @Test
    void throwsConflictExceptionWhenUsernameIsTaken() {
        when(userRepository.existsByUsername("sam")).thenReturn(true);

        assertThatThrownBy(() -> service.register(registerRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Username already taken");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginWithCorrectPassword() {
        User user = User.builder()
                .id(7L)
                .username("sam")
                .passwordHash("hashed-secret")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed-secret")).thenReturn(true);
        when(jwtService.generate(7L, Role.STUDENT)).thenReturn("jwt-token");

        AuthResponse response = service.login(loginRequest());

        assertThat(response.token).isEqualTo("jwt-token");
        assertThat(response.user.id).isEqualTo(7L);
    }

    @Test
    void throwsUnauthorizedExceptionWhenPasswordIsWrong() {
        User user = User.builder()
                .id(7L)
                .username("sam")
                .passwordHash("hashed-secret")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed-secret")).thenReturn(false);

        assertThatThrownBy(() -> service.login(loginRequest()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void throwsUnauthorizedExceptionWhenUserDoesNotExist() {
        when(userRepository.findByUsername("sam")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginRequest()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void throwsForbiddenExceptionWhenAccountIsDisabled() {
        User user = User.builder()
                .id(7L)
                .username("sam")
                .passwordHash("hashed-secret")
                .role(Role.STUDENT)
                .status(Status.DISABLED)
                .build();
        when(userRepository.findByUsername("sam")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed-secret")).thenReturn(true);

        assertThatThrownBy(() -> service.login(loginRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Account is disabled");
    }
}
