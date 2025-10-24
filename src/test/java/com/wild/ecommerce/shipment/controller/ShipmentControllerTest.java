package com.wild.ecommerce.shipment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.ecommerce.auth.service.JwtService;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.shipment.dto.ShipOrderRequest;
import com.wild.ecommerce.shipment.dto.ShipmentInfoDTO;
import com.wild.ecommerce.shipment.dto.TrackingInfoDTO;
import com.wild.ecommerce.shipment.service.ShipmentService;
import com.wild.ecommerce.user.model.Role;
import com.wild.ecommerce.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipmentController.class)
public class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShipmentService shippingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID orderId;
    private User testUser;
    private ShipOrderRequest shipOrderRequest;
    private ShipmentInfoDTO shipmentInfoDTO;
    private TrackingInfoDTO trackingInfoDTO;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);

        shipOrderRequest = new ShipOrderRequest(
                "FedEx",
                "TRACK123456"
        );

        shipmentInfoDTO = new ShipmentInfoDTO(
                orderId,
                Status.SHIPPED,
                "TRACK123456",
                "FedEx",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3)
        );

        trackingInfoDTO = new TrackingInfoDTO(
                orderId,
                Status.DELIVERED,
                "TRACK123456",
                "FedEx",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(3),
                "Delivered"
        );
    }

    @Test
    @WithMockUser
    void shipOrder_ShouldReturnShipmentInfo_WhenValidRequest() throws Exception {
        // Arrange
        when(shippingService.shipOrder(eq(orderId), any(ShipOrderRequest.class), eq(testUser.getEmail())))
                .thenReturn(shipmentInfoDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/shipments/{orderId}", orderId)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shipOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.carrier").value("FedEx"))
                .andExpect(jsonPath("$.trackingNumber").value("TRACK123456"))
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        verify(shippingService).shipOrder(eq(orderId), any(ShipOrderRequest.class), eq(testUser.getEmail()));
    }

    @Test
    @WithMockUser
    void shipOrder_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Arrange
        ShipOrderRequest invalidRequest = new ShipOrderRequest("", "");

        // Act & Assert
        mockMvc.perform(post("/api/v1/shipments/{orderId}", orderId)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shipOrder_ShouldReturnUnauthorized_WhenNoAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/shipments/{orderId}", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shipOrderRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getTrackingInfo_ShouldReturnTrackingInfo_WhenOrderExists() throws Exception {
        // Arrange
        when(shippingService.getTrackingInfo(orderId, testUser.getEmail()))
                .thenReturn(trackingInfoDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/shipments/{orderId}", orderId)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.trackingNumber").value("TRACK123456"))
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        verify(shippingService).getTrackingInfo(orderId, testUser.getEmail());
    }

    @Test
    void getTrackingInfo_ShouldReturnUnauthorized_WhenNoAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/shipments/{orderId}", orderId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void markAsDelivered_ShouldReturnUpdatedShipmentInfo_WhenSuccessful() throws Exception {
        // Arrange
        ShipmentInfoDTO deliveredShipment = new ShipmentInfoDTO(
                orderId,
                Status.DELIVERED,
                "TRACK123456",
                "FedEx",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        );

        when(shippingService.markAsDelivered(orderId, testUser.getEmail()))
                .thenReturn(deliveredShipment);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/shipments/{orderId}", orderId)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        verify(shippingService).markAsDelivered(orderId, testUser.getEmail());
    }

    @Test
    void markAsDelivered_ShouldReturnUnauthorized_WhenNoAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/api/v1/shipments/{orderId}", orderId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shipOrder_ShouldHandleServiceException_WhenOrderNotFound() throws Exception {
        // Arrange
        when(shippingService.shipOrder(eq(orderId), any(ShipOrderRequest.class), eq(testUser.getEmail())))
                .thenThrow(new RuntimeException("Order not found"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/shipments/{orderId}", orderId)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shipOrderRequest)))
                .andExpect(status().is5xxServerError());
    }
}
