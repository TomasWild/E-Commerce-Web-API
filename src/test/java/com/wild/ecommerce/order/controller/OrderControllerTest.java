package com.wild.ecommerce.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.ecommerce.address.dto.AddressDTO;
import com.wild.ecommerce.auth.service.JwtService;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.order.dto.CreateOrderRequest;
import com.wild.ecommerce.order.dto.OrderDTO;
import com.wild.ecommerce.order.dto.UpdateOrderRequest;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.order.service.OrderService;
import com.wild.ecommerce.payment.dto.PaymentDTO;
import com.wild.ecommerce.payment.model.PaymentMethod;
import com.wild.ecommerce.user.model.Role;
import com.wild.ecommerce.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User user;
    private UUID orderId;
    private UUID addressId;
    private OrderDTO orderDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPassword("Pass@word123");
        user.setRole(Role.USER);

        orderId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        AddressDTO addressDTO = new AddressDTO(
                UUID.randomUUID(),
                "",
                "",
                "",
                "",
                ""
        );

        PaymentDTO paymentDTO = new PaymentDTO(
                UUID.randomUUID(),
                "",
                "",
                "",
                "",
                ""
        );

        orderDTO = new OrderDTO(
                orderId,
                user.getEmail(),
                BigDecimal.valueOf(99.99),
                Status.PENDING,
                LocalDateTime.now(),
                List.of(),
                paymentDTO,
                addressDTO
        );
    }

    @Test
    @WithMockUser
    void placeOrder_ShouldReturnCreatedStatus() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(addressId, PaymentMethod.STRIPE);

        when(orderService.placeOrder(any(CreateOrderRequest.class), eq(user.getEmail())))
                .thenReturn(orderDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .with(user(user))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(orderId.toString()));

        verify(orderService).placeOrder(any(CreateOrderRequest.class), eq(user.getEmail()));
    }

    @Test
    @WithMockUser
    void placeOrder_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CreateOrderRequest invalidRequest = new CreateOrderRequest(null, null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .with(user(user))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getAllOrders_ShouldReturnPagedOrders() throws Exception {
        // Arrange
        PageResponse<OrderDTO> pageResponse = new PageResponse<>(
                List.of(orderDTO),
                0,
                10,
                1L,
                1
        );

        when(orderService.getAllOrders(any(Pageable.class), eq(user.getEmail())))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders")
                        .with(user(user))
                        .with(csrf())
                        .param("pageNumber", "0")
                        .param("pageSize", "10")
                        .param("sortBy", "id")
                        .param("sortOrder", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(orderService).getAllOrders(any(Pageable.class), eq(user.getEmail()));
    }

    @Test
    @WithMockUser
    void getAllOrders_WithDescendingSort_ShouldReturnSortedOrders() throws Exception {
        // Arrange
        PageResponse<OrderDTO> pageResponse = new PageResponse<>(
                List.of(orderDTO),
                0,
                10,
                1L,
                1
        );

        when(orderService.getAllOrders(any(Pageable.class), eq(user.getEmail())))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders")
                        .with(user(user))
                        .with(csrf())
                        .param("sortOrder", "DESC"))
                .andExpect(status().isOk());

        verify(orderService).getAllOrders(any(Pageable.class), eq(user.getEmail()));
    }

    @Test
    @WithMockUser
    void getOrderById_ShouldReturnOrder() throws Exception {
        // Arrange
        when(orderService.getOrderById(orderId, user.getEmail()))
                .thenReturn(orderDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .with(user(user))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService).getOrderById(orderId, user.getEmail());
    }

    @Test
    @WithMockUser
    void updateOrder_ShouldReturnUpdatedOrder() throws Exception {
        // Arrange
        UpdateOrderRequest request = new UpdateOrderRequest(Status.SHIPPED, addressId);
        OrderDTO updatedOrder = new OrderDTO(
                orderId,
                user.getEmail(),
                BigDecimal.valueOf(99.99),
                Status.SHIPPED,
                LocalDateTime.now(),
                List.of(),
                new PaymentDTO(
                        UUID.randomUUID(),
                        "",
                        "",
                        "",
                        "",
                        ""
                ),
                new AddressDTO(
                        UUID.randomUUID(),
                        "",
                        "",
                        "",
                        "",
                        ""
                )
        );

        when(orderService.updateOrder(eq(orderId), any(UpdateOrderRequest.class), eq(user.getEmail())))
                .thenReturn(updatedOrder);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/orders/{id}", orderId)
                        .with(user(user))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        verify(orderService).updateOrder(eq(orderId), any(UpdateOrderRequest.class), eq(user.getEmail()));
    }

    @Test
    @WithMockUser
    void updateOrder_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        UpdateOrderRequest invalidRequest = new UpdateOrderRequest(null, addressId);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/orders/{id}", orderId)
                        .with(user(user))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void deleteOrder_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(orderService).deleteOrder(orderId, user.getEmail());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .with(user(user))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(orderService).deleteOrder(orderId, user.getEmail());
    }

    @Test
    void placeOrder_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(addressId, PaymentMethod.STRIPE);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
