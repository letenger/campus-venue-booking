package com.campus.venue.dto;

import com.campus.venue.domain.Notification;
import com.campus.venue.domain.NotificationType;

import java.time.Instant;

public class NotificationDto {
    public Long id;
    public Long userId;
    public NotificationType type;
    public String title;
    public String content;
    public boolean read;
    public Long relatedBookingId;
    public Instant createdAt;

    public static NotificationDto from(Notification n) {
        NotificationDto d = new NotificationDto();
        d.id = n.getId();
        d.userId = n.getUserId();
        d.type = n.getType();
        d.title = n.getTitle();
        d.content = n.getContent();
        d.read = n.isRead();
        d.relatedBookingId = n.getRelatedBookingId();
        d.createdAt = n.getCreatedAt();
        return d;
    }
}
