package com.wild.ecommerce.order.dto;

import com.wild.ecommerce.address.dto.AddressDTO;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.payment.dto.PaymentDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDTO(
        UUID id,
        String email,
        BigDecimal totalAmount,
        Status status,
        LocalDateTime orderDate,
        List<OrderItemDTO> orderItems,
        PaymentDTO payment,
        AddressDTO address
) {
}
