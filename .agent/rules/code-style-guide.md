---
trigger: always_on
---


## Exception Handling

1. **Never use generic `RuntimeException`** - Only use `RuntimeException` for truly unexpected/generic errors that don't fall into any category
   
2. **Use appropriate custom exceptions:**
   - `ResourceNotFoundException` → 404 Not Found (for missing resources: users, products, categories)
   - `BadRequestException` → 400 Bad Request (for validation errors and business rule violations)
   - `UnauthorizedSellerException` → 403 Forbidden (for seller permission violations)
   - `ProductEndedException` → 410 Gone (for operations on ended auctions)
   - `InvalidBidAmountException` → 400 Bad Request (for bid validation errors)
   - `AccountInactiveException` → 403 Forbidden (for inactive account access)
   - ...

3. **When creating new custom exceptions:**
   - Create the exception class in `src/main/java/com/taitrinh/online_auction/exception/`
   - Add an `@ExceptionHandler` method in `GlobalExceptionHandler.java`
   - Return appropriate HTTP status code
   - Use Vietnamese error messages for user-facing errors

4. **Exception constructor patterns:**
   - Provide multiple constructors for different ID types (Long, Integer, String)
   - Follow existing patterns in `ResourceNotFoundException`

---

## Security Configuration

5. **When creating new API endpoints:**
   - Update `SecurityConfig.java` if the endpoint needs to be public/unauthenticated
   - Use `.permitAll()` for public endpoints (e.g., product viewing, registration, password reset)
   - Use `.authenticated()` for endpoints requiring login
   - Use role-based access for admin endpoints (e.g., `.hasRole("ADMIN")`)

6. **Authentication patterns:**
   - Use `@PreAuthorize("hasRole('SELLER')")` for seller-only endpoints
   - Use `@PreAuthorize("hasRole('ADMIN')")` for admin-only endpoints
   - Extract user ID from `UserDetailsImpl` in controllers using `@AuthenticationPrincipal`

---

## Testing & Development Workflow

7. **Don't compile code to test** - Just apply the code changes. The user will test and report specific errors

8. **When user reports errors:**
   - Wait for specific error messages/line numbers
   - Fix only what's reported
   - Don't make assumptions about other potential issues

---

## Database & Transactions

9. **Transaction handling:**
    - Use `@Transactional` for methods that modify data
    - Use `@Transactional(readOnly = true)` for read-only operations
    - Use `@Transactional(propagation = Propagation.REQUIRES_NEW)` for operations that should run in separate transactions

10. **Cascade operations:**
    - Configure cascade properly in entity relationships

---

## API Response Patterns

11. **Consistent response format:**
    - Use `ApiResponse<T>` wrapper for all API responses
    - Use `ErrorResponse<T>` wrapper for all errors (handle in global exception)
    - Include success status, data, message, and timestamp
    - Use appropriate HTTP status codes

12. **Pagination:**
    - Always paginate list endpoints

---

## Code Organization

13. **DTOs:**
    - Separate request and response DTOs
    - Use validation annotations (`@NotNull`, `@NotBlank`, etc.)
    - Keep DTOs focused and specific to use case

14. **Mappers:**
    - Use separate mapper classes/interfaces
    - One method per mapping type
    - Include all necessary data transformations

15. **Logging:**
    - Use `@Slf4j` annotation
    - Log important operations (create, update, delete)
    - Log errors with full stack traces
    - Don't log sensitive information (passwords, tokens)

---

## Business Rules

16. **Product creation:**
    - Minimum 3 images required
    - Only one primary image allowed
    - Products can only belong to leaf categories (not parent categories)
    - Only users with seller role can create products

17. **Category management:**
    - Maximum 2-level hierarchy (parent → child)
    - Cannot delete categories with products
    - Cannot delete parent categories with children
    - Category names must be unique at the same level

18. **Bidding rules:**
    - Cannot bid on own products
    - Cannot bid on ended auctions
    - Bid amount must follow price step rules
    - Seller can see all bidder names, others see masked names, the bidder can see his own name. The same rules apply for comment module

---

## Import Organization

19. **Import order:**
    - Java standard library imports
    - Third-party library imports (Spring, Lombok, etc.)
    - Project imports (grouped by: dto, entity, exception, mapper, repository, util)
    - Maintain alphabetical order within each group
