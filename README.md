# 🏆 BidStorm - Online Auction Platform

A comprehensive, production-ready online auction platform built with Spring Boot, featuring automatic bidding, real-time updates, secure payment processing, and a complete post-auction order fulfillment system.

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture & Design Patterns](#architecture--design-patterns)
- [Key Features](#key-features)
- [Authentication & Security](#authentication--security)
- [Third-Party Integrations](#third-party-integrations)
- [Database Schema](#database-schema)
- [API Documentation](#api-documentation)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Project Structure](#project-structure)

---

## 🎯 Overview

BidStorm is a feature-rich online auction platform that allows users to buy and sell products through a competitive bidding process. The platform supports:

- **Multiple user roles**: Guest, Bidder, Seller, and Admin
- **Automatic bidding system**: Smart bidding algorithm that helps users win auctions at the lowest possible price
- **Real-time notifications**: WebSocket-based notifications for bids, comments, and order updates
- **Secure payment processing**: Stripe integration with escrow-like payment flow
- **Complete order fulfillment**: Multi-step order completion with chat between buyer and seller
- **Advanced search**: PostgreSQL Full-Text Search with Vietnamese language support
- **Email notifications**: Comprehensive email system for all important events

---

## 🛠️ Tech Stack

### Backend
- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 4.0.0** - Core framework
  - Spring Web MVC - RESTful API
  - Spring Data JPA - Database access & ORM
  - Spring Security - Authentication & authorization
  - Spring Mail - Email notifications
  - Spring WebSocket - Real-time bidirectional communication
  - Spring Validation - Request validation
- **PostgreSQL** - Primary database with advanced features:
  - Full-Text Search with `unaccent` and `pg_trgm` extensions
  - Partial indexes for performance optimization
  - Triggers for automatic search vector updates
- **Hibernate** - JPA implementation with optimized queries
- **Lombok** - Reduce boilerplate code
- **MapStruct** - Type-safe bean mapping

### Security
- **JWT (JSON Web Tokens)** - Stateless authentication with refresh token rotation
- **BCrypt** - Password hashing (12 rounds)
- **Google OAuth2** - Google One Tap sign-in with JWT credential verification
- **Role-based Access Control (RBAC)** - Hierarchical role system (Admin > Seller > Bidder)

### Third-Party Services
- **AWS S3** - Image and file storage with automatic optimization
- **Stripe** - Payment processing with PaymentIntent API
- **Google API Client** - OAuth2 verification
- **Gmail SMTP** - Transactional email delivery

### API Documentation
- **SpringDoc OpenAPI 3** - Auto-generated Swagger documentation available at `/swagger-ui.html`

### Build & Development
- **Maven** - Dependency management and build automation
- **Git** - Version control

---

## 🏗️ Architecture & Design Patterns

### Layered Architecture

The application follows a clean **layered architecture** with clear separation of concerns:

```
┌─────────────────────────────────────┐
│         Controller Layer            │  ← REST endpoints, request validation
├─────────────────────────────────────┤
│          Service Layer              │  ← Business logic, transactions
├─────────────────────────────────────┤
│        Repository Layer             │  ← Data access, query methods
├─────────────────────────────────────┤
│          Entity Layer               │  ← JPA entities, database mapping
└─────────────────────────────────────┘
```

### Design Patterns

1. **Repository Pattern**
   - All database access goes through Spring Data JPA repositories
   - Custom repository methods for complex queries
   - Query optimization with JOIN FETCH to avoid N+1 problems

2. **DTO Pattern**
   - Separation of request, response, and internal DTOs
   - MapStruct for automatic entity ↔ DTO mapping
   - Request validation using Jakarta Validation annotations

3. **Service Layer Pattern**
   - Business logic encapsulated in service classes
   - Transaction management with `@Transactional`
   - Separate services for different domain concerns

4. **Dependency Injection**
   - Constructor-based injection for all dependencies
   - Immutable dependencies with `@RequiredArgsConstructor` (Lombok)

5. **Global Exception Handling**
   - Centralized exception handling with `@ControllerAdvice`
   - Custom exception classes for different error scenarios
   - Consistent error response format with `ErrorResponse<T>`

6. **Scheduled Tasks**
   - Automated product end processing with `@Scheduled`
   - Auction completion detection and winner notification

7. **Template Method Pattern**
   - Email service with specialized email builders for different event types

8. **Strategy Pattern**
   - Different authentication strategies (local, OAuth2)

---

## ✨ Key Features

### 1. User Management

#### User Roles
- **Guest**: Browse products, view listings, search
- **Bidder**: Place bids, add favorites, ask questions, request seller upgrade
- **Seller**: Create products, manage listings, respond to questions, reject bidders
- **Admin**: Manage categories, users, products, approve seller upgrades

#### Account Features
- Email/password registration with OTP verification
- Google One Tap sign-in (OAuth2)
- Password reset with OTP verification (multi-step flow)
- Profile management (avatar, personal info, password change)
- User rating system (positive/negative with comments)

### 2. Product Management

#### Product Creation (Sellers)
- Minimum 3 images + 1 primary image required
- Image upload to AWS S3 with automatic optimization
- Rich text description with WYSIWYG support
- Configurable auction parameters:
  - Starting price
  - Price step (minimum bid increment)
  - Buy Now price (optional)
  - Auto-extend feature (extends auction when bid placed near end time)
  - Allow/disallow unrated bidders
- Category assignment (only leaf categories, 2-level hierarchy)

#### Product Browsing (All Users)
- Home page with top products:
  - Top 5 ending soon
  - Top 5 most bids
  - Top 5 highest price
- Category-based browsing with pagination
- Full-text search with Vietnamese support (accent-insensitive)
- Sort by: end time, price
- Highlight recently posted products (configurable time window)

#### Product Details
- Complete product information with image gallery
- Seller and current highest bidder info with ratings
- Relative time display for ending soon products
- 5 related products from same category
- Q&A section (threaded comments)
- Bid history with name masking (privacy protection)

### 3. Automatic Bidding System

One of the most sophisticated features - allows bidders to win at the lowest possible price without manual intervention.

#### How It Works
1. Bidder sets a **max bid amount** (private)
2. System shows only the **minimum amount needed to win** (public)
3. When another bidder bids, the system automatically outbids them (up to max bid)
4. If two bidders have the same max bid, the **earlier** bidder wins

#### Example Scenario
```
Product: iPhone 15 Pro
Starting Price: $800
Price Step: $10

Bidder A sets max bid: $950  → Product shows $800 (A is highest)
Bidder B sets max bid: $900  → Product shows $910 (A still highest, auto-outbid B)
Bidder C sets max bid: $1,000 → Product shows $960 (C is highest, outbid A)
Bidder A increases max: $1,000 → Product shows $1,000 (A wins, bid earlier than C)
```

This ensures bidders don't overpay while staying competitive!

### 4. Bidding Rules & Validation

- Cannot bid on own products
- Cannot bid on ended auctions
- Bid amount must follow price step rules
- Rating requirements (80% positive rating, or unrated with seller permission)
- Seller can reject/block specific bidders
- Auto-extend: Adds 10 minutes if bid placed within last 5 minutes (configurable)

### 5. Buy Now Feature

- Instant purchase at fixed price
- Immediately ends auction
- Creates order and assigns winner
- Notifies seller and previous bidders

### 6. Post-Auction Order Completion

Complete escrow-like payment and fulfillment system:

#### Order Statuses
1. **PENDING_PAYMENT** - Waiting for buyer to pay via Stripe
2. **PAID** - Payment received, held in escrow (Stripe PaymentIntent)
3. **SHIPPED** - Seller confirms shipment with tracking number
4. **COMPLETED** - Buyer confirms receipt, funds released to seller
5. **CANCELLED** - Seller cancels before payment (auto -1 rating to buyer)

#### Order Features
- Stripe secure payment processing
- Shipping address and phone collection
- Tracking number management
- Real-time chat between buyer and seller (WebSocket)
- Mutual rating system after completion
- Either party can update their rating

### 7. Real-Time Communication

#### WebSocket Features
- Bid notifications (new bid, outbid alerts)
- Comment notifications (new questions/replies)
- Order chat between buyer and seller
- JWT authentication for WebSocket connections

#### Channels
- `/user/queue/bid-notifications` - Personal bid updates
- `/user/queue/comment-notifications` - Q&A updates
- `/topic/order-chat/{productId}` - Order-specific chat

### 8. Email Notification System

Comprehensive email notifications for all important events:

#### Bidding Events
- Bid placed successfully (to bidder and seller)
- Outbid notification (to previous highest bidder)
- Bidder rejected (to rejected bidder)

#### Auction Completion
- Winner notification (to winning bidder and seller)
- No winner notification (to seller if no bids)

#### Q&A System
- Question posted (to seller)
- Answer posted (to all bidders and question asker)

#### Order Events
- Payment confirmation
- Shipment confirmation
- Order completion

All emails are HTML-formatted with beautiful templates and contain direct links to relevant pages.

### 9. Admin Dashboard

- User management (view, activate/deactivate, upgrade to seller)
- Category management (CRUD, prevent deletion with products)
- Product moderation (remove inappropriate listings)
- Seller upgrade request approval system
- Statistics and analytics:
  - Revenue trends
  - New products/users
  - Top bidders leaderboard
  - Top sellers leaderboard
  - Seller upgrade requests

### 10. Search & Filtering

#### PostgreSQL Full-Text Search
- Vietnamese language support with `unaccent` extension
- Accent-insensitive search (e.g., "dien thoai" matches "điện thoại")
- Search in title and description with weighted ranking
- Automatic search vector updates via database triggers

#### Filters
- By category (with parent-child hierarchy)
- By price range
- By auction status (active/ended)
- Sort by: end time, price, popularity

---

## 🔐 Authentication & Security

### JWT-Based Authentication

#### Access Token
- Short-lived (1 hour)
- Contains user ID, email, role
- Sent in `Authorization: Bearer <token>` header
- Validated on every request

#### Refresh Token
- Long-lived (7 days)
- Stored in HTTP-only, Secure, SameSite cookie
- Cannot be accessed by JavaScript (XSS protection)
- Single-use with automatic rotation

#### Refresh Token Rotation
Secure token rotation prevents token reuse attacks:

1. Client sends refresh token
2. Server validates and marks it as **revoked**
3. Server generates NEW access + refresh tokens
4. Old refresh token is linked to new one (`replaced_by`)
5. If old token is reused → **security breach detected** → revoke ALL user tokens

#### Token Security Features
- Tokens stored in database for revocation support
- Partial indexes for fast lookup of active tokens
- Automatic cleanup of expired tokens
- Token family tracking for breach detection

### OAuth2 - Google One Tap

Modern, streamlined Google sign-in experience:

1. Frontend displays Google One Tap UI
2. User selects Google account
3. Google returns JWT credential (not authorization code)
4. Backend verifies JWT signature with Google API
5. Backend creates/authenticates user
6. Returns our own JWT access + refresh tokens

**Benefits over traditional OAuth2 flow:**
- One-click sign-in (no redirect)
- Better UX on mobile
- More secure (no code exchange)

### Security Configuration

#### Role Hierarchy
```
ADMIN > SELLER > BIDDER
```

Admins inherit all seller and bidder permissions.
Sellers inherit all bidder permissions.

#### Endpoint Protection

**Public (unauthenticated):**
- Product browsing (GET only)
- Category browsing
- Authentication endpoints
- Stripe webhooks

**Authenticated:**
- Profile management
- Bidding
- Favorites
- Comments

**Seller role required:**
- Product creation/editing
- Bid rejection
- Order shipment confirmation

**Admin role required:**
- Category management
- User management
- Seller upgrade approval
- Statistics viewing

#### Password Security
- BCrypt hashing with 12 rounds
- Password strength validation
- Secure password reset flow with OTP

#### CORS Configuration
- Configurable allowed origins
- Credentials support for cookies
- Proper preflight handling

---

## 🔌 Third-Party Integrations

### 1. AWS S3 - File Storage

#### Features
- Automatic image upload for products and avatars
- Image optimization and validation:
  - Products: 800x600 minimum, 5MB max
  - Avatars: Any size, 5MB max
  - Supported formats: JPG, JPEG, PNG, WEBP, GIF
- Public read access for uploaded files
- Automatic file deletion when products are removed
- Unique filename generation with UUID

#### Configuration
```yaml
aws:
  s3:
    access-key-id: ${AWS_ACCESS_KEY_ID}
    secret-access-key: ${AWS_SECRET_ACCESS_KEY}
    bucket-name: ${AWS_S3_BUCKET_NAME}
    region: ${AWS_S3_REGION}
```

#### Service Methods
- `uploadFile(MultipartFile, folder)` - Upload product images
- `uploadAvatar(MultipartFile, folder)` - Upload user avatars
- `deleteFile(fileUrl)` - Delete files from S3

### 2. Stripe - Payment Processing

#### Payment Flow
1. **Order Creation**: Product ends with winner
2. **PaymentIntent Created**: Server creates PaymentIntent with order amount
3. **Client Confirms**: Frontend uses Stripe.js to confirm payment
4. **Webhook Notification**: Stripe sends `payment_intent.succeeded` event
5. **Order Updated**: Server marks order as PAID
6. **Shipment**: Seller confirms shipment
7. **Completion**: Buyer confirms receipt
8. **Transfer**: Funds released to seller (requires Stripe Connect in production)

#### Webhook Security
- Signature verification using webhook secret
- Protected endpoint (public, but verified)
- Idempotent payment processing

#### Configuration
```yaml
stripe:
  api:
    key: ${STRIPE_API_KEY}
  publishable:
    key: ${STRIPE_PUBLISHABLE_KEY}
  webhook:
    secret: ${STRIPE_WEBHOOK_SECRET}
  currency: USD
```

#### Note on Transfers
Current implementation logs transfers. In production, use **Stripe Connect** to actually transfer funds to seller accounts.

### 3. Google OAuth2 - One Tap Sign-In

#### Implementation
- Uses Google Identity Services (One Tap)
- JWT credential verification with `google-api-client`
- Validates token signature and expiration
- Extracts email, name, and Google ID
- Creates user if first login, otherwise authenticates

#### Configuration
```yaml
google:
  oauth:
    client-id: ${GOOGLE_OAUTH_CLIENT_ID}
```

#### Security
- Token signature verification
- Issuer validation (accounts.google.com)
- Audience validation (matches client ID)
- Expiration check

### 4. Gmail SMTP - Email Service

#### Features
- HTML email templates for all event types
- Embedded styling for email client compatibility
- Direct links to relevant pages in the application
- Beautiful, professional design

#### Email Types
- Registration OTP
- Email verification
- Password reset OTP
- Bid notifications
- Winner notifications
- Order updates
- Comment notifications

#### Configuration
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}  # Use App Password for Gmail
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

### 5. Google reCAPTCHA v3

#### Protection Against
- Bot registrations
- Automated spam

#### Integration
- Frontend sends reCAPTCHA token with registration request
- Backend verifies token with Google reCAPTCHA API
- Configurable score threshold

#### Configuration
```yaml
recaptcha:
  secret-key: ${RECAPTCHA_SECRET_KEY}
  site-key: ${RECAPTCHA_SITE_KEY}
  verify-url: https://www.google.com/recaptcha/api/siteverify
```

---

## 💾 Database Schema

### Core Tables

#### Users & Authentication
- **users** - User accounts with OAuth support
  - Role-based access (bidder/seller/admin)
  - Rating system (positive/negative counts)
  - Seller temporary access (7-day trial with expiration)
  - OAuth provider tracking (LOCAL/GOOGLE)
- **roles** - User roles (BIDDER, SELLER, ADMIN)
- **email_otps** - One-time passwords for email verification and password reset
- **refresh_tokens** - JWT refresh tokens with rotation tracking

#### Products & Categories
- **categories** - 2-level category hierarchy (parent → child)
- **products** - Auction listings with:
  - Full-text search vector (auto-updated by trigger)
  - Auto-extend configuration
  - Winner tracking
  - Bid count and view count
- **product_images** - Multiple images per product (min 3, 1 primary)
- **description_logs** - Audit trail of description changes

#### Bidding System
- **bid_history** - All bids with automatic bidding support:
  - `bid_amount` - Public visible bid
  - `max_bid_amount` - Private maximum willing to pay
- **blocked_bidders** - Seller-rejected bidders
- **favorites** - User watchlists

#### Communication
- **comments** - Threaded Q&A system (self-referencing parent_id)
- **order_chat_messages** - Chat between buyer and seller after auction

#### Order Management
- **order_completions** - Post-auction order flow:
  - Payment status tracking (Stripe PaymentIntent)
  - Shipping information
  - Multi-stage status (PENDING_PAYMENT → PAID → SHIPPED → COMPLETED)
- **reviews** - Mutual ratings between buyers and sellers

#### Admin
- **upgrade_requests** - Bidder to seller upgrade requests
- **system_configs** - Dynamic configuration (auto-extend time, highlight duration)

### Database Extensions

```sql
CREATE EXTENSION IF NOT EXISTS "unaccent";     -- Vietnamese accent removal
CREATE EXTENSION IF NOT EXISTS "pg_trgm";      -- Trigram similarity
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";    -- UUID generation
```

### Key Indexes

**Performance Optimizations:**
- Full-text search: `idx_products_fts` (GIN index on search_vector)
- Active products: `idx_products_active` (is_ended, end_time)
- Email lookup: `idx_users_email`
- Active refresh tokens: Partial index `WHERE revoked_at IS NULL`
- Active seller privileges: Partial index `WHERE seller_expires_at > NOW()`

**Query Optimizations:**
- JOIN FETCH in repositories to avoid N+1 queries
- Composite indexes for frequent query patterns
- Partial indexes for filtered queries

---

## 📚 API Documentation

### Swagger UI

Interactive API documentation available at:
```
http://localhost:8080/swagger-ui.html
```

### API Structure

#### Authentication (`/api/v1/auth`)
- `POST /register` - Register new account
- `POST /login` - Email/password login
- `POST /logout` - Logout and revoke tokens
- `POST /refresh-token` - Get new access token
- `POST /google-one-tap` - Google sign-in
- `POST /forgot-password` - Request password reset OTP
- `POST /verify-reset-password-otp` - Verify OTP
- `POST /reset-password` - Set new password
- `POST /verify-email` - Verify email with OTP
- `POST /resend-email-verification-otp` - Resend OTP

#### Products (`/api/v1/products`)
- `GET /` - List products with filtering and pagination
- `GET /{slug}` - Get product details
- `POST /` - Create product (SELLER)
- `PUT /{id}` - Update product (SELLER)
- `PATCH /{id}/description` - Append to description (SELLER)
- `DELETE /{id}` - Delete product (SELLER)
- `POST /{id}/bids` - Place bid
- `GET /{id}/bids` - Get bid history
- `DELETE /{id}/bidders/{bidderId}` - Reject bidder (SELLER)
- `POST /{id}/buy-now` - Buy now

#### Categories (`/api/v1/categories`)
- `GET /` - List all categories
- `GET /{id}` - Get category with products
- `POST /` - Create category (ADMIN)
- `PUT /{id}` - Update category (ADMIN)
- `DELETE /{id}` - Delete category (ADMIN)

#### Comments (`/api/v1/comments`)
- `GET /products/{productId}` - Get comments for product
- `POST /products/{productId}` - Post comment
- `POST /{commentId}/reply` - Reply to comment

#### Profile (`/api/v1/profile`)
- `GET /` - Get profile
- `PUT /` - Update profile
- `PUT /password` - Change password
- `GET /favorites` - Get favorite products
- `POST /favorites/{productId}` - Add to favorites
- `DELETE /favorites/{productId}` - Remove from favorites
- `GET /bids` - Get bidding history
- `GET /won-products` - Get won auctions
- `GET /seller/products` - Get seller's products
- `GET /seller/ended-products` - Get seller's ended products
- `POST /reviews` - Submit review
- `PUT /reviews/{id}` - Update review

#### Admin (`/api/v1/admin`)
- User management
- Category management
- Product moderation
- Seller upgrade approvals
- Statistics endpoints

#### Orders (`/api/v1/orders`)
- Order completion workflow
- Payment processing
- Shipment tracking
- Order chat

#### WebSocket (`/ws`)
- Real-time bid notifications
- Comment notifications
- Order chat

### Response Format

#### Success Response
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation successful",
  "timestamp": "2026-01-04T14:30:00+07:00"
}
```

#### Error Response
```json
{
  "success": false,
  "error": "Error message in Vietnamese",
  "timestamp": "2026-01-04T14:30:00+07:00"
}
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **PostgreSQL 14** or higher
- **Maven 3.8** or higher
- **AWS S3 account** (for file storage)
- **Stripe account** (for payments)
- **Gmail account** (for email notifications)
- **Google Cloud account** (for OAuth2)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/online-auction.git
   cd online-auction
   ```

2. **Set up PostgreSQL database**
   ```bash
   # Create database
   createdb auction_db
   
   # Run schema initialization
   psql -d auction_db -f database/schema.sql
   
   # (Optional) Load seed data
   psql -d auction_db -f database/seed_data.sql
   ```

3. **Configure environment variables**
   
   Copy `.env.example` to `.env` and fill in your credentials:
   ```bash
   cp .env.example .env
   ```
   
   See [Configuration](#configuration) section below for all required variables.

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

6. **Access the application**
   - API: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## ⚙️ Configuration

### Environment Variables

Create a `.env` file in the project root with the following variables:

#### Database Configuration
```env
DB_URL=jdbc:postgresql://localhost:5432/auction_db
DB_USER=your_db_username
DB_PASSWORD=your_db_password
```

#### Server Configuration
```env
PORT=8080
APP_URL=http://localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

#### AWS S3 Configuration
```env
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
AWS_S3_BUCKET_NAME=your-bucket-name
AWS_S3_REGION=ap-southeast-2
DEFAULT_AVATAR_URL=https://your-bucket.s3.region.amazonaws.com/default-avatar.png
```

#### Stripe Configuration
```env
STRIPE_API_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

#### Email Configuration
```env
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password  # Use Gmail App Password
```

#### Google OAuth2 Configuration
```env
GOOGLE_OAUTH_CLIENT_ID=your-client-id.apps.googleusercontent.com
```

#### reCAPTCHA Configuration
```env
RECAPTCHA_SECRET_KEY=6Le...
RECAPTCHA_SITE_KEY=6Le...
```

### Application Configuration

Key settings in `application.yaml`:

#### JWT Token Expiration
```yaml
jwt:
  access-token-expiration: 3600000      # 1 hour
  refresh-token-expiration: 604800000   # 7 days
```

#### OTP Expiration
```yaml
otp:
  expiration-minutes: 10
```

#### Cookie Settings
```yaml
cookie:
  secure: true          # Set to false for local development (HTTP)
  same-site: Strict     # CSRF protection
```

#### File Upload Limits
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 30MB
```

---

## 📁 Project Structure

```
online-auction/
├── src/
│   ├── main/
│   │   ├── java/com/taitrinh/online_auction/
│   │   │   ├── config/               # Configuration classes
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── S3Config.java
│   │   │   │   ├── StripeConfig.java
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controller/           # REST controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── BidController.java
│   │   │   │   ├── CategoryController.java
│   │   │   │   ├── ProfileController.java
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── OrderCompletionController.java
│   │   │   │   └── StripeWebhookController.java
│   │   │   ├── dto/                  # Data Transfer Objects
│   │   │   │   ├── request/          # Request DTOs
│   │   │   │   └── response/         # Response DTOs
│   │   │   ├── entity/               # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── BidHistory.java
│   │   │   │   ├── Category.java
│   │   │   │   ├── OrderCompletion.java
│   │   │   │   └── RefreshToken.java
│   │   │   ├── enums/                # Enumerations
│   │   │   │   ├── OtpPurpose.java
│   │   │   │   └── OrderStatus.java
│   │   │   ├── exception/            # Custom exceptions
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── UnauthorizedSellerException.java
│   │   │   │   └── InvalidRefreshTokenException.java
│   │   │   ├── mapper/               # MapStruct mappers
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── ProductMapper.java
│   │   │   │   └── BidMapper.java
│   │   │   ├── repository/           # Spring Data repositories
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── ProductRepository.java
│   │   │   │   ├── BidHistoryRepository.java
│   │   │   │   └── RefreshTokenRepository.java
│   │   │   ├── scheduler/            # Scheduled tasks
│   │   │   │   └── ProductEndScheduler.java
│   │   │   ├── security/             # Security components
│   │   │   │   ├── JwtUtil.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── service/              # Business logic services
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── OAuth2Service.java
│   │   │   │   ├── ProductService.java
│   │   │   │   ├── BidService.java
│   │   │   │   ├── CategoryService.java
│   │   │   │   ├── ProfileService.java
│   │   │   │   ├── OrderCompletionService.java
│   │   │   │   ├── StripePaymentService.java
│   │   │   │   ├── S3Service.java
│   │   │   │   ├── EmailService.java
│   │   │   │   └── email/            # Specialized email builders
│   │   │   └── util/                 # Utility classes
│   │   │       ├── NameMaskingUtil.java
│   │   │       └── SlugUtil.java
│   │   └── resources/
│   │       ├── application.yaml       # Main configuration
│   │       └── static/                # Static resources
│   └── test/                          # Test classes
├── database/
│   ├── schema.sql                     # Database schema
│   └── seed_data.sql                  # Sample data (optional)
├── .env.example                       # Environment variables template
├── pom.xml                            # Maven dependencies
└── README.md                          # This file
```

### Key Components

#### Controllers
Handle HTTP requests, validate input, and delegate to services.

#### Services
Contain business logic, transaction management, and orchestration of multiple repositories.

#### Repositories
Data access layer using Spring Data JPA with custom query methods.

#### Mappers
Type-safe entity ↔ DTO conversion using MapStruct.

#### Security
JWT token generation/validation, authentication filter, and user details service.

#### Scheduler
Automated tasks for auction end processing.

---

## 📊 Monitoring & Logging

### Logging Levels
```yaml
logging:
  level:
    com.taitrinh.online_auction: DEBUG
    org.springframework.messaging: DEBUG
    org.springframework.web.socket: DEBUG
```

### Log Files
- Application logs: `app.log` (if configured)
- Spring Boot default console logging

### Recommended Production Setup
- **ELK Stack** (Elasticsearch, Logstash, Kibana) for log aggregation
- **Grafana** for metrics visualization
- **Prometheus** for metrics collection

---

## 🔒 Security Best Practices

### Implemented
✅ Password hashing with BCrypt (12 rounds)
✅ JWT with refresh token rotation
✅ HTTP-only, Secure, SameSite cookies
✅ CORS configuration
✅ SQL injection prevention (JPA/Hibernate)
✅ XSS prevention (input validation)
✅ CSRF protection (stateless API)
✅ Rate limiting (via Stripe webhook signature)
✅ Email verification
✅ Password reset with OTP
✅ Role-based access control

### Production Recommendations
- Enable HTTPS (TLS/SSL)
- Use environment-specific secrets
- Implement rate limiting
- Add request logging
- Regular security audits
- Dependency vulnerability scanning

---

## 📝 License

This project is developed as part of a Advanced Web Application Development course.