# Implementation Guide - Online Auction System

> **Purpose**: This document serves as a technical reference for implementing future features. It contains architectural decisions, patterns used, and guidelines for consistent development.

---

## 🏗️ Architecture Overview

### Layered Architecture

```
┌─────────────────────────────────────────────────┐
│               Controller Layer                   │  ← REST endpoints, validation
├─────────────────────────────────────────────────┤
│                Service Layer                     │  ← Business logic
├─────────────────────────────────────────────────┤
│              Repository Layer                    │  ← Data access (JPA)
├─────────────────────────────────────────────────┤
│                Database (PostgreSQL)             │  ← Data persistence
└─────────────────────────────────────────────────┘

         ↕️ Data Flow via DTOs (MapStruct)
```

### Package Structure Convention

```
com.taitrinh.online_auction/
├── controller/        # @RestController - HTTP endpoints
├── service/          # @Service - Business logic
├── repository/       # @Repository - Data access
├── entity/           # @Entity - JPA entities
├── dto/              # Data Transfer Objects
│   ├── {feature}/    # Group DTOs by feature
│   └── shared/       # Common DTOs (ApiResponse, ErrorResponse)
├── mapper/           # @Mapper - MapStruct interfaces
├── exception/        # Custom exceptions & handlers
├── config/           # @Configuration - Spring configs
└── util/             # Utility classes
```

---

## 🎯 Design Patterns & Best Practices

### 1. **API Response Pattern**

**All API responses** use standardized wrapper:

```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private ZonedDateTime timestamp;

    // Factory methods
    public static <T> ApiResponse<T> ok(T data, String message) { }
    public static <T> ApiResponse<T> error(String message) { }
}
```

**Usage**:

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(@PathVariable Long id) {
    ProductDetailResponse product = productService.getProductDetail(id);
    return ResponseEntity.ok(ApiResponse.ok(product, "Product retrieved successfully"));
}
```

**Why**: Consistent response structure across all endpoints, easier frontend integration.

---

### 2. **DTO Pattern**

#### Separate DTOs for Different Views

**Don't**: Use one DTO for all scenarios

```java
// ❌ Bad: Heavy DTO for list view
ProductDTO {
    Long id;
    String title;
    List<ProductImageDTO> images;  // Too much data for list
    List<BidHistoryDTO> bidHistory;  // Not needed in list
    String fullDescription;  // Heavy field
    // ... many more fields
}
```

**Do**: Create specialized DTOs

```java
// ✅ Good: Light DTO for lists
ProductListResponse {
    Long id;
    String title;
    String thumbnailUrl;  // Only one image
    BigDecimal currentPrice;
    Integer bidCount;
}

// ✅ Good: Complete DTO for detail view
ProductDetailResponse {
    Long id;
    String title;
    List<ProductImageResponse> images;  // All images
    String description;  // Full HTML
    List<DescriptionLogResponse> descriptionLogs;
    UserBasicInfo seller;
    UserBasicInfo highestBidder;
    // ... all fields needed for detail page
}
```

**Why**: Reduces payload size, faster responses, better performance.

---

### 3. **MapStruct Mapping Pattern**

#### Basic Mapping

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Simple field mapping
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    ProductListResponse toListResponse(Product product);
}
```

#### Complex Mapping with Custom Methods

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Use custom method for complex logic
    @Mapping(target = "thumbnailUrl", source = "product", qualifiedByName = "getThumbnailUrl")
    ProductListResponse toListResponse(Product product);

    // Custom method
    @Named("getThumbnailUrl")
    default String getThumbnailUrl(Product product) {
        return product.getImages().stream()
                .filter(ProductImage::getIsPrimary)
                .findFirst()
                .map(ProductImage::getUrl)
                .orElse(null);
    }
}
```

#### Mapping with Additional Parameters

```java
// Mapper interface
ProductListResponse toListResponse(Product product, Integer newProductHighlightMin);

// Usage in Service
productMapper.toListResponse(product, getNewProductHighlightMin());
```

**Why**: Clean separation of mapping logic, type-safe, compile-time checking.

---

### 4. **Repository Query Pattern**

#### Custom JPQL Queries

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Simple query
    @Query("SELECT p FROM Product p WHERE p.isEnded = false ORDER BY p.endTime ASC")
    List<Product> findTop5EndingSoon(Pageable pageable);

    // Query with joins for performance
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.seller " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    // Full-text search (PostgreSQL specific)
    @Query("SELECT p FROM Product p WHERE LOWER(FUNCTION('unaccent', p.title)) " +
           "LIKE LOWER(FUNCTION('unaccent', CONCAT('%', :keyword, '%')))")
    Page<Product> searchByTitle(@Param("keyword") String keyword, Pageable pageable);
}
```

**Tips**:

- Use `LEFT JOIN FETCH` to avoid N+1 queries
- Use `@Param` for named parameters
- Use `Pageable` for pagination support
- Use `FUNCTION()` for database-specific functions

---

### 5. **Service Layer Pattern**

#### Transaction Management

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    // Read-only transaction for queries
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long id) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return productMapper.toDetailResponse(product);
    }

    // Write transaction for updates
    @Transactional
    public void incrementViewCount(Long productId) {
        productRepository.findById(productId).ifPresent(product -> {
            product.setViewCount(product.getViewCount() + 1);
            productRepository.save(product);
        });
    }
}
```

**Why**:

- `@Transactional(readOnly = true)` optimizes read operations
- `@Transactional` ensures data consistency for writes
- `@RequiredArgsConstructor` + `final` for dependency injection
- `@Slf4j` for logging

---

### 6. **Controller Pattern**

#### Complete Example

```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for product operations")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    @Operation(summary = "Get product details", description = "Retrieve complete product information")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(
            @Parameter(description = "Product ID", example = "1")
            @PathVariable Long id) {

        ProductDetailResponse product = productService.getProductDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(product, "Success"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products")
    public ResponseEntity<ApiResponse<Page<ProductListResponse>>> searchProducts(
            @Parameter(description = "Search keyword")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") @Min(0) Integer page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) Integer size) {

        // Build request object
        ProductSearchRequest request = ProductSearchRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();

        Page<ProductListResponse> products = productService.searchProducts(request);
        return ResponseEntity.ok(ApiResponse.ok(products, "Success"));
    }
}
```

**Key Points**:

- `@Tag` for Swagger grouping
- `@Operation` for endpoint documentation
- `@Parameter` for parameter documentation
- Always return `ApiResponse<T>` wrapper
- Use `@Valid` for request body validation

---

## 🔐 Security Patterns (For Future Implementation)

### JWT Authentication Flow

```
┌──────┐                                      ┌──────┐
│Client│                                      │Server│
└──┬───┘                                      └──┬───┘
   │ POST /api/v1/auth/login                    │
   │ {email, password}                          │
   ├───────────────────────────────────────────>│
   │                                             │
   │                    ┌─────────────────────┐ │
   │                    │ 1. Validate user     │ │
   │                    │ 2. Generate tokens   │ │
   │                    │    - AccessToken     │ │
   │                    │    - RefreshToken    │ │
   │                    └─────────────────────┘ │
   │                                             │
   │ {accessToken, refreshToken, user}          │
   │<───────────────────────────────────────────┤
   │                                             │
   │ GET /api/v1/products/1                     │
   │ Authorization: Bearer {accessToken}        │
   ├───────────────────────────────────────────>│
   │                                             │
   │                    ┌─────────────────────┐ │
   │                    │ 1. Verify token      │ │
   │                    │ 2. Extract user info │ │
   │                    │ 3. Process request   │ │
   │                    └─────────────────────┘ │
   │                                             │
   │ {data}                                     │
   │<───────────────────────────────────────────┤
```

### Role-Based Access Control (RBAC)

```java
// Entity
@Entity
public class User {
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;  // ADMIN, SELLER, BIDDER

    public boolean isAdmin() {
        return role.getId() == Role.ADMIN;
    }

    public boolean isSeller() {
        return role.getId() == Role.SELLER ||
               (role.getId() == Role.BIDDER &&
                sellerExpiresAt != null &&
                sellerExpiresAt.isAfter(ZonedDateTime.now()));
    }
}

// Controller (Future implementation)
@PreAuthorize("hasRole('SELLER')")
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @Valid @RequestBody CreateProductRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
    // Only sellers can create products
}

@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
    // Only admins can delete products
}
```

---

## 📧 Email Notification Pattern (For Future Implementation)

### Email Template Structure

```java
// Service
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendBidPlacedEmail(User seller, User bidder, Product product, BigDecimal bidAmount) {
        Context context = new Context();
        context.setVariable("sellerName", seller.getFullName());
        context.setVariable("bidderName", bidder.getFullName());
        context.setVariable("productTitle", product.getTitle());
        context.setVariable("bidAmount", bidAmount);
        context.setVariable("productUrl", "http://frontend.com/products/" + product.getId());

        String htmlContent = templateEngine.process("email/bid-placed", context);

        sendHtmlEmail(seller.getEmail(), "New bid on your product", htmlContent);
    }
}
```

### Email Events

```java
// Event
@Getter
public class BidPlacedEvent extends ApplicationEvent {
    private final Long productId;
    private final Long bidderId;
    private final BigDecimal bidAmount;

    public BidPlacedEvent(Object source, Long productId, Long bidderId, BigDecimal bidAmount) {
        super(source);
        this.productId = productId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
    }
}

// Listener
@Component
public class BidPlacedEventListener {

    private final EmailService emailService;

    @EventListener
    @Async
    public void handleBidPlaced(BidPlacedEvent event) {
        // Send email to seller
        // Send email to previous highest bidder
        emailService.sendBidPlacedNotifications(event);
    }
}

// Publisher (in BidService)
@Service
public class BidService {

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void placeBid(Long productId, Long userId, BigDecimal amount) {
        // Place bid logic
        // ...

        // Publish event
        eventPublisher.publishEvent(new BidPlacedEvent(this, productId, userId, amount));
    }
}
```

---

## 🎲 Auto-Bidding Pattern (For Future Implementation)

### Auto-Bid Logic

```java
@Service
public class AutoBidService {

    /**
     * Process auto-bids when a new bid is placed
     *
     * Algorithm:
     * 1. Find all auto-bids for this product (excluding current bidder)
     * 2. Sort by max_bid_amount DESC, created_at ASC
     * 3. Place counter-bid if auto-bidder can outbid current price
     * 4. Stop when no auto-bidder can outbid
     */
    @Transactional
    public void processAutoBids(Long productId, Long currentBidderId, BigDecimal currentPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<AutoBid> autoBids = autoBidRepository
                .findActiveAutoBidsForProduct(productId)
                .stream()
                .filter(ab -> !ab.getBidder().getId().equals(currentBidderId))
                .sorted((a, b) -> {
                    int cmp = b.getMaxBidAmount().compareTo(a.getMaxBidAmount());
                    return cmp != 0 ? cmp : a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .toList();

        for (AutoBid autoBid : autoBids) {
            BigDecimal nextBidAmount = currentPrice.add(product.getPriceStep());

            if (nextBidAmount.compareTo(autoBid.getMaxBidAmount()) <= 0) {
                // Auto-bidder can outbid
                placeBid(productId, autoBid.getBidder().getId(), nextBidAmount);
                return; // Only one auto-bid at a time
            }
        }
    }
}
```

---

## 🔄 Auto-Extend Pattern (For Future Implementation)

### Scheduled Job

```java
@Service
public class AuctionSchedulerService {

    private final ProductRepository productRepository;
    private final SystemConfigRepository systemConfigRepository;

    /**
     * Check for auctions that need auto-extension
     * Runs every minute
     */
    @Scheduled(fixedRate = 60000) // 60 seconds
    public void processAutoExtend() {
        Integer triggerMin = systemConfigRepository
                .findByKey(SystemConfig.AUTO_EXTEND_TRIGGER_MIN)
                .map(SystemConfig::getIntValue)
                .orElse(5);

        Integer extendByMin = systemConfigRepository
                .findByKey(SystemConfig.AUTO_EXTEND_BY_MIN)
                .map(SystemConfig::getIntValue)
                .orElse(10);

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime triggerTime = now.plusMinutes(triggerMin);

        List<Product> productsToExtend = productRepository
                .findProductsNeedingAutoExtend(triggerTime);

        for (Product product : productsToExtend) {
            if (product.getAutoExtend() && hasRecentBid(product, triggerMin)) {
                product.setEndTime(product.getEndTime().plusMinutes(extendByMin));
                productRepository.save(product);

                log.info("Extended auction {} by {} minutes", product.getId(), extendByMin);
            }
        }
    }

    private boolean hasRecentBid(Product product, Integer minutes) {
        ZonedDateTime since = ZonedDateTime.now().minusMinutes(minutes);
        return bidHistoryRepository
                .findRecentBidForProduct(product.getId(), since)
                .isPresent();
    }
}
```

---

## 🎨 Frontend Integration Guidelines

### API Response Structure

```typescript
// TypeScript interfaces for frontend
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

// Usage example
async function fetchProducts(page: number = 0): Promise<ProductListResponse[]> {
  const response = await fetch(`/api/v1/products/search?page=${page}&size=20`);
  const json: ApiResponse<Page<ProductListResponse>> = await response.json();

  if (!json.success) {
    throw new Error(json.message);
  }

  return json.data.content;
}
```

### Handling Masked Data

```typescript
// Bidder names are masked in responses
interface BidHistoryResponse {
  id: number;
  bidderName: string; // e.g., "****Khoa"
  bidAmount: string; // Formatted as "25,000,000"
  bidTime: string;
}

// Display as-is, no need to unmask
<div>
  <span>{bid.bidderName}</span>
  <span>{bid.bidAmount} VNĐ</span>
</div>;
```

---

## 📊 Database Migration Strategy

### Flyway Setup (Recommended for Production)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

```properties
# application.yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

```sql
-- src/main/resources/db/migration/V1__initial_schema.sql
CREATE EXTENSION IF NOT EXISTS "unaccent";
-- ... rest of schema
```

**Why**: Version-controlled database changes, team collaboration, production safety.

---

## 🧪 Testing Strategy

### Repository Tests

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldFindTop5EndingSoon() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);

        // When
        List<Product> products = productRepository.findTop5EndingSoon(pageable);

        // Then
        assertThat(products).hasSize(5);
        assertThat(products).isSortedAccordingTo(
            Comparator.comparing(Product::getEndTime)
        );
    }
}
```

### Service Tests

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldGetProductDetail() {
        // Given
        Long productId = 1L;
        Product product = new Product();
        product.setId(productId);

        when(productRepository.findByIdWithDetails(productId))
            .thenReturn(Optional.of(product));

        // When
        ProductDetailResponse result = productService.getProductDetail(productId);

        // Then
        verify(productRepository).findByIdWithDetails(productId);
        verify(productMapper).toDetailResponse(product);
    }
}
```

---

## 📈 Performance Optimization Tips

### 1. N+1 Query Problem

**Problem**:

```java
// ❌ Bad: Triggers N+1 queries
List<Product> products = productRepository.findAll();
for (Product product : products) {
    System.out.println(product.getSeller().getFullName());  // Lazy load
}
```

**Solution**:

```java
// ✅ Good: Single query with JOIN FETCH
@Query("SELECT p FROM Product p LEFT JOIN FETCH p.seller")
List<Product> findAllWithSeller();
```

### 2. Pagination

Always use pagination for list endpoints:

```java
Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
Page<Product> products = productRepository.findAll(pageable);
```

### 3. Indexes

Add indexes for frequently queried columns:

```sql
CREATE INDEX idx_products_end_time ON products(end_time);
CREATE INDEX idx_products_category ON products(category_id);
```

### 4. DTO Projection

Use DTO projections instead of entities for read-only queries:

```java
@Query("SELECT new com.taitrinh.online_auction.dto.ProductSummaryDTO(" +
       "p.id, p.title, p.currentPrice) " +
       "FROM Product p")
List<ProductSummaryDTO> findAllSummaries();
```

---

## 🚨 Common Pitfalls & Solutions

### 1. Bidirectional Relationships

**Problem**: Infinite recursion in JSON serialization

```java
// ❌ Product has @OneToMany List<BidHistory>
// ❌ BidHistory has @ManyToOne Product
// Result: Stack overflow when serializing
```

**Solution**: Use DTOs, never return entities directly

```java
// ✅ ProductDetailResponse (DTO) has List<BidHistoryResponse>
// ✅ No circular reference
```

### 2. Timezone Issues

**Always use `ZonedDateTime`**:

```java
// ❌ Bad
private LocalDateTime createdAt;

// ✅ Good
private ZonedDateTime createdAt;
```

### 3. Null Handling in Mappers

```java
// ✅ Always check nulls in custom methods
@Named("maskUserName")
default String maskUserName(User user) {
    if (user == null || user.getFullName() == null) {
        return null;
    }
    // ... masking logic
}
```

---

## 📚 Quick Reference

### Common Annotations

```java
// Spring annotations
@RestController        // REST controller
@Service              // Service component
@Repository           // Data access component
@Configuration        // Configuration class
@Component            // Generic component

// JPA annotations
@Entity               // JPA entity
@Table                // Table mapping
@Id                   // Primary key
@GeneratedValue       // Auto-generated value
@ManyToOne            // Many-to-one relationship
@OneToMany            // One-to-many relationship
@JoinColumn           // Foreign key column

// Validation annotations
@Valid                // Trigger validation
@NotNull              // Not null
@NotBlank             // Not null and not empty
@Min, @Max            // Numeric range
@Email                // Email format
@Pattern              // Regex pattern

// Lombok annotations
@Data                 // Getter, Setter, toString, equals, hashCode
@Getter, @Setter      // Generate getters/setters
@NoArgsConstructor    // No-args constructor
@AllArgsConstructor   // All-args constructor
@Builder              // Builder pattern
@RequiredArgsConstructor  // Constructor for final fields
@Slf4j                // Logger

// MapStruct annotations
@Mapper               // MapStruct mapper interface
@Mapping              // Field mapping
@Named                // Named custom method

// Swagger annotations
@Tag                  // Group endpoints
@Operation            // Endpoint description
@Parameter            // Parameter description
@Schema               // DTO schema
```

---

**Last Updated**: December 3, 2025
