package com.ecommerce.repository;

import com.ecommerce.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserEmailOrderByTimestampDesc(String email);
    List<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType);
}
