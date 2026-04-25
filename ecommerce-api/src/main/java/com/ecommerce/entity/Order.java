package com.ecommerce.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.CONFIRMED;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Constructor vacío
    public Order() {
        this.status = Status.CONFIRMED;
    }

    // Constructor con todos los argumentos
    public Order(Long id, User user, List<OrderItem> items, BigDecimal total, 
                 Status status, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.items = items;
        this.total = total;
        this.status = status != null ? status : Status.CONFIRMED;
        this.createdAt = createdAt;
    }

    // Builder manual
    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public enum Status {
        CONFIRMED, CANCELLED
    }

    // Clase Builder interna
    public static class OrderBuilder {
        private Long id;
        private User user;
        private List<OrderItem> items;
        private BigDecimal total;
        private Status status = Status.CONFIRMED;
        private LocalDateTime createdAt;

        public OrderBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrderBuilder user(User user) {
            this.user = user;
            return this;
        }

        public OrderBuilder items(List<OrderItem> items) {
            this.items = items;
            return this;
        }

        public OrderBuilder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        public OrderBuilder status(Status status) {
            this.status = status;
            return this;
        }

        public OrderBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Order build() {
            return new Order(id, user, items, total, status, createdAt);
        }
    }
}