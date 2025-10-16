package com.wild.ecommerce.cart.controller;

import com.wild.ecommerce.cart.dto.AddToCartRequest;
import com.wild.ecommerce.cart.dto.CartDTO;
import com.wild.ecommerce.cart.dto.ReplaceCartItemRequest;
import com.wild.ecommerce.cart.dto.UpdateQuantityRequest;
import com.wild.ecommerce.cart.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@Tag(name = "Carts", description = "Endpoints for managing carts")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<CartDTO> addProductToCart(@Valid @RequestBody AddToCartRequest request) {
        CartDTO cart = cartService.addProductToCart(request.productId(), request.quantity());

        return new ResponseEntity<>(cart, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCurrentUserCart() {
        CartDTO cart = cartService.getCurrentUserCart();

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartDTO> updateProductQuantity(
            @PathVariable("productId") UUID productId,
            @Valid @RequestBody UpdateQuantityRequest request
    ) {
        CartDTO cart = cartService.updateProductQuantity(productId, request.quantity());

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @PutMapping("/items")
    public ResponseEntity<CartDTO> replaceCartItems(@Valid @RequestBody List<ReplaceCartItemRequest> items) {
        CartDTO cart = cartService.replaceCartItems(items);

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @DeleteMapping("/items/product/{productId}")
    public ResponseEntity<CartDTO> removeProductFromCart(@PathVariable("productId") UUID productId) {
        CartDTO cart = cartService.removeProductFromCart(productId);

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartDTO> removeCartItem(@PathVariable("cartItemId") UUID cartItemId) {
        CartDTO cart = cartService.removeCartItem(cartItemId);

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<CartDTO> clearCart() {
        CartDTO cart = cartService.clearCart();

        return new ResponseEntity<>(cart, HttpStatus.OK);
    }
}
