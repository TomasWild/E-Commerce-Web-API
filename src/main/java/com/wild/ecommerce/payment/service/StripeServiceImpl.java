package com.wild.ecommerce.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.wild.ecommerce.payment.dto.StripePaymentDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StripeServiceImpl implements StripeService {

    @Override
    public PaymentIntent paymentIntent(StripePaymentDTO stripePaymentDTO) throws StripeException {
        final Customer customer;

        CustomerSearchParams searchParams = CustomerSearchParams.builder()
                .setQuery("email:'" + stripePaymentDTO.email() + "'")
                .build();

        CustomerSearchResult searchResult = Customer.search(searchParams);

        if (searchResult.getData().isEmpty()) {
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setName(stripePaymentDTO.name())
                    .setEmail(stripePaymentDTO.email())
                    .setAddress(
                            CustomerCreateParams.Address.builder()
                                    .setCountry(stripePaymentDTO.addressDTO().country())
                                    .setState(stripePaymentDTO.addressDTO().state())
                                    .setCity(stripePaymentDTO.addressDTO().city())
                                    .setLine1(stripePaymentDTO.addressDTO().street())
                                    .setPostalCode(stripePaymentDTO.addressDTO().postalCode())
                                    .build()
                    )
                    .build();

            customer = Customer.create(customerParams);
        } else {
            customer = searchResult.getData().getFirst();
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(stripePaymentDTO.amountInCents())
                .setCurrency(stripePaymentDTO.currency())
                .setCustomer(customer.getId())
                .setDescription(stripePaymentDTO.description())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(
                                        PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER
                                )
                                .build()
                )
                .putAllMetadata(stripePaymentDTO.metadata())
                .build();

        return PaymentIntent.create(params);
    }
}
