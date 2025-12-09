package com.taitrinh.online_auction.mapper;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.taitrinh.online_auction.dto.product.BidHistoryResponse;
import com.taitrinh.online_auction.dto.product.CreateProductResponse;
import com.taitrinh.online_auction.dto.product.ProductDetailResponse;
import com.taitrinh.online_auction.dto.product.ProductListResponse;
import com.taitrinh.online_auction.entity.BidHistory;
import com.taitrinh.online_auction.entity.DescriptionLog;
import com.taitrinh.online_auction.entity.Product;
import com.taitrinh.online_auction.entity.ProductImage;
import com.taitrinh.online_auction.entity.User;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Map Product to ProductListResponse (for lists)
    @Mapping(target = "thumbnailUrl", source = "product", qualifiedByName = "getThumbnailUrl")
    @Mapping(target = "categoryId", source = "product.category.id")
    @Mapping(target = "categoryName", source = "product.category.name")
    @Mapping(target = "categorySlug", source = "product.category.slug")
    @Mapping(target = "sellerId", source = "product.seller.id")
    @Mapping(target = "sellerName", source = "product.seller.fullName")
    @Mapping(target = "sellerRating", source = "product.seller", qualifiedByName = "getRatingPercentage")
    @Mapping(target = "highestBidderId", source = "product.highestBidder.id")
    @Mapping(target = "highestBidderName", source = "product.highestBidder", qualifiedByName = "maskUserName")
    @Mapping(target = "highestBidderRating", source = "product.highestBidder", qualifiedByName = "getRatingPercentage")
    @Mapping(target = "isNew", expression = "java(product.isNew(newProductHighlightMin))")
    @Mapping(target = "hasBuyNow", expression = "java(product.hasBuyNow())")
    ProductListResponse toListResponse(Product product, Integer newProductHighlightMin);

    // Map Product to ProductDetailResponse (for detail view)
    @Mapping(target = "images", source = "product.images", qualifiedByName = "mapImages")
    @Mapping(target = "categoryId", source = "product.category.id")
    @Mapping(target = "categoryName", source = "product.category.name")
    @Mapping(target = "categorySlug", source = "product.category.slug")
    @Mapping(target = "parentCategoryName", source = "product.category.parent.name")
    @Mapping(target = "parentCategorySlug", source = "product.category.parent.slug")
    @Mapping(target = "seller", source = "product.seller", qualifiedByName = "mapUserBasicInfo")
    @Mapping(target = "highestBidderName", source = "product.highestBidder", qualifiedByName = "maskUserName")
    @Mapping(target = "highestBidderRating", source = "product.highestBidder", qualifiedByName = "getRatingPercentage")
    @Mapping(target = "winnerName", source = "product.winner", qualifiedByName = "maskUserName")
    @Mapping(target = "winnerRating", source = "product.winner", qualifiedByName = "getRatingPercentage")
    @Mapping(target = "isAutoExtend", source = "product.autoExtend")
    @Mapping(target = "isEnded", source = "product.isEnded")
    @Mapping(target = "isNew", expression = "java(product.isNew(newProductHighlightMin))")
    @Mapping(target = "hasBuyNow", expression = "java(product.hasBuyNow())")
    @Mapping(target = "descriptionLogs", source = "product.descriptionLogs", qualifiedByName = "mapDescriptionLogs")
    ProductDetailResponse toDetailResponse(Product product, Integer newProductHighlightMin);

    // Helper method to get thumbnail URL (first image or first primary image)
    @Named("getThumbnailUrl")
    default String getThumbnailUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .findFirst()
                .or(() -> product.getImages().stream().findFirst())
                .map(ProductImage::getUrl)
                .orElse(null);
    }

    // Helper method to mask user name (show only last 3-4 characters)
    @Named("maskUserName")
    default String maskUserName(User user) {
        if (user == null || user.getFullName() == null) {
            return null;
        }
        String fullName = user.getFullName();
        if (fullName.length() <= 4) {
            return "****" + fullName;
        }
        String visiblePart = fullName.substring(fullName.length() - 4);
        return "****" + visiblePart;
    }

    // Helper method to get rating percentage
    @Named("getRatingPercentage")
    default Double getRatingPercentage(User user) {
        if (user == null) {
            return null;
        }
        return user.getRatingPercentage();
    }

    // Map User to UserBasicInfo
    @Named("mapUserBasicInfo")
    default ProductDetailResponse.UserBasicInfo mapUserBasicInfo(User user) {
        if (user == null) {
            return null;
        }
        return ProductDetailResponse.UserBasicInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .positiveRating(user.getPositiveRating())
                .negativeRating(user.getNegativeRating())
                .ratingPercentage(user.getRatingPercentage())
                .build();
    }

    // Map images
    @Named("mapImages")
    default List<ProductDetailResponse.ProductImageResponse> mapImages(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
                .sorted((a, b) -> {
                    // Primary image first
                    if (a.getIsPrimary() && !b.getIsPrimary())
                        return -1;
                    if (!a.getIsPrimary() && b.getIsPrimary())
                        return 1;
                    // Then by sort order
                    return a.getSortOrder().compareTo(b.getSortOrder());
                })
                .map(img -> ProductDetailResponse.ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getUrl())
                        .displayOrder(img.getSortOrder())
                        .build())
                .toList();
    }

    // Map description logs
    @Named("mapDescriptionLogs")
    default List<ProductDetailResponse.DescriptionLogResponse> mapDescriptionLogs(List<DescriptionLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        return logs.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Newest first
                .map(log -> ProductDetailResponse.DescriptionLogResponse.builder()
                        .id(log.getId())
                        .updatedContent(log.getContent())
                        .updatedAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    // Map BidHistory to BidHistoryResponse (with masked bidder name)
    @Mapping(target = "bidderName", source = "bidHistory.bidder", qualifiedByName = "maskUserName")
    @Mapping(target = "bidAmount", source = "bidHistory.bidAmount", qualifiedByName = "formatCurrency")
    @Mapping(target = "bidTime", source = "createdAt")
    BidHistoryResponse toBidHistoryResponse(BidHistory bidHistory);

    // Helper method to format currency
    @Named("formatCurrency")
    default String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
        return formatter.format(amount);
    }

    // Map list of BidHistory
    default List<BidHistoryResponse> toBidHistoryResponseList(List<BidHistory> bidHistories) {
        if (bidHistories == null) {
            return List.of();
        }
        return bidHistories.stream()
                .map(this::toBidHistoryResponse)
                .toList();
    }

    // Map Product to CreateProductResponse (after creation)
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "sellerName", source = "seller.fullName")
    @Mapping(target = "isAutoExtend", source = "autoExtend")
    @Mapping(target = "imageCount", source = "images", qualifiedByName = "getImageCount")
    CreateProductResponse toCreateProductResponse(Product product);

    // Helper method to get image count
    @Named("getImageCount")
    default Integer getImageCount(List<ProductImage> images) {
        return images != null ? images.size() : 0;
    }
}
