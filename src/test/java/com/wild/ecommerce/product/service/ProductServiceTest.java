package com.wild.ecommerce.product.service;

import com.wild.ecommerce.category.model.Category;
import com.wild.ecommerce.category.repository.CategoryRepository;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.product.dto.CreateProductRequest;
import com.wild.ecommerce.product.dto.ProductDTO;
import com.wild.ecommerce.product.dto.UpdateProductRequest;
import com.wild.ecommerce.product.mapper.ProductMapper;
import com.wild.ecommerce.product.model.Product;
import com.wild.ecommerce.product.repository.ProductRepository;
import com.wild.ecommerce.storage.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID categoryId;
    private Category category;
    private UUID productId;
    private Product product;
    private ProductDTO productDTO;
    private MultipartFile mockImage;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        productId = UUID.randomUUID();

        category = new Category();
        category.setId(categoryId);
        category.setName("Test Category");

        product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setBrand("Test Brand");
        product.setDescription("Test Description");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setStock(10);
        product.setCategory(category);
        product.setImageUrl("https://example.com/image.jpg");

        productDTO = new ProductDTO(
                productId,
                "Test Product",
                "Test Brand",
                "Test description",
                BigDecimal.valueOf(99.99),
                10,
                "https://example.com/image.jpg",
                "Test Category"
        );

        mockImage = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
    }

    @Test
    void createProduct_WithImage_ShouldUploadToS3AndReturnProductDTO() {
        // Arrange
        CreateProductRequest request = new CreateProductRequest(
                "Test Product",
                "Test Brand",
                "Test description",
                BigDecimal.valueOf(99.99),
                10,
                mockImage,
                categoryId
        );

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(minioService.uploadImage(mockImage)).thenReturn("https://s3.amazonaws.com/image.jpg");
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.apply(product)).thenReturn(productDTO);

        // Act
        ProductDTO result = productService.createProduct(request);

        // Assert
        assertNotNull(result);
        assertEquals(productDTO.id(), result.id());

        verify(categoryRepository).findById(categoryId);
        verify(minioService).uploadImage(mockImage);
        verify(productRepository).save(any(Product.class));
        verify(productMapper).apply(product);
    }

    @Test
    void createProduct_WithoutImage_ShouldUseDefaultImage() {
        // Arrange
        CreateProductRequest request = new CreateProductRequest(
                "Test Product",
                "Test Brand",
                "Test description",
                BigDecimal.valueOf(99.99),
                10,
                null,
                category.getId()
        );

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.apply(product)).thenReturn(productDTO);

        // Act
        ProductDTO result = productService.createProduct(request);

        // Assert
        assertNotNull(result);

        verify(minioService, never()).uploadImage(any());
        verify(productRepository).save(argThat(p ->
                "https://placehold.net/400x400.png".equals(p.getImageUrl())
        ));
    }

    @Test
    void createProduct_WithInvalidCategory_ShouldThrowException() {
        // Arrange
        CreateProductRequest request = new CreateProductRequest(
                "Test Product",
                "Test Brand",
                "Test description",
                BigDecimal.valueOf(99.99),
                10,
                null,
                categoryId
        );

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                productService.createProduct(request)
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    void getAllProducts_ShouldReturnPageOfProducts() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        PageImpl<Product> productPage = new PageImpl<>(List.of(product));

        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), eq(pageable)))
                .thenReturn(productPage);
        when(productMapper.apply(product)).thenReturn(productDTO);
        when(productMapper.apply(product)).thenReturn(productDTO);

        // Act
        PageResponse<ProductDTO> result = productService.getAllProducts(
                pageable, "Test", "Brand", categoryId
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(productRepository).findAll(ArgumentMatchers.<Specification<Product>>any(), eq(pageable));
    }

    @Test
    void getProductById_WithValidId_ShouldReturnProductDTO() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.apply(product)).thenReturn(productDTO);

        // Act
        ProductDTO result = productService.getProductById(productId);

        // Assert
        assertNotNull(result);
        assertEquals(productId, result.id());
        assertEquals("Test Product", result.name());

        verify(productRepository).findById(productId);
    }

    @Test
    void getProductById_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                productService.getProductById(productId)
        );
    }

    @Test
    void updateProduct_WithNewImage_ShouldDeleteOldAndUploadNew() {
        // Arrange
        UpdateProductRequest request = new UpdateProductRequest(
                "Updated Product",
                "Updated Brand",
                "Updated description",
                BigDecimal.valueOf(149.99),
                20,
                mockImage,
                categoryId
        );

        product.setImageUrl("https://s3.amazonaws.com/old-image.jpg");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(minioService.uploadImage(mockImage)).thenReturn("https://s3.amazonaws.com/new-image.jpg");
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.apply(product)).thenReturn(productDTO);

        // Act
        ProductDTO result = productService.updateProduct(productId, request);

        // Assert
        assertNotNull(result);

        verify(minioService).deleteImage("https://s3.amazonaws.com/old-image.jpg");
        verify(minioService).uploadImage(mockImage);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_WithDefaultImage_ShouldNotDeleteOldImage() {
        // Arrange
        UpdateProductRequest request = new UpdateProductRequest(
                "Updated Product",
                null,
                null,
                null,
                0,
                mockImage,
                null
        );

        product.setImageUrl("https://placehold.net/400x400.png");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(minioService.uploadImage(mockImage)).thenReturn("https://s3.amazonaws.com/new-image.jpg");
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.apply(product)).thenReturn(productDTO);

        // Act
        productService.updateProduct(productId, request);

        // Assert
        verify(minioService, never()).deleteImage(anyString());
        verify(minioService).uploadImage(mockImage);
    }

    @Test
    void updateProduct_WithNewCategory_ShouldUpdateCategory() {
        // Arrange
        UUID id = UUID.randomUUID();
        Category newCategory = new Category();
        newCategory.setId(id);
        newCategory.setName("Test Category");

        UpdateProductRequest request = new UpdateProductRequest(
                null,
                null,
                null,
                null,
                0,
                mockImage,
                id
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(id)).thenReturn(Optional.of(newCategory));
        when(minioService.uploadImage(mockImage)).thenReturn("https://s3.amazonaws.com/image.jpg");
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.apply(product)).thenReturn(productDTO);

        // Act
        productService.updateProduct(productId, request);

        // Assert
        verify(categoryRepository).findById(id);
        verify(productRepository).save(argThat(p -> p.getCategory().getId().equals(id)));
    }

    @Test
    void deleteProduct_WithCustomImage_ShouldDeleteFromS3() {
        // Arrange
        product.setImageUrl("https://s3.amazonaws.com/image.jpg");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act
        productService.deleteProduct(productId);

        // Assert
        verify(minioService).deleteImage("https://s3.amazonaws.com/image.jpg");
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_WithDefaultImage_ShouldNotDeleteFromS3() {
        // Arrange
        product.setImageUrl("https://placehold.net/400x400.png");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act
        productService.deleteProduct(productId);

        // Assert
        verify(minioService, never()).deleteImage(anyString());
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                productService.deleteProduct(productId)
        );
        verify(productRepository, never()).delete((Product) any());
    }
}
