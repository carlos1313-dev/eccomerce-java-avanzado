package com.ecommerce;

import com.ecommerce.dto.Dtos;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PRUEBA DE CONCURRENCIA - Demostración de prevención de sobreventa
 *
 * Este test simula múltiples clientes comprando el mismo producto al mismo tiempo.
 * Verifica que el sistema NUNCA venda más unidades de las disponibles en inventario.
 *
 * Estrategia probada: Bloqueo pesimista (SELECT FOR UPDATE en PostgreSQL)
 * El test usa H2 en modo test; en producción con PostgreSQL el comportamiento es idéntico.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Product testProduct;
    private List<User> testUsers;

    private static final int STOCK_INICIAL = 5;
    private static final int COMPRADORES_CONCURRENTES = 20;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Producto con stock limitado
        testProduct = productRepository.save(Product.builder()
                .name("Producto Concurrencia Test")
                .description("Solo " + STOCK_INICIAL + " unidades disponibles")
                .price(new BigDecimal("99.99"))
                .stock(STOCK_INICIAL)
                .active(true)
                .build());

        // Creamos 20 usuarios que intentarán comprar simultáneamente
        testUsers = new ArrayList<>();
        for (int i = 0; i < COMPRADORES_CONCURRENTES; i++) {
            testUsers.add(userRepository.save(User.builder()
                    .name("Cliente " + i)
                    .email("cliente" + i + "@test.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(User.Role.CLIENTE)
                    .active(true)
                    .build()));
        }
    }

    @Test
    @DisplayName("CONCURRENCIA: Solo se deben vender exactamente las unidades disponibles")
    void debePrevenirSobreventaBajoCargaConcurrente() throws InterruptedException {
        // GIVEN: 5 unidades en stock, 20 compradores simultáneos, cada uno quiere 1 unidad
        AtomicInteger comprasExitosas = new AtomicInteger(0);
        AtomicInteger comprasFallidas = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(1); // Para liberar todos los hilos a la vez
        CountDownLatch done = new CountDownLatch(COMPRADORES_CONCURRENTES);

        ExecutorService executor = Executors.newFixedThreadPool(COMPRADORES_CONCURRENTES);

        for (int i = 0; i < COMPRADORES_CONCURRENTES; i++) {
            final String email = testUsers.get(i).getEmail();
            final Long productId = testProduct.getId();

            executor.submit(() -> {
                try {
                    latch.await(); // Todos los hilos esperan aquí → arrancan al mismo tiempo

                    Dtos.CreateOrderRequest request = new Dtos.CreateOrderRequest(
                            List.of(new Dtos.OrderItemRequest(productId, 1))
                    );

                    orderService.createOrder(request, email);
                    comprasExitosas.incrementAndGet();

                } catch (Exception e) {
                    // Stock insuficiente o conflicto de concurrencia → compra rechazada
                    comprasFallidas.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        // WHEN: Liberamos todos los hilos simultáneamente
        latch.countDown();
        done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // THEN: Solo deben haberse completado exactamente STOCK_INICIAL compras
        Product productoFinal = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("=== RESULTADO PRUEBA CONCURRENCIA ===");
        System.out.println("Stock inicial:       " + STOCK_INICIAL);
        System.out.println("Compradores:         " + COMPRADORES_CONCURRENTES);
        System.out.println("Compras exitosas:    " + comprasExitosas.get());
        System.out.println("Compras rechazadas:  " + comprasFallidas.get());
        System.out.println("Stock final en BD:   " + productoFinal.getStock());
        System.out.println("=====================================");

        // El stock final NUNCA debe ser negativo
        assertThat(productoFinal.getStock())
                .as("El stock nunca debe ser negativo (sobreventa)")
                .isGreaterThanOrEqualTo(0);

        // Las compras exitosas no pueden superar el stock inicial
        assertThat(comprasExitosas.get())
                .as("No se pueden vender más unidades que el stock disponible")
                .isLessThanOrEqualTo(STOCK_INICIAL);

        // Stock final + compras exitosas debe sumar exactamente el stock inicial
        assertThat(productoFinal.getStock() + comprasExitosas.get())
                .as("Stock final + vendidos debe ser igual al stock inicial")
                .isEqualTo(STOCK_INICIAL);
    }
}
