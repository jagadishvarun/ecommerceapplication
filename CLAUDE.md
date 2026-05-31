# Ecommerce Backend

Spring Boot 4 REST API for an e-commerce platform. Java 17, PostgreSQL, JWT authentication.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.6 |
| Language | Java 17 |
| Database | PostgreSQL 16 (port 5434) |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Validation | Jakarta Bean Validation |
| Boilerplate | Lombok |
| Build | Maven |

## Running Locally

```bash
# Start PostgreSQL
docker-compose up -d

# Run the application
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. Database schema is auto-managed via `ddl-auto=update`.

## Project Structure

```
src/main/java/com/example/ecommerce/
├── common/
│   ├── config/
│   │   ├── AppConfig.java           # PasswordEncoder bean
│   │   └── SecurityConfig.java      # JWT filter chain, @EnableMethodSecurity
│   ├── exception/
│   │   ├── ApiException.java        # RuntimeException with HttpStatus
│   │   └── GlobalExceptionHandler.java  # @RestControllerAdvice
│   └── security/
│       ├── JwtService.java          # Token generation and validation
│       └── JwtAuthenticationFilter.java # Per-request JWT extraction
├── user/                            # Auth module
├── product/                         # Product catalogue
├── cart/                            # Shopping cart
├── order/                           # Order placement and management
└── payment/                         # Payment processing
```

Each domain module follows the same structure: `entity/` → `repository/` → `dto/` → `service/` → `controller/`.

## Modules

### user
Handles registration, login, and Spring Security identity.

- `User` implements `UserDetails`; `getUsername()` returns email
- Roles: `ROLE_USER`, `ROLE_ADMIN`
- JWT secret and expiration configured in `application.properties`

**Endpoints** — public

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login, returns JWT |

### product
Product catalogue with soft delete and JPA Specification filtering.

- Products are never hard-deleted — `active = false` hides them
- Write operations (`POST`, `PUT`, `DELETE`) require `ROLE_ADMIN` via `@PreAuthorize`
- `GET` endpoints are public (permitted in `SecurityConfig`)
- Stock is decremented atomically via `@Modifying` query: `UPDATE ... WHERE stockQuantity >= qty`

**Categories:** `ELECTRONICS`, `CLOTHING`, `BOOKS`, `HOME`, `SPORTS`, `BEAUTY`, `FOOD`, `TOYS`, `AUTOMOTIVE`, `OTHER`

**Endpoints**

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/products` | Public | List with `?search=&category=&page=&size=` |
| GET | `/api/products/{id}` | Public | Single product |
| POST | `/api/products` | ADMIN | Create |
| PUT | `/api/products/{id}` | ADMIN | Update |
| DELETE | `/api/products/{id}` | ADMIN | Soft delete |

### cart
Persistent cart — one per user, created lazily on first access.

- `CartItem` has a unique constraint on `(cart_id, product_id)`
- Adding an already-present product increments quantity rather than creating a duplicate
- `unitPrice` is snapshotted at add-time — price changes don't affect the cart
- Stock is validated on add and update

**Endpoints** — require JWT

| Method | Path | Description |
|---|---|---|
| GET | `/api/cart` | Get (or create) the user's cart |
| POST | `/api/cart/items` | Add item |
| PUT | `/api/cart/items/{itemId}` | Update quantity |
| DELETE | `/api/cart/items/{itemId}` | Remove item |
| DELETE | `/api/cart` | Clear cart |

### order
Order placed from cart contents. Product data is snapshotted into `OrderItem`.

- Placing an order: validates cart not empty → decrements stock atomically → creates order → clears cart
- `OrderItem` stores `productId`, `productName`, `productImageUrl`, `unitPrice` as snapshots — unaffected by future product edits or deletes
- Status flow: `PENDING` → `CONFIRMED` → `SHIPPED` → `DELIVERED` or `CANCELLED`
- Updating a `DELIVERED` or `CANCELLED` order is blocked

**Endpoints**

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/orders` | User | Place order from cart |
| GET | `/api/orders/my` | User | Paginated own order history |
| GET | `/api/orders/my/{id}` | User | Single own order |
| GET | `/api/orders` | ADMIN | All orders paginated |
| PATCH | `/api/orders/{id}/status` | ADMIN | Update order status |

### payment
Simulated payment processing (no external gateway). Swap `UUID.randomUUID()` for a real gateway reference when integrating Stripe/Razorpay.

- One payment per order (`@OneToOne`)
- Paying a PENDING order marks it `CONFIRMED`
- A prior `FAILED` payment does not block a new payment attempt
- Refund (admin only): marks payment `REFUNDED` and order `CANCELLED`

**Payment methods:** `CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `NET_BANKING`, `WALLET`

**Endpoints**

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/payments` | User | Pay for a PENDING order |
| GET | `/api/payments/order/{orderId}` | User | Get own payment by order |
| GET | `/api/payments` | ADMIN | All payments, newest first |
| POST | `/api/payments/{id}/refund` | ADMIN | Refund payment |

## Security

- All `/api/auth/**` routes are public
- `GET /api/products` and `GET /api/products/**` are public
- Everything else requires a valid JWT in the `Authorization: Bearer <token>` header
- Admin-only operations are guarded with `@PreAuthorize("hasRole('ADMIN')")` on the service layer
- Method security is enabled via `@EnableMethodSecurity` in `SecurityConfig`

## Error Handling

All errors return a consistent JSON shape:

```json
{ "error": "message" }
```

Validation errors return field-level messages:

```json
{ "fieldName": "constraint message" }
```

`ApiException(message, HttpStatus)` is the standard way to throw domain errors from any service.

## Database

- PostgreSQL running on `localhost:5434` (mapped from container port 5432)
- Credentials: `ecommerce_user` / `ecommerce_pass`, database `ecommerce`
- Schema is managed automatically by Hibernate (`ddl-auto=update`)
- `docker-compose.yml` at the project root starts a named volume `postgres_data` for persistence