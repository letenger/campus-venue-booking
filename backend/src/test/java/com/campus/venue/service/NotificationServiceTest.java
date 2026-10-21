package com.campus.venue.service;

import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.common.Role;
import com.campus.venue.domain.Notification;
import com.campus.venue.domain.NotificationType;
import com.campus.venue.dto.NotificationDto;
import com.campus.venue.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationService}: ownership checks on read marking and
 * the mark-all-read path.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest extends ServiceTestSupport {

    private static final long USER_ID = 42L;
    private static final long OTHER_USER_ID = 99L;
    private static final long NOTIFICATION_ID = 5L;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService service;

    private Notification notification(long ownerId) {
        return Notification.builder()
                .id(NOTIFICATION_ID)
                .userId(ownerId)
                .type(NotificationType.APPROVAL_REQUIRED)
                .title("Booking needs a decision")
                .content("Badminton Court has a new request")
                .read(false)
                .build();
    }

    @Test
    void shouldMarkOwnNotificationAsRead() {
        givenCurrentUser(USER_ID, Role.STUDENT);
        Notification target = notification(USER_ID);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(target));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markRead(NOTIFICATION_ID);

        assertThat(target.isRead()).isTrue();
        verify(notificationRepository).save(target);
    }

    @Test
    void throwsResourceNotFoundExceptionWhenMarkingSomeoneElsesNotification() {
        givenCurrentUser(OTHER_USER_ID, Role.STUDENT);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification(USER_ID)));

        assertThatThrownBy(() -> service.markRead(NOTIFICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notification not found");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void throwsResourceNotFoundExceptionWhenNotificationMissing() {
        givenCurrentUser(USER_ID, Role.STUDENT);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(NOTIFICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notification not found");
    }

    @Test
    void shouldMarkAllUnreadNotificationsAsRead() {
        givenCurrentUser(USER_ID, Role.STUDENT);
        Notification first = notification(USER_ID);
        Notification second = Notification.builder()
                .id(6L)
                .userId(USER_ID)
                .type(NotificationType.APPROVED)
                .title("Booking approved")
                .content("Your request was approved")
                .read(false)
                .build();
        when(notificationRepository.findByUserIdAndReadFalse(USER_ID)).thenReturn(List.of(first, second));

        service.markAllRead();

        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void shouldListNotificationsForCurrentUser() {
        givenCurrentUser(USER_ID, Role.STUDENT);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(notification(USER_ID)));

        List<NotificationDto> notifications = service.listForCurrentUser();

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).title).isEqualTo("Booking needs a decision");
        assertThat(notifications.get(0).type).isEqualTo(NotificationType.APPROVAL_REQUIRED);
    }
}
