package com.wild.ecommerce.cart.mapper;

import com.wild.ecommerce.cart.dto.CartDTO;
import com.wild.ecommerce.cart.model.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CartMapper implements Function<Cart, CartDTO> {

    private final CartItemMapper cartItemMapper;

    @Override
    public CartDTO apply(Cart cart) {
        return new CartDTO(
                cart.getId(),
                cart.getItems()
                        .stream()
                        .map(cartItemMapper)
                        .collect(Collectors.toSet()),
                cart.getUser().getId(),
                cart.getTotalPrice(),
                cart.getTotalItems());
    }
}
