package com.wild.ecommerce.cart.service;

import com.wild.ecommerce.cart.dto.CartDTO;
import com.wild.ecommerce.cart.dto.ReplaceCartItemRequest;

import java.util.List;
import java.util.UUID;

public interface CartService {

    CartDTO addProductToCart(UUID productId, int quantity);

    CartDTO getCurrentUserCart();

    CartDTO updateProductQuantity(UUID productId, int quantity);

    CartDTO removeProductFromCart(UUID productId);

    CartDTO removeCartItem(UUID cartItemId);

    CartDTO clearCart();

    CartDTO replaceCartItems(List<ReplaceCartItemRequest> cartItems);
}
