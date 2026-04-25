package com.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entityType;

    private Long entityId;

    private String userEmail;

    @Column(length = 2000)
    private String details;

    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    
    
    

    // Constructor vacío (requerido por JPA)
    public AuditLog() {
    }

    // Constructor con todos los argumentos
    public AuditLog(Long id, String action, String entityType, Long entityId, 
                    String userEmail, String details, Outcome outcome, LocalDateTime timestamp) {
        this.id = id;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.userEmail = userEmail;
        this.details = details;
        this.outcome = outcome;
        this.timestamp = timestamp;
    }

    // Builder manual
    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public enum Outcome {
        SUCCESS, FAILURE, DENIED
    }

    // Clase Builder interna
    public static class AuditLogBuilder {
        private Long id;
        private String action;
        private String entityType;
        private Long entityId;
        private String userEmail;
        private String details;
        private Outcome outcome;
        private LocalDateTime timestamp;

        public AuditLogBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuditLogBuilder action(String action) {
            this.action = action;
            return this;
        }

        public AuditLogBuilder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public AuditLogBuilder entityId(Long entityId) {
            this.entityId = entityId;
            return this;
        }

        public AuditLogBuilder userEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public AuditLogBuilder details(String details) {
            this.details = details;
            return this;
        }

        public AuditLogBuilder outcome(Outcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public AuditLogBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AuditLog build() {
            return new AuditLog(id, action, entityType, entityId, userEmail, details, outcome, timestamp);
        }
    }
}