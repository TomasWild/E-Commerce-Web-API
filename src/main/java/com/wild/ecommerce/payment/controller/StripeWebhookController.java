package com.wild.ecommerce.payment.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.wild.ecommerce.common.exception.ResourceNotFoundException;
import com.wild.ecommerce.order.model.Order;
import com.wild.ecommerce.order.model.Status;
import com.wild.ecommerce.order.repository.OrderRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stripe/webhooks")
@RequiredArgsConstructor
@Tag(name = "Stripe Webhooks", description = "Endpoints for handling Stripe webhooks")
public class StripeWebhookController {

    private final OrderRepository orderRepository;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhookEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        final Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
            case "payment_intent.canceled" -> handlePaymentIntentCanceled(event);
        }

        return new ResponseEntity<>("Webhook received", HttpStatus.OK);
    }

    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("PaymentIntent not found in event"));

        String orderId = paymentIntent.getMetadata().get("orderId");

        if (orderId != null) {
            updateOrderStatus(UUID.fromString(orderId), Status.CONFIRMED);
        }
    }

    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("PaymentIntent not found in event"));

        String orderId = paymentIntent.getMetadata().get("orderId");

        if (orderId != null) {
            updateOrderStatus(UUID.fromString(orderId), Status.FAILED);
        }
    }

    private void handlePaymentIntentCanceled(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("PaymentIntent not found in event"));

        String orderId = paymentIntent.getMetadata().get("orderId");

        if (orderId != null) {
            updateOrderStatus(UUID.fromString(orderId), Status.CANCELLED);
        }
    }

    protected void updateOrderStatus(UUID orderId, Status status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        order.setStatus(status);
        orderRepository.save(order);
    }
}
