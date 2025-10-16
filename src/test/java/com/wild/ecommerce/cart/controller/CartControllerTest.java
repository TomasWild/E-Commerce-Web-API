package com.wild.ecommerce.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.ecommerce.auth.service.JwtService;
import com.wild.ecommerce.cart.dto.AddToCartRequest;
import com.wild.ecommerce.cart.dto.CartDTO;
import com.wild.ecommerce.cart.dto.CartItemDTO;
import com.wild.ecommerce.cart.dto.ReplaceCartItemRequest;
import com.wild.ecommerce.cart.dto.UpdateQuantityRequest;
import com.wild.ecommerce.cart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private CartDTO cartDTO;
    private UUID productId;
    private UUID cartId;
    private UUID cartItemId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        cartId = UUID.randomUUID();
        cartItemId = UUID.randomUUID();
        userId = UUID.randomUUID();

        CartItemDTO cartItem = new CartItemDTO(
                cartItemId,
                productId,
                "Test Product",
                new BigDecimal("29.99"),
                2,
                new BigDecimal("59.98")
        );

        cartDTO = new CartDTO(
                cartId,
                Set.of(cartItem),
                userId,
                new BigDecimal("59.98"),
                2
        );
    }

    @Test
    void shouldAddProductToCart_WhenRequestIsValid() throws Exception {
        // Arrange
        AddToCartRequest request = new AddToCartRequest(productId, 2);

        when(cartService.addProductToCart(productId, 2)).thenReturn(cartDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(59.98))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void shouldReturnBadRequest_WhenAddProductRequestIsInvalid() throws Exception {
        // Arrange
        String invalidRequest = "{\"productId\": null, \"quantity\": -1}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnCartForCurrentUser_WhenCartExists() throws Exception {
        // Arrange
        when(cartService.getCurrentUserCart()).thenReturn(cartDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.totalPrice").value(59.98))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void shouldReturnEmptyCart_WhenNoItemsExist() throws Exception {
        // Arrange
        CartDTO emptyCart = new CartDTO(
                cartId,
                new HashSet<>(),
                userId,
                BigDecimal.ZERO,
                0
        );

        when(cartService.getCurrentUserCart()).thenReturn(emptyCart);

        // Act & Assert
        mockMvc.perform(get("/api/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalPrice").value(0))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void shouldUpdateProductQuantity_WhenValidQuantityProvided() throws Exception {
        // Arrange
        UpdateQuantityRequest request = new UpdateQuantityRequest(5);

        CartItemDTO updatedItem = new CartItemDTO(
                cartItemId,
                productId,
                "Test Product",
                new BigDecimal("29.99"),
                5,
                new BigDecimal("149.95")
        );

        CartDTO updatedCart = new CartDTO(
                cartId,
                Set.of(updatedItem),
                userId,
                new BigDecimal("149.95"),
                5
        );

        when(cartService.updateProductQuantity(productId, 5)).thenReturn(updatedCart);

        // Act & Assert
        mockMvc.perform(put("/api/v1/carts/items/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalPrice").value(149.95))
                .andExpect(jsonPath("$.totalItems").value(5));
    }

    @Test
    void shouldReturnBadRequest_WhenQuantityIsInvalid() throws Exception {
        // Arrange
        String invalidRequestJson = "{\"quantity\": 0}";

        // Act & Assert
        mockMvc.perform(put("/api/v1/carts/items/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReplaceAllCartItems_WhenValidListProvided() throws Exception {
        // Arrange
        UUID product1Id = UUID.randomUUID();
        UUID product2Id = UUID.randomUUID();

        List<ReplaceCartItemRequest> items = List.of(
                new ReplaceCartItemRequest(product1Id, 2),
                new ReplaceCartItemRequest(product2Id, 3)
        );

        CartItemDTO item1 = new CartItemDTO(
                UUID.randomUUID(),
                product1Id,
                "Product 1",
                new BigDecimal("19.99"),
                2,
                new BigDecimal("39.98")
        );

        CartItemDTO item2 = new CartItemDTO(
                UUID.randomUUID(),
                product2Id,
                "Product 2",
                new BigDecimal("15.00"),
                3,
                new BigDecimal("45.00")
        );

        CartDTO replacedCart = new CartDTO(
                cartId,
                Set.of(item1, item2),
                userId,
                new BigDecimal("84.98"),
                5
        );

        when(cartService.replaceCartItems(any())).thenReturn(replacedCart);

        // Act & Assert
        mockMvc.perform(put("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(items)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalPrice").value(84.98))
                .andExpect(jsonPath("$.totalItems").value(5));
    }

    @Test
    void shouldReturnEmptyCart_WhenReplacingWithEmptyList() throws Exception {
        // Arrange
        List<ReplaceCartItemRequest> emptyItems = new ArrayList<>();

        CartDTO emptyCart = new CartDTO(
                cartId,
                new HashSet<>(),
                userId,
                BigDecimal.ZERO,
                0
        );

        when(cartService.replaceCartItems(any())).thenReturn(emptyCart);

        // Act & Assert
        mockMvc.perform(put("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyItems)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalPrice").value(0));
    }

    @Test
    void shouldRemoveProductFromCart_WhenProductExists() throws Exception {
        // Arrange
        CartDTO emptyCart = new CartDTO(
                cartId,
                new HashSet<>(),
                userId,
                BigDecimal.ZERO,
                0
        );

        when(cartService.removeProductFromCart(productId)).thenReturn(emptyCart);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/carts/items/product/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.totalPrice").value(0))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void shouldRemoveCartItem_WhenItemExists() throws Exception {
        // Arrange
        CartDTO emptyCart = new CartDTO(
                cartId,
                new HashSet<>(),
                userId,
                BigDecimal.ZERO,
                0
        );

        when(cartService.removeCartItem(cartItemId)).thenReturn(emptyCart);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/carts/items/{cartItemId}", cartItemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.totalPrice").value(0))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void shouldClearCart_WhenRequestedByUser() throws Exception {
        // Arrange
        CartDTO emptyCart = new CartDTO(
                cartId,
                new HashSet<>(),
                userId,
                BigDecimal.ZERO,
                0
        );

        when(cartService.clearCart()).thenReturn(emptyCart);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.totalPrice").value(0))
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
