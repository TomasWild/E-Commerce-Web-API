package com.wild.ecommerce.order.repository;

import com.wild.ecommerce.address.model.Address;
import com.wild.ecommerce.order.model.Order;
import com.wild.ecommerce.order.model.OrderItem;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.product.model.Product;
import com.wild.ecommerce.util.TestAuditorConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@Import(TestAuditorConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class OrderRepositoryTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Order order;
    private Address address;
    private Product product;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        address = new Address();
        address.setCountry("USA");
        address.setState("California");
        address.setCity("San Francisco");
        address.setStreet("123 Main St");
        address.setPostalCode("94102");
        entityManager.persist(address);

        product = new Product();
        product.setName("Test Product");
        product.setBrand("Test Brand");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(100);
        entityManager.persist(product);

        order = new Order();
        order.setEmail("test@example.com");
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setStatus(Status.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setAddress(address);
    }

    @Test
    void givenValidOrder_whenSave_thenOrderIsPersisted() {
        // When
        Order savedOrder = orderRepository.save(order);
        entityManager.flush();

        // Then
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getEmail()).isEqualTo("test@example.com");
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void givenOrdersWithSameEmail_whenFindByEmail_thenReturnAllMatchingOrders() {
        // Given
        orderRepository.save(order);

        Order anotherOrder = new Order();
        anotherOrder.setEmail("test@example.com");
        anotherOrder.setTotalAmount(new BigDecimal("200.00"));
        anotherOrder.setStatus(Status.CONFIRMED);
        anotherOrder.setOrderDate(LocalDateTime.now());
        anotherOrder.setAddress(address);
        orderRepository.save(anotherOrder);

        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> result = orderRepository.findByEmail(pageable, "test@example.com");

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Order::getEmail)
                .containsOnly("test@example.com");
    }

    @Test
    void givenNoOrdersForEmail_whenFindByEmail_thenReturnEmptyPage() {
        // Given
        orderRepository.save(order);
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> result = orderRepository.findByEmail(pageable, "nonexistent@example.com");

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void givenOrderWithItems_whenFindByIdWithItems_thenReturnOrderWithAllItems() {
        // Given
        OrderItem item1 = new OrderItem();
        item1.setOrder(order);
        item1.setProduct(product);
        item1.setQuantity(2);
        item1.setPrice(new BigDecimal("50.00"));

        OrderItem item2 = new OrderItem();
        item2.setOrder(order);
        item2.setProduct(product);
        item2.setQuantity(1);
        item2.setPrice(new BigDecimal("50.00"));

        order.getOrderItems().add(item1);
        order.getOrderItems().add(item2);

        Order savedOrder = orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Order> result = orderRepository.findByIdWithItems(savedOrder.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderItems()).hasSize(2);
        assertThat(result.get().getOrderItems())
                .extracting(OrderItem::getQuantity)
                .containsExactlyInAnyOrder(2, 1);
    }

    @Test
    void givenNonexistentOrderId_whenFindByIdWithItems_thenReturnEmptyOptional() {
        // When
        Optional<Order> result = orderRepository.findByIdWithItems(UUID.randomUUID());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void givenOrderWithItems_whenFindByIdWithItems_thenOrderItemsAreEagerlyFetched() {
        // Given
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("100.00"));
        order.getOrderItems().add(item);

        Order savedOrder = orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Order> result = orderRepository.findByIdWithItems(savedOrder.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderItems()).isNotEmpty();
    }

    @Test
    void givenManyOrdersForEmail_whenFindByEmail_thenResultsArePaginated() {
        // Given
        for (int i = 0; i < 15; i++) {
            Order order = new Order();
            order.setEmail("pagination@example.com");
            order.setTotalAmount(new BigDecimal("100.00"));
            order.setStatus(Status.PENDING);
            order.setOrderDate(LocalDateTime.now().minusDays(i));
            order.setAddress(address);
            orderRepository.save(order);
        }
        entityManager.flush();

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> firstPage = orderRepository.findByEmail(pageable, "pagination@example.com");

        // Then
        assertThat(firstPage.getContent()).hasSize(10);
        assertThat(firstPage.getTotalElements()).isEqualTo(15);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    void givenOrderWithItems_whenSaveOrder_thenItemsAreAlsoSavedViaCascade() {
        // Given
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(3);
        item.setPrice(new BigDecimal("33.33"));
        order.getOrderItems().add(item);

        // When
        Order savedOrder = orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // Then
        Order foundOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertThat(foundOrder.getOrderItems()).hasSize(1);
    }

    @Test
    void givenOrderWithItems_whenDeleteOrder_thenItemsAreAlsoRemoved() {
        // Given
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("100.00"));
        order.getOrderItems().add(item);

        Order savedOrder = orderRepository.save(order);
        UUID orderId = savedOrder.getId();
        entityManager.flush();

        // When
        orderRepository.deleteById(orderId);
        entityManager.flush();

        // Then
        Optional<Order> result = orderRepository.findById(orderId);
        assertThat(result).isEmpty();
    }
}
