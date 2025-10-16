package com.wild.ecommerce.cart.service;

import com.wild.ecommerce.cart.dto.CartDTO;
import com.wild.ecommerce.cart.dto.ReplaceCartItemRequest;
import com.wild.ecommerce.cart.mapper.CartMapper;
import com.wild.ecommerce.cart.model.Cart;
import com.wild.ecommerce.cart.model.CartItem;
import com.wild.ecommerce.cart.repository.CartItemRepository;
import com.wild.ecommerce.cart.repository.CartRepository;
import com.wild.ecommerce.common.exception.InvalidCartOperationException;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.common.exception.UserNotAuthenticatedException;
import com.wild.ecommerce.product.model.Product;
import com.wild.ecommerce.product.repository.ProductRepository;
import com.wild.ecommerce.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional
    public CartDTO addProductToCart(UUID productId, int quantity) {
        if (quantity < 0) {
            throw new InvalidCartOperationException("Quantity must be non-negative");
        }

        Cart cart = getOrCreateCurrentUserCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + productId + "' not found"));

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.increaseQuantity(quantity);
        } else {
            CartItem cartItem = new CartItem(product, product.getPrice(), quantity);
            cart.addItem(cartItem);
        }

        Cart savedCart = cartRepository.save(cart);

        return cartMapper.apply(savedCart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDTO getCurrentUserCart() {
        Cart cart = getOrCreateCurrentUserCart();

        return cartMapper.apply(cart);
    }

    @Override
    @Transactional
    public CartDTO updateProductQuantity(UUID productId, int quantity) {
        if (quantity < 0) {
            throw new InvalidCartOperationException("Quantity must be non-negative");
        }

        Cart cart = getOrCreateCurrentUserCart();

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + productId + "' not found"));

        if (quantity == 0) {
            cart.removeItem(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(quantity);
        }

        Cart savedCart = cartRepository.save(cart);

        return cartMapper.apply(savedCart);
    }

    @Override
    @Transactional
    public CartDTO removeProductFromCart(UUID productId) {
        Cart cart = getOrCreateCurrentUserCart();

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + productId + "' not found"));

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        Cart savedCart = cartRepository.save(cart);

        return cartMapper.apply(savedCart);
    }

    @Override
    @Transactional
    public CartDTO removeCartItem(UUID cartItemId) {
        Cart cart = getOrCreateCurrentUserCart();

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item with ID '" + cartItemId + "' not found"));

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        Cart savedCart = cartRepository.save(cart);

        return cartMapper.apply(savedCart);
    }

    @Override
    @Transactional
    public CartDTO clearCart() {
        Cart cart = getOrCreateCurrentUserCart();
        cart.clearItems();

        Cart savedCart = cartRepository.save(cart);

        return cartMapper.apply(savedCart);
    }

    @Override
    @Transactional
    public CartDTO replaceCartItems(List<ReplaceCartItemRequest> cartItems) {
        Cart cart = getOrCreateCurrentUserCart();
        cart.clearItems();

        for (ReplaceCartItemRequest request : cartItems) {
            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + request.productId() + "' not found"));

            CartItem newItem = new CartItem(product, product.getPrice(), request.quantity());
            cart.addItem(newItem);
        }

        Cart savedCart = cartRepository.save(cart);

        return cartMapper.apply(savedCart);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotAuthenticatedException("User not authenticated");
        }

        return (User) authentication.getPrincipal();
    }

    private Cart getOrCreateCurrentUserCart() {
        User currentUser = getCurrentUser();

        return cartRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(currentUser);
                    newCart.setItems(new HashSet<>());

                    return cartRepository.save(newCart);
                });
    }
}
