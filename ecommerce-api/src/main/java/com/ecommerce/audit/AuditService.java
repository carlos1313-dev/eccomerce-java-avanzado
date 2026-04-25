package com.ecommerce.audit;

import com.ecommerce.entity.AuditLog;
import com.ecommerce.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service

public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * Registra un evento de auditoría de forma asíncrona para no bloquear
     * la transacción principal. Usa Propagation.REQUIRES_NEW para que el
     * registro de auditoría no sea afectado si la transacción principal hace rollback.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Long entityId,
                    String userEmail, String details, AuditLog.Outcome outcome) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .userEmail(userEmail)
                    .details(details)
                    .outcome(outcome)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Error al guardar log de auditoría: {}", e.getMessage());
        }
    }

    public void logSuccess(String action, String entityType, Long entityId, String userEmail, String details) {
        log(action, entityType, entityId, userEmail, details, AuditLog.Outcome.SUCCESS);
    }

    public void logFailure(String action, String entityType, Long entityId, String userEmail, String details) {
        log(action, entityType, entityId, userEmail, details, AuditLog.Outcome.FAILURE);
    }

    public void logDenied(String action, String entityType, Long entityId, String userEmail, String details) {
        log(action, entityType, entityId, userEmail, details, AuditLog.Outcome.DENIED);
    }
}
