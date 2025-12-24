package com.taitrinh.online_auction.entity;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_end_time", columnList = "end_time"),
        @Index(name = "idx_products_created", columnList = "created_at"),
        @Index(name = "idx_products_price", columnList = "current_price"),
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_active", columnList = "is_ended, end_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true, length = 500)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "starting_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal startingPrice;

    @Column(name = "current_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "buy_now_price", precision = 15, scale = 2)
    private BigDecimal buyNowPrice;

    @Column(name = "price_step", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceStep;

    @Column(name = "auto_extend", nullable = false)
    @Builder.Default
    private Boolean autoExtend = true;

    @Column(name = "allow_unrated_bidders", nullable = false)
    @Builder.Default
    private Boolean allowUnratedBidders = false;

    @Column(name = "start_time", nullable = false)
    @Builder.Default
    private ZonedDateTime startTime = ZonedDateTime.now();

    @Column(name = "end_time", nullable = false)
    private ZonedDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @ManyToOne
    @JoinColumn(name = "highest_bidder_id")
    private User highestBidder;

    @Column(name = "bid_count", nullable = false)
    @Builder.Default
    private Integer bidCount = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "is_ended", nullable = false)
    @Builder.Default
    private Boolean isEnded = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BidHistory> bidHistory = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DescriptionLog> descriptionLogs = new ArrayList<>();

    // Helper methods

    /**
     * Check if product is newly posted (within N minutes from system config)
     * Used to highlight new products differently on listing pages
     */
    public boolean isNew(Integer newProductHighlightMin) {
        return createdAt != null &&
                newProductHighlightMin != null &&
                createdAt.isAfter(ZonedDateTime.now().minusMinutes(newProductHighlightMin));
    }

    /**
     * Check if product is ending soon (within 3 days)
     * Used to display end time in relative format (e.g., "3 days left", "10 minutes
     * left")
     */
    public boolean isEnding() {
        return endTime != null &&
                endTime.isBefore(ZonedDateTime.now().plusDays(3));
    }

    /**
     * Check if the auction has ended.
     * Returns true if:
     * 1. The auction was manually ended (Buy Now, etc.) - isEnded flag is true in
     * DB
     * 2. The auction time has expired - endTime has passed
     * 
     * This hybrid approach allows:
     * - Manual early endings via "Buy Now" (sets isEnded = true in database)
     * - Automatic time-based endings (computed in real-time, no cron jobs needed)
     * 
     * @return true if auction has ended (manually or by time), false otherwise
     */
    public boolean isEnded() {
        // If manually ended (buy now, cancelled, etc.), respect the database flag
        if (isEnded != null && isEnded) {
            return true;
        }
        // Otherwise, check if the auction time has naturally expired
        return endTime != null && endTime.isBefore(ZonedDateTime.now());
    }

    public boolean shouldAutoExtend(Integer globalExtendTriggerMin) {
        return autoExtend &&
                endTime != null &&
                globalExtendTriggerMin != null &&
                endTime.isBefore(ZonedDateTime.now().plusMinutes(globalExtendTriggerMin));
    }
}
