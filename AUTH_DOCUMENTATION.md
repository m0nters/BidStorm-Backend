# Authentication System Documentation

## Overview

This document describes the complete authentication and authorization system implemented for an online auction platform - BidStorm. The system uses JWT (JSON Web Tokens) for stateless authentication with bcrypt password hashing.

## Architecture

### Security Components

- **Spring Security 6.x**: Core security framework
- **JWT (JSON Web Tokens)**: Stateless authentication using JJWT 0.12.6
- **BCrypt**: Password hashing with 12 rounds
- **Access Token**: Short-lived token (1 hour) for API authentication
- **Refresh Token**: Long-lived token (7 days) for obtaining new access tokens

### Key Features

1. ✅ User registration with email OTP verification
2. ✅ Login with JWT token generation
3. ✅ Refresh token mechanism
4. ✅ BCrypt password encryption
5. ✅ Role-based access control (ADMIN, SELLER, BIDDER)
6. ✅ Email OTP verification system
7. ✅ Secure logout with token invalidation
8. ✅ Automatic seller ID extraction from JWT

## API Endpoints

### Base URL: `/api/auth`

#### 1. Register

**POST** `/api/auth/register`

Register a new bidder account. Sends OTP to email for verification.

**Request Body:**

```json
{
  "email": "john.doe@example.com",
  "password": "Password123!",
  "fullName": "John Doe",
  "address": "123 Main Street, District 1, Ho Chi Minh City",
  "birthDate": "1990-05-15",
  "recaptchaToken": "03AGdBq26..."
}
```

**Password Requirements:**

- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character (@$!%\*?&)

**Response (201 Created):**

```json
{
  "success": true,
  "message": "Registration successful. Please check your email for OTP verification.",
  "data": null
}
```

#### 2. Login

**POST** `/api/auth/login`

Authenticate user and receive JWT tokens.

**Request Body:**

```json
{
  "email": "john.doe@example.com",
  "password": "Password123!"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "john.doe@example.com",
      "fullName": "John Doe",
      "role": "BIDDER",
      "emailVerified": true,
      "isActive": true
    }
  }
}
```

#### 3. Refresh Token

**POST** `/api/auth/refresh`

Generate a new access token using refresh token.

**Request Body:**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "john.doe@example.com",
      "fullName": "John Doe",
      "role": "BIDDER",
      "emailVerified": true,
      "isActive": true
    }
  }
}
```

#### 4. Verify OTP

**POST** `/api/auth/verify-otp`

Verify email address using OTP sent during registration.

**Request Body:**

```json
{
  "email": "john.doe@example.com",
  "otpCode": "123456"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": null
}
```

#### 5. Resend OTP

**POST** `/api/auth/resend-otp?email=john.doe@example.com`

Resend verification OTP to user's email.

**Response (200 OK):**

```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": null
}
```

#### 6. Logout

**POST** `/api/auth/logout`

Logout user and invalidate refresh token.

**Headers:**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

## Protected Endpoints

### How to Use JWT Tokens

1. After login, save the `accessToken` from the response
2. Include it in the `Authorization` header for all protected endpoints:
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

### Example: Create Product (Seller Only)

**POST** `/api/v1/products`

**Headers:**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Request Body:**

```json
{
  "categoryId": 5,
  "title": "iPhone 15 Pro Max 256GB - Brand New",
  "description": "<p>Brand new iPhone 15 Pro Max with 256GB storage...</p>",
  "startingPrice": 20000000,
  "priceStep": 100000,
  "buyNowPrice": 30000000,
  "endTime": "2025-12-15T23:59:59+07:00",
  "autoExtend": true,
  "images": [
    {
      "imageUrl": "https://example.com/images/product1.jpg",
      "isPrimary": true,
      "sortOrder": 1
    },
    {
      "imageUrl": "https://example.com/images/product2.jpg",
      "isPrimary": false,
      "sortOrder": 2
    },
    {
      "imageUrl": "https://example.com/images/product3.jpg",
      "isPrimary": false,
      "sortOrder": 3
    }
  ]
}
```

**Note:** The seller ID is automatically extracted from the JWT token. You don't need to include it in the request.

## Role-Based Access Control

### Roles

- **ADMIN**: Full system access
- **SELLER**: Can create products, update descriptions, manage their products
- **BIDDER**: Can browse, bid on products, add to watchlist

### Access Rules

#### Public Endpoints (No Authentication)

- GET `/api/products/**` - Browse products
- GET `/api/categories/**` - Browse categories
- POST `/api/auth/register` - Register
- POST `/api/auth/login` - Login
- POST `/api/auth/verify-otp` - Verify OTP
- POST `/api/auth/resend-otp` - Resend OTP

#### Seller Endpoints (ROLE_SELLER)

- POST `/api/v1/products` - Create product
- PUT `/api/v1/products/{id}/description` - Update product description

#### Admin Endpoints (ROLE_ADMIN)

- `/api/admin/**` - All admin endpoints

#### Authenticated Endpoints

- POST `/api/auth/logout` - Logout
- POST `/api/auth/refresh` - Refresh token

## Configuration

### Environment Variables

Add these to your `.env` file:

```properties
# JWT Configuration
JWT_SECRET=your-256-bit-secret-key-change-this-in-production-environment-please-use-a-strong-secret
```

**Important:** Generate a strong secret key for production:

```bash
openssl rand -base64 64
```

### Application Properties

Already configured in `application.yaml`:

```yaml
jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 3600000 # 1 hour in milliseconds
  refresh-token-expiration: 604800000 # 7 days in milliseconds

otp:
  expiration-minutes: 10
```

## Security Implementation Details

### Password Hashing

- **Algorithm**: BCrypt with 12 rounds
- Passwords are hashed before storing in database
- Never store plain text passwords

### JWT Token Structure

**Access Token Claims:**

```json
{
  "sub": "john.doe@example.com",
  "userId": 1,
  "role": "BIDDER",
  "iat": 1701734400,
  "exp": 1701738000
}
```

**Refresh Token Claims:**

```json
{
  "sub": "john.doe@example.com",
  "userId": 1,
  "type": "refresh",
  "iat": 1701734400,
  "exp": 1702339200
}
```

### Security Filter Chain

1. **JwtAuthenticationFilter**: Validates JWT token and sets authentication
2. **UsernamePasswordAuthenticationFilter**: Handles form-based authentication
3. **SecurityConfig**: Configures endpoint access rules

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    address TEXT,
    birth_date DATE,
    role_id SMALLINT NOT NULL REFERENCES roles(id),
    seller_expires_at TIMESTAMP WITH TIME ZONE,
    seller_upgraded_by BIGINT REFERENCES users(id),
    positive_rating INTEGER DEFAULT 0,
    negative_rating INTEGER DEFAULT 0,
    email_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### Roles Table

```sql
CREATE TABLE roles (
    id SMALLINT PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL
);

INSERT INTO roles (id, name) VALUES
(1, 'ADMIN'),
(2, 'SELLER'),
(3, 'BIDDER');
```

### Email OTPs Table

```sql
CREATE TABLE email_otps (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_otps_lookup ON email_otps(email, otp_code, purpose, is_used);
```

### Refresh Tokens Table

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token TEXT NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(token)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
```

## Error Handling

### Common Error Responses

**400 Bad Request - Validation Error:**

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email must be valid"
    },
    {
      "field": "password",
      "message": "Password must contain at least one uppercase letter"
    }
  ]
}
```

**401 Unauthorized:**

```json
{
  "success": false,
  "message": "Invalid credentials or account inactive"
}
```

**403 Forbidden:**

```json
{
  "success": false,
  "message": "Access denied. Insufficient permissions."
}
```

## Testing with Swagger UI

1. Start the application: `mvn spring-boot:run`
2. Open Swagger UI: http://localhost:8080/swagger-ui.html
3. Register a new user
4. Login and copy the `accessToken`
5. Click the **Authorize** button (🔒) at the top
6. Enter: `Bearer YOUR_ACCESS_TOKEN`
7. Click **Authorize**
8. Now you can test protected endpoints

## Security Best Practices

### ✅ Implemented

- BCrypt password hashing with 12 rounds
- JWT with HMAC-SHA256 signing
- Stateless authentication
- Token expiration
- Refresh token rotation
- CSRF protection disabled (JWT-based)
- Role-based access control
- Email verification with OTP

### 🔒 TODO for Production

1. **reCAPTCHA Integration**: Implement Google reCAPTCHA v2 verification
2. **Email Service**: Configure SMTP/SendGrid/AWS SES for OTP emails
3. **Rate Limiting**: Add rate limiting to prevent brute force attacks
4. **HTTPS**: Enable HTTPS in production
5. **Secret Management**: Use environment variables or secret managers
6. **Token Blacklisting**: Implement token blacklist for logout
7. **Account Lockout**: Lock account after N failed login attempts
8. **Password Reset**: Implement forgot password functionality
9. **2FA**: Optional two-factor authentication
10. **Audit Logging**: Log all authentication events

## Troubleshooting

### Issue: "Invalid or expired token"

- **Cause**: Access token expired (1 hour)
- **Solution**: Use refresh token endpoint to get a new access token

### Issue: "Email already exists"

- **Cause**: Attempting to register with existing email
- **Solution**: Use login instead or use a different email

### Issue: "Invalid or expired OTP"

- **Cause**: OTP expired (10 minutes) or incorrect
- **Solution**: Use resend OTP endpoint to get a new code

### Issue: "User does not have seller permission"

- **Cause**: Trying to create product with BIDDER role
- **Solution**: Request seller upgrade from admin

## Sample cURL Commands

### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "Password123!",
    "fullName": "John Doe",
    "address": "123 Main St",
    "birthDate": "1990-05-15",
    "recaptchaToken": "dummy-token"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "Password123!"
  }'
```

### Create Product (with JWT)

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": 5,
    "title": "iPhone 15 Pro Max",
    "description": "Brand new iPhone",
    "startingPrice": 20000000,
    "priceStep": 100000,
    "endTime": "2025-12-15T23:59:59+07:00",
    "autoExtend": true,
    "images": [
      {"imageUrl": "https://example.com/img1.jpg", "isPrimary": true, "sortOrder": 1},
      {"imageUrl": "https://example.com/img2.jpg", "isPrimary": false, "sortOrder": 2},
      {"imageUrl": "https://example.com/img3.jpg", "isPrimary": false, "sortOrder": 3}
    ]
  }'
```

## Additional Resources

- [JWT.io](https://jwt.io/) - JWT debugger
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [JJWT Documentation](https://github.com/jwtk/jjwt)
- [BCrypt Calculator](https://bcrypt-generator.com/)
