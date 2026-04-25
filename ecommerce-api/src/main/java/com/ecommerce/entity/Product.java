package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * CONCURRENCIA - Control Optimista:
     * @Version hace que Hibernate incluya la versión en el WHERE al hacer UPDATE.
     * Si dos transacciones intentan modificar el mismo producto simultáneamente,
     * la segunda lanzará OptimisticLockException porque la versión ya cambió.
     * Esto evita la sobreventa sin bloquear filas en la base de datos.
     */
    @Version
    private Long version;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    /**
     * Descuenta stock de forma segura. Lanza excepción si no hay suficiente inventario.
     * Este método se ejecuta dentro de una transacción (@Transactional en el servicio).
     */
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalStateException(
                "Stock insuficiente para el producto '%s'. Disponible: %d, solicitado: %d"
                    .formatted(this.name, this.stock, quantity)
            );
        }
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }
}
