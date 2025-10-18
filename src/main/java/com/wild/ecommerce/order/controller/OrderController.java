package com.wild.ecommerce.order.controller;

import com.wild.ecommerce.common.dto.PageResponse;
import com.wild.ecommerce.order.dto.CreateOrderRequest;
import com.wild.ecommerce.order.dto.OrderDTO;
import com.wild.ecommerce.order.dto.UpdateOrderRequest;
import com.wild.ecommerce.order.service.OrderService;
import com.wild.ecommerce.user.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints for managing orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<UUID> placeOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal User user
    ) {
        OrderDTO order = orderService.placeOrder(request, user.getEmail());

        return new ResponseEntity<>(order.id(), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PagedModel<OrderDTO>> getAllOrders(
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = "ASC") String sortOrder,
            @AuthenticationPrincipal User user
    ) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        PageResponse<OrderDTO> ordersResponse = orderService.getAllOrders(pageable, user.getEmail());
        Page<OrderDTO> orders = ordersResponse.toPage();

        return new ResponseEntity<>(new PagedModel<>(orders), HttpStatus.OK);
    }

    @GetMapping("{id}")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal User user
    ) {
        OrderDTO order = orderService.getOrderById(id, user.getEmail());

        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @PatchMapping("{id}")
    public ResponseEntity<OrderDTO> updateOrder(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateOrderRequest request,
            @AuthenticationPrincipal User user
    ) {
        OrderDTO order = orderService.updateOrder(id, request, user.getEmail());

        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal User user
    ) {
        orderService.deleteOrder(id, user.getEmail());

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
