package com.taitrinh.online_auction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.OrderChatMessage;

@Repository
public interface OrderChatMessageRepository extends JpaRepository<OrderChatMessage, Long> {

    /**
     * Find all chat messages for a product ordered by creation time ascending
     */
    List<OrderChatMessage> findByProduct_IdOrderByCreatedAtAsc(Long productId);
}
