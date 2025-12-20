package com.taitrinh.online_auction.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
import com.taitrinh.online_auction.entity.User;
import com.taitrinh.online_auction.mapper.ProductMapper;
import com.taitrinh.online_auction.repository.BidHistoryRepository;
import com.taitrinh.online_auction.repository.CategoryRepository;
import com.taitrinh.online_auction.repository.DescriptionLogRepository;
import com.taitrinh.online_auction.repository.ProductRepository;
import com.taitrinh.online_auction.repository.UserRepository;
import com.taitrinh.online_auction.util.SlugUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final ConfigService configService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DescriptionLogRepository descriptionLogRepository;
    private final ProductMapper productMapper;
    private final ApplicationContext applicationContext;

    /**
     * Get top 5 products ending soon
     */
    @Transactional(readOnly = true)
    public List<ProductListResponse> getTop5EndingSoon() {
        log.debug("Getting top 5 products ending soon");
        Pageable pageable = PageRequest.of(0, 5);
        List<Product> products = productRepository.findTop5EndingSoon(pageable);
        Integer highlightMin = configService.getNewProductHighlightMin();
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
        Integer highlightMin = configService.getNewProductHighlightMin();
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
        Integer highlightMin = configService.getNewProductHighlightMin();
        return products.stream()
                .map(product -> productMapper.toListResponse(product, highlightMin))
                .toList();
    }

    /**
     * Get products by category with pagination
     * Automatically detects if category is parent (has children) or leaf category
     * - If parent category: returns products from all subcategories
     * - If leaf category: returns products from that specific category
     */
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProductsByCategory(Integer categoryId, Integer page, Integer size,
            String sortBy, String sortDirection) {
        log.debug("Getting products by category: {}, page: {}, size: {}", categoryId, page, size);

        // Validate category exists and load with children
        Category category = categoryRepository.findByIdWithChildren(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy category với id: " + categoryId));

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Product> productPage;

        // Auto-detect: if category has children, get products from all subcategories
        if (category.hasChildren()) {
            log.debug("Category {} is a parent category, fetching products from all subcategories", categoryId);

            // Get all subcategory IDs
            List<Integer> subcategoryIds = category.getChildren().stream()
                    .map(Category::getId)
                    .toList();

            // If no subcategories, return empty page
            if (subcategoryIds.isEmpty()) {
                productPage = Page.empty();
            } else {
                // Query products from all subcategories
                productPage = productRepository.findByCategoryIdIn(subcategoryIds, pageable);
            }
        } else {
            // Leaf category: get products directly from this category
            log.debug("Category {} is a leaf category, fetching products directly", categoryId);
            productPage = productRepository.findByCategoryId(categoryId, pageable);
        }

        Integer highlightMin = configService.getNewProductHighlightMin();
        return productPage.map(product -> productMapper.toListResponse(product, highlightMin));
    }

    /**
     * Search products with filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductListResponse> searchProducts(ProductSearchRequest request) {
        log.debug("Searching products with request: {}", request);

        // Map Java property names to database column names for native queries
        String sortField = mapSortFieldToColumn(request.getSortBy());

        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
                Sort.by(direction, sortField));

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

        Integer highlightMin = configService.getNewProductHighlightMin();
        return productPage.map(product -> productMapper.toListResponse(product, highlightMin));
    }

    /**
     * Map Java property names to database column names for native queries
     */
    private String mapSortFieldToColumn(String sortBy) {
        return switch (sortBy) {
            case "endTime" -> "end_time";
            case "currentPrice" -> "current_price";
            case "createdAt" -> "created_at";
            case "bidCount" -> "bid_count";
            default -> sortBy;
        };
    }

    /**
     * Get product detail by ID
     * For testing backend purpose only, in production, we use the service below
     * (by slug)
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetailById(Long id, Long viewerId) {
        log.debug("Getting product detail for id: {}", id);

        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + id));

        // Increment view count (in a separate transaction to avoid locking)
        // Call through Spring proxy to ensure REQUIRES_NEW propagation works
        applicationContext.getBean(ProductService.class).incrementViewCount(id);

        // Update product view count for response
        product.setViewCount(product.getViewCount() + 1);

        Integer highlightMin = configService.getNewProductHighlightMin();

        // Check if viewer is the seller
        boolean isSeller = viewerId != null && product.getSeller() != null &&
                viewerId.equals(product.getSeller().getId());

        // Use unified mapper with isSeller flag
        return productMapper.toDetailResponseWithViewer(product, highlightMin, viewerId, isSeller);
    }

    /**
     * Get product detail by slug
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetailBySlug(String slug, Long viewerId) {
        log.debug("Getting product detail for slug: {}", slug);

        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với slug: " + slug));

        // Increment view count (in a separate transaction to avoid locking)
        // Call through Spring proxy to ensure REQUIRES_NEW propagation works
        applicationContext.getBean(ProductService.class).incrementViewCount(product.getId());

        // Update product view count for response
        product.setViewCount(product.getViewCount() + 1);

        Integer highlightMin = configService.getNewProductHighlightMin();

        // Check if viewer is the seller
        boolean isSeller = viewerId != null && product.getSeller() != null &&
                viewerId.equals(product.getSeller().getId());

        // Use unified mapper with isSeller flag
        return productMapper.toDetailResponseWithViewer(product, highlightMin, viewerId, isSeller);
    }

    /**
     * Get related products in the same category (excluding current product)
     */
    @Transactional(readOnly = true)
    public List<ProductListResponse> getRelatedProducts(Long productId) {
        log.debug("Getting related products for product: {}", productId);

        // Fetch the product to get its category
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + productId));

        Integer categoryId = product.getCategory().getId();
        log.debug("Found category: {} for product: {}", categoryId, productId);

        Pageable pageable = PageRequest.of(0, 5);
        List<Product> products = productRepository.findRelatedProducts(categoryId, productId, pageable);
        Integer highlightMin = configService.getNewProductHighlightMin();

        return products.stream()
                .map(p -> productMapper.toListResponse(p, highlightMin))
                .toList();
    }

    /**
     * Get bid history for a product
     */
    @Transactional(readOnly = true)
    public List<BidHistoryResponse> getBidHistory(Long productId, Long viewerId) {
        log.debug("Getting bid history for product: {}", productId);

        // Verify product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + productId));

        List<BidHistory> bidHistories = bidHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId);

        // Check if viewer is the seller (to unmask all names)
        boolean isSeller = viewerId != null && product.getSeller() != null &&
                viewerId.equals(product.getSeller().getId());

        return bidHistories.stream()
                .map(bh -> productMapper.toBidHistoryResponse(bh, viewerId, isSeller))
                .toList();
    }

    /**
     * Increment view count for a product
     * Uses REQUIRES_NEW to ensure this runs in a separate transaction,
     * even when called from a read-only transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementViewCount(Long productId) {
        try {
            productRepository.findById(productId).ifPresent(product -> {
                product.setViewCount(product.getViewCount() + 1);
                productRepository.save(product);
            });
            log.debug("View count incremented for product: {}", productId);
        } catch (Exception e) {
            log.error("Error incrementing view count for product: {}", productId, e);
            // Don't throw exception, just log it
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán hoặc người bán không hoạt động"));

        // Validate seller has permission to sell
        if (!seller.isSeller()) {
            throw new RuntimeException("Người dùng không có quyền bán");
        }

        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy category với id: " + request.getCategoryId()));

        // Validate that the category is not a parent category (products can only belong
        // to subcategories)
        if (category.hasChildren()) {
            throw new RuntimeException("Không thể tạo sản phẩm cho category cha. Vui lòng chọn một category con.");
        }

        // Validate buyNowPrice if provided
        if (request.getBuyNowPrice() != null &&
                request.getBuyNowPrice().compareTo(request.getStartingPrice()) <= 0) {
            throw new RuntimeException("Giá mua ngay phải lớn hơn giá khởi điểm");
        }

        // Validate minimum 3 images
        if (request.getImages() == null || request.getImages().size() < 3) {
            throw new RuntimeException("Phải có ít nhất 3 ảnh cho sản phẩm đấu giá");
        }

        // Validate at least one image is marked as primary
        long primaryCount = request.getImages().stream()
                .filter(CreateProductRequest.ProductImageRequest::getIsPrimary)
                .count();
        if (primaryCount == 0) {
            throw new RuntimeException("Phải có ít nhất một ảnh được đánh dấu là ảnh chính");
        }
        if (primaryCount > 1) {
            throw new RuntimeException("Chỉ có thể có một ảnh được đánh dấu là ảnh chính");
        }

        String slug = SlugUtils.toSlug(request.getTitle());
        slug = SlugUtils.makeUnique(slug, productRepository::existsBySlug);

        // Create product entity
        Product product = Product.builder()
                .seller(seller)
                .category(category)
                .title(request.getTitle())
                .slug(slug)
                .description(request.getDescription())
                .startingPrice(request.getStartingPrice())
                .currentPrice(request.getStartingPrice()) // Initially same as starting price
                .buyNowPrice(request.getBuyNowPrice())
                .priceStep(request.getPriceStep())
                .autoExtend(request.getAutoExtend())
                .allowUnratedBidders(request.getAllowUnratedBidders())
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + productId));

        // Validate seller owns the product
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Chỉ người bán sản phẩm này mới có quyền cập nhật mô tả của sản phẩm");
        }

        // Validate product is not ended
        if (product.getIsEnded()) {
            throw new RuntimeException("Không thể cập nhật mô tả sản phẩm đã kết thúc");
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
