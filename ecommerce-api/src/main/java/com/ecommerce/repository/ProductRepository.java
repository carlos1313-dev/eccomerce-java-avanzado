package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByActiveTrue();

    Optional<Product> findByIdAndActiveTrue(Long id);

    /**
     * CONCURRENCIA - Bloqueo pesimista como alternativa al optimista:
     * PESSIMISTIC_WRITE coloca un SELECT FOR UPDATE en PostgreSQL,
     * impidiendo que otras transacciones lean o modifiquen la fila
     * hasta que la transacción actual termine.
     * Útil cuando la tasa de conflictos es alta.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.active = true")
    Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);
}
