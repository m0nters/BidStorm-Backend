package com.taitrinh.online_auction.entity;

import java.time.ZonedDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_completions", indexes = {
        @Index(name = "idx_order_completions_product", columnList = "product_id"),
        @Index(name = "idx_order_completions_winner", columnList = "winner_id"),
        @Index(name = "idx_order_completions_status", columnList = "status"),
        @Index(name = "idx_order_completions_stripe_pi", columnList = "stripe_payment_intent_id"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id", nullable = false)
    private User winner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;

    @Column(name = "stripe_transfer_id")
    private String stripeTransferId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "paid_at")
    private ZonedDateTime paidAt;

    @Column(name = "shipped_at")
    private ZonedDateTime shippedAt;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public enum OrderStatus {
        PENDING_PAYMENT, // Waiting for buyer to pay
        PAID, // Money held in escrow
        SHIPPED, // Seller confirmed shipment
        COMPLETED, // Buyer confirmed receipt, money transferred
        CANCELLED // Seller cancelled before payment
    }
}
