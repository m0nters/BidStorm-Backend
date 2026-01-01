package com.taitrinh.online_auction.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.taitrinh.online_auction.service.OrderCompletionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final OrderCompletionService orderCompletionService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            // Verify webhook signature
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("✅ Stripe webhook received: {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // Handle the event
        log.debug("Processing webhook event type: {}", event.getType());
        switch (event.getType()) {
            case "payment_intent.succeeded":
                log.debug("Attempting to deserialize PaymentIntent...");
                try {
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .deserializeUnsafe();

                    if (paymentIntent != null && paymentIntent.getId() != null) {
                        log.info("✅ PaymentIntent succeeded: {}", paymentIntent.getId());
                        orderCompletionService.handlePaymentSuccess(paymentIntent.getId());
                    } else {
                        log.warn("❌ PaymentIntent object is null or missing ID");
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to deserialize PaymentIntent: {}", e.getMessage());
                }
                break;
            case "charge.succeeded":
                log.debug("Attempting to deserialize Charge...");
                try {
                    com.stripe.model.Charge charge = (com.stripe.model.Charge) event.getDataObjectDeserializer()
                            .deserializeUnsafe();

                    if (charge != null && charge.getPaymentIntent() != null) {
                        log.info("✅ Charge succeeded for PaymentIntent: {}", charge.getPaymentIntent());
                        orderCompletionService.handlePaymentSuccess(charge.getPaymentIntent());
                    } else {
                        log.warn("❌ Charge is null or missing PaymentIntent");
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to deserialize Charge: {}", e.getMessage());
                }
                break;
            case "payment_intent.payment_failed":
                PaymentIntent failedIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);

                if (failedIntent != null) {
                    log.warn("PaymentIntent failed: {}", failedIntent.getId());
                }
                break;

            default:
                log.debug("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook received");
    }
}
