package com.wild.ecommerce.product.repository;

import com.wild.ecommerce.category.model.Category;
import com.wild.ecommerce.product.model.Product;
import com.wild.ecommerce.product.specification.ProductSpecification;
import com.wild.ecommerce.util.TestAuditorConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(TestAuditorConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProductRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("testDB")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        category = new Category();
        category.setName("Test Category");
        category.setDescription("Test category description");
        entityManager.persist(category);

        product = new Product();
        product.setName("Test Product");
        product.setBrand("Test Brand");
        product.setDescription("Test product description");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(10);
        product.setImageUrl("https://example.com/image.jpg");
        product.setCategory(category);
    }

    @Test
    void shouldSaveProductWhenValidDataProvided() {
        // Act
        Product savedProduct = productRepository.save(product);

        // Assert
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("Test Product");
        assertThat(savedProduct.getBrand()).isEqualTo("Test Brand");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(savedProduct.getStock()).isEqualTo(10);
        assertThat(savedProduct.getCategory()).isEqualTo(category);
    }

    @Test
    void shouldFindProductByIdWhenExists() {
        // Arrange
        Product savedProduct = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<Product> foundProduct = productRepository.findById(savedProduct.getId());

        // Assert
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo("Test Product");
        assertThat(foundProduct.get().getBrand()).isEqualTo("Test Brand");
    }

    @Test
    void shouldReturnEmptyOptionalWhenProductDoesNotExist() {
        // Arrange
        UUID randomId = UUID.randomUUID();

        // Act
        Optional<Product> foundProduct = productRepository.findById(randomId);

        // Assert
        assertThat(foundProduct).isEmpty();
    }

    @Test
    void shouldReturnAllProductsWhenMultipleExist() {
        // Arrange
        Product product2 = new Product();
        product2.setName("Test Product 2");
        product2.setBrand("Test Brand 2");
        product2.setDescription("Test product 2 description");
        product2.setPrice(new BigDecimal("29.99"));
        product2.setStock(50);
        product2.setCategory(category);

        productRepository.save(product);
        productRepository.save(product2);

        // Act
        List<Product> products = productRepository.findAll();

        // Assert
        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName)
                .containsExactlyInAnyOrder("Test Product", "Test Product 2");
    }

    @Test
    void shouldFilterProductsByNameContainingKeyword() {
        // Arrange
        Product product2 = new Product();
        product2.setName("Test New Product");
        product2.setBrand("Test New Brand");
        product2.setDescription("Test new product description");
        product2.setPrice(new BigDecimal("1299.99"));
        product2.setStock(5);
        product2.setCategory(category);

        productRepository.save(product);
        productRepository.save(product2);

        // Act
        Specification<Product> spec = ProductSpecification.filterBy("new", null, null);
        List<Product> results = productRepository.findAll(spec);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getName()).isEqualTo("Test New Product");
    }

    @Test
    void shouldFilterProductsByBrandContainingKeyword() {
        // Arrange
        Product product2 = new Product();
        product2.setName("Test New Product");
        product2.setBrand("Test New Brand");
        product2.setDescription("Test new product description");
        product2.setPrice(new BigDecimal("1299.99"));
        product2.setStock(5);
        product2.setCategory(category);

        productRepository.save(product);
        productRepository.save(product2);

        // Act
        Specification<Product> spec = ProductSpecification.filterBy(null, "new", null);
        List<Product> results = productRepository.findAll(spec);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getBrand()).isEqualTo("Test New Brand");
    }

    @Test
    void shouldFilterProductsByCategoryId() {
        // Arrange
        Category category2 = new Category();
        category2.setName("Test Category 2");
        category2.setDescription("Test category 2 description");
        entityManager.persist(category2);

        Product product2 = new Product();
        product2.setName("Test Product 2");
        product2.setBrand("Test Brand 2");
        product2.setDescription("Test product 2 description");
        product2.setPrice(new BigDecimal("299.99"));
        product2.setStock(20);
        product2.setCategory(category2);

        productRepository.save(product);
        productRepository.save(product2);

        // Act
        Specification<Product> spec = ProductSpecification.filterBy(null, null, category.getId());
        List<Product> results = productRepository.findAll(spec);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getCategory().getName()).isEqualTo("Test Category");
    }

    @Test
    void shouldFilterProductsByMultipleCriteria() {
        // Arrange
        Product product2 = new Product();
        product2.setName("Test New Product");
        product2.setBrand("Test Awesome brand");
        product2.setDescription("Test awesome new product description");
        product2.setPrice(new BigDecimal("199.99"));
        product2.setStock(25);
        product2.setCategory(category);

        productRepository.save(product);
        productRepository.save(product2);

        // Act
        Specification<Product> spec = ProductSpecification.filterBy("new", "awesome", category.getId());
        List<Product> results = productRepository.findAll(spec);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getName()).isEqualTo("Test New Product");
    }

    @Test
    void shouldReturnAllProductsWhenNoFiltersApplied() {
        // Arrange
        productRepository.save(product);

        // Act
        Specification<Product> spec = ProductSpecification.filterBy(null, null, null);
        List<Product> results = productRepository.findAll(spec);

        // Assert
        assertThat(results).hasSize(1);
    }

    @Test
    void shouldFilterProductsCaseInsensitively() {
        // Arrange
        productRepository.save(product);

        // Act
        Specification<Product> spec = ProductSpecification.filterBy("PRODUCT", "BRAND", null);
        List<Product> results = productRepository.findAll(spec);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getName()).isEqualTo("Test Product");
    }

    @Test
    void shouldUpdateProductWhenChangesApplied() {
        // Arrange
        Product savedProduct = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        // Act
        savedProduct.setPrice(new BigDecimal("899.99"));
        savedProduct.setStock(5);
        Product updatedProduct = productRepository.save(savedProduct);

        // Assert
        assertThat(updatedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("899.99"));
        assertThat(updatedProduct.getStock()).isEqualTo(5);
    }

    @Test
    void shouldDeleteProductWhenExistingIdProvided() {
        // Arrange
        Product savedProduct = productRepository.save(product);
        UUID productId = savedProduct.getId();

        // Act
        productRepository.deleteById(productId);
        Optional<Product> deletedProduct = productRepository.findById(productId);

        // Assert
        assertThat(deletedProduct).isEmpty();
    }
}
