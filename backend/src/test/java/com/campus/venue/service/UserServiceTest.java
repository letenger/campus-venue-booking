package com.campus.venue.service;

import com.campus.venue.common.Gender;
import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.common.Role;
import com.campus.venue.common.Status;
import com.campus.venue.domain.User;
import com.campus.venue.dto.ProfileUpdateRequest;
import com.campus.venue.dto.UserDto;
import com.campus.venue.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}: profile updates only touch provided fields,
 * status changes and role-filtered listing.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest extends ServiceTestSupport {

    private static final long USER_ID = 42L;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService service;

    private User student() {
        return User.builder()
                .id(USER_ID)
                .username("sam")
                .realName("Sam Carter")
                .role(Role.STUDENT)
                .gender(Gender.MALE)
                .age(15)
                .phone("000")
                .studentNo("S2026")
                .status(Status.ACTIVE)
                .build();
    }

    @Test
    void shouldUpdateOnlyProvidedProfileFields() {
        givenCurrentUser(USER_ID, Role.STUDENT);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(student()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setPhone("555-0100");

        UserDto updated = service.updateProfile(req);

        assertThat(updated.phone).isEqualTo("555-0100");
        assertThat(updated.realName).isEqualTo("Sam Carter");
        assertThat(updated.age).isEqualTo(15);
    }

    @Test
    void throwsResourceNotFoundExceptionWhenProfileUserMissing() {
        givenCurrentUser(USER_ID, Role.STUDENT);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(new ProfileUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void shouldChangeAccountStatus() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(student()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto updated = service.setStatus(USER_ID, Status.DISABLED);

        assertThat(updated.status).isEqualTo(Status.DISABLED);
    }

    @Test
    void throwsResourceNotFoundExceptionWhenStatusTargetMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setStatus(USER_ID, Status.DISABLED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void shouldListUsersFilteredByRole() {
        User teacher = User.builder().id(7L).username("msLee").role(Role.TEACHER)
                .status(Status.ACTIVE).build();
        when(userRepository.findByRole(Role.TEACHER, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(teacher)));

        Page<UserDto> page = service.list(Role.TEACHER, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).role).isEqualTo(Role.TEACHER);
    }

    @Test
    void shouldListAllUsersWhenNoRoleFilter() {
        when(userRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(student())));

        Page<UserDto> page = service.list(null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        verify(userRepository).findAll(PageRequest.of(0, 10));
    }
}
