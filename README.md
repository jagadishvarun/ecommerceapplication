# Ecommerce Backend

A REST API for an e-commerce platform built with Spring Boot 4, PostgreSQL, and JWT authentication.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.6 |
| Language | Java 17 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT |
| Build | Maven |
| Container | Docker / Kubernetes |

## Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven (or use the included `mvnw` wrapper)

### Run Locally

```bash
# Start PostgreSQL
docker-compose up -d

# Run the application
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

## API Overview

### Authentication (public)

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive a JWT |

### Products (GET endpoints public, write endpoints require ADMIN)

| Method | Path | Description |
|---|---|---|
| GET | `/api/products` | List products (`?search=&category=&page=&size=`) |
| GET | `/api/products/{id}` | Get a single product |
| POST | `/api/products` | Create a product |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Soft-delete a product |

**Categories:** `ELECTRONICS`, `CLOTHING`, `BOOKS`, `HOME`, `SPORTS`, `BEAUTY`, `FOOD`, `TOYS`, `AUTOMOTIVE`, `OTHER`

### Cart (requires JWT)

| Method | Path | Description |
|---|---|---|
| GET | `/api/cart` | Get the user's cart |
| POST | `/api/cart/items` | Add an item |
| PUT | `/api/cart/items/{itemId}` | Update item quantity |
| DELETE | `/api/cart/items/{itemId}` | Remove an item |
| DELETE | `/api/cart` | Clear the cart |

### Orders (requires JWT)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/orders` | User | Place order from cart |
| GET | `/api/orders/my` | User | Own order history |
| GET | `/api/orders/my/{id}` | User | Single own order |
| GET | `/api/orders` | ADMIN | All orders |
| PATCH | `/api/orders/{id}/status` | ADMIN | Update order status |

**Status flow:** `PENDING` → `CONFIRMED` → `SHIPPED` → `DELIVERED` / `CANCELLED`

### Payments (requires JWT)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/payments` | User | Pay for a pending order |
| GET | `/api/payments/order/{orderId}` | User | Get payment by order |
| GET | `/api/payments` | ADMIN | All payments |
| POST | `/api/payments/{id}/refund` | ADMIN | Refund a payment |

**Payment methods:** `CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `NET_BANKING`, `WALLET`

## Authentication

Include the JWT from `/api/auth/login` in all protected requests:

```
Authorization: Bearer <token>
```

## Error Responses

All errors return a consistent JSON shape:

```json
{ "error": "message" }
```

Validation errors include field-level detail:

```json
{ "fieldName": "constraint message" }
```

## Docker

```bash
# Build image
docker build -t ecommerce-api .

# Start with Docker Compose (app + PostgreSQL)
docker-compose up
```

## Kubernetes

Manifests are in the `k8s/` directory.

```bash
kubectl apply -f k8s/
```

## Postman

Import `Ecommerce.postman_collection.json` to get a ready-made collection of all API requests.