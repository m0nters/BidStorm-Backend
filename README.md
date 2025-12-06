# StormBid - Backend API

## 📋 Tổng Quan Dự Án

**StormBid** là một nền tảng đấu giá trực tuyến toàn diện, cho phép người dùng mua bán sản phẩm thông qua hình thức đấu giá. Hệ thống được xây dựng theo kiến trúc RESTful API với Spring Boot, cung cấp các chức năng hoàn chỉnh cho cả người mua (bidder), người bán (seller), và quản trị viên (administrator).

### 🎯 Các Tính Năng Chính

#### 1. **Người Dùng Ẩn Danh (Guest)**

- Xem danh sách danh mục 2 cấp
- Xem top 5 sản phẩm (gần kết thúc, nhiều lượt ra giá, giá cao nhất)
- Tìm kiếm sản phẩm với full-text search (hỗ trợ tiếng Việt không dấu)
- Xem chi tiết sản phẩm với đầy đủ thông tin
- Xem lịch sử đấu giá (thông tin bidder được che)
- Đăng ký tài khoản với xác thực OTP qua email

#### 2. **Người Mua (Bidder)**

- Lưu sản phẩm vào danh sách yêu thích (Watch List)
- Đặt giá sản phẩm (với kiểm tra điểm đánh giá)
- Xem lịch sử đấu giá chi tiết
- Hỏi người bán về sản phẩm
- Quản lý hồ sơ cá nhân và xem điểm đánh giá
- Xin nâng cấp thành seller (trong 7 ngày)

#### 3. **Người Bán (Seller)**

- Đăng sản phẩm đấu giá với đầy đủ thông tin
- Bổ sung/cập nhật mô tả sản phẩm
- Từ chối lượt ra giá của bidder
- Trả lời câu hỏi từ người mua
- Quản lý sản phẩm đang bán

#### 4. **Quản Trị Viên (Administrator)**

- Quản lý danh mục (CRUD operations)
- Gỡ bỏ sản phẩm vi phạm
- Quản lý người dùng
- Duyệt nâng cấp tài khoản bidder → seller
- Xem dashboard thống kê

#### 5. **Hệ Thống**

- Gửi email tự động cho các giao dịch quan trọng
- Đấu giá tự động (auto-bidding)
- Tự động gia hạn đấu giá
- Quy trình thanh toán sau đấu giá

---

## 🛠️ Tech Stack

### Core Framework & Language

- **Java 21** - Programming language
- **Spring Boot 4.0.0** - Application framework
- **Maven** - Dependency management & build tool

### Database

- **PostgreSQL** - Primary database
- **Spring Data JPA** - ORM framework
- **Hibernate** - JPA implementation

### API Documentation

- **SpringDoc OpenAPI 2.8.13** - Swagger/OpenAPI documentation
- Access at: `http://localhost:8080/swagger-ui.html`

### Object Mapping

- **MapStruct 1.6.3** - Type-safe bean mapping
- Eliminates boilerplate code for DTO conversions

### Code Quality

- **Lombok** - Reduces boilerplate code (getters, setters, builders)
- **Jakarta Validation** - Request validation

### Database Features

- **PostgreSQL Extensions**:
  - `unaccent` - Vietnamese full-text search without diacritics
  - `pg_trgm` - Trigram matching for fuzzy search
  - `uuid-ossp` - UUID generation

---

## 📁 Cấu Trúc Dự Án

```
src/main/java/com/taitrinh/online_auction/
├── OnlineAuctionApplication.java          # Main application entry point
├── controller/                             # REST API endpoints
│   ├── CategoryController.java            # Category management APIs
│   └── ProductController.java             # Product browsing & search APIs
├── dto/                                    # Data Transfer Objects
│   ├── ApiResponse.java                   # Standard API response wrapper
│   ├── ErrorResponse.java                 # Error response structure
│   ├── ValidationError.java               # Validation error details
│   ├── category/                          # Category DTOs
│   │   ├── CategoryResponse.java
│   │   └── CreateCategoryRequest.java
│   └── product/                           # Product DTOs
│       ├── ProductListResponse.java       # For list views
│       ├── ProductDetailResponse.java     # For detail view
│       ├── ProductSearchRequest.java      # Search parameters
│       └── BidHistoryResponse.java        # Bid history
├── entity/                                # JPA Entities
│   ├── User.java                         # User accounts
│   ├── Role.java                         # User roles
│   ├── Category.java                     # Product categories
│   ├── Product.java                      # Auction products
│   ├── ProductImage.java                 # Product images
│   ├── BidHistory.java                   # Bid records
│   ├── AutoBid.java                      # Auto-bidding
│   ├── BlockedBidder.java                # Blocked bidders per product
│   ├── Favorite.java                     # User's watch list
│   ├── AuctionQuestion.java              # Q&A on products
│   ├── AuctionAnswer.java                # Answers to questions
│   ├── DescriptionLog.java               # Product description updates
│   ├── Review.java                       # User reviews
│   ├── UpgradeRequest.java               # Bidder→Seller upgrade requests
│   ├── EmailOtp.java                     # Email OTP verification
│   ├── RefreshToken.java                 # JWT refresh tokens
│   ├── SystemConfig.java                 # System configurations
│   ├── OrderCompletion.java              # Order completion flow
│   └── OrderChatMessage.java             # Seller-buyer chat
├── exception/                             # Exception handling
│   └── GlobalExceptionHandler.java       # Centralized error handling
├── mapper/                                # MapStruct mappers
│   ├── CategoryMapper.java               # Category entity ↔ DTO
│   └── ProductMapper.java                # Product entity ↔ DTO
├── repository/                            # Data access layer
│   ├── CategoryRepository.java           # Category queries
│   ├── ProductRepository.java            # Product queries
│   ├── BidHistoryRepository.java         # Bid history queries
│   └── SystemConfigRepository.java       # System config queries
└── service/                               # Business logic
    ├── CategoryService.java              # Category operations
    └── ProductService.java               # Product operations

database/
└── online-auction.sql                    # Database schema
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **PostgreSQL 14+**
- IDE with Java support (IntelliJ IDEA recommended)

### Database Setup

1. **Create PostgreSQL database**:

```sql
CREATE DATABASE online_auction;
```

2. **Run the schema script**:

```bash
psql -U your_username -d online_auction -f database/online-auction.sql
```

This will:

- Enable required PostgreSQL extensions (`unaccent`, `pg_trgm`, `uuid-ossp`)
- Create all tables with proper indexes
- Insert default roles (admin, seller, bidder)

### Configuration

1. **Create `.env` file** in the project root:

```properties
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/online_auction
DB_USER=your_username
DB_PASSWORD=your_password

# Server Configuration
PORT=8080
```

2. **Update `application.yaml`** if needed (already configured to read from `.env`)

### Build & Run

#### Using Maven

```bash
# Clean and compile
mvn clean compile

# Run the application
mvn spring-boot:run

# Build JAR file
mvn clean package
java -jar target/online-auction-0.0.1-SNAPSHOT.jar
```

#### Using IDE

1. Open project in IntelliJ IDEA
2. Wait for Maven to download dependencies
3. Run `OnlineAuctionApplication.java`

### Verify Installation

1. **Application**: http://localhost:8080
2. **Swagger UI**: http://localhost:8080/swagger-ui.html
3. **API Docs**: http://localhost:8080/v3/api-docs

---

## 📚 API Documentation

API documentation is automatically generated using **SpringDoc OpenAPI (Swagger)**.

### Access Swagger UI

```
http://localhost:8080/swagger-ui.html
```

### Available API Groups

#### 1. **Category Management** (`/api/v1/categories`)

- Get all categories (hierarchy)
- Get parent categories only
- Get category by ID
- Get children of a category
- Create/Update/Delete category

#### 2. **Product Browsing** (`/api/v1/products`)

- **Top 5 Lists** (for homepage):
  - `/top/ending-soon` - Products ending soonest
  - `/top/most-bids` - Most bid count
  - `/top/highest-price` - Highest current price
- **Browse & Search**:
  - `/category/{id}` - Products by category (paginated)
  - `/search` - Full-text search with filters (paginated)
- **Product Details**:
  - `/{id}` - Complete product information
  - `/{id}/related` - Related products in same category
  - `/{id}/bid-history` - Bid history with masked names

---

## 🎨 Implementation Details

### DTO Design Pattern

The system uses **separate DTOs for different views**:

- **`ProductListResponse`** - Lightweight DTO for lists/grids

  - Contains: thumbnail, basic info, current bidder (masked)
  - Used in: search results, category listings, top 5 lists

- **`ProductDetailResponse`** - Complete DTO for detail page
  - Contains: all images, full description, seller info, bid history
  - Includes nested objects: images, description logs, user details

This separation **reduces payload size** and **improves performance**.

### Full-Text Search

Implements **Vietnamese full-text search** using PostgreSQL:

```sql
-- Search without diacritics
SELECT * FROM products
WHERE LOWER(unaccent(title)) LIKE LOWER(unaccent('%dien thoai%'))
```

Features:

- ✅ Search "dien thoai" finds "điện thoại"
- ✅ Search "iphone" finds "iPhone", "IPHONE", "iPhOnE"
- ✅ Combines with category filtering
- ✅ Supports multiple sort options

### Data Masking

Bidder names are **masked for privacy**:

```
Original: "Nguyễn Văn Khoa"
Masked:   "****Khoa"
```

Implementation in `ProductMapper.java`:

```java
@Named("maskUserName")
default String maskUserName(User user) {
    String fullName = user.getFullName();
    if (fullName.length() <= 4) {
        return "****" + fullName;
    }
    String visiblePart = fullName.substring(fullName.length() - 4);
    return "****" + visiblePart;
}
```

### Pagination & Sorting

All list endpoints support:

- **Pagination**: `?page=0&size=20`
- **Sorting**: `?sortBy=endTime&sortDirection=asc`

Sort fields:

- `endTime` - Auction end time
- `currentPrice` - Current bid price
- `createdAt` - Creation time
- `bidCount` - Number of bids

### System Configuration

Dynamic configurations stored in `system_configs` table:

| Key                         | Description                            | Default    |
| --------------------------- | -------------------------------------- | ---------- |
| `new_product_highlight_min` | Duration to highlight new products     | 60 minutes |
| `auto_extend_trigger_min`   | Time before end to trigger auto-extend | 5 minutes  |
| `auto_extend_by_min`        | Extension duration                     | 10 minutes |
| `allow_unrated_bidders`     | Allow bidders with no ratings          | true/false |
| `seller_temp_duration_days` | Temporary seller permission duration   | 7 days     |

---

## 🔄 Current Implementation Status

### ✅ Completed Features

#### **Phase 1: Core Infrastructure**

- [x] Database schema with all entities
- [x] Spring Boot application setup
- [x] PostgreSQL configuration with extensions
- [x] Global exception handling
- [x] API response standardization
- [x] Swagger/OpenAPI documentation

#### **Phase 2: Category Management**

- [x] Category CRUD operations
- [x] 2-level category hierarchy
- [x] Category validation (prevent delete with products)
- [x] MapStruct mappers for categories

#### **Phase 3: Product Browsing (Guest Features)**

- [x] Top 5 products (ending soon, most bids, highest price)
- [x] Product listing by category (with pagination)
- [x] Full-text search (Vietnamese support)
- [x] Product detail view
- [x] Bid history with masked names
- [x] Related products
- [x] View counter
- [x] New product highlighting

### 🚧 Pending Implementation

#### **Phase 4: User Authentication & Authorization**

- [ ] User registration with email OTP
- [ ] Login with JWT (AccessToken + RefreshToken)
- [ ] Password hashing (bcrypt/scrypt)
- [ ] Role-based access control (RBAC)
- [ ] OAuth2 integration (Google, Facebook, etc.)

#### **Phase 5: Bidding System**

- [ ] Place bid (normal bidding)
- [ ] Auto-bidding mechanism
- [ ] Bid validation (rating check)
- [ ] Block bidders
- [ ] Auto-extend auction

#### **Phase 6: Seller Features**

- [ ] Create auction product
- [ ] Upload multiple images
- [ ] Update product description (append-only)
- [ ] Answer questions
- [ ] Manage auctions

#### **Phase 7: Bidder Features**

- [ ] Watch list / Favorites
- [ ] Ask questions about products
- [ ] View won auctions
- [ ] Request seller upgrade

#### **Phase 8: Rating & Review System**

- [ ] Rate seller/buyer after transaction
- [ ] View rating history
- [ ] Rating percentage calculation

#### **Phase 9: Order Completion Flow**

- [ ] Payment integration (MoMo/ZaloPay/VNPay/Stripe)
- [ ] Shipping address submission
- [ ] Order tracking
- [ ] Seller-buyer chat

#### **Phase 10: Admin Panel**

- [ ] User management
- [ ] Product moderation (remove violations)
- [ ] Approve seller upgrade requests
- [ ] Dashboard with statistics
- [ ] System configuration management

#### **Phase 11: Notification System**

- [ ] Email service integration
- [ ] Email templates
- [ ] Notification triggers (bid placed, auction ended, etc.)

#### **Phase 12: Monitoring & Logging**

- [ ] Grafana/ELK Stack integration
- [ ] Application logs
- [ ] Performance monitoring
- [ ] Error tracking

---

## 📝 Development Notes

### Database Indexes

Strategic indexes for query optimization:

```sql
-- Product indexes
CREATE INDEX idx_products_end_time ON products(end_time);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_price ON products(current_price);
CREATE INDEX idx_products_active ON products(is_ended, end_time);

-- User indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_rating ON users(positive_rating, negative_rating);

-- Bid history index
CREATE INDEX idx_bid_history_product_time ON bid_history(product_id, created_at);
```

### MapStruct Configuration

MapStruct is configured in `pom.xml` to work with Lombok:

```xml
<annotationProcessorPaths>
    <!-- Lombok must be before MapStruct -->
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </path>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>1.6.3</version>
    </path>
</annotationProcessorPaths>
```

### Entity Relationships

Key relationships in the domain model:

- **User** ← many Products (as seller)
- **User** ← many BidHistory (as bidder)
- **Product** → many BidHistory
- **Product** → many ProductImages
- **Product** → many DescriptionLogs
- **Category** ← many Products
- **Category** ← many Categories (self-referencing for hierarchy)

---

## 🧪 Testing

### Manual Testing with Swagger

1. Start the application
2. Open Swagger UI: http://localhost:8080/swagger-ui.html
3. Test endpoints directly from the browser

### Sample API Calls

```bash
# Get top 5 ending soon
curl http://localhost:8080/api/v1/products/top/ending-soon

# Search products
curl "http://localhost:8080/api/v1/products/search?keyword=iphone&page=0&size=20"

# Get product details
curl http://localhost:8080/api/v1/products/1

# Get products by category
curl "http://localhost:8080/api/v1/products/category/5?page=0&size=20&sortBy=endTime&sortDirection=asc"
```

---

## 📖 Additional Resources

### Related Documentation

- [Project Requirements](Project%20requirements.md) - Full requirement specifications
- [Database Schema](database/online-auction.sql) - Complete database structure
- [Swagger UI](http://localhost:8080/swagger-ui.html) - Live API documentation

### External References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [MapStruct Guide](https://mapstruct.org/)
- [PostgreSQL Full-Text Search](https://www.postgresql.org/docs/current/textsearch.html)
- [SpringDoc OpenAPI](https://springdoc.org/)

---

## 📄 License

This project is developed as a final project for WNC course.
