package com.ecommerce.controller;

import com.ecommerce.entity.AuditLog;
import com.ecommerce.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auditoría", description = "Registro de eventos del sistema (solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "Obtener todos los logs de auditoría")
    public ResponseEntity<List<AuditLog>> getAll() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @GetMapping("/user/{email}")
    @Operation(summary = "Obtener logs de auditoría por usuario")
    public ResponseEntity<List<AuditLog>> getByUser(@PathVariable String email) {
        return ResponseEntity.ok(auditLogRepository.findByUserEmailOrderByTimestampDesc(email));
    }

    @GetMapping("/entity/{entityType}")
    @Operation(summary = "Obtener logs de auditoría por tipo de entidad")
    public ResponseEntity<List<AuditLog>> getByEntity(@PathVariable String entityType) {
        return ResponseEntity.ok(auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType));
    }
}
