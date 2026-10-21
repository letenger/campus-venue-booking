package com.campus.venue.service;

import com.campus.venue.domain.AuditLog;
import com.campus.venue.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AuditService}: every field of the action must reach the
 * audit_logs table, including the actor for traceability.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService service;

    @Test
    void shouldPersistAuditEntryWithAllFields() {
        service.record(7L, "APPROVE_BOOKING", "BOOKING", 100L, "Sam Carter 2026-10-26");

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(saved.capture());
        AuditLog entry = saved.getValue();
        assertThat(entry.getActorId()).isEqualTo(7L);
        assertThat(entry.getAction()).isEqualTo("APPROVE_BOOKING");
        assertThat(entry.getTargetType()).isEqualTo("BOOKING");
        assertThat(entry.getTargetId()).isEqualTo(100L);
        assertThat(entry.getDetail()).isEqualTo("Sam Carter 2026-10-26");
    }

    @Test
    void shouldPersistSystemActionsWithNullActor() {
        service.record(null, "AUTO_NOSHOW", "BOOKING", 100L, null);

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(saved.capture());
        assertThat(saved.getValue().getActorId()).isNull();
        assertThat(saved.getValue().getAction()).isEqualTo("AUTO_NOSHOW");
    }
}
