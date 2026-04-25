package com.ecommerce.service;

import com.ecommerce.audit.AuditService;
import com.ecommerce.dto.Dtos;
import com.ecommerce.entity.*;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    /**
     * CONCURRENCIA - Estrategia de control de sobreventa:
     *
     * Usamos control PESIMISTA (PESSIMISTIC_WRITE = SELECT FOR UPDATE en PostgreSQL).
     * Razón: en un escenario de e-commerce con alta concurrencia de compras del mismo
     * producto, el conflicto es probable y el control optimista generaría muchos reintentos.
     * El bloqueo pesimista garantiza que solo UNA transacción procesa el stock a la vez.
     *
     * El aislamiento REPEATABLE_READ evita lecturas fantasma durante la transacción.
     *
     * Flujo:
     * 1. La transacción hace SELECT FOR UPDATE sobre cada producto → otros hilos esperan
     * 2. Validamos y decrementamos el stock de forma segura
     * 3. Al hacer commit, los bloqueos se liberan y el siguiente hilo ve el stock actualizado
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Dtos.OrderResponse createOrder(Dtos.CreateOrderRequest request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userEmail));

        // Ordenamos los IDs para evitar deadlocks cuando múltiples transacciones
        // intentan bloquear los mismos productos en diferente orden
        List<Long> sortedProductIds = request.items().stream()
                .map(Dtos.OrderItemRequest::productId)
                .distinct()
                .sorted()
                .toList();

        // Adquirimos los bloqueos en orden consistente
        List<Product> lockedProducts = sortedProductIds.stream()
                .map(id -> productRepository.findByIdWithPessimisticLock(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id)))
                .toList();

        // Construimos los ítems y validamos stock
        List<OrderItem> items = request.items().stream()
                .map(itemReq -> {
                    Product product = lockedProducts.stream()
                            .filter(p -> p.getId().equals(itemReq.productId()))
                            .findFirst()
                            .orElseThrow();

                    try {
                        product.decreaseStock(itemReq.quantity());
                    } catch (IllegalStateException e) {
                        throw new InsufficientStockException(e.getMessage());
                    }

                    BigDecimal subtotal = product.getPrice()
                            .multiply(BigDecimal.valueOf(itemReq.quantity()));

                    return OrderItem.builder()
                            .product(product)
                            .quantity(itemReq.quantity())
                            .unitPrice(product.getPrice())
                            .subtotal(subtotal)
                            .build();
                })
                .toList();

        // Calculamos el total usando Streams
        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(user)
                .total(total)
                .build();
        order = orderRepository.save(order);

        // Asignamos la referencia a la orden en cada ítem
        final Order savedOrder = order;
        items.forEach(item -> item.setOrder(savedOrder));
        savedOrder.setItems(items);
        orderRepository.save(savedOrder);

        auditService.logSuccess("CREATE_ORDER", "ORDER", savedOrder.getId(), userEmail,
                "Orden creada con %d ítem(s), total: %s".formatted(items.size(), total));

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<Dtos.OrderResponse> getMyOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return orderRepository.findByUserIdWithItems(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Dtos.OrderResponse> getAllOrders() {
        return orderRepository.findAllWithItems()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Dtos.OrderResponse getOrderById(Long id, String userEmail, boolean isAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + id));

        // Un CLIENTE solo puede ver sus propias órdenes
        if (!isAdmin && !order.getUser().getEmail().equals(userEmail)) {
            auditService.logDenied("VIEW_ORDER", "ORDER", id, userEmail,
                    "Intento de acceso a orden de otro usuario");
            throw new AccessDeniedException("No tiene permiso para ver esta orden");
        }

        return toResponse(order);
    }

    private Dtos.OrderResponse toResponse(Order order) {
        List<Dtos.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new Dtos.OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new Dtos.OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getName(),
                itemResponses,
                order.getTotal(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
}
