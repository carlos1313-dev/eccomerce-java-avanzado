package com.ecommerce.dto;

import com.ecommerce.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs usando record de Java moderno (Java 16+).
 * Los records son clases inmutables con equals, hashCode y toString generados automáticamente.
 * Perfectos para DTOs porque representan datos que no cambian en tránsito.
 */
public class Dtos {

    // ============================================================
    // AUTH
    // ============================================================

    public record RegisterRequest(
        @NotBlank(message = "El nombre es requerido")
        String name,

        @NotBlank(message = "El email es requerido")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
        String password,

        @NotNull(message = "El rol es requerido")
        User.Role role
    ) {}

    public record LoginRequest(
        @NotBlank(message = "El email es requerido")
        @Email
        String email,

        @NotBlank(message = "La contraseña es requerida")
        String password
    ) {}

    public record AuthResponse(
        String token,
        String email,
        String name,
        String role
    ) {}

    public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean active,
        LocalDateTime createdAt
    ) {}

    // ============================================================
    // PRODUCTS
    // ============================================================

    public record ProductRequest(
        @NotBlank(message = "El nombre del producto es requerido")
        String name,

        String description,

        @NotNull(message = "El precio es requerido")
        BigDecimal price,

        @NotNull(message = "El stock inicial es requerido")
        Integer stock
    ) {}

    public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}

    // ============================================================
    // ORDERS
    // ============================================================

    public record OrderItemRequest(
        @NotNull(message = "El ID del producto es requerido")
        Long productId,

        @NotNull(message = "La cantidad es requerida")
        Integer quantity
    ) {}

    public record CreateOrderRequest(
        @NotNull(message = "La orden debe tener al menos un ítem")
        @jakarta.validation.constraints.Size(min = 1, message = "La orden debe tener al menos un ítem")
        List<OrderItemRequest> items
    ) {}

    public record OrderItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {}

    public record OrderResponse(
        Long id,
        Long userId,
        String userName,
        List<OrderItemResponse> items,
        BigDecimal total,
        String status,
        LocalDateTime createdAt
    ) {}

    // ============================================================
    // ERRORS
    // ============================================================

    public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
    ) {}

    public record ValidationErrorResponse(
        int status,
        String error,
        List<FieldError> fieldErrors,
        LocalDateTime timestamp
    ) {
        public record FieldError(String field, String message) {}
    }
}
