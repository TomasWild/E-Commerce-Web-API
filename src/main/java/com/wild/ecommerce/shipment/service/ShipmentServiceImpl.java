package com.wild.ecommerce.shipment.service;

import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.order.model.Order;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.order.repository.OrderRepository;
import com.wild.ecommerce.shipment.dto.ShipOrderRequest;
import com.wild.ecommerce.shipment.dto.ShipmentInfoDTO;
import com.wild.ecommerce.shipment.dto.TrackingInfoDTO;
import com.wild.ecommerce.order.mapper.ShipmentInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    private final OrderRepository orderRepository;
    private final ShipmentInfoMapper shippingInfoMapper;

    @Override
    @Transactional
    public void initiateShipping(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID '" + orderId + "' not found"));

        if (order.getStatus() != Status.CONFIRMED) {
            log.warn("Cannot initiate shipping for order {} with status {}", orderId, order.getStatus());
            return;
        }

        order.setStatus(Status.SHIPPED);

        orderRepository.save(order);

        log.info("Shipping initiated for order {}", orderId);
    }

    @Override
    @Transactional
    public ShipmentInfoDTO shipOrder(UUID orderId, ShipOrderRequest request, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID '" + orderId + "' not found"));

        if (!order.getEmail().equals(userEmail)) {
            throw new IllegalStateException("Order does not belong to the user");
        }

        if (order.getStatus() != Status.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be shipped. Current status: " + order.getStatus());
        }

        String trackingNumber = (request.trackingNumber() == null || request.trackingNumber().isBlank())
                ? generateTrackingNumber()
                : request.trackingNumber();

        order.setStatus(Status.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        order.setCarrier(request.carrier());
        order.setShippedDate(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        log.info("Order {} marked as shipped by user {}", orderId, userEmail);

        return shippingInfoMapper.apply(savedOrder, trackingNumber, request.carrier());
    }

    @Override
    @Transactional
    public ShipmentInfoDTO markAsDelivered(UUID orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID '" + orderId + "' not found"));

        if (!order.getEmail().equals(userEmail)) {
            throw new IllegalStateException("Order does not belong to the user");
        }

        if (order.getStatus() != Status.SHIPPED) {
            throw new IllegalStateException("Only shipped orders can be marked as delivered. Current status: " + order.getStatus());
        }

        order.setStatus(Status.DELIVERED);
        order.setShippedDate(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        log.info("Order {} marked as delivered by user {}", orderId, userEmail);

        return shippingInfoMapper.apply(savedOrder, order.getTrackingNumber(), order.getCarrier());
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingInfoDTO getTrackingInfo(UUID orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID '" + orderId + "' not found"));

        if (!order.getEmail().equals(userEmail)) {
            throw new IllegalStateException("Order does not belong to the user");
        }

        return new TrackingInfoDTO(
                order.getId(),
                order.getStatus(),
                order.getTrackingNumber() != null ? order.getTrackingNumber() : "N/A",
                order.getCarrier() != null ? order.getCarrier() : "N/A",
                order.getOrderDate(),
                order.getStatus() == Status.SHIPPED ? LocalDateTime.now() : null,
                order.getStatus() == Status.DELIVERED ? LocalDateTime.now() : null,
                getStatusMessage(order.getStatus())
        );
    }

    private String generateTrackingNumber() {
        Random random = new Random();

        return String.format("TRK%d%06d",
                System.currentTimeMillis() % 10000000,
                random.nextInt(1000000)
        );
    }

    private String getStatusMessage(Status status) {
        return switch (status) {
            case PENDING -> "Order is pending payment confirmation";
            case CONFIRMED -> "Order confirmed, preparing for shipment";
            case SHIPPED -> "Order has been shipped and is on the way";
            case DELIVERED -> "Order has been delivered";
            case FAILED -> "Payment failed, order cannot be processed";
            case CANCELLED -> "Order has been cancelled";
        };
    }
}
