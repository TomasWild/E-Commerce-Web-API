package com.wild.ecommerce.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.wild.ecommerce.address.dto.AddressDTO;
import com.wild.ecommerce.payment.dto.StripePaymentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StripeServiceTest {

    @InjectMocks
    private StripeServiceImpl stripeService;

    private StripePaymentDTO paymentDTO;
    private Customer mockCustomer;
    private PaymentIntent mockPaymentIntent;

    @BeforeEach
    void setUp() {
        AddressDTO addressDTO = new AddressDTO(
                UUID.randomUUID(),
                "US",
                "California",
                "San Francisco",
                "123 Main St",
                "94105"
        );

        paymentDTO = new StripePaymentDTO(
                10000L,
                "usd",
                "John Doe",
                "john.doe@example.com",
                "Test payment",
                addressDTO,
                Map.of("orderId", "12345", "productId", "67890")
        );

        mockCustomer = mock(Customer.class);
        lenient().when(mockCustomer.getId()).thenReturn("cus_test123");

        mockPaymentIntent = mock(PaymentIntent.class);
        lenient().when(mockPaymentIntent.getId()).thenReturn("pi_test123");
    }

    @Test
    void paymentIntent_WhenCustomerExists_ShouldUseExistingCustomer() throws StripeException {
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class);
             MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {

            // Arrange
            CustomerSearchResult searchResult = mock(CustomerSearchResult.class);
            when(searchResult.getData()).thenReturn(List.of(mockCustomer));

            customerMock.when(() -> Customer.search(any(CustomerSearchParams.class)))
                    .thenReturn(searchResult);

            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(mockPaymentIntent);

            // Act
            PaymentIntent result = stripeService.paymentIntent(paymentDTO);

            // Assert
            assertNotNull(result);
            assertEquals("pi_test123", result.getId());

            customerMock.verify(() -> Customer.search(any(CustomerSearchParams.class)), times(1));
            customerMock.verify(() -> Customer.create(any(CustomerCreateParams.class)), never());
            paymentIntentMock.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)), times(1));
        }
    }

    @Test
    void paymentIntent_WhenCustomerDoesNotExist_ShouldCreateNewCustomer() throws StripeException {
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class);
             MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {

            // Arrange
            CustomerSearchResult searchResult = mock(CustomerSearchResult.class);
            when(searchResult.getData()).thenReturn(Collections.emptyList());

            customerMock.when(() -> Customer.search(any(CustomerSearchParams.class)))
                    .thenReturn(searchResult);
            customerMock.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(mockCustomer);

            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(mockPaymentIntent);

            // Act
            PaymentIntent result = stripeService.paymentIntent(paymentDTO);

            // Assert
            assertNotNull(result);
            assertEquals("pi_test123", result.getId());

            customerMock.verify(() -> Customer.search(any(CustomerSearchParams.class)), times(1));
            customerMock.verify(() -> Customer.create(any(CustomerCreateParams.class)), times(1));
            paymentIntentMock.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)), times(1));
        }
    }

    @Test
    void paymentIntent_WhenStripeThrowsException_ShouldPropagateException() {
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {

            // Arrange
            customerMock.when(() -> Customer.search(any(CustomerSearchParams.class)))
                    .thenThrow(
                            new StripeException("Stripe API error", "request_id", "code", 500) {
                            }
                    );

            // Act & Assert
            assertThrows(StripeException.class, () -> stripeService.paymentIntent(paymentDTO));
        }
    }

    @Test
    void paymentIntent_ShouldSetCorrectPaymentIntentParameters() throws StripeException {
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class);
             MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {

            // Arrange
            CustomerSearchResult searchResult = mock(CustomerSearchResult.class);
            when(searchResult.getData()).thenReturn(List.of(mockCustomer));

            customerMock.when(() -> Customer.search(any(CustomerSearchParams.class)))
                    .thenReturn(searchResult);

            // Act & Assert
            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenAnswer(invocation -> {
                        PaymentIntentCreateParams params = invocation.getArgument(0);
                        assertEquals(10000L, params.getAmount());
                        assertEquals("usd", params.getCurrency());
                        assertEquals("cus_test123", params.getCustomer());
                        assertEquals("Test payment", params.getDescription());
                        assertNotNull(params.getAutomaticPaymentMethods());
                        assertTrue(params.getAutomaticPaymentMethods().getEnabled());
                        assertEquals(2, params.getMetadata().size());
                        return mockPaymentIntent;
                    });

            stripeService.paymentIntent(paymentDTO);

            paymentIntentMock.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)), times(1));
        }
    }

    @Test
    void paymentIntent_ShouldSearchCustomerByEmail() throws StripeException {
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class);
             MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {

            // Arrange
            CustomerSearchResult searchResult = mock(CustomerSearchResult.class);
            when(searchResult.getData()).thenReturn(List.of(mockCustomer));

            // Act & Assert
            customerMock.when(() -> Customer.search(any(CustomerSearchParams.class)))
                    .thenAnswer(invocation -> {
                        CustomerSearchParams params = invocation.getArgument(0);
                        assertTrue(params.getQuery().contains("john.doe@example.com"));
                        return searchResult;
                    });

            paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(mockPaymentIntent);

            stripeService.paymentIntent(paymentDTO);

            customerMock.verify(() -> Customer.search(any(CustomerSearchParams.class)), times(1));
        }
    }
}
