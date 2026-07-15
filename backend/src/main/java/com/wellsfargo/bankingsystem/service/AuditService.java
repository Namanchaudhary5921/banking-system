package com.wellsfargo.bankingsystem.service;

import com.wellsfargo.bankingsystem.model.AuditLog;
import com.wellsfargo.bankingsystem.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Central place every other service calls to record who-did-what-when.
 * Kept separate from business logic so audit trail behavior can change
 * (e.g. push to Kafka, write-only external store) without touching
 * transaction/account logic.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String entityType, String entityId, String action, String details, String performedBy) {
        auditLogRepository.save(new AuditLog(entityType, entityId, action, details, performedBy));
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsFor(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }
}
