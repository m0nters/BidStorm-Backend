package com.taitrinh.online_auction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.taitrinh.online_auction.entity.OrderCompletion;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StripePaymentService {

    @Value("${stripe.currency}")
    private String currency;

    /**
     * Create a PaymentIntent for the order
     * This holds the payment in escrow until manually captured or confirmed
     */
    public PaymentIntent createPaymentIntent(OrderCompletion order) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(order.getAmountCents())
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .putMetadata("order_id", order.getId().toString())
                .putMetadata("product_id", order.getProduct().getId().toString())
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        log.info("Created PaymentIntent {} for order {}", paymentIntent.getId(), order.getId());

        return paymentIntent;
    }

    /**
     * Transfer money to seller
     * Note: This requires Stripe Connect in production
     * For now, just log the transfer
     */
    public void transferToSeller(OrderCompletion order) {
        log.info("Transfer initiated for order {}: {} {} to seller",
                order.getId(), order.getAmountCents(), order.getCurrency());

        // TODO: In production, use Stripe Connect to actually transfer funds
        // For now, we just mark it as transferred
        // Example:
        // Transfer transfer = Transfer.create(
        // TransferCreateParams.builder()
        // .setAmount(order.getAmountCents())
        // .setCurrency(order.getCurrency())
        // .setDestination("seller_stripe_account_id")
        // .build()
        // );
    }
}
