package com.wild.ecommerce.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.wild.ecommerce.payment.dto.StripePaymentDTO;

public interface StripeService {

    PaymentIntent paymentIntent(StripePaymentDTO stripePaymentDTO) throws StripeException;
}
