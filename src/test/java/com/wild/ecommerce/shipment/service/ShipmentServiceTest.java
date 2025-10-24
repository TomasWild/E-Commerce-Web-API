package com.wild.ecommerce.shipment.service;

import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.order.mapper.ShipmentInfoMapper;
import com.wild.ecommerce.order.model.Order;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.order.repository.OrderRepository;
import com.wild.ecommerce.shipment.dto.ShipOrderRequest;
import com.wild.ecommerce.shipment.dto.ShipmentInfoDTO;
import com.wild.ecommerce.shipment.dto.TrackingInfoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShipmentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShipmentInfoMapper shippingInfoMapper;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    private UUID orderId;
    private String userEmail;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userEmail = "test@example.com";

        order = new Order();
        order.setId(orderId);
        order.setEmail(userEmail);
        order.setStatus(Status.CONFIRMED);
        order.setOrderDate(LocalDateTime.now());
    }

    @Test
    void initiateShipping_shouldUpdateStatusToShipped_whenOrderIsConfirmed() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        shipmentService.initiateShipping(orderId);

        // Then
        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(order);
        assertThat(order.getStatus()).isEqualTo(Status.SHIPPED);
    }

    @Test
    void initiateShipping_shouldThrowException_whenOrderNotFound() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shipmentService.initiateShipping(orderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order with ID '" + orderId + "' not found");

        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void initiateShipping_shouldNotUpdateStatus_whenOrderNotConfirmed() {
        // Given
        order.setStatus(Status.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        shipmentService.initiateShipping(orderId);

        // Then
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any());
        assertThat(order.getStatus()).isEqualTo(Status.PENDING);
    }

    @Test
    void shipOrder_shouldShipOrder_whenValidRequestWithTrackingNumber() {
        // Given
        ShipOrderRequest request = new ShipOrderRequest("FedEx", "TRACK123");
        ShipmentInfoDTO expectedDTO = new ShipmentInfoDTO(
                orderId,
                Status.SHIPPED,
                "TRACK123",
                "FedEx",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3)
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(shippingInfoMapper.apply(any(Order.class), eq("TRACK123"), eq("FedEx")))
                .thenReturn(expectedDTO);

        // When
        ShipmentInfoDTO result = shipmentService.shipOrder(orderId, request, userEmail);

        // Then
        assertThat(result).isEqualTo(expectedDTO);
        assertThat(order.getStatus()).isEqualTo(Status.SHIPPED);
        assertThat(order.getTrackingNumber()).isEqualTo("TRACK123");
        assertThat(order.getCarrier()).isEqualTo("FedEx");
        assertThat(order.getShippedDate()).isNotNull();

        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(order);
        verify(shippingInfoMapper).apply(order, "TRACK123", "FedEx");
    }

    @Test
    void shipOrder_shouldGenerateTrackingNumber_whenNotProvided() {
        // Given
        ShipOrderRequest request = new ShipOrderRequest("UPS", null);
        ShipmentInfoDTO expectedDTO = new ShipmentInfoDTO(
                orderId,
                Status.SHIPPED,
                "TRK123456",
                "UPS",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3)
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(shippingInfoMapper.apply(any(Order.class), anyString(), eq("UPS")))
                .thenReturn(expectedDTO);

        // When
        ShipmentInfoDTO result = shipmentService.shipOrder(orderId, request, userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(order.getTrackingNumber()).isNotNull();
        assertThat(order.getTrackingNumber()).startsWith("TRK");

        verify(orderRepository).save(order);
    }

    @Test
    void shipOrder_shouldThrowException_whenOrderNotFound() {
        // Given
        ShipOrderRequest request = new ShipOrderRequest("FedEx", "TRACK123");
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shipmentService.shipOrder(orderId, request, userEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order with ID '" + orderId + "' not found");
    }

    @Test
    void shipOrder_shouldThrowException_whenOrderDoesNotBelongToUser() {
        // Given
        ShipOrderRequest request = new ShipOrderRequest("FedEx", "TRACK123");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> shipmentService.shipOrder(orderId, request, "different@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Order does not belong to the user");
    }

    @Test
    void shipOrder_shouldThrowException_whenOrderNotConfirmed() {
        // Given
        order.setStatus(Status.PENDING);
        ShipOrderRequest request = new ShipOrderRequest("FedEx", "TRACK123");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> shipmentService.shipOrder(orderId, request, userEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only confirmed orders can be shipped");
    }

    @Test
    void markAsDelivered_shouldUpdateStatusToDelivered_whenOrderIsShipped() {
        // Given
        order.setStatus(Status.SHIPPED);
        order.setTrackingNumber("TRACK123");
        order.setCarrier("FedEx");

        ShipmentInfoDTO expectedDTO = new ShipmentInfoDTO(
                orderId,
                Status.DELIVERED,
                "TRACK123",
                "FedEx",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3)
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(shippingInfoMapper.apply(any(Order.class), eq("TRACK123"), eq("FedEx")))
                .thenReturn(expectedDTO);

        // When
        ShipmentInfoDTO result = shipmentService.markAsDelivered(orderId, userEmail);

        // Then
        assertThat(result).isEqualTo(expectedDTO);
        assertThat(order.getStatus()).isEqualTo(Status.DELIVERED);
        assertThat(order.getShippedDate()).isNotNull();

        verify(orderRepository).save(order);
    }

    @Test
    void markAsDelivered_shouldThrowException_whenOrderNotFound() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shipmentService.markAsDelivered(orderId, userEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order with ID '" + orderId + "' not found");
    }

    @Test
    void markAsDelivered_shouldThrowException_whenOrderDoesNotBelongToUser() {
        // Given
        order.setStatus(Status.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> shipmentService.markAsDelivered(orderId, "different@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Order does not belong to the user");
    }

    @Test
    void markAsDelivered_shouldThrowException_whenOrderNotShipped() {
        // Given
        order.setStatus(Status.CONFIRMED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> shipmentService.markAsDelivered(orderId, userEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only shipped orders can be marked as delivered");
    }

    @Test
    void getTrackingInfo_shouldReturnTrackingInfo_whenOrderExists() {
        // Given
        order.setStatus(Status.SHIPPED);
        order.setTrackingNumber("TRACK123");
        order.setCarrier("FedEx");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        TrackingInfoDTO result = shipmentService.getTrackingInfo(orderId, userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.status()).isEqualTo(Status.SHIPPED);
        assertThat(result.trackingNumber()).isEqualTo("TRACK123");
        assertThat(result.carrier()).isEqualTo("FedEx");
        assertThat(result.statusMessage()).contains("shipped");
    }

    @Test
    void getTrackingInfo_shouldReturnNAForMissingFields_whenTrackingNotSet() {
        // Given
        order.setStatus(Status.CONFIRMED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        TrackingInfoDTO result = shipmentService.getTrackingInfo(orderId, userEmail);

        // Then
        assertThat(result.trackingNumber()).isEqualTo("N/A");
        assertThat(result.carrier()).isEqualTo("N/A");
    }

    @Test
    void getTrackingInfo_shouldThrowException_whenOrderNotFound() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> shipmentService.getTrackingInfo(orderId, userEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order with ID '" + orderId + "' not found");
    }

    @Test
    void getTrackingInfo_shouldThrowException_whenOrderDoesNotBelongToUser() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> shipmentService.getTrackingInfo(orderId, "different@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Order does not belong to the user");
    }
}
