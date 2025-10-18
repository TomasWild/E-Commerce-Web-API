package com.wild.ecommerce.order.mapper;

import com.wild.ecommerce.order.dto.OrderItemDTO;
import com.wild.ecommerce.order.model.OrderItem;
import com.wild.ecommerce.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class OrderItemMapper implements Function<OrderItem, OrderItemDTO> {

    private final ProductMapper productMapper;

    @Override
    public OrderItemDTO apply(OrderItem orderItem) {
        return new OrderItemDTO(
                orderItem.getId(),
                orderItem.getQuantity(),
                orderItem.getPrice(),
                orderItem.getTotal(),
                orderItem.getProduct() != null
                        ? productMapper.apply(orderItem.getProduct())
                        : null
        );
    }
}
