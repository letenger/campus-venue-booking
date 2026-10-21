package com.campus.venue.service;

import com.campus.venue.common.ResourceNotFoundException;
import com.campus.venue.domain.Notification;
import com.campus.venue.domain.NotificationType;
import com.campus.venue.dto.NotificationDto;
import com.campus.venue.repository.NotificationRepository;
import com.campus.venue.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(Long userId, NotificationType type, String title, String content, Long bookingId) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .relatedBookingId(bookingId)
                .build());
    }

    public List<NotificationDto> listForCurrentUser() {
        Long uid = SecurityUtils.currentUserId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(uid).stream()
                .map(NotificationDto::from)
                .toList();
    }

    public void markRead(Long id) {
        Long uid = SecurityUtils.currentUserId();
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getUserId().equals(uid)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    public void markAllRead() {
        Long uid = SecurityUtils.currentUserId();
        notificationRepository.findByUserIdAndReadFalse(uid)
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }
}
