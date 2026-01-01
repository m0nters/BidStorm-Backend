package com.taitrinh.online_auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.OrderCompletion;

@Repository
public interface OrderCompletionRepository extends JpaRepository<OrderCompletion, Long> {

    Optional<OrderCompletion> findByProduct_Id(Long productId);

    Optional<OrderCompletion> findByStripePaymentIntentId(String stripePaymentIntentId);
}
