package com.wild.ecommerce.order.service;

import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.order.dto.CreateOrderRequest;
import com.wild.ecommerce.order.dto.OrderDTO;
import com.wild.ecommerce.order.dto.UpdateOrderRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderDTO placeOrder(CreateOrderRequest request, String userEmail);

    PageResponse<OrderDTO> getAllOrders(Pageable pageable, String userEmail);

    OrderDTO getOrderById(UUID id, String userEmail);

    OrderDTO updateOrder(UUID id, UpdateOrderRequest request, String userEmail);

    void deleteOrder(UUID id, String userEmail);
}
