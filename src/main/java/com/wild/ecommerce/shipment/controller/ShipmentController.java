package com.wild.ecommerce.shipment.controller;

import com.wild.ecommerce.shipment.dto.ShipOrderRequest;
import com.wild.ecommerce.shipment.dto.ShipmentInfoDTO;
import com.wild.ecommerce.shipment.dto.TrackingInfoDTO;
import com.wild.ecommerce.shipment.service.ShipmentService;
import com.wild.ecommerce.user.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Endpoints for managing order shipments")
public class ShipmentController {

    private final ShipmentService shippingService;

    @PostMapping("{orderId}")
    public ResponseEntity<ShipmentInfoDTO> shipOrder(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody ShipOrderRequest request,
            @AuthenticationPrincipal User user
    ) {
        ShipmentInfoDTO shippingInfo = shippingService.shipOrder(orderId, request, user.getEmail());

        return new ResponseEntity<>(shippingInfo, HttpStatus.OK);
    }

    @GetMapping("{orderId}")
    public ResponseEntity<TrackingInfoDTO> getTrackingInfo(
            @PathVariable("orderId") UUID orderId,
            @AuthenticationPrincipal User user
    ) {
        TrackingInfoDTO trackingInfo = shippingService.getTrackingInfo(orderId, user.getEmail());

        return new ResponseEntity<>(trackingInfo, HttpStatus.OK);
    }

    @PatchMapping("{orderId}")
    public ResponseEntity<ShipmentInfoDTO> markAsDelivered(
            @PathVariable("orderId") UUID orderId,
            @AuthenticationPrincipal User user
    ) {
        ShipmentInfoDTO shippingInfo = shippingService.markAsDelivered(orderId, user.getEmail());

        return new ResponseEntity<>(shippingInfo, HttpStatus.OK);
    }
}
