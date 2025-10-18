package com.wild.ecommerce.order.service;

import com.wild.ecommerce.address.model.Address;
import com.wild.ecommerce.address.repository.AddressRepository;
import com.wild.ecommerce.cart.model.Cart;
import com.wild.ecommerce.cart.model.CartItem;
import com.wild.ecommerce.cart.repository.CartRepository;
import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.order.dto.CreateOrderRequest;
import com.wild.ecommerce.order.dto.OrderDTO;
import com.wild.ecommerce.order.dto.UpdateOrderRequest;
import com.wild.ecommerce.order.mapper.OrderMapper;
import com.wild.ecommerce.order.model.Order;
import com.wild.ecommerce.order.model.OrderItem;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.order.repository.OrderRepository;
import com.wild.ecommerce.payment.model.Payment;
import com.wild.ecommerce.product.model.Product;
import com.wild.ecommerce.product.repository.ProductRepository;
import com.wild.ecommerce.user.model.User;
import com.wild.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional
    public OrderDTO placeOrder(CreateOrderRequest request, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: '" + userEmail + "' not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user with email: '" + userEmail + "'"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot place order with empty cart");
        }

        Address address = addressRepository.findById(request.addressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address with ID '" + request.addressId() + "' not found"));

        if (!address.getUser().getEmail().equals(userEmail)) {
            throw new IllegalStateException("Address does not belong to the user");
        }

        Order order = new Order();
        order.setEmail(userEmail);
        order.setStatus(Status.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setAddress(address);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException("Not enough stock for product '" + product.getName() + "'");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setOrder(order);
            orderItems.add(orderItem);

            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            totalAmount = totalAmount.add(orderItem.getTotal());
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        Payment payment = new Payment();
        payment.setPaymentMethod(request.paymentMethod());
        payment.setOrder(order);

        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        cart.clearItems();
        cartRepository.save(cart);

        log.info("Order placed successfully with id: {} for user: {}", savedOrder.getId(), userEmail);

        return orderMapper.apply(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderDTO> getAllOrders(Pageable pageable, String userEmail) {
        Page<Order> orderPage = orderRepository.findByEmail(pageable, userEmail);

        Page<OrderDTO> orderDTOPage = orderPage.map(orderMapper);

        return new PageResponse<>(orderDTOPage);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(UUID id, String userEmail) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID '" + id + "' not found"));

        if (!order.getEmail().equals(userEmail)) {
            throw new IllegalStateException("Order does not belong to the user");
        }

        return orderMapper.apply(order);
    }

    @Override
    @Transactional
    public OrderDTO updateOrder(UUID id, UpdateOrderRequest request, String userEmail) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID '" + id + "' not found"));

        if (!order.getEmail().equals(userEmail)) {
            throw new IllegalStateException("Order does not belong to the user");
        }

        if (request.status() != null) {
            order.setStatus(request.status());
        }

        if (request.addressId() != null) {
            Address address = addressRepository.findById(request.addressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address with ID '" + request.addressId() + "' not found"));

            if (!address.getUser().getEmail().equals(userEmail)) {
                throw new IllegalStateException("Address does not belong to the user");
            }

            order.setAddress(address);
        }

        Order updatedOrder = orderRepository.save(order);

        return orderMapper.apply(updatedOrder);
    }

    @Override
    @Transactional
    public void deleteOrder(UUID id, String userEmail) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID '" + id + "' not found"));

        if (!order.getEmail().equals(userEmail)) {
            throw new IllegalStateException("Order does not belong to the user");
        }

        if (order.getStatus() != Status.PENDING && order.getStatus() != Status.CANCELLED) {
            throw new IllegalStateException("Only pending or cancelled orders can be deleted");
        }

        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            product.setStock(product.getStock() + orderItem.getQuantity());

            productRepository.save(product);
        }

        orderRepository.delete(order);
    }
}
