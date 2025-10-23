package com.wild.ecommerce.payment.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.wild.ecommerce.auth.service.JwtService;
import com.wild.ecommerce.order.model.Order;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.order.repository.OrderRepository;
import com.wild.ecommerce.shipment.service.ShipmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StripeWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "stripe.webhook.secret=test_webhook_secret"
})
public class StripeWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private ShipmentService shippingService;

    private MockedStatic<Webhook> webhookMock;
    private static final String WEBHOOK_SECRET = "test_webhook_secret";
    private static final String VALID_SIGNATURE = "valid_signature";
    private static final String INVALID_SIGNATURE = "invalid_signature";
    private static final UUID ORDER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        webhookMock = mockStatic(Webhook.class);
    }

    @AfterEach
    void tearDown() {
        webhookMock.close();
    }

    @Test
    void givenInvalidSignature_WhenWebhookReceived_ThenReturnBadRequest() throws Exception {
        // Given
        String payload = "{}";

        webhookMock.when(() -> Webhook.constructEvent(payload, INVALID_SIGNATURE, WEBHOOK_SECRET))
                .thenThrow(new SignatureVerificationException("Invalid signature", INVALID_SIGNATURE));

        // When & Then
        mockMvc.perform(post("/api/v1/stripe/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", INVALID_SIGNATURE)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid signature"));

        verifyNoInteractions(orderRepository);
    }

    @Test
    void givenPaymentIntentSucceeded_WhenWebhookReceived_ThenConfirmOrder() throws Exception {
        // Given
        String payload = createWebhookPayload("payment_intent.succeeded");
        Event event = createMockEvent("payment_intent.succeeded", ORDER_ID);

        webhookMock.when(() -> Webhook.constructEvent(eq(payload), eq(VALID_SIGNATURE), anyString()))
                .thenReturn(event);

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(Status.PENDING);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When & Then
        mockMvc.perform(post("/api/v1/stripe/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", VALID_SIGNATURE)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));

        verify(orderRepository).findById(ORDER_ID);
        verify(orderRepository).save(argThat(o -> o.getStatus() == Status.CONFIRMED));
    }

    @Test
    void givenPaymentIntentFailed_WhenWebhookReceived_ThenMarkOrderAsFailed() throws Exception {
        // Given
        String payload = createWebhookPayload("payment_intent.payment_failed");
        Event event = createMockEvent("payment_intent.payment_failed", ORDER_ID);

        webhookMock.when(() -> Webhook.constructEvent(eq(payload), eq(VALID_SIGNATURE), anyString()))
                .thenReturn(event);

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(Status.PENDING);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When & Then
        mockMvc.perform(post("/api/v1/stripe/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", VALID_SIGNATURE)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));

        verify(orderRepository).save(argThat(o -> o.getStatus() == Status.FAILED));
    }

    @Test
    void givenPaymentIntentCanceled_WhenWebhookReceived_ThenMarkOrderAsCancelled() throws Exception {
        // Given
        String payload = createWebhookPayload("payment_intent.canceled");
        Event event = createMockEvent("payment_intent.canceled", ORDER_ID);

        webhookMock.when(() -> Webhook.constructEvent(eq(payload), eq(VALID_SIGNATURE), anyString()))
                .thenReturn(event);

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setStatus(Status.PENDING);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When & Then
        mockMvc.perform(post("/api/v1/stripe/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", VALID_SIGNATURE)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));

        verify(orderRepository).save(argThat(o -> o.getStatus() == Status.CANCELLED));
    }

    @Test
    void givenMissingStripeSignatureHeader_WhenWebhookReceived_ThenReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/stripe/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenUnknownEventType_WhenWebhookReceived_ThenDoNotUpdateOrder() throws Exception {
        // Given
        String payload = createWebhookPayload("unknown.event");
        Event event = createMockEvent("unknown.event", ORDER_ID);

        webhookMock.when(() -> Webhook.constructEvent(eq(payload), eq(VALID_SIGNATURE), anyString()))
                .thenReturn(event);

        // When & Then
        mockMvc.perform(post("/api/v1/stripe/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", VALID_SIGNATURE)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));

        verifyNoInteractions(orderRepository);
    }

    @Test
    void givenMissingOrderIdInMetadata_WhenWebhookReceived_ThenIgnoreEvent() throws Exception {
        // Given
        String payload = createWebhookPayload("payment_intent.succeeded");
        Event event = createMockEvent("payment_intent.succeeded", null);

        webhookMock.when(() -> Webhook.constructEvent(eq(payload), eq(VALID_SIGNATURE), anyString()))
                .thenReturn(event);

        // When & Then
        mockMvc.perform(post("/api/v1/stripe/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", VALID_SIGNATURE)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));

        verifyNoInteractions(orderRepository);
    }

    @Test
    void givenWrongHttpMethod_WhenRequestSent_ThenReturnMethodNotAllowed() throws Exception {
        // When & Then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/stripe/webhooks"))
                .andExpect(status().isMethodNotAllowed());
    }

    private String createWebhookPayload(String eventType) {
        return String.format("{\"type\":\"%s\",\"data\":{\"object\":{}}}", eventType);
    }

    private Event createMockEvent(String eventType, UUID orderId) {
        Event event = mock(Event.class);
        Event.Data eventData = mock(Event.Data.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        PaymentIntent paymentIntent = mock(PaymentIntent.class);

        Map<String, String> metadata = new HashMap<>();
        if (orderId != null) {
            metadata.put("orderId", orderId.toString());
        }

        when(event.getType()).thenReturn(eventType);
        when(event.getData()).thenReturn(eventData);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(paymentIntent));
        when(paymentIntent.getMetadata()).thenReturn(metadata);

        return event;
    }
}
