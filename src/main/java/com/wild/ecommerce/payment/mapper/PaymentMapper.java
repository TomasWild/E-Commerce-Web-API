package com.wild.ecommerce.payment.mapper;

import com.wild.ecommerce.payment.dto.PaymentDTO;
import com.wild.ecommerce.payment.model.Payment;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class PaymentMapper implements Function<Payment, PaymentDTO> {

    @Override
    public PaymentDTO apply(Payment payment) {
        return new PaymentDTO(
                payment.getId(),
                payment.getStripePaymentId(),
                payment.getStripeName(),
                payment.getStripeStatus(),
                payment.getStripeResponseMessage(),
                payment.getPaymentMethod().name()
        );
    }
}
