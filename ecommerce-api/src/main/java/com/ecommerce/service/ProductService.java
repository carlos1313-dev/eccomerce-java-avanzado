package com.ecommerce.service;

import com.ecommerce.audit.AuditService;
import com.ecommerce.dto.Dtos;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AuditService auditService;

    public List<Dtos.ProductResponse> findAll() {
        return productRepository.findAllByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Dtos.ProductResponse findById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }

    @Transactional
    public Dtos.ProductResponse create(Dtos.ProductRequest request, String adminEmail) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .build();

        product = productRepository.save(product);
        auditService.logSuccess("CREATE_PRODUCT", "PRODUCT", product.getId(), adminEmail,
                "Producto creado: " + product.getName() + ", stock inicial: " + product.getStock());

        return toResponse(product);
    }

    @Transactional
    public Dtos.ProductResponse update(Long id, Dtos.ProductRequest request, String adminEmail) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());

        product = productRepository.save(product);
        auditService.logSuccess("UPDATE_PRODUCT", "PRODUCT", product.getId(), adminEmail,
                "Producto actualizado: " + product.getName());

        return toResponse(product);
    }

    /**
     * Soft delete: el producto queda en base de datos pero se marca como inactivo.
     * Esto preserva el historial de órdenes que referencian este producto.
     */
    @Transactional
    public void softDelete(Long id, String adminEmail) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));

        product.setActive(false);
        productRepository.save(product);

        auditService.logSuccess("DELETE_PRODUCT", "PRODUCT", id, adminEmail,
                "Producto eliminado (soft delete): " + product.getName());
    }

    public Dtos.ProductResponse toResponse(Product p) {
        return new Dtos.ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStock(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
