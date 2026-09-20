# my-ecommerce

This repository contains independently deployable services for the e-commerce platform:

```text
my-ecommerce/
├── auth-service/          # users, passwords, roles, permissions, JWTs
├── catalog-service/       # products, categories, variants, images
├── inventory-service/     # locations, units, reservations, movements
└── ecommerce-admin-ui/    # Next.js operations UI
```

## Local ports

| Component | Port |
| --- | ---: |
| Auth service | 8085 |
| Catalog service | 8081 |
| Inventory service | 8082 |
| Next.js UI | 3000 |
| Auth PostgreSQL | 5434 |
| Catalog PostgreSQL | 5432 |
| Inventory PostgreSQL | 5433 |

Authentication is owned only by `auth-service`. It issues short-lived RS256 access
tokens and rotates opaque, hashed refresh tokens. Catalog and Inventory validate
the access token locally using the Auth service's public JWKS endpoint; they do not
connect to the Auth database or call Auth for every request.

Public catalog reads remain available without a token. Catalog mutations require
catalog permissions. Administrative Inventory APIs require a bearer token and the
specific inventory permission for the operation. The internal Catalog SKU lookup
uses a separate configured service secret (`CATALOG_SERVICE_TOKEN` on Inventory,
`INVENTORY_SERVICE_TOKEN` on Catalog), not the human `INVENTORY_READ` permission.

See [auth-service/README.md](auth-service/README.md) for the complete authentication,
authorization, key management, local development, and security-testing guide.

## Start locally

1. Start Auth PostgreSQL and Auth service from `auth-service`.
2. Start Catalog PostgreSQL and Catalog from `catalog-service`.
3. Start Inventory PostgreSQL and Inventory from `inventory-service`.
4. Start the web application from `ecommerce-admin-ui`.

Each service has its own `docker-compose.yml`; no database is shared between services.

```bash
cd auth-service && docker compose up -d auth-db
mvn -s /tmp/maven-clean-settings.xml -gs /tmp/maven-clean-settings.xml spring-boot:run
```

The equivalent service-specific commands and environment variables are documented
in each service README.

### Required local service-to-service authentication

Inventory validates each SKU through Catalog's protected internal endpoint. The
two services must be started with the same development-only shared secret. This
is separate from a user's JWT and from the `INVENTORY_READ` permission.

Start Catalog with the token:

```bash
cd catalog-service
INVENTORY_SERVICE_TOKEN=dev-inventory-token mvn spring-boot:run
```

Start Inventory with the same token:

```bash
cd inventory-service
CATALOG_SERVICE_TOKEN=dev-inventory-token \
CATALOG_SERVICE_URL=http://localhost:8081 \
mvn spring-boot:run
```

If this token is missing or different between the services, Inventory can be
healthy while SKU requests return `503`, because its Catalog lookup fails. Check
the internal service call directly:

```bash
curl -i \
  -H 'X-Inventory-Service-Token: dev-inventory-token' \
  http://localhost:8081/internal/catalog/skus/<SKU>
```

This should return `200` for an existing active SKU. The browser still needs a
fresh Auth JWT with the required Inventory permission for administrative API
requests.

### Admin dashboard data coverage

The dashboard shows Catalog product/category counts, an active-product count,
and Catalog request health. Inventory dashboard cards are loaded from the
authenticated `GET /api/v1/inventory/summary` endpoint, which calculates total,
available, reserved, damaged, and active-location counts from physical
`InventoryUnit` records. Catalog status and Inventory status are separate; a
healthy Catalog does not prove that Inventory is available.
