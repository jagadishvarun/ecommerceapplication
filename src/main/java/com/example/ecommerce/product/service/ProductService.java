package com.example.ecommerce.product.service;

import com.example.ecommerce.common.exception.ApiException;
import com.example.ecommerce.product.dto.ProductRequest;
import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.entity.Category;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Page<ProductResponse> getAll(String search, Category category, Pageable pageable) {
        Specification<Product> spec = isActive();
        if (search != null && !search.isBlank()) {
            spec = spec.and(nameContains(search));
        }
        if (category != null) {
            spec = spec.and(hasCategory(category));
        }
        return productRepository.findAll(spec, pageable).map(ProductResponse::from);
    }

    public ProductResponse getById(Long id) {
        return ProductResponse.from(findActiveById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findActiveById(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        return ProductResponse.from(productRepository.save(product));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long id) {
        Product product = findActiveById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    private Product findActiveById(Long id) {
        return productRepository.findOne(isActive().and(hasId(id)))
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));
    }

    private static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    private static Specification<Product> nameContains(String search) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    }

    private static Specification<Product> hasCategory(Category category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    private static Specification<Product> hasId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }
}