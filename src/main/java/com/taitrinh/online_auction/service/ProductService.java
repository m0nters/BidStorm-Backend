package com.taitrinh.online_auction.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taitrinh.online_auction.dto.product.BidHistoryResponse;
import com.taitrinh.online_auction.dto.product.CreateProductRequest;
import com.taitrinh.online_auction.dto.product.CreateProductResponse;
import com.taitrinh.online_auction.dto.product.ProductDetailResponse;
import com.taitrinh.online_auction.dto.product.ProductListResponse;
import com.taitrinh.online_auction.dto.product.ProductSearchRequest;
import com.taitrinh.online_auction.dto.product.UpdateProductDescriptionRequest;
import com.taitrinh.online_auction.entity.BidHistory;
import com.taitrinh.online_auction.entity.Category;
import com.taitrinh.online_auction.entity.DescriptionLog;
import com.taitrinh.online_auction.entity.Product;
import com.taitrinh.online_auction.entity.ProductImage;
import com.taitrinh.online_auction.entity.SystemConfig;
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.mapper.ProductMapper;
import com.taitrinh.online_auction.repository.BidHistoryRepository;
import com.taitrinh.online_auction.repository.CategoryRepository;
import com.taitrinh.online_auction.repository.DescriptionLogRepository;
import com.taitrinh.online_auction.repository.ProductRepository;
import com.taitrinh.online_auction.repository.SystemConfigRepository;
import com.taitrinh.online_auction.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DescriptionLogRepository descriptionLogRepository;
    private final ProductMapper productMapper;

    // Default values for system configs
    private static final Integer DEFAULT_NEW_PRODUCT_HIGHLIGHT_MIN = 60; // 60 minutes

    /**
     * Get top 5 products ending soon
     */
    @Transactional(readOnly = true)
    public List<ProductListResponse> getTop5EndingSoon() {
        log.debug("Getting top 5 products ending soon");
        Pageable pageable = PageRequest.of(0, 5);
        List<Product> products = productRepository.findTop5EndingSoon(pageable);
        Integer highlightMin = getNewProductHighlightMin();
        return products.stream()
                .map(product -> productMapper.toListResponse(product, highlightMin))
                .toList();
    }

    /**
     * Get top 5 products with highest bid count
     */
    @Transactional(readOnly = true)
    public List<ProductListResponse> getTop5ByBidCount() {
        log.debug("Getting top 5 products by bid count");
        Pageable pageable = PageRequest.of(0, 5);
        List<Product> products = productRepository.findTop5ByBidCount(pageable);
        Integer highlightMin = getNewProductHighlightMin();
        return products.stream()
                .map(product -> productMapper.toListResponse(product, highlightMin))
                .toList();
    }

    /**
     * Get top 5 products with highest price
     */
    @Transactional(readOnly = true)
    public List<ProductListResponse> getTop5ByPrice() {
        log.debug("Getting top 5 products by price");
        Pageable pageable = PageRequest.of(0, 5);
        List<Product> products = productRepository.findTop5ByPrice(pageable);
        Integer highlightMin = getNewProductHighlightMin();
        return products.stream()
                .map(product -> productMapper.toListResponse(product, highlightMin))
                .toList();
    }

    /**
     * Get products by category with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProductsByCategory(Integer categoryId, Integer page, Integer size,
            String sortBy, String sortDirection) {
        log.debug("Getting products by category: {}, page: {}, size: {}", categoryId, page, size);

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Product> productPage = productRepository.findByCategoryIdOrParentId(categoryId, pageable);
        Integer highlightMin = getNewProductHighlightMin();

        return productPage.map(product -> productMapper.toListResponse(product, highlightMin));
    }

    /**
     * Search products with filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductListResponse> searchProducts(ProductSearchRequest request) {
        log.debug("Searching products with request: {}", request);

        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
                Sort.by(direction, request.getSortBy()));

        Page<Product> productPage;

        // Search logic
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            if (request.getCategoryId() != null) {
                // Search by keyword and category
                productPage = productRepository.searchByTitleAndCategory(
                        request.getKeyword().trim(),
                        request.getCategoryId(),
                        pageable);
            } else {
                // Search by keyword only
                productPage = productRepository.searchByTitle(request.getKeyword().trim(), pageable);
            }
        } else if (request.getCategoryId() != null) {
            // Filter by category only
            productPage = productRepository.findByCategoryIdOrParentId(request.getCategoryId(), pageable);
        } else {
            // No filters, return all active products
            productPage = productRepository.findAll(pageable);
        }

        Integer highlightMin = getNewProductHighlightMin();
        return productPage.map(product -> productMapper.toListResponse(product, highlightMin));
    }

    /**
     * Get product detail by ID
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long id) {
        log.debug("Getting product detail for id: {}", id);

        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Increment view count (in a separate transaction to avoid locking)
        incrementViewCount(id);

        Integer highlightMin = getNewProductHighlightMin();
        return productMapper.toDetailResponse(product, highlightMin);
    }

    /**
     * Get related products in the same category (excluding current product)
     */
    @Transactional(readOnly = true)
    public List<ProductListResponse> getRelatedProducts(Long productId, Integer categoryId) {
        log.debug("Getting related products for product: {} in category: {}", productId, categoryId);

        Pageable pageable = PageRequest.of(0, 5);
        List<Product> products = productRepository.findRelatedProducts(categoryId, productId, pageable);
        Integer highlightMin = getNewProductHighlightMin();

        return products.stream()
                .map(product -> productMapper.toListResponse(product, highlightMin))
                .toList();
    }

    /**
     * Get bid history for a product
     */
    @Transactional(readOnly = true)
    public List<BidHistoryResponse> getBidHistory(Long productId) {
        log.debug("Getting bid history for product: {}", productId);

        // Verify product exists
        productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        List<BidHistory> bidHistories = bidHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return productMapper.toBidHistoryResponseList(bidHistories);
    }

    /**
     * Increment view count for a product (async operation)
     */
    @Transactional
    public void incrementViewCount(Long productId) {
        try {
            productRepository.findById(productId).ifPresent(product -> {
                product.setViewCount(product.getViewCount() + 1);
                productRepository.save(product);
            });
        } catch (Exception e) {
            log.error("Error incrementing view count for product: {}", productId, e);
            // Don't throw exception, just log it
        }
    }

    /**
     * Get new product highlight duration from system config
     */
    private Integer getNewProductHighlightMin() {
        try {
            return systemConfigRepository.findByKey(SystemConfig.NEW_PRODUCT_HIGHLIGHT_MIN)
                    .map(SystemConfig::getIntValue)
                    .orElse(DEFAULT_NEW_PRODUCT_HIGHLIGHT_MIN);
        } catch (Exception e) {
            log.warn("Error getting new product highlight config, using default: {}",
                    DEFAULT_NEW_PRODUCT_HIGHLIGHT_MIN, e);
            return DEFAULT_NEW_PRODUCT_HIGHLIGHT_MIN;
        }
    }

    /**
     * Create a new auction product (Seller only)
     */
    @Transactional
    public CreateProductResponse createProduct(CreateProductRequest request, Long sellerId) {
        log.debug("Creating product for seller: {}", sellerId);

        // Validate seller exists and is active
        User seller = userRepository.findActiveUserById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found or inactive"));

        // Validate seller has permission to sell
        if (!seller.isSeller()) {
            throw new RuntimeException("User does not have seller permission");
        }

        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        // Validate buyNowPrice if provided
        if (request.getBuyNowPrice() != null &&
                request.getBuyNowPrice().compareTo(request.getStartingPrice()) <= 0) {
            throw new RuntimeException("Buy now price must be greater than starting price");
        }

        // Validate at least one image is marked as primary
        long primaryCount = request.getImages().stream()
                .filter(CreateProductRequest.ProductImageRequest::getIsPrimary)
                .count();
        if (primaryCount == 0) {
            throw new RuntimeException("At least one image must be marked as primary");
        }
        if (primaryCount > 1) {
            throw new RuntimeException("Only one image can be marked as primary");
        }

        // Create product entity
        Product product = Product.builder()
                .seller(seller)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .startingPrice(request.getStartingPrice())
                .currentPrice(request.getStartingPrice()) // Initially same as starting price
                .buyNowPrice(request.getBuyNowPrice())
                .priceStep(request.getPriceStep())
                .autoExtend(request.getAutoExtend())
                .endTime(request.getEndTime())
                .bidCount(0)
                .viewCount(0)
                .isEnded(false)
                .build();

        // Create and add images
        List<ProductImage> images = request.getImages().stream()
                .map(imgReq -> ProductImage.builder()
                        .product(product)
                        .url(imgReq.getImageUrl())
                        .isPrimary(imgReq.getIsPrimary())
                        .sortOrder(imgReq.getSortOrder())
                        .build())
                .toList();

        product.setImages(new ArrayList<>(images));

        // Save product (cascade will save images)
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully with id: {}", savedProduct.getId());

        return productMapper.toCreateProductResponse(savedProduct);
    }

    /**
     * Update product description (append only, cannot replace)
     */
    @Transactional
    public void updateProductDescription(Long productId, Long sellerId,
            UpdateProductDescriptionRequest request) {
        log.debug("Updating description for product: {} by seller: {}", productId, sellerId);

        // Validate product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        // Validate seller owns the product
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Only the product owner can update description");
        }

        // Validate product is not ended
        if (product.getIsEnded()) {
            throw new RuntimeException("Cannot update description of ended product");
        }

        // Append new description to existing one
        String updatedDescription = product.getDescription() + "\n\n" +
                "✏️ " + ZonedDateTime.now().toLocalDate() + "\n\n" +
                request.getAdditionalDescription();
        product.setDescription(updatedDescription);

        // Save description log
        DescriptionLog descriptionLog = DescriptionLog.builder()
                .product(product)
                .content(request.getAdditionalDescription())
                .build();
        descriptionLogRepository.save(descriptionLog);

        productRepository.save(product);

        log.info("Product description updated for product: {}", productId);
    }
}
