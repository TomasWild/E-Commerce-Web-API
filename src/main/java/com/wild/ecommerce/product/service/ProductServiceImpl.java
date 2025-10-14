package com.wild.ecommerce.product.service;

import com.wild.ecommerce.category.model.Category;
import com.wild.ecommerce.category.repository.CategoryRepository;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.common.util.BeanUtil;
import com.wild.ecommerce.product.dto.CreateProductRequest;
import com.wild.ecommerce.product.dto.ProductDTO;
import com.wild.ecommerce.product.dto.UpdateProductRequest;
import com.wild.ecommerce.product.mapper.ProductMapper;
import com.wild.ecommerce.product.model.Product;
import com.wild.ecommerce.product.repository.ProductRepository;
import com.wild.ecommerce.product.specification.ProductSpecification;
import com.wild.ecommerce.storage.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final MinioService minioService;

    private static final String DEFAULT_IMAGE_URL = "https://placehold.net/400x400.png";

    @Override
    @Transactional
    public ProductDTO createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID '" + request.categoryId() + "' not found"));

        Product product = new Product();
        product.setName(request.name());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(category);

        if (request.image() != null && !request.image().isEmpty()) {
            String imageUrl = minioService.uploadImage(request.image());
            product.setImageUrl(imageUrl);
            log.info("Uploaded product image to S3: {}", imageUrl);
        } else {
            product.setImageUrl(DEFAULT_IMAGE_URL);
        }

        Product savedProduct = productRepository.save(product);

        return productMapper.apply(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductDTO> getAllProducts(Pageable pageable, String name, String brand, UUID categoryId) {
        Specification<Product> specification = ProductSpecification.filterBy(name, brand, categoryId);

        Page<ProductDTO> page = productRepository.findAll(specification, pageable)
                .map(productMapper);

        return new PageResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + id + "' not found"));

        return productMapper.apply(product);
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + id + "' not found"));

        if (request.categoryId() != null) {
            if (!product.getCategory().getId().equals(request.categoryId())) {
                Category category = categoryRepository.findById(request.categoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category with ID '" + request.categoryId() + "' not found"));

                product.setCategory(category);
            }
        }

        BeanUtil.copyNonNullProperties(request, product);

        if (request.image() != null && !request.image().isEmpty()) {
            String oldImageUrl = product.getImageUrl();

            if (oldImageUrl != null && !oldImageUrl.equals(DEFAULT_IMAGE_URL)) {
                try {
                    minioService.deleteImage(oldImageUrl);
                    log.info("Deleted old product image from S3: {}", oldImageUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete old image from S3: {}", oldImageUrl, e);
                }
            }
        }

        String newImageUrl = minioService.uploadImage(request.image());
        product.setImageUrl(newImageUrl);
        log.info("Uploaded new product image to S3: {}", newImageUrl);

        Product updatedProduct = productRepository.save(product);

        return productMapper.apply(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + id + "' not found"));

        String imageUrl = product.getImageUrl();

        if (imageUrl != null && !imageUrl.equals(DEFAULT_IMAGE_URL)) {
            try {
                minioService.deleteImage(imageUrl);
                log.info("Deleted product image from S3: {}", imageUrl);
            } catch (Exception e) {
                log.warn("Failed to delete image from S3: {}", imageUrl, e);
            }
        }

        productRepository.delete(product);
    }
}
