package com.wild.ecommerce.order.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.wild.ecommerce.address.mapper.AddressMapper;
import com.wild.ecommerce.address.model.Address;
import com.wild.ecommerce.address.repository.AddressRepository;
import com.wild.ecommerce.cart.model.Cart;
import com.wild.ecommerce.cart.model.CartItem;
import com.wild.ecommerce.cart.repository.CartRepository;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.common.exception.PaymentProcessingException;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.order.dto.CreateOrderRequest;
import com.wild.ecommerce.order.dto.OrderDTO;
import com.wild.ecommerce.order.dto.UpdateOrderRequest;
import com.wild.ecommerce.order.mapper.OrderMapper;
import com.wild.ecommerce.order.model.Order;
import com.wild.ecommerce.order.model.OrderItem;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.order.repository.OrderRepository;
import com.wild.ecommerce.payment.dto.StripePaymentDTO;
import com.wild.ecommerce.payment.model.PaymentMethod;
import com.wild.ecommerce.payment.service.StripeService;
import com.wild.ecommerce.product.model.Product;
import com.wild.ecommerce.product.repository.ProductRepository;
import com.wild.ecommerce.user.model.User;
import com.wild.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    @SuppressWarnings("unused")
    private AddressMapper addressMapper;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private User user;
    private Cart cart;
    private Product product;
    private Address address;
    private CreateOrderRequest createOrderRequest;
    private final String userEmail = "test@example.com";
    private final UUID addressId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(userEmail);
        user.setFirstName("John");

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setStock(10);

        address = new Address();
        address.setId(addressId);
        address.setCountry("USA");
        address.setState("CA");
        address.setCity("San Francisco");
        address.setStreet("123 Test St");
        address.setPostalCode("94102");
        address.setUser(user);

        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);

        CartItem cartItem = new CartItem();
        cartItem.setId(UUID.randomUUID());
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setPrice(product.getPrice());
        cartItem.setCart(cart);

        cart.setItems(new HashSet<>(List.of(cartItem)));

        createOrderRequest = new CreateOrderRequest(addressId, PaymentMethod.STRIPE);
    }

    @Test
    void givenValidUserCartAndAddress_WhenPlaceOrder_ThenOrderIsCreatedAndPaymentIntentGenerated() throws StripeException {
        // Given
        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        Order savedOrder = new Order();
        savedOrder.setId(orderId);
        savedOrder.setEmail(userEmail);
        savedOrder.setStatus(Status.PENDING);
        savedOrder.setTotalAmount(BigDecimal.valueOf(200.00));

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_test_123");
        when(stripeService.paymentIntent(any(StripePaymentDTO.class))).thenReturn(paymentIntent);

        OrderDTO expectedOrderDTO = new OrderDTO(
                orderId, userEmail, BigDecimal.valueOf(200.00),
                Status.PENDING, LocalDateTime.now(), List.of(), null, null
        );
        when(orderMapper.apply(any(Order.class))).thenReturn(expectedOrderDTO);

        // When
        OrderDTO result = orderService.placeOrder(createOrderRequest, userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.email()).isEqualTo(userEmail);

        verify(orderRepository, times(2)).save(orderCaptor.capture());
        verify(productRepository).save(productCaptor.capture());
        verify(cartRepository).save(cart);
        verify(stripeService).paymentIntent(any(StripePaymentDTO.class));

        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getStock()).isEqualTo(8);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void givenNonexistentUser_WhenPlaceOrder_ThenThrowResourceNotFoundException() {
        // Given
        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(createOrderRequest, userEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with email: '" + userEmail + "' not found");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void givenUserWithoutCart_WhenPlaceOrder_ThenThrowResourceNotFoundException() {
        // Given
        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(createOrderRequest, userEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart not found for user with email");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void givenEmptyCart_WhenPlaceOrder_ThenThrowIllegalStateException() {
        // Given
        cart.setItems(new HashSet<>());
        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(createOrderRequest, userEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot place order with empty cart");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void givenMissingAddress_WhenPlaceOrder_ThenThrowResourceNotFoundException() {
        // Given
        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(createOrderRequest, userEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address with ID");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void givenAddressNotOwnedByUser_WhenPlaceOrder_ThenThrowIllegalStateException() {
        // Given
        User differentUser = new User();
        differentUser.setEmail("different@example.com");
        address.setUser(differentUser);

        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(createOrderRequest, userEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Address does not belong to the user");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void givenInsufficientProductStock_WhenPlaceOrder_ThenThrowIllegalStateException() {
        // Given
        product.setStock(1);

        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(createOrderRequest, userEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough stock for product");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void givenValidOrder_WhenStripePaymentFails_ThenThrowPaymentProcessingException() throws StripeException {
        // Given
        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        Order savedOrder = new Order();
        savedOrder.setId(orderId);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        when(stripeService.paymentIntent(any(StripePaymentDTO.class)))
                .thenThrow(new StripeException("Payment failed", "request_id", "code", 400) {
                });

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(createOrderRequest, userEmail))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessage("Failed to create Stripe PaymentIntent");

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void givenExistingOrders_WhenGetAllOrders_ThenReturnOrderPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Order order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setEmail(userEmail);

        Page<Order> orderPage = new PageImpl<>(List.of(order1));
        when(orderRepository.findByEmail(pageable, userEmail)).thenReturn(orderPage);

        OrderDTO orderDTO = new OrderDTO(
                order1.getId(), userEmail, BigDecimal.valueOf(100.00),
                Status.PENDING, LocalDateTime.now(), List.of(), null, null
        );
        when(orderMapper.apply(order1)).thenReturn(orderDTO);

        // When
        PageResponse<OrderDTO> result = orderService.getAllOrders(pageable, userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().email()).isEqualTo(userEmail);

        verify(orderRepository).findByEmail(pageable, userEmail);
    }

    @Test
    void givenExistingOrder_WhenGetOrderById_ThenReturnOrderDTO() {
        // Given
        Order order = new Order();
        order.setId(orderId);
        order.setEmail(userEmail);
        order.setStatus(Status.PENDING);

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(order));

        OrderDTO orderDTO = new OrderDTO(
                orderId, userEmail, BigDecimal.valueOf(100.00),
                Status.PENDING, LocalDateTime.now(), List.of(), null, null
        );
        when(orderMapper.apply(order)).thenReturn(orderDTO);

        // When
        OrderDTO result = orderService.getOrderById(orderId, userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.email()).isEqualTo(userEmail);

        verify(orderRepository).findByIdWithItems(orderId);
    }

    @Test
    void givenNonexistentOrder_WhenGetOrderById_ThenThrowResourceNotFoundException() {
        // Given
        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(orderId, userEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order with ID");

        verify(orderRepository).findByIdWithItems(orderId);
    }

    @Test
    void givenOrderNotOwnedByUser_WhenGetOrderById_ThenThrowIllegalStateException() {
        // Given
        Order order = new Order();
        order.setId(orderId);
        order.setEmail("different@example.com");

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(orderId, userEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Order does not belong to the user");
    }

    @Test
    void givenExistingOrder_WhenUpdateStatus_ThenPersistNewStatus() {
        // Given
        Order order = new Order();
        order.setId(orderId);
        order.setEmail(userEmail);
        order.setStatus(Status.PENDING);

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(Status.CONFIRMED, null);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderDTO orderDTO = new OrderDTO(
                orderId, userEmail, BigDecimal.valueOf(100.00),
                Status.CONFIRMED, LocalDateTime.now(), List.of(), null, null
        );
        when(orderMapper.apply(order)).thenReturn(orderDTO);

        // When
        OrderDTO result = orderService.updateOrder(orderId, updateRequest, userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(Status.CONFIRMED);

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(Status.CONFIRMED);
    }

    @Test
    void givenExistingOrder_WhenUpdateAddress_ThenPersistNewAddress() {
        // Given
        Order order = new Order();
        order.setId(orderId);
        order.setEmail(userEmail);
        order.setStatus(Status.PENDING);

        UUID newAddressId = UUID.randomUUID();
        Address newAddress = new Address();
        newAddress.setId(newAddressId);
        newAddress.setUser(user);

        UpdateOrderRequest updateRequest = new UpdateOrderRequest(Status.PENDING, newAddressId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(addressRepository.findById(newAddressId)).thenReturn(Optional.of(newAddress));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderDTO orderDTO = new OrderDTO(
                orderId, userEmail, BigDecimal.valueOf(100.00),
                Status.PENDING, LocalDateTime.now(), List.of(), null, null
        );
        when(orderMapper.apply(order)).thenReturn(orderDTO);

        // When
        OrderDTO result = orderService.updateOrder(orderId, updateRequest, userEmail);

        // Then
        assertThat(result).isNotNull();

        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getAddress()).isEqualTo(newAddress);
    }

    @Test
    void givenPendingOrder_WhenDeleteOrder_ThenRemoveOrderAndRestoreStock() {
        // Given
        Order order = new Order();
        order.setId(orderId);
        order.setEmail(userEmail);
        order.setStatus(Status.PENDING);

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setOrder(order);

        order.setOrderItems(List.of(orderItem));

        int initialStock = product.getStock();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        orderService.deleteOrder(orderId, userEmail);

        // Then
        verify(orderRepository).delete(order);
        verify(productRepository).save(productCaptor.capture());

        Product restoredProduct = productCaptor.getValue();
        assertThat(restoredProduct.getStock()).isEqualTo(initialStock + 2);
    }

    @Test
    void givenConfirmedOrder_WhenDeleteOrder_ThenThrowIllegalStateException() {
        // Given
        Order order = new Order();
        order.setId(orderId);
        order.setEmail(userEmail);
        order.setStatus(Status.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.deleteOrder(orderId, userEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only pending or cancelled orders can be deleted");

        verify(orderRepository, never()).delete(any());
    }

    @Test
    void givenCancelledOrder_WhenDeleteOrder_ThenRemoveOrder() {
        // Given
        Order order = new Order();
        order.setId(orderId);
        order.setEmail(userEmail);
        order.setStatus(Status.CANCELLED);
        order.setOrderItems(new ArrayList<>());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        orderService.deleteOrder(orderId, userEmail);

        // Then
        verify(orderRepository).delete(order);
    }
}
