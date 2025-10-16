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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart cart;
    private Product product;
    private CartDTO cartDTO;
    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        product = new Product();
        product.setId(productId);
        product.setPrice(BigDecimal.valueOf(99.99));

        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);
        cart.setItems(new HashSet<>());

        cartDTO = new CartDTO(cart.getId(), new HashSet<>(), userId, BigDecimal.ZERO, 1);

        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getPrincipal()).thenReturn(user);
    }

    @Test
    void addProductToCart_ShouldAddNewProduct_WhenProductNotInCart() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        CartDTO result = cartService.addProductToCart(productId, 2);

        // Assert
        assertNotNull(result);
        assertEquals(1, cart.getItems().size());
        verify(cartRepository).save(cart);
        verify(cartMapper).apply(cart);
    }

    @Test
    void addProductToCart_ShouldIncreaseQuantity_WhenProductAlreadyInCart() {
        // Arrange
        CartItem existingItem = new CartItem(product, product.getPrice(), 3);
        cart.addItem(existingItem);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        cartService.addProductToCart(productId, 2);

        // Assert
        assertEquals(5, existingItem.getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void addProductToCart_ShouldCreateNewCart_WhenUserHasNoCart() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        CartDTO result = cartService.addProductToCart(productId, 1);

        // Assert
        assertNotNull(result);
        verify(cartRepository, times(2)).save(any(Cart.class));
    }

    @Test
    void addProductToCart_ShouldThrowException_WhenProductNotFound() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                cartService.addProductToCart(productId, 1)
        );

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addProductToCart_ShouldThrowException_WhenQuantityIsNegative() {
        // Act & Assert
        assertThrows(InvalidCartOperationException.class, () ->
                cartService.addProductToCart(productId, -1)
        );

        verify(cartRepository, never()).save(any());
    }

    @Test
    void addProductToCart_ShouldAcceptZeroQuantity() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        CartDTO result = cartService.addProductToCart(productId, 0);

        // Assert
        assertNotNull(result);
        verify(cartRepository).save(cart);
    }

    @Test
    void getCurrentUserCart_ShouldReturnCart_WhenCartExists() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        CartDTO result = cartService.getCurrentUserCart();

        // Assert
        assertNotNull(result);
        verify(cartRepository).findByUserId(userId);
        verify(cartMapper).apply(cart);
    }

    @Test
    void getCurrentUserCart_ShouldCreateAndReturnNewCart_WhenCartDoesNotExist() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        CartDTO result = cartService.getCurrentUserCart();

        // Assert
        assertNotNull(result);
        verify(cartRepository).save(any(Cart.class));
        verify(cartMapper).apply(cart);
    }

    @Test
    void getCurrentUserCart_ShouldThrowException_WhenUserNotAuthenticated() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act & Assert
        assertThrows(UserNotAuthenticatedException.class, () ->
                cartService.getCurrentUserCart()
        );
    }

    @Test
    void getCurrentUserCart_ShouldThrowException_WhenAuthenticationIsNull() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act & Assert
        assertThrows(UserNotAuthenticatedException.class, () ->
                cartService.getCurrentUserCart()
        );
    }

    @Test
    void updateProductQuantity_ShouldUpdateQuantity_WhenValidQuantity() {
        // Arrange
        CartItem cartItem = new CartItem(product, product.getPrice(), 2);
        cart.addItem(cartItem);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cartItem.getCart());
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        cartService.updateProductQuantity(productId, 5);

        // Assert
        assertEquals(5, cartItem.getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void updateProductQuantity_ShouldRemoveItem_WhenQuantityIsZero() {
        // Arrange
        CartItem cartItem = new CartItem(product, cart.getTotalPrice(), 2);
        cart.addItem(cartItem);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cartItem.getCart());
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        cartService.updateProductQuantity(productId, 0);

        // Assert
        assertTrue(cart.getItems().isEmpty());
        verify(cartItemRepository).delete(cartItem);
        verify(cartRepository).save(cart);
    }

    @Test
    void updateProductQuantity_ShouldThrowException_WhenProductNotInCart() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateProductQuantity(productId, 3)
        );
    }

    @Test
    void updateProductQuantity_ShouldThrowException_WhenQuantityIsNegative() {
        // Act & Assert
        assertThrows(InvalidCartOperationException.class, () ->
                cartService.updateProductQuantity(productId, -1)
        );
    }

    @Test
    void removeProductFromCart_ShouldRemoveProduct_WhenProductExists() {
        // Arrange
        CartItem cartItem = new CartItem(product, product.getPrice(), 2);
        cart.addItem(cartItem);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        cartService.removeProductFromCart(productId);

        // Assert
        assertTrue(cart.getItems().isEmpty());
        verify(cartItemRepository).delete(cartItem);
        verify(cartRepository).save(cart);
    }

    @Test
    void removeProductFromCart_ShouldThrowException_WhenProductNotInCart() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                cartService.removeProductFromCart(productId)
        );
    }

    @Test
    void removeCartItem_ShouldRemoveItem_WhenItemExists() {
        // Arrange
        UUID cartItemId = UUID.randomUUID();
        CartItem cartItem = new CartItem(product, product.getPrice(), 2);
        cartItem.setId(cartItemId);
        cart.addItem(cartItem);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        cartService.removeCartItem(cartItemId);

        // Assert
        assertTrue(cart.getItems().isEmpty());
        verify(cartItemRepository).delete(cartItem);
        verify(cartRepository).save(cart);
    }

    @Test
    void removeCartItem_ShouldThrowException_WhenItemNotFound() {
        // Arrange
        UUID cartItemId = UUID.randomUUID();
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                cartService.removeCartItem(cartItemId)
        );
    }

    @Test
    void clearCart_ShouldRemoveAllItems() {
        // Arrange
        CartItem item1 = new CartItem(product, product.getPrice(), 2);
        CartItem item2 = new CartItem(product, product.getPrice(), 3);
        cart.addItem(item1);
        cart.addItem(item2);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        cartService.clearCart();

        // Assert
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void clearCart_ShouldWork_WhenCartIsAlreadyEmpty() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        cartService.clearCart();

        // Assert
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void replaceCartItems_ShouldReplaceAllItems() {
        // Arrange
        UUID product1Id = UUID.randomUUID();
        UUID product2Id = UUID.randomUUID();

        Product product1 = new Product();
        product1.setId(product1Id);
        product1.setPrice(BigDecimal.valueOf(10.00));

        Product product2 = new Product();
        product2.setId(product2Id);
        product2.setPrice(BigDecimal.valueOf(20.00));

        List<ReplaceCartItemRequest> requests = Arrays.asList(
                new ReplaceCartItemRequest(product1Id, 2),
                new ReplaceCartItemRequest(product2Id, 3)
        );

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(product1Id)).thenReturn(Optional.of(product1));
        when(productRepository.findById(product2Id)).thenReturn(Optional.of(product2));
        when(cartRepository.save(cart)).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        cartService.replaceCartItems(requests);

        // Assert
        assertEquals(2, cart.getItems().size());
        verify(cartRepository).save(cart);
    }

    @Test
    void replaceCartItems_ShouldThrowException_WhenProductNotFound() {
        // Arrange
        UUID invalidProductId = UUID.randomUUID();
        List<ReplaceCartItemRequest> requests = List.of(
                new ReplaceCartItemRequest(invalidProductId, 2)
        );

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(invalidProductId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                cartService.replaceCartItems(requests)
        );
    }

    @Test
    void replaceCartItems_ShouldHandleEmptyList() {
        // Arrange
        CartItem existingItem = new CartItem(product, product.getPrice(), 2);
        cart.addItem(existingItem);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        cartService.replaceCartItems(new ArrayList<>());

        // Assert
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void replaceCartItems_ShouldClearExistingItems_BeforeAddingNew() {
        // Arrange
        CartItem existingItem = new CartItem(product, product.getPrice(), 5);
        cart.addItem(existingItem);

        UUID newProductId = UUID.randomUUID();
        Product newProduct = new Product();
        newProduct.setId(newProductId);
        newProduct.setPrice(BigDecimal.valueOf(50.00));

        List<ReplaceCartItemRequest> requests = List.of(
                new ReplaceCartItemRequest(newProductId, 1)
        );

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(newProductId)).thenReturn(Optional.of(newProduct));
        when(cartRepository.save(cart)).thenReturn(cart);
        when(cartMapper.apply(cart)).thenReturn(cartDTO);

        // Act
        cartService.replaceCartItems(requests);

        // Assert
        assertEquals(1, cart.getItems().size());
        assertFalse(cart.getItems().contains(existingItem));
        verify(cartRepository).save(cart);
    }
}
