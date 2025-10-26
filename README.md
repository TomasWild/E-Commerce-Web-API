# Ecommerce API
A Spring Boot backend for an e-commerce platform. It provides authentication with email verification, 
product and category management, shopping cart, orders, shipping, Stripe payments, Redis caching, and MinIO S3-compatible 
object storage. The project is containerized via Docker and ships with a full local stack: MySQL, Redis, Mailpit, 
MinIO, and Stripe CLI.

## Features
- JWT authentication (register, login) with email verification via Mailpit
- Role-based access control (admin-only endpoints for product/category management)
- Products, Categories, Cart, Orders, Address, Shipment modules
- Stripe payments with webhook listener
- Redis caching
- S3-compatible storage (MinIO by default)
- Actuator health/info/metrics endpoints
- OpenAPI/Swagger UI docs

## Tech stack
- Java 25, Spring Boot (Web, Security, JPA, Validation, Mail, Actuator)
- MySQL, Redis
- Stripe, MinIO (S3-compatible)
- Gradle
- Testcontainers (for testing)
- OpenAPI via springdoc-openapi

## Project modules and base routes
Security rules (from SecurityConfig):
- Swagger/OpenAPI: `/swagger-ui/**`, `/v3/api-docs/**` are public
- Auth: `/api/v1/auth/**` is public (register, login, verify, resend)
- Stripe Webhooks: `/api/v1/stripe/webhooks/**` is public (for Stripe)
- Users: `/api/v1/users/**` requires authentication
- Categories: `GET /api/v1/categories/**` public; `POST|PUT|DELETE` require role `ADMIN`
- Products: `GET /api/v1/products/**` public; `POST|PUT|DELETE` require role `ADMIN`
- Carts, Addresses, Orders, Shipping: `/api/v1/{carts|addresses|orders|shipping}/**` require authentication

Auth endpoints:
- POST `/api/v1/auth/register`
- POST `/api/v1/auth/login`
- GET `/api/v1/auth/verify?token=...`
- POST `/api/v1/auth/resend-verification?email=...`

When authenticated, pass the JWT using:
- Header: `Authorization: Bearer <token>`

## API documentation
Once the app is running:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Quick start with Docker (recommended)
Prerequisites:
- Docker Desktop

1) Create a `.env` file in the project root with at least:

```
# App
APP_BASE_URL=http://localhost:8080

# MySQL
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=ecommerce
MYSQL_USER=ecommerce
MYSQL_PASSWORD=ecommerce

# JWT
JWT_SECRET=change-me-to-a-long-random-string
JWT_EXPIRATION=3600000                 # 1 hour in ms
JWT_REFRESH_EXPIRATION=2592000000      # 30 days in ms

# MinIO (S3-compatible)
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
# App uses these for S3 access – you can reuse the root creds
MINIO_ENDPOINT=http://minio:9000
MINIO_BUCKET_NAME=ecommerce
MINIO_ACCESS_KEY=${MINIO_ROOT_USER}
MINIO_SECRET_KEY=${MINIO_ROOT_PASSWORD}

# Stripe
STRIPE_API_KEY=sk_test_xxx
# If you don’t verify signatures (stripe-cli uses --skip-verify), this can be blank
STRIPE_WEBHOOK_SECRET=

# Optional
REDIS_PASSWORD=
```

2) Start the stack:

- On Windows (PowerShell): `docker-compose up -d` or `docker compose up -d`

This brings up:
- App: http://localhost:8080
- MySQL: localhost:3306
- Redis: localhost:6379
- Mailpit (UI/SMTP): http://localhost:8025 (SMTP on 1025)
- MinIO (API/Console): http://localhost:9000 and http://localhost:9001
- Stripe CLI webhook forwarder (for local webhooks)

3) Open Swagger UI: http://localhost:8080/swagger-ui/index.html

Email verification: open http://localhost:8025, find the email, and click the verification link.

Bucket note: ensure the `MINIO_BUCKET_NAME` exists. If not auto-created in service code, create it via MinIO Console 
(http://localhost:9001).

## Running locally without Docker
Prerequisites:
- JDK 25
- MySQL 9+
- Redis 8+
- Mailpit (optional for email testing)
- MinIO or any S3-compatible storage

1) Set environment variables (PowerShell example):

```
$env:APP_BASE_URL="http://localhost:8080"
$env:MYSQL_DB="ecommerce"
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="root"
$env:JWT_SECRET="change-me"
$env:JWT_EXPIRATION="3600000"
$env:JWT_REFRESH_EXPIRATION="2592000000"
$env:MINIO_ENDPOINT="http://localhost:9000"
$env:MINIO_BUCKET_NAME="ecommerce"
$env:MINIO_ACCESS_KEY="minioadmin"
$env:MINIO_SECRET_KEY="minioadmin"
$env:STRIPE_API_KEY="sk_test_xxx"
$env:STRIPE_WEBHOOK_SECRET=""
```

Also, ensure your local services are running at:
- MySQL: `jdbc:mysql://localhost:3306/ecommerce` (ensure database exists)
- Redis: `localhost:6379`
- Mailpit SMTP: host `localhost`, port `1025` (project defaults)

2) Start the app:

- Windows: `./gradlew.bat bootRun`

## Configuration reference (application.properties)
Key properties sourced from environment variables:
- `app.base-url` -> `APP_BASE_URL`
- `spring.datasource.url` -> `MYSQL_DB` (local) or overridden by `SPRING_DATASOURCE_URL` in Docker
- `spring.datasource.username` -> `MYSQL_USER`
- `spring.datasource.password` -> `MYSQL_PASSWORD`
- `spring.data.redis.host`/`port` -> `SPRING_DATA_REDIS_HOST`/`SPRING_DATA_REDIS_PORT` in Docker
- `spring.mail.host` -> `MAIL_HOST` (default: localhost)
- `spring.mail.port` -> `MAIL_PORT` (default: 1025)
- `security.jwt.secret` -> `JWT_SECRET`
- `security.jwt.expiration` -> `JWT_EXPIRATION`
- `security.jwt.refresh-token-expiration` -> `JWT_REFRESH_EXPIRATION`
- `minio.s3.endpoint` -> `MINIO_ENDPOINT`
- `minio.s3.bucket_name` -> `MINIO_BUCKET_NAME`
- `minio.s3.access_key` -> `MINIO_ACCESS_KEY`
- `minio.s3.secret_key` -> `MINIO_SECRET_KEY`
- `stripe.api_key` -> `STRIPE_API_KEY`
- `stripe.webhook.secret` -> `STRIPE_WEBHOOK_SECRET`

## Building, testing, and running
- Build: `./gradlew build`
- Run tests: `./gradlew test`
- Run app: `./gradlew bootRun`
- Create Docker image (compose does this automatically): `docker build -t ecommerce .`

The provided Dockerfile builds the app using Gradle and runs the resulting JAR on OpenJDK 25.
