package com.wild.ecommerce.shipment.service;

import com.wild.ecommerce.shipment.dto.ShipOrderRequest;
import com.wild.ecommerce.shipment.dto.ShipmentInfoDTO;
import com.wild.ecommerce.shipment.dto.TrackingInfoDTO;

import java.util.UUID;

public interface ShipmentService {

    void initiateShipping(UUID orderId);

    ShipmentInfoDTO shipOrder(UUID orderId, ShipOrderRequest request, String userEmail);

    ShipmentInfoDTO markAsDelivered(UUID orderId, String userEmail);

    TrackingInfoDTO getTrackingInfo(UUID orderId, String userEmail);
}
