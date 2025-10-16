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
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@Import(TestAuditorConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CartRepositoryTest {

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
    private CartRepository cartRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();

        user1 = createUser("test@example.com");
        user2 = createUser("another@example.com");
    }

    @Test
    void saveCart_shouldPersistCartForUser() {
        // Given
        Cart cart = new Cart();
        cart.setUser(user1);
        cart.setItems(new HashSet<>());

        // When
        Cart savedCart = cartRepository.save(cart);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(savedCart.getId()).isNotNull();
        assertThat(savedCart.getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    void findByUserId_shouldReturnCartForExistingUser() {
        // Given
        Cart cart = createCartForUser(user1);
        entityManager.persistAndFlush(cart);
        entityManager.clear();

        // When
        Optional<Cart> foundCart = cartRepository.findByUserId(user1.getId());

        // Then
        assertThat(foundCart).isPresent();
        assertThat(foundCart.get().getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    void findByUserId_shouldReturnEmptyForNonExistingUser() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();

        // When
        Optional<Cart> foundCart = cartRepository.findByUserId(nonExistentUserId);

        // Then
        assertThat(foundCart).isEmpty();
    }

    @Test
    void findByUserId_shouldReturnCorrectCartWhenMultipleCartsExist() {
        // Given
        Cart cart1 = createCartForUser(user1);
        Cart cart2 = createCartForUser(user2);
        entityManager.persist(cart1);
        entityManager.persist(cart2);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Cart> foundCart = cartRepository.findByUserId(user1.getId());

        // Then
        assertThat(foundCart).isPresent();
        assertThat(foundCart.get().getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    void saveCartWithItems_shouldPersistCartAndItems() {
        // Given
        Cart cart = createCartForUser(user1);
        CartItem item1 = createCartItem(cart, 2, new BigDecimal("10.00"));
        CartItem item2 = createCartItem(cart, 1, new BigDecimal("25.00"));

        cart.addItem(item1);
        cart.addItem(item2);

        // When
        Cart savedCart = cartRepository.save(cart);
        entityManager.flush();
        entityManager.clear();

        // Then
        Cart retrievedCart = cartRepository.findById(savedCart.getId()).orElseThrow();
        assertThat(retrievedCart.getItems()).hasSize(2);
        assertThat(retrievedCart.getTotalItems()).isEqualTo(3);
        assertThat(retrievedCart.getTotalPrice()).isEqualByComparingTo(new BigDecimal("45.00"));
    }

    @Test
    void deleteCart_shouldCascadeDeleteCartItems() {
        // Given
        Cart cart = createCartForUser(user1);
        CartItem item = createCartItem(cart, 1, new BigDecimal("10.00"));
        cart.addItem(item);

        Cart savedCart = cartRepository.save(cart);
        entityManager.flush();
        UUID cartId = savedCart.getId();
        entityManager.clear();

        // When
        cartRepository.deleteById(cartId);
        entityManager.flush();

        // Then
        Optional<Cart> deletedCart = cartRepository.findById(cartId);
        assertThat(deletedCart).isEmpty();
    }

    @Test
    void updateCart_shouldAddNewItem() {
        // Given
        Cart cart = createCartForUser(user1);
        Cart savedCart = cartRepository.save(cart);
        entityManager.flush();
        entityManager.clear();

        // When
        Cart cartToUpdate = cartRepository.findById(savedCart.getId()).orElseThrow();
        CartItem newItem = createCartItem(cartToUpdate, 5, new BigDecimal("15.00"));
        cartToUpdate.addItem(newItem);

        cartRepository.save(cartToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Then
        Cart updatedCart = cartRepository.findById(savedCart.getId()).orElseThrow();
        assertThat(updatedCart.getItems()).hasSize(1);
        assertThat(updatedCart.getTotalItems()).isEqualTo(5);
    }

    @Test
    void updateCart_shouldRemoveItem() {
        // Given
        Cart cart = createCartForUser(user1);
        CartItem item1 = createCartItem(cart, 2, new BigDecimal("10.00"));
        CartItem item2 = createCartItem(cart, 1, new BigDecimal("25.00"));

        cart.addItem(item1);
        cart.addItem(item2);

        Cart savedCart = cartRepository.save(cart);
        entityManager.flush();
        entityManager.clear();

        // When
        Cart cartToUpdate = cartRepository.findById(savedCart.getId()).orElseThrow();
        CartItem itemToRemove = cartToUpdate.getItems().iterator().next();
        cartToUpdate.removeItem(itemToRemove);

        cartRepository.save(cartToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Then
        Cart updatedCart = cartRepository.findById(savedCart.getId()).orElseThrow();
        assertThat(updatedCart.getItems()).hasSize(1);
    }

    @Test
    void updateCart_shouldClearAllItems() {
        // Given
        Cart cart = createCartForUser(user1);
        cart.addItem(createCartItem(cart, 2, new BigDecimal("10.00")));
        cart.addItem(createCartItem(cart, 1, new BigDecimal("25.00")));

        Cart savedCart = cartRepository.save(cart);
        entityManager.flush();
        entityManager.clear();

        // When
        Cart cartToUpdate = cartRepository.findById(savedCart.getId()).orElseThrow();
        cartToUpdate.clearItems();

        cartRepository.save(cartToUpdate);
        entityManager.flush();
        entityManager.clear();

        // Then
        Cart updatedCart = cartRepository.findById(savedCart.getId()).orElseThrow();
        assertThat(updatedCart.getItems()).isEmpty();
        assertThat(updatedCart.getTotalItems()).isZero();
        assertThat(updatedCart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void saveCart_shouldFailWhenUserAlreadyHasCart() {
        // Given
        Cart cart1 = createCartForUser(user1);
        cartRepository.save(cart1);
        entityManager.flush();

        // When & Then
        Cart cart2 = createCartForUser(user1);

        assertThrows(
                Exception.class,
                () -> {
                    cartRepository.save(cart2);
                    entityManager.flush();
                }
        );
    }

    private User createUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("TestPass123!");
        user.setRole(Role.USER);

        return entityManager.persist(user);
    }

    private Product createProduct() {
        Product product = new Product();
        product.setName("Test Product");
        product.setBrand("Test Brand");
        product.setDescription("Test product");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setStock(100);

        return entityManager.persist(product);
    }

    private Cart createCartForUser(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setItems(new HashSet<>());

        return cart;
    }

    private CartItem createCartItem(Cart cart, int quantity, BigDecimal price) {
        Product product = createProduct();

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setQuantity(quantity);
        item.setPrice(price);
        item.setProduct(product);

        return item;
    }
}
