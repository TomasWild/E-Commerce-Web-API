package com.wild.ecommerce.category.repository;

import com.wild.ecommerce.category.model.Category;
import com.wild.ecommerce.util.TestAuditorConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(TestAuditorConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CategoryRepositoryTest {

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
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category testCategory1;
    private Category testCategory2;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();

        testCategory1 = new Category();
        testCategory1.setName("Test Category 1");
        testCategory1.setDescription("Description for test category 1");

        testCategory2 = new Category();
        testCategory2.setName("Test Category 2");
        testCategory2.setDescription("Description for test category 2");
    }

    @Test
    void shouldSaveAndFindCategory() {
        // Given
        Category savedCategory = categoryRepository.save(testCategory1);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Category> found = categoryRepository.findById(savedCategory.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Category 1");
        assertThat(found.get().getDescription()).isEqualTo("Description for test category 1");
    }

    @Test
    void findByNameIgnoreCase_shouldFindExactMatch() {
        // Given
        categoryRepository.save(testCategory1);
        entityManager.flush();

        // When
        Optional<Category> found = categoryRepository.findByNameIgnoreCase("Test Category 1");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Category 1");
    }

    @Test
    void findByNameIgnoreCase_shouldBeCaseInsensitive() {
        // Given
        categoryRepository.save(testCategory1);
        entityManager.flush();

        // When
        Optional<Category> foundLower = categoryRepository.findByNameIgnoreCase("test category 1");
        Optional<Category> foundUpper = categoryRepository.findByNameIgnoreCase("TEST CATEGORY 1");
        Optional<Category> foundMixed = categoryRepository.findByNameIgnoreCase("TeSt CaTeGoRy 1");

        // Then
        assertThat(foundLower).isPresent();
        assertThat(foundUpper).isPresent();
        assertThat(foundMixed).isPresent();
        assertThat(foundLower.get().getName()).isEqualTo("Test Category 1");
    }

    @Test
    void findByNameIgnoreCase_shouldReturnEmptyWhenNotFound() {
        // Given
        categoryRepository.save(testCategory1);
        entityManager.flush();

        // When
        Optional<Category> found = categoryRepository.findByNameIgnoreCase("NonExistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void existsByNameIgnoreCase_shouldReturnTrueWhenExists() {
        // Given
        categoryRepository.save(testCategory1);
        entityManager.flush();

        // When
        boolean exists = categoryRepository.existsByNameIgnoreCase("Test Category 1");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByNameIgnoreCase_shouldBeCaseInsensitive() {
        // Given
        categoryRepository.save(testCategory1);
        entityManager.flush();

        // When & Then
        assertThat(categoryRepository.existsByNameIgnoreCase("test category 1")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("TEST CATEGORY 1")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("TeSt CaTeGoRy 1")).isTrue();
    }

    @Test
    void existsByNameIgnoreCase_shouldReturnFalseWhenNotExists() {
        // Given
        categoryRepository.save(testCategory1);
        entityManager.flush();

        // When
        boolean exists = categoryRepository.existsByNameIgnoreCase("NonExistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldFindAllCategories() {
        // Given
        categoryRepository.save(testCategory1);
        categoryRepository.save(testCategory2);
        entityManager.flush();

        // When
        List<Category> categories = categoryRepository.findAll();

        // Then
        assertThat(categories).hasSize(2);
        assertThat(categories)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Test Category 1", "Test Category 2");
    }

    @Test
    void shouldUpdateCategory() {
        // Given
        Category savedCategory = categoryRepository.save(testCategory1);
        entityManager.flush();
        entityManager.clear();

        // When
        savedCategory.setDescription("Updated description");
        Category updatedCategory = categoryRepository.save(savedCategory);
        entityManager.flush();

        // Then
        assertThat(updatedCategory.getDescription()).isEqualTo("Updated description");
        assertThat(categoryRepository.findById(savedCategory.getId()))
                .isPresent()
                .hasValueSatisfying(cat ->
                        assertThat(cat.getDescription()).isEqualTo("Updated description")
                );
    }

    @Test
    void shouldDeleteCategory() {
        // Given
        Category savedCategory = categoryRepository.save(testCategory1);
        entityManager.flush();
        UUID categoryId = savedCategory.getId();

        // When
        categoryRepository.deleteById(categoryId);
        entityManager.flush();

        // Then
        assertThat(categoryRepository.findById(categoryId)).isEmpty();
    }
}
