package com.wild.ecommerce.order.mapper;

import com.wild.ecommerce.address.mapper.AddressMapper;
import com.wild.ecommerce.order.dto.OrderDTO;
import com.wild.ecommerce.order.model.Order;
import com.wild.ecommerce.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMapper implements Function<Order, OrderDTO> {

    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final AddressMapper addressMapper;

    @Override
    public OrderDTO apply(Order order) {
        return new OrderDTO(
                order.getId(),
                order.getEmail(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getOrderDate(),
                order.getOrderItems() != null
                        ? order.getOrderItems().stream()
                        .map(orderItemMapper)
                        .collect(Collectors.toList())
                        : Collections.emptyList(),
                order.getPayment() != null
                        ? paymentMapper.apply(order.getPayment())
                        : null,
                order.getAddress() != null
                        ? addressMapper.apply(order.getAddress())
                        : null
        );
    }
}
