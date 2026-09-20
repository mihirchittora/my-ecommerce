# E-commerce Catalog Service

A production-oriented Product Catalog module for an e-commerce application.

## Stack

- Java 21
- Spring Boot 3.5.6
- Spring Web
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Flyway
- SpringDoc OpenAPI / Swagger UI
- JUnit 5 + MockMvc + Testcontainers 2.0.5 (BOM-managed)
- Docker / Docker Compose, including Colima on macOS

## Features

- Recursive category hierarchy with parent/child validation
- Category and product CRUD
- Product variants / globally unique SKU model
- Optional product expiry dates returned as ISO `YYYY-MM-DD` values
- Product search, category filtering, pagination, and sorting
- Bean validation and business validation with structured errors
- Database-level SKU uniqueness and duplicate-SKU conflict handling
- Local JPEG, PNG, and WEBP image upload, download, and deletion
- Product-level or variant-level image association
- Flyway migrations
- PostgreSQL integration tests using Testcontainers

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Engine, Docker Desktop, or Colima

On macOS with Colima, start the runtime before running the application or tests:

```bash
colima start --network-address
docker context use colima
docker info
```

The repository detects the default Colima socket automatically. No `DOCKER_HOST`,
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`, or `TESTCONTAINERS_HOST_OVERRIDE` exports
are required for the normal local workflow.

## Run locally on macOS

### Start PostgreSQL

The application expects PostgreSQL on `localhost:5432`:

```bash
docker-compose up -d postgres
docker-compose ps
```

If your Docker installation provides the Compose v2 plugin, the equivalent command is:

```bash
docker compose up -d postgres
```

### Start the application

```bash
mvn spring-boot:run
```

Application URLs:

- API: http://localhost:8081
- Health: http://localhost:8081/actuator/health
- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs

Stop the PostgreSQL service when it is no longer needed:

```bash
docker-compose stop postgres
```

The Docker Compose volume is retained by this command.

## Test locally

Integration tests use Testcontainers 2.0.5 and start an isolated `postgres:17`
container. The Compose PostgreSQL service is not required for the test suite.

With Colima or Docker Desktop running:

```bash
mvn clean test
```

The test configuration is repository-owned and detects these local sockets without
requiring shell setup:

- Colima: `~/.colima/default/docker.sock`
- Docker Desktop: `~/.docker/run/docker.sock`

If your shell already contains stale Docker variables, run:

```bash
env -u DOCKER_HOST \
  -u TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE \
  -u TESTCONTAINERS_HOST_OVERRIDE \
  mvn clean test
```

For explicit Colima configuration, use Testcontainers' documented variables:

```bash
export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export TESTCONTAINERS_HOST_OVERRIDE="$(colima ls -j | jq -r '.address')"
mvn clean test
```

The integration suite covers product CRUD, pagination and filtering, validation,
category hierarchy rules, SKU uniqueness, and image upload/download/deletion.

## Database configuration

Default local database settings:

- Database: `ecommerce`
- Username: `ecommerce`
- Password: `ecommerce`
- Host: `localhost`
- Port: `5432`

Override these values with environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
PORT
IMAGE_STORAGE_DIR
IMAGE_MAX_SIZE_BYTES
```

The default image storage directory is `./uploads`. Image binaries are stored on
the local filesystem; PostgreSQL stores image metadata and storage keys only.

## Authentication and authorization

Catalog validates Auth service RS256 bearer tokens locally through
`AUTH_JWK_SET_URI` (default `http://localhost:8085/.well-known/jwks.json`) and
checks `AUTH_ISSUER` and `AUTH_AUDIENCE`. Public GET product/category browsing and
image-file reads remain anonymous. Mutations require these permissions:

```text
POST product                         PRODUCT_CREATE
PUT product                          PRODUCT_UPDATE
DELETE product                       PRODUCT_DELETE
POST/PUT/DELETE category              CATEGORY_CREATE/UPDATE/DELETE
POST product image                    PRODUCT_IMAGE_UPLOAD
DELETE product image                 PRODUCT_IMAGE_DELETE
GET /internal/catalog/skus/{sku}     SERVICE_INVENTORY (service secret header)
```

Catalog never receives or stores the Auth private key. It uses Spring Security's
resource-server JWKS caching. Set `APP_SECURITY_ENABLED=false` only for isolated
legacy domain tests; the production default is `true`.

The internal SKU lookup is not authorized with the human `INVENTORY_READ`
permission. Inventory sends the configured `CATALOG_SERVICE_TOKEN` as the
`X-Inventory-Service-Token` header; Catalog validates the matching
`INVENTORY_SERVICE_TOKEN` secret and grants only the `SERVICE_INVENTORY` service
authority for this endpoint. User/admin authentication remains JWT + permissions.

## Core APIs

### Categories

```text
POST   /api/v1/categories
GET    /api/v1/categories/{id}
GET    /api/v1/categories?parentId={id}
PUT    /api/v1/categories/{id}
DELETE /api/v1/categories/{id}
```

Categories reject blank names, invalid or duplicate sibling slugs, hierarchy cycles,
self-parenting, and deletion while children or products still exist.

### Products

```text
POST   /api/v1/products
GET    /api/v1/products/{id}
GET    /api/v1/products
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}
```

Pagination and sorting use separate query parameters. `sort` is a string in
`property,direction` format:

```text
GET /api/v1/products
GET /api/v1/products?page=0&size=20&sort=name,asc
GET /api/v1/products?categoryId=<uuid>&page=0&size=20&sort=name,asc
GET /api/v1/products?search=iphone&page=0&size=20&sort=name,asc
```

Defaults are `page=0`, `size=20`, and `sort=name,asc`. The maximum page size is 100.

Allowed sort properties:

- `name`
- `brand`
- `slug`
- `createdAt`
- `updatedAt`

Product and variant validation rejects blank names/SKUs, bogus values such as
`string`, non-positive prices, unsupported currencies, and nonexistent categories.
Duplicate SKUs return HTTP 409 Conflict.

Products may include an optional `expiryDate` in `YYYY-MM-DD` format. The date is
stored at product level and is returned by all product endpoints; omitting it
keeps the product non-expiring. The admin UI highlights products whose expiry date
is before the current date.

For the standalone Inventory service, Catalog exposes this internal read-only
SKU contract:

```text
GET /internal/catalog/skus/{sku}
```

It returns the normalized SKU, `variantId`, `productId`, and whether both the
product and variant are active.

### Product images

```text
POST   /api/v1/products/{productId}/images
GET    /api/v1/products/{productId}/images/{imageId}/file
DELETE /api/v1/products/{productId}/images/{imageId}
```

Upload an image with multipart form data:

```bash
curl -X POST \
  -F 'file=@./product.png' \
  'http://localhost:8081/api/v1/products/<product-id>/images'
```

Supported formats are JPEG, PNG, and WEBP. The maximum file size is 5 MB, and the
server generates the storage filename.

## Quick verification

```bash
curl -fsS http://localhost:8081/actuator/health
curl -i http://localhost:8081/api/v1/products
curl -i 'http://localhost:8081/api/v1/products?page=0&size=20&sort=name,asc'
curl -i 'http://localhost:8081/api/v1/products?categoryId=<category-id>&page=0&size=20&sort=name,asc'
```

Swagger should show `page`, `size`, and `sort` as separate query parameters with
useful defaults. A request without `sort` is valid.

## Database migrations

Flyway migrations live in:

```text
src/main/resources/db/migration
```

The current migrations are:

- `V1__init_catalog.sql` — initial catalog schema
- `V2__catalog_hardening_and_images.sql` — validation, indexes, and image metadata
- `V3__catalog_integrity_constraints.sql` — database integrity constraints, including global SKU uniqueness

Hibernate runs with `ddl-auto=validate`; schema changes must be made through Flyway.

## Error responses

The API returns structured JSON errors without stack traces. Common statuses are:

- `400 Bad Request` — validation, malformed pagination, unsupported currency/image, or invalid hierarchy request
- `404 Not Found` — missing product, category, variant, or image
- `409 Conflict` — duplicate SKU, duplicate sibling category, or prohibited category operation

## Next module

The intended next domain module is Inventory Management. Inventory should reference
the sellable `ProductVariant` by SKU and maintain quantities such as:

```text
onHand
reserved
available
```
