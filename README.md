# my-ecommerce

This repository contains independently deployable services for the e-commerce platform:

```text
my-ecommerce/
├── auth-service/          # users, passwords, roles, permissions, JWTs
├── catalog-service/       # products, categories, variants, images
├── inventory-service/     # locations, units, reservations, movements
├── order-service/         # historical orders, snapshots, idempotency, orchestration
├── cart-service/          # customer-owned mutable SKU carts
├── customer-service/      # customer profiles and current addresses
├── payment-service/       # payment orchestration, gateway references, attempts, refunds
└── ecommerce-admin-ui/    # Next.js operations UI
```

## Local ports

| Component | Container name | Port |
| --- | --- | ---: |
| Auth service | `ecommerce-auth-service` | 8085 |
| Catalog service | `ecommerce-catalog-service` | 8081 |
| Inventory service | `ecommerce-inventory-service` | 8082 |
| Order service | `ecommerce-order-service` | 8083 |
| Cart service | `ecommerce-cart-service` | 8084 |
| Customer service | `ecommerce-customer-service` | 8086 |
| Payment service | `ecommerce-payment-service` | 8087 |
| Next.js UI | — | 3000 |
| Auth PostgreSQL | `ecommerce-auth-db` | 5434 |
| Catalog PostgreSQL | `ecommerce-catalog-db` | 5432 |
| Inventory PostgreSQL | `ecommerce-inventory-db` | 5433 |
| Order PostgreSQL | `ecommerce-order-db` | 5435 |
| Cart PostgreSQL | `ecommerce-cart-db` | 5436 |
| Customer PostgreSQL | `ecommerce-customer-db` | 5437 |

Authentication is owned only by `auth-service`. It issues short-lived RS256 access
tokens and rotates opaque, hashed refresh tokens. Catalog and Inventory validate
the access token locally using the Auth service's public JWKS endpoint; they do not
connect to the Auth database or call Auth for every request.

Public catalog reads remain available without a token. Catalog mutations require
catalog permissions. Administrative Inventory APIs require a bearer token and the
specific inventory permission for the operation. The internal Catalog SKU lookup
uses a separate configured service secret (`CATALOG_SERVICE_TOKEN` on Inventory,
`ORDER_SERVICE_TOKEN` on Order, or `CART_SERVICE_TOKEN` on Cart), not a human
inventory permission.

Cart is a separate customer-facing service. It stores only SKU and quantity in
its own database, enriches reads from Catalog, and delegates checkout to Order.
Adding to Cart never reserves Inventory; Order coordinates reservation during
checkout. See [cart-service/README.md](cart-service/README.md) for its API,
lifecycle, integration contracts, and verification guide.

Customer is a separate customer-facing service. It owns the current customer
profile and address book in its own `customer_db`; Auth remains the source of
truth for identity and credentials. Customer identifies the profile from JWT
`sub`, never stores passwords or tokens, and never reads another service's
database. It lazily creates an idempotent profile on the first
`/api/v1/customers/me` call because Auth currently has no profile callback. See
[customer-service/README.md](customer-service/README.md) and
[docs/api-contracts.md](docs/api-contracts.md) for its contract.

Payment is a gateway-agnostic orchestration service. External gateways process the
payment; Payment Service owns payment state, provider references, attempts, verified
webhooks, and refunds. It never stores card numbers, CVV/PINs, gateway secrets, or
reads another service's database. Its internal operations endpoints require
`PAYMENT_READ`, `PAYMENT_REFUND`, or `PAYMENT_RETRY` as appropriate. See
[payment-service/README.md](payment-service/README.md) and
[docs/api-contracts.md](docs/api-contracts.md).

The internal admin UI exposes read-only Cart support at `/carts` and
`/carts/{id}` when the operator has `CART_READ`. Cart quantities, current
Inventory availability, and historical Order pricing remain separate concepts;
the admin UI does not provide Cart checkout or reservation mutations. See
[docs/api-contracts.md](docs/api-contracts.md) for the staff Cart contract.

See [auth-service/README.md](auth-service/README.md) for the complete authentication,
authorization, key management, local development, and security-testing guide.

## Start locally with Docker on macOS

Docker Desktop or Colima must be running. Verify Docker before starting:

```bash
docker info
```

Each backend has its own `docker-compose.yml` and database. The Compose files use
separate networks, so service-to-service URLs use `host.docker.internal` from
inside containers. The browser uses `localhost` because the UI runs on the Mac.

### Build JARs required by Auth and Order

The Auth and Order Dockerfiles copy an existing JAR from `target/`; their Docker
build does not run Maven. Rebuild these JARs whenever their Java source changes:

```bash
cd /Users/mihirchittora/Desktop/my-ecommerce
mvn -f auth-service/pom.xml -DskipTests package
mvn -f order-service/pom.xml -DskipTests package
docker-compose -f auth-service/docker-compose.yml build --no-cache auth-service
docker-compose -f order-service/docker-compose.yml build --no-cache order-service
```

Catalog and Inventory use multi-stage Dockerfiles that run Maven inside the image,
so a local Maven package step is not required for those two services. The
`--no-cache` build is important for Auth and Order because their Dockerfiles copy
the locally generated JAR into the image.

### Start Auth

The example creates a local admin account. Change the example password for your
own machine; never use these development credentials outside local development.

```bash
cd /Users/mihirchittora/Desktop/my-ecommerce/auth-service
AUTH_DB_NAME=auth_db \
AUTH_DB_USERNAME=postgres \
AUTH_DB_PASSWORD=postgres \
AUTH_ISSUER=http://localhost:8085 \
AUTH_AUDIENCE=ecommerce-api \
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001 \
INITIAL_ADMIN_EMAIL=admin@example.com \
INITIAL_ADMIN_PASSWORD='ChangeMe123!@#' \
docker-compose up -d --build
```

The bootstrap admin values are used only when the Auth database is initialized.
Changing them later does not reset an existing admin password.

### Start Catalog

```bash
cd /Users/mihirchittora/Desktop/my-ecommerce/catalog-service
AUTH_ISSUER=http://localhost:8085 \
AUTH_AUDIENCE=ecommerce-api \
AUTH_JWK_SET_URI=http://host.docker.internal:8085/.well-known/jwks.json \
INVENTORY_SERVICE_TOKEN=dev-inventory-to-catalog \
ORDER_SERVICE_TOKEN=dev-order-to-catalog \
docker-compose up -d --build
```

### Start Inventory

```bash
cd /Users/mihirchittora/Desktop/my-ecommerce/inventory-service
INVENTORY_DB_USERNAME=inventory \
INVENTORY_DB_PASSWORD=inventory \
CATALOG_SERVICE_URL=http://host.docker.internal:8081 \
AUTH_ISSUER=http://localhost:8085 \
AUTH_AUDIENCE=ecommerce-api \
AUTH_JWK_SET_URI=http://host.docker.internal:8085/.well-known/jwks.json \
CATALOG_SERVICE_TOKEN=dev-inventory-to-catalog \
ORDER_SERVICE_TOKEN=dev-order-to-inventory \
docker-compose up -d --build
```

### Start Order

```bash
cd /Users/mihirchittora/Desktop/my-ecommerce/order-service
CATALOG_SERVICE_URL=http://host.docker.internal:8081 \
INVENTORY_SERVICE_URL=http://host.docker.internal:8082 \
AUTH_ISSUER=http://localhost:8085 \
AUTH_AUDIENCE=ecommerce-api \
AUTH_JWK_SET_URI=http://host.docker.internal:8085/.well-known/jwks.json \
CATALOG_SERVICE_TOKEN=dev-order-to-catalog \
INVENTORY_SERVICE_TOKEN=dev-order-to-inventory \
docker-compose up -d --build --force-recreate
```

### Start Cart

```bash
cd /Users/mihirchittora/Desktop/my-ecommerce/cart-service
CATALOG_SERVICE_URL=http://host.docker.internal:8081 \
ORDER_SERVICE_URL=http://host.docker.internal:8083 \
AUTH_ISSUER=http://localhost:8085 \
AUTH_AUDIENCE=ecommerce-api \
AUTH_JWK_SET_URI=http://host.docker.internal:8085/.well-known/jwks.json \
CART_CATALOG_SERVICE_TOKEN=dev-cart-to-catalog \
docker-compose up -d --build
```

### Start Customer

```bash
cd /Users/mihirchittora/Desktop/my-ecommerce/customer-service
AUTH_ISSUER=http://localhost:8085 \
AUTH_AUDIENCE=ecommerce-api \
AUTH_JWK_SET_URI=http://host.docker.internal:8085/.well-known/jwks.json \
docker-compose up -d --build
```

### Start Payment

```bash
cd /Users/mihirchittora/Desktop/my-ecommerce/payment-service
AUTH_ISSUER=http://localhost:8085 \
AUTH_AUDIENCE=ecommerce-api \
AUTH_JWK_SET_URI=http://host.docker.internal:8085/.well-known/jwks.json \
ORDER_SERVICE_URL=http://host.docker.internal:8083 \
docker-compose up -d --build
```

### Start the admin UI

```bash
cd /Users/mihirchittora/Desktop/my-ecommerce/ecommerce-admin-ui
cp .env.example .env.local   # only needed the first time
npm install                  # only needed the first time
npm run dev
```

Open <http://localhost:3000> and sign in with the Auth admin credentials.

### Verify and stop the stack

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

curl -fsS http://localhost:8085/actuator/health
curl -fsS http://localhost:8081/actuator/health
curl -fsS http://localhost:8082/actuator/health
curl -fsS http://localhost:8083/actuator/health
curl -fsS http://localhost:8084/actuator/health
curl -fsS http://localhost:8086/actuator/health
curl -fsS http://localhost:8087/actuator/health
```

Stop a service without deleting its database volume:

```bash
(cd auth-service && docker-compose down)
(cd catalog-service && docker-compose down)
(cd inventory-service && docker-compose down)
(cd order-service && docker-compose down)
(cd cart-service && docker-compose down)
(cd customer-service && docker-compose down)
(cd payment-service && docker-compose down)
```

Use `docker-compose down -v` only when you intentionally want to delete that
service's local database data and start from an empty database.

### Environment variables and local values

The values below are for local development only. Compose defaults most database,
port, and URL settings, but the explicit values below make the Auth issuer,
browser URLs, service credentials, and initial admin account clear.

#### Auth service

| Variable | Local value | Relevance |
| --- | --- | --- |
| `AUTH_DB_NAME` | `auth_db` | Auth PostgreSQL database name. |
| `AUTH_DB_USERNAME` | `postgres` | Auth PostgreSQL user. |
| `AUTH_DB_PASSWORD` | `postgres` | Auth PostgreSQL local password. |
| `AUTH_ISSUER` | `http://localhost:8085` | JWT `iss` claim; downstream services must use the same value. |
| `AUTH_AUDIENCE` | `ecommerce-api` | JWT audience required by backend APIs. |
| `ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:3001` | Browser origins allowed by Auth CORS. |
| `INITIAL_ADMIN_EMAIL` | `admin@example.com` | Email for the first local admin user. |
| `INITIAL_ADMIN_PASSWORD` | `ChangeMe123!@#` | Password for that local admin user. |

Auth also has application defaults for `AUTH_ACCESS_TOKEN_TTL` (`PT15M`),
`AUTH_REFRESH_TOKEN_TTL` (`P30D`), `AUTH_MAX_FAILED_ATTEMPTS` (`5`), and
`AUTH_LOCK_DURATION` (`PT15M`). They do not need to be set for the local Docker
workflow.

#### Catalog, Inventory, Order, Customer, and Payment connectivity

| Variable | Local value | Relevance |
| --- | --- | --- |
| `AUTH_ISSUER` | `http://localhost:8085` | Must match the issuer Auth puts in JWTs. |
| `AUTH_AUDIENCE` | `ecommerce-api` | Must match Auth's JWT audience. |
| `AUTH_JWK_SET_URI` | `http://host.docker.internal:8085/.well-known/jwks.json` | Container-to-host URL used to fetch Auth's public signing keys. |
| `CATALOG_SERVICE_URL` | `http://host.docker.internal:8081` | Inventory/Order URL for Catalog from inside Docker. |
| `INVENTORY_SERVICE_URL` | `http://host.docker.internal:8082` | Order URL for Inventory from inside Docker. |
| `CUSTOMER_DB_URL` | `jdbc:postgresql://customer-db:5432/customer_db` | Dedicated Customer database URL inside Compose. |

`AUTH_ISSUER` and `AUTH_JWK_SET_URI` intentionally use different hostnames:
the issuer must match the JWT value (`localhost`), while the JWK request must be
reachable from a container (`host.docker.internal`).

#### Service-to-service tokens

These are development-only shared secrets. The caller's variable and the
receiver's variable must contain the same value:

| Caller | Receiver | Caller variable | Receiver variable | Local value |
| --- | --- | --- | --- | --- |
| Inventory | Catalog | `CATALOG_SERVICE_TOKEN` | `INVENTORY_SERVICE_TOKEN` | `dev-inventory-to-catalog` |
| Order | Catalog | `CATALOG_SERVICE_TOKEN` | `ORDER_SERVICE_TOKEN` | `dev-order-to-catalog` |
| Order | Inventory | `INVENTORY_SERVICE_TOKEN` | `ORDER_SERVICE_TOKEN` | `dev-order-to-inventory` |
| Cart | Catalog | `CART_CATALOG_SERVICE_TOKEN` | `CART_SERVICE_TOKEN` | `dev-cart-to-catalog` |

These secrets authenticate internal SKU, reservation, confirmation, and release
calls. They are separate from a user's JWT and from permissions such as
`INVENTORY_READ` or `ORDER_READ`. Do not commit real secrets to the repository.

#### Database and server values fixed by Compose

These values are already set in each Compose file and normally do not need to be
exported:

| Service | Database URL inside Compose | Database | App port | Host port |
| --- | --- | --- | ---: | ---: |
| Auth | `jdbc:postgresql://auth-db:5432/auth_db` | `auth_db` | 8085 | 8085 |
| Catalog | `jdbc:postgresql://postgres:5432/ecommerce` | `ecommerce` | 8081 | 8081 |
| Inventory | `jdbc:postgresql://inventory-postgres:5432/inventory_db` | `inventory_db` | 8082 | 8082 |
| Order | `jdbc:postgresql://order-db:5432/order_db` | `order_db` | 8083 | 8083 |
| Customer | `jdbc:postgresql://customer-db:5432/customer_db` | `customer_db` | 8086 | 8086 |
| Payment | `jdbc:postgresql://payment-db:5432/payment_db` | `payment_db` | 8087 | 8087 |

Do not replace these container-side database hostnames with `localhost`:
`localhost` inside a container means that same container, not the database
container.

#### Admin UI

Copy `ecommerce-admin-ui/.env.example` to `.env.local`:

| Variable | Value | Relevance |
| --- | --- | --- |
| `NEXT_PUBLIC_AUTH_API_URL` | `http://localhost:8085` | Auth login, refresh, and user APIs. |
| `NEXT_PUBLIC_CATALOG_API_URL` | `http://localhost:8081` | Catalog products, categories, variants, and images. |
| `NEXT_PUBLIC_INVENTORY_API_URL` | `http://localhost:8082` | Inventory stock, units, locations, and reservations. |
| `NEXT_PUBLIC_ORDER_API_URL` | `http://localhost:8083` | Order list, detail, history, and cancellation APIs. |
| `NEXT_PUBLIC_CART_API_URL` | `http://localhost:8084` | Read-only Cart support list and detail APIs. |
| `NEXT_PUBLIC_CUSTOMER_API_URL` | `http://localhost:8086` | Customer Service base URL for customer self-service and User Management administration. |
| `NEXT_PUBLIC_PAYMENT_API_URL` | `http://localhost:8087` | Payment Service operations list, detail, attempts, and refund APIs. |

The `NEXT_PUBLIC_` prefix is required by Next.js because these URLs are used by
browser code. Do not use Docker-only hostnames such as `host.docker.internal` in
the UI environment.

The Admin UI has a shared permission-gated User Management section with separate
Customers, Service users, Roles, and Permissions screens. Customers use
Customer Service's `CUSTOMER_READ`/`CUSTOMER_UPDATE` admin contracts for profile,
status, and shipping/billing address operations. Service users and roles use
Auth's administrative contracts; customer signup remains public Auth behavior.

## Service ownership and connected admin flow

```text
                         ecommerce-admin-ui :3000
                                  |
             +--------------------+--------------------+--------------------+
             |                    |                    |                    |
             v                    v                    v                    v
       Catalog :8081        Inventory :8082        Order :8083        Payment :8087
       current products     physical stock         historical orders       payment state,
       variants and SKUs    units and reservations snapshots and history   attempts, refunds
             \                    |                    /                    /
              \                   |                   /                    /
                         Auth :8085
                  identity and permissions
```

The operational navigation connects the domains without changing ownership:

```text
Product → Variant → SKU → Inventory → Reservation → Order
Order → Order Item → SKU → Product snapshot
Order → Reservation → Inventory Units
Payment → Order reference → Order Service
Payment → Customer reference → Customer Service
```

Catalog owns current product, variant, SKU, image, and price data. Inventory owns locations, aggregate stock, physical units, reservations, and movements. Order owns the commercial transaction, historical snapshots, totals, status, history, and remote reservation references. Payment owns gateway orchestration state, attempts, provider references, webhooks, and refunds; external gateways perform the actual processing. Customer owns customer profiles and addresses. Auth owns users, JWTs, roles, permissions, and refresh sessions. The admin UI composes these APIs and keeps each service's data authoritative.

Order is a separate deployable service with its own database. It references the
Catalog/Inventory SKU over HTTP, never imports their entities, and never shares
their database. See [order-service/README.md](order-service/README.md) for the
state machine, API, snapshot, idempotency, reservation, recovery, Docker, and
Testcontainers design.

### Required local service-to-service authentication

The token mapping in the startup commands is intentional. Inventory calls
Catalog with `CATALOG_SERVICE_TOKEN`, which Catalog validates against its
`INVENTORY_SERVICE_TOKEN`. Order calls Catalog and Inventory with its own
caller-specific tokens. Cart calls Catalog with its Cart-specific token. If a token is missing or different, the target service
may be healthy while the remote request returns `401`, `403`, or `503`.

For example, after both Catalog and Inventory are running, check the protected
Catalog SKU lookup directly:

```bash
curl -i \
  -H 'X-Inventory-Service-Token: dev-inventory-to-catalog' \
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

Payment metrics are intentionally not calculated in the browser. Payment Service
currently exposes operational list/detail data but no efficient summary endpoint;
add one before introducing dashboard totals for payment states.
