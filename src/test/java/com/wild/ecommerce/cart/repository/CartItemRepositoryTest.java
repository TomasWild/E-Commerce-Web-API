package com.wild.ecommerce.cart.repository;

import com.wild.ecommerce.cart.model.Cart;
import com.wild.ecommerce.cart.model.CartItem;
import com.wild.ecommerce.product.model.Product;
import com.wild.ecommerce.user.model.Role;
import com.wild.ecommerce.user.model.User;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(TestAuditorConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CartItemRepositoryTest {

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
    private CartItemRepository cartItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();

        product = new Product();
        product.setName("Test Product");
        product.setBrand("Test Brand");
        product.setDescription("Test product");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setStock(100);
        entityManager.persist(product);

        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test.user@example.com");
        user.setPassword("TestPass123!");
        user.setRole(Role.USER);
        entityManager.persist(user);

        cart = new Cart();
        cart.setUser(user);
        entityManager.persist(cart);

        entityManager.flush();
    }

    @Test
    void shouldSaveCartItemWithValidData() {
        // Given
        CartItem cartItem = new CartItem(product, BigDecimal.valueOf(99.99), 2);
        cartItem.setCart(cart);

        // When
        CartItem saved = cartItemRepository.save(cartItem);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(saved.getProduct()).isNotNull();
        assertThat(saved.getCart()).isNotNull();

    }

    @Test
    void shouldFindCartItemByIdWhenExists() {
        // Given
        CartItem cartItem = new CartItem(product, BigDecimal.valueOf(49.99), 1);
        cartItem.setCart(cart);
        CartItem saved = entityManager.persist(cartItem);
        entityManager.flush();

        // When
        Optional<CartItem> found = cartItemRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getQuantity()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyOptionalWhenCartItemDoesNotExist() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<CartItem> found = cartItemRepository.findById(nonExistentId);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAllCartItemsWhenMultipleExist() {
        // Given
        CartItem item1 = new CartItem(product, BigDecimal.valueOf(29.99), 3);
        item1.setCart(cart);
        CartItem item2 = new CartItem(product, BigDecimal.valueOf(39.99), 1);
        item2.setCart(cart);

        entityManager.persist(item1);
        entityManager.persist(item2);
        entityManager.flush();

        // When
        List<CartItem> allItems = cartItemRepository.findAll();

        // Then
        assertThat(allItems).hasSize(2);
    }

    @Test
    void shouldUpdateQuantityAndPriceWhenCartItemExists() {
        // Given
        CartItem cartItem = new CartItem(product, BigDecimal.valueOf(19.99), 2);
        cartItem.setCart(cart);
        CartItem saved = entityManager.persist(cartItem);
        entityManager.flush();
        entityManager.clear();

        // When
        CartItem toUpdate = cartItemRepository.findById(saved.getId()).orElseThrow();
        toUpdate.setQuantity(5);
        toUpdate.setPrice(BigDecimal.valueOf(24.99));
        cartItemRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        // Then
        CartItem updated = cartItemRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(5);
        assertThat(updated.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(24.99));
    }

    @Test
    void shouldDeleteCartItemByIdWhenExists() {
        // Given
        CartItem cartItem = new CartItem(product, BigDecimal.valueOf(15.99), 1);
        cartItem.setCart(cart);
        CartItem saved = entityManager.persist(cartItem);
        UUID savedId = saved.getId();
        entityManager.flush();

        // When
        cartItemRepository.deleteById(savedId);
        entityManager.flush();

        // Then
        Optional<CartItem> deleted = cartItemRepository.findById(savedId);
        assertThat(deleted).isEmpty();
    }

    @Test
    void shouldDeleteAllCartItemsFromRepository() {
        // Given
        CartItem item1 = new CartItem(product, BigDecimal.valueOf(9.99), 2);
        item1.setCart(cart);
        CartItem item2 = new CartItem(product, BigDecimal.valueOf(14.99), 3);
        item2.setCart(cart);

        entityManager.persist(item1);
        entityManager.persist(item2);
        entityManager.flush();

        // When
        cartItemRepository.deleteAll();
        entityManager.flush();

        // Then
        List<CartItem> allItems = cartItemRepository.findAll();
        assertThat(allItems).isEmpty();
    }

    @Test
    void shouldCalculateTotalPriceBasedOnUnitPriceAndQuantity() {
        // Given
        CartItem cartItem = new CartItem(product, BigDecimal.valueOf(10.00), 3);
        cartItem.setCart(cart);
        CartItem saved = entityManager.persist(cartItem);
        entityManager.flush();

        // When
        BigDecimal totalPrice = saved.getTotalPrice();

        // Then
        assertThat(totalPrice).isEqualByComparingTo(BigDecimal.valueOf(30.00));
    }

    @Test
    void shouldLoadAssociatedProductWhenFetchingCartItem() {
        // Given
        CartItem cartItem = new CartItem(product, BigDecimal.valueOf(50.00), 1);
        cartItem.setCart(cart);
        CartItem saved = entityManager.persist(cartItem);
        entityManager.flush();
        entityManager.clear();

        // When
        CartItem found = cartItemRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getProduct()).isNotNull();
        assertThat(found.getProduct().getName()).isEqualTo("Test Product");
    }
}
