package com.campus.venue.service;

import com.campus.venue.common.ConflictException;
import com.campus.venue.common.ForbiddenException;
import com.campus.venue.common.Status;
import com.campus.venue.common.UnauthorizedException;
import com.campus.venue.domain.User;
import com.campus.venue.dto.AuthRequest;
import com.campus.venue.dto.AuthResponse;
import com.campus.venue.dto.RegisterRequest;
import com.campus.venue.dto.UserDto;
import com.campus.venue.repository.UserRepository;
import com.campus.venue.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new ConflictException("Username already taken");
        }
        User u = User.builder()
                .username(req.getUsername())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .realName(req.getRealName())
                .role(req.getRole())
                .gender(req.getGender())
                .age(req.getAge())
                .phone(req.getPhone())
                .studentNo(req.getStudentNo())
                .classId(req.getClassId())
                .status(Status.ACTIVE)
                .build();
        u = userRepository.save(u);
        auditService.record(u.getId(), "REGISTER", "USER", u.getId(), null);
        return new AuthResponse(jwtService.generate(u.getId(), u.getRole()), UserDto.from(u));
    }

    public AuthResponse login(AuthRequest req) {
        User u = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        if (u.getStatus() == Status.DISABLED) {
            throw new ForbiddenException("Account is disabled");
        }
        return new AuthResponse(jwtService.generate(u.getId(), u.getRole()), UserDto.from(u));
    }
}
