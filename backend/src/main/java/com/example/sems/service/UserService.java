package com.example.sems.service;

import com.example.sems.common.Role;
import com.example.sems.common.ResourceNotFoundException;
import com.example.sems.common.Status;
import com.example.sems.domain.User;
import com.example.sems.dto.ProfileUpdateRequest;
import com.example.sems.dto.UserDto;
import com.example.sems.repository.UserRepository;
import com.example.sems.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto getMe() {
        User u = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDto.from(u);
    }

    public UserDto updateProfile(ProfileUpdateRequest req) {
        User u = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (req.getRealName() != null) {
            u.setRealName(req.getRealName());
        }
        if (req.getGender() != null) {
            u.setGender(req.getGender());
        }
        if (req.getAge() != null) {
            u.setAge(req.getAge());
        }
        if (req.getPhone() != null) {
            u.setPhone(req.getPhone());
        }
        return UserDto.from(userRepository.save(u));
    }

    public Page<UserDto> list(Role role, Pageable pageable) {
        Page<User> page = (role == null)
                ? userRepository.findAll(pageable)
                : userRepository.findByRole(role, pageable);
        return page.map(UserDto::from);
    }

    public UserDto setStatus(Long id, Status status) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        u.setStatus(status);
        return UserDto.from(userRepository.save(u));
    }
}
