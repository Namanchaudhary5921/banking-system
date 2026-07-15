package com.wellsfargo.bankingsystem.repository;

import com.wellsfargo.bankingsystem.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);
    List<AuditLog> findAllByOrderByTimestampDesc();
}
