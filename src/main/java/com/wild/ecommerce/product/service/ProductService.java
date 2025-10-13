package com.wild.ecommerce.product.service;

import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.product.dto.CreateProductRequest;
import com.wild.ecommerce.product.dto.ProductDTO;
import com.wild.ecommerce.product.dto.UpdateProductRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {
    ProductDTO createProduct(CreateProductRequest request);

    PageResponse<ProductDTO> getAllProducts(Pageable pageable, String name, String brand, UUID categoryId);

    ProductDTO getProductById(UUID id);

    ProductDTO updateProduct(UUID id, UpdateProductRequest request);

    void deleteProduct(UUID id);
}
