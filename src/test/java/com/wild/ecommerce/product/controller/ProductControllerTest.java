package com.wild.ecommerce.product.controller;

import com.wild.ecommerce.auth.service.JwtService;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.product.dto.CreateProductRequest;
import com.wild.ecommerce.product.dto.ProductDTO;
import com.wild.ecommerce.product.dto.UpdateProductRequest;
import com.wild.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID productId;
    private UUID categoryId;
    private ProductDTO productDTO;
    private MockMultipartFile imageFile;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

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

        byte[] imageContent = new byte[51 * 1024];
        imageFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                imageContent
        );
    }

    @Test
    void createProduct_WithValidRequest_ShouldReturnCreated() throws Exception {
        // Arrange
        when(productService.createProduct(any(CreateProductRequest.class)))
                .thenReturn(productDTO);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/products")
                        .file(imageFile)
                        .param("name", "Test Product")
                        .param("brand", "Test Brand")
                        .param("description", "Test description")
                        .param("price", "99.99")
                        .param("stock", "10")
                        .param("categoryId", categoryId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().string("\"" + productId.toString() + "\""));

        verify(productService).createProduct(any(CreateProductRequest.class));
    }

    @Test
    void createProduct_WithoutImage_ShouldReturnCreated() throws Exception {
        // Arrange
        when(productService.createProduct(any(CreateProductRequest.class)))
                .thenReturn(productDTO);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/products")
                        .param("name", "Test Product")
                        .param("brand", "Test Brand")
                        .param("description", "Test description")
                        .param("price", "99.99")
                        .param("stock", "10")
                        .param("categoryId", categoryId.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().string("\"" + productId.toString() + "\""));
    }

    @Test
    void createProduct_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/v1/products")
                        .param("name", "")
                        .param("price", "-10")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(CreateProductRequest.class));
    }

    @Test
    void createProduct_WithInvalidPrice_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/v1/products")
                        .param("name", "Test Product")
                        .param("brand", "Test Brand")
                        .param("description", "Test description")
                        .param("price", "-50.00")
                        .param("stock", "10")
                        .param("categoryId", "1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(CreateProductRequest.class));
    }

    @Test
    void createProduct_WithInvalidStock_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/v1/products")
                        .param("name", "Test Product")
                        .param("brand", "Test Brand")
                        .param("description", "Test description")
                        .param("price", "99.99")
                        .param("stock", "-5")
                        .param("categoryId", "1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(CreateProductRequest.class));
    }

    @Test
    void getAllProducts_WithDefaultParameters_ShouldReturnPagedProducts() throws Exception {
        // Arrange
        PageImpl<ProductDTO> page = new PageImpl<>(List.of(productDTO));
        PageResponse<ProductDTO> pageResponse = new PageResponse<>(page);

        when(productService.getAllProducts(any(), eq(null), eq(null), eq(null)))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.content[0].id").value(productId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Test Product"))
                .andExpect(jsonPath("$.content[0].brand").value("Test Brand"))
                .andExpect(jsonPath("$.content[0].price").value(99.99));

        verify(productService).getAllProducts(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id")),
                null, null, null
        );
    }

    @Test
    void getAllProducts_WithCustomPagination_ShouldReturnPagedProducts() throws Exception {
        // Arrange
        PageImpl<ProductDTO> page = new PageImpl<>(List.of(productDTO));
        PageResponse<ProductDTO> pageResponse = new PageResponse<>(page);

        when(productService.getAllProducts(any(), eq(null), eq(null), eq(null)))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products")
                        .param("pageNumber", "2")
                        .param("pageSize", "20")
                        .param("sortBy", "name")
                        .param("sortOrder", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0));

        verify(productService).getAllProducts(
                PageRequest.of(2, 20, Sort.by(Sort.Direction.DESC, "name")),
                null, null, null
        );
    }

    @Test
    void getAllProducts_WithFilters_ShouldApplyFilters() throws Exception {
        // Arrange
        PageImpl<ProductDTO> page = new PageImpl<>(List.of(productDTO));
        PageResponse<ProductDTO> pageResponse = new PageResponse<>(page);

        when(productService.getAllProducts(any(), eq("Test"), eq("Brand"), eq(categoryId)))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products")
                        .param("name", "Test")
                        .param("brand", "Brand")
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Product"));

        verify(productService).getAllProducts(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id")),
                "Test", "Brand", categoryId
        );
    }

    @Test
    void getAllProducts_WithInvalidSortOrder_ShouldDefaultToASC() throws Exception {
        // Arrange
        PageImpl<ProductDTO> page = new PageImpl<>(List.of(productDTO));
        PageResponse<ProductDTO> pageResponse = new PageResponse<>(page);

        when(productService.getAllProducts(any(), eq(null), eq(null), eq(null)))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products")
                        .param("sortOrder", "INVALID"))
                .andExpect(status().isOk());

        verify(productService).getAllProducts(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id")),
                null, null, null
        );
    }

    @Test
    void getAllProducts_WithMultipleProducts_ShouldReturnAll() throws Exception {
        // Arrange
        UUID product2Id = UUID.randomUUID();
        ProductDTO product2 = new ProductDTO(
                product2Id,
                "Product 2",
                "Brand 2",
                "Description 2",
                BigDecimal.valueOf(199.99),
                20,
                "https://example.com/image2.jpg",
                "Test Category 2"
        );

        PageImpl<ProductDTO> page = new PageImpl<>(List.of(productDTO, product2));
        PageResponse<ProductDTO> pageResponse = new PageResponse<>(page);

        // Act & Assert
        when(productService.getAllProducts(any(), eq(null), eq(null), eq(null)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(productId.toString()))
                .andExpect(jsonPath("$.content[1].id").value(product2Id.toString()));
    }

    @Test
    void getAllProducts_WithEmptyResult_ShouldReturnEmptyPage() throws Exception {
        // Arrange
        Page<ProductDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        PageResponse<ProductDTO> pageResponse = new PageResponse<>(emptyPage);

        when(productService.getAllProducts(any(), eq(null), eq(null), eq(null)))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getProductById_WithValidId_ShouldReturnProduct() throws Exception {
        // Arrange
        when(productService.getProductById(productId)).thenReturn(productDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.brand").value("Test Brand"))
                .andExpect(jsonPath("$.description").value("Test description"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/image.jpg"))
                .andExpect(jsonPath("$.categoryName").value("Test Category"));

        verify(productService).getProductById(productId);
    }

    @Test
    void getProductById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Arrange
        UUID invalidId = UUID.randomUUID();
        when(productService.getProductById(invalidId))
                .thenThrow(new ResourceNotFoundException("Product with ID '" + invalidId + "' not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/" + invalidId))
                .andExpect(status().isNotFound());

        verify(productService).getProductById(invalidId);
    }

    @Test
    void updateProduct_WithValidRequest_ShouldReturnUpdatedProduct() throws Exception {
        // Arrange
        var updatedProduct = new ProductDTO(
                productId,
                "Updated Product",
                "Updated Brand",
                "Updated description",
                BigDecimal.valueOf(149.99),
                20,
                "https://example.com/new-image.jpg",
                "Test Category"
        );

        when(productService.updateProduct(eq(productId), any(UpdateProductRequest.class)))
                .thenReturn(updatedProduct);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/products/" + productId)
                        .file(imageFile)
                        .param("name", "Updated Product")
                        .param("brand", "Updated Brand")
                        .param("description", "Updated description")
                        .param("price", "149.99")
                        .param("stock", "20")
                        .param("categoryId", categoryId.toString())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Product"))
                .andExpect(jsonPath("$.brand").value("Updated Brand"))
                .andExpect(jsonPath("$.price").value(149.99));

        verify(productService).updateProduct(eq(productId), any(UpdateProductRequest.class));
    }

    @Test
    void updateProduct_WithPartialUpdate_ShouldReturnUpdatedProduct() throws Exception {
        // Arrange
        when(productService.updateProduct(eq(productId), any(UpdateProductRequest.class)))
                .thenReturn(productDTO);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/products/" + productId)
                        .file(imageFile)
                        .param("name", "Updated Name")
                        .param("brand", "Updated Brand")
                        .param("description", "Updated description")
                        .param("price", "99.99")
                        .param("stock", "10")
                        .param("categoryId", categoryId.toString())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk());

        verify(productService).updateProduct(eq(productId), any(UpdateProductRequest.class));
    }

    @Test
    void updateProduct_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Arrange
        UUID invalidId = UUID.randomUUID();
        when(productService.updateProduct(eq(invalidId), any(UpdateProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product with ID " + invalidId + " not found"));

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/products/" + invalidId)
                        .file(imageFile)
                        .param("name", "Updated Product")
                        .param("brand", "Updated Brand")
                        .param("description", "Updated description")
                        .param("price", "99.99")
                        .param("stock", "10")
                        .param("categoryId", categoryId.toString())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_WithValidId_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(productService).deleteProduct(productId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/products/" + productId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(productService).deleteProduct(productId);
    }

    @Test
    void deleteProduct_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Arrange
        UUID invalidId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Product with ID '" + invalidId + "' not found"))
                .when(productService).deleteProduct(invalidId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/products/" + invalidId))
                .andExpect(status().isNotFound());

        verify(productService).deleteProduct(invalidId);
    }
}
