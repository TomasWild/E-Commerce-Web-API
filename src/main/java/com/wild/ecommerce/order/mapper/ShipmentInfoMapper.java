package com.wild.ecommerce.order.mapper;

import com.wild.ecommerce.common.util.TriFunction;
import com.wild.ecommerce.order.model.Order;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.shipment.dto.ShipmentInfoDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ShipmentInfoMapper implements TriFunction<Order, String, String, ShipmentInfoDTO> {

    @Override
    public ShipmentInfoDTO apply(Order order, String trackingNumber, String carrier) {
        return new ShipmentInfoDTO(
                order.getId(),
                order.getStatus(),
                trackingNumber,
                carrier,
                order.getOrderDate(),
                order.getStatus() == Status.SHIPPED || order.getStatus() == Status.DELIVERED
                        ? LocalDateTime.now() : null
        );
    }
}
