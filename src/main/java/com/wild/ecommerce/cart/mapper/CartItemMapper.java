package com.wild.ecommerce.cart.mapper;

import com.wild.ecommerce.cart.dto.CartItemDTO;
import com.wild.ecommerce.cart.model.CartItem;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class CartItemMapper implements Function<CartItem, CartItemDTO> {

    @Override
    public CartItemDTO apply(CartItem cartItem) {
        return new CartItemDTO(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getPrice(),
                cartItem.getQuantity(),
                cartItem.getTotalPrice()
        );
    }
}
