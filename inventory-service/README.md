# Inventory Service

Standalone itemized inventory service for the `my-ecommerce` repository. It owns
its own PostgreSQL database and references Catalog SKUs through HTTP; it does not
import Catalog entities or connect to the Catalog database.

## Runtime

- Java 21
- Spring Boot 3.5.6
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Flyway migrations
- SpringDoc / Swagger UI
- Testcontainers 2.0.5 (BOM-managed)
- Docker or Colima for local PostgreSQL and integration tests

The local ports are:

| Service | Port |
| --- | ---: |
| Catalog | 8081 |
| Inventory API | 8082 |
| Inventory PostgreSQL | 5433 |
| Web application | 3000 |

## Prerequisites

Install Java 21, Maven 3.9+, and Docker Desktop, Docker Engine, or optional
Colima. Start the selected Docker runtime before running the application or
tests. Testcontainers uses the Docker API and the repository test bootstrap
honors an explicit `DOCKER_HOST` when one is configured.

## Start the services manually

### 1. Start Catalog on port 8081

In a separate terminal:

```text
cd ../catalog-service
docker compose up -d postgres
mvn spring-boot:run
```

Catalog endpoints are available at `http://localhost:8081`; its internal SKU
contract is:

```text
GET /internal/catalog/skus/{sku}
```

The response contains `sku`, `variantId`, `productId`, and `active`.

### 2. Start Inventory PostgreSQL on port 5433

From this directory:

```text
docker compose up -d inventory-postgres
```

Default local database values are `inventory_db`, user `inventory`, password
`inventory`. Override them with `INVENTORY_DB_URL`, `INVENTORY_DB_USERNAME`, and
`INVENTORY_DB_PASSWORD`; do not commit real credentials.

### 3. Start Inventory on port 8082

```bash
mvn spring-boot:run
```

Before starting Inventory, configure the same service token used by Catalog.
Inventory calls Catalog to validate SKUs, so the Inventory health endpoint can
be `UP` while SKU requests return `503` if this service-to-service token is
missing or does not match.

For a local development run, start Catalog with:

```text
cd ../catalog-service
mvn spring-boot:run
```

Then start Inventory in this directory with the matching values from `.env` or
`.env.example`:

```text
mvn spring-boot:run
```

Verify the Catalog-to-Inventory service authentication before opening the UI:

```text
curl -i \
  -H 'X-Inventory-Service-Token: dev-inventory-token' \
  http://localhost:8081/internal/catalog/skus/<SKU>
```

This must return `200` for an existing active SKU. The service token is not a
user JWT and must not be replaced with the human `INVENTORY_READ` permission.

Useful URLs:

```text
http://localhost:8082/actuator/health
http://localhost:8082/swagger-ui.html
http://localhost:8082/v3/api-docs
```

## Docker execution

To start the Inventory API and its PostgreSQL container together instead:

```text
docker compose up --build
```

The Compose application uses `host.docker.internal:8081` to reach a Catalog
running in another independent Compose project. The Compose file supplies the
Linux `host-gateway` mapping as well.

## Run tests

Tests keep Testcontainers and use an isolated PostgreSQL 17 container; the local
Compose database is not required. From this directory:

```text
mvn clean test
```

The suite verifies Flyway startup, idempotent receive, unit listing, reservation
listing and confirmation, transfers, explicit negative adjustments, validation,
and an actual concurrent reservation race where one of two callers can reserve
the last unit.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `INVENTORY_DB_URL` | `jdbc:postgresql://localhost:5433/inventory_db` | Inventory JDBC URL |
| `INVENTORY_DB_USERNAME` | `inventory` | Local DB user |
| `INVENTORY_DB_PASSWORD` | `inventory` | Local-only development password |
| `CATALOG_SERVICE_URL` | `http://localhost:8081` | Catalog base URL |
| `CATALOG_SERVICE_TOKEN` | `dev-inventory-to-catalog` | Shared secret sent as `X-Inventory-Service-Token` for the protected internal SKU lookup |
| `SERVER_PORT` | `8082` | Inventory HTTP port |
| `INVENTORY_EXPIRATION_FIXED_DELAY_MS` | `60000` | Reservation expiry poll interval |
| `AUTH_ISSUER` | `http://localhost:8085` | Auth JWT issuer |
| `AUTH_AUDIENCE` | `ecommerce-api` | Required JWT audience |
| `AUTH_JWK_SET_URI` | `http://localhost:8085/.well-known/jwks.json` | Auth public JWKS |

## Authentication and authorization

Every administrative Inventory endpoint requires an Auth-issued bearer JWT.
Inventory validates the signature, issuer, audience, and expiry locally through
JWKS; it does not query the Auth database. Endpoints authorize by permission: `INVENTORY_READ` for
aggregate/reservation reads, `INVENTORY_UNIT_READ` for units, and the matching
`INVENTORY_RECEIVE`, `INVENTORY_ADJUST`, `INVENTORY_TRANSFER`,
`INVENTORY_RESERVE`, `INVENTORY_CONFIRM`, `INVENTORY_RELEASE`,
`INVENTORY_RECONCILE`, or `INVENTORY_LOCATION_MANAGE` for mutations.

The Catalog SKU call is a separate service-to-service authentication path. Configure
the same high-entropy `CATALOG_SERVICE_TOKEN` secret on Inventory and
`INVENTORY_SERVICE_TOKEN` on Catalog. Inventory sends it as
`X-Inventory-Service-Token`; it is not a user JWT and does not represent the
human `INVENTORY_READ` permission. Catalog accepts that header only for the
internal SKU endpoint. Set `APP_SECURITY_ENABLED=false` only for isolated legacy
domain tests; the production default is `true`.

User authentication and service authentication are intentionally separate:

- User/admin APIs use Auth-issued JWTs and endpoint permissions.
- Inventory-to-Catalog uses the configured service secret for this local/simple
  deployment. A client-credentials or service JWT flow can replace it later
  without making a human user permission the service identity.

Raw inventory-unit data is an internal/admin capability and requires
`INVENTORY_UNIT_READ`; it is not a public storefront API. A future storefront
availability API should expose only aggregate availability and should have its
own public contract and authorization policy.

Hibernate uses `ddl-auto=validate`; schema changes must be made with Flyway.

## Domain model and rules

The itemized relationship is:

```text
SKU
└── InventoryItem (one SKU at one Location)
    └── InventoryUnit (one physical item)
```

`sku` identifies the sellable catalog variant. `inventoryUnitId` is the server-
generated UUID of one tracked physical unit. `serialNumber` is manufacturer or
business serial metadata and may be absent; `imei` is device identity metadata
for applicable products and may be absent. None of these metadata values replaces
the SKU, and `inventoryUnitId` is not a serial number or IMEI.

Reservations map physical units, not only a count:

```text
Reservation
└── ReservationUnit
    └── InventoryUnit
```

An `InventoryItem` aggregate stores quantity and reserved quantity for a SKU and
location; its `InventoryUnit` rows are the physical source of truth. Every physical
unit has a server-generated `unitCode`. Optional serial, IMEI, and barcode values
are stored as metadata; uploaded or caller-provided unit codes are never trusted.

Unit statuses are `AVAILABLE`, `RESERVED`, `ALLOCATED`, `IN_TRANSIT`, `SOLD`,
`RETURNED`, `DAMAGED`, and `LOST`. Aggregate `quantity` counts active/non-disposed
units; `reservedQuantity` counts `RESERVED` units; `available` is their difference.
SOLD, DAMAGED, and LOST units remain in history but are not active stock.

All state-changing operations write a movement record. Receive, adjustment,
reservation, and transfer reference IDs are idempotency keys: repeating a
successful request does not create duplicate stock. Negative adjustments require
the exact unit IDs to remove and never accept only a number.

Reservations lock the aggregate and candidate unit rows in PostgreSQL before
changing them. Reservation confirmation marks units `SOLD` and reduces both
quantity and reserved quantity. Release, cancellation, and expiration return
reserved units to `AVAILABLE`. The scheduled expiry job only releases
reservations whose `expiresAt` has passed.

Shipping allocation is Inventory-owned. The protected
`POST /api/v1/inventory/reservations/{reservationId}/shipping-transition`
endpoint accepts `ALLOCATE`, `IN_TRANSIT`, or `RELEASE` plus the exact reserved
unit IDs and a stable Shipping reference. Inventory locks the reservation and
units, writes `SHIPPING` movement history, and returns the authoritative
reservation/unit statuses. Shipping never writes Inventory data directly.

## API examples

Create a location:

```bash
curl -sS -X POST http://localhost:8082/api/v1/inventory/locations \
  -H 'Content-Type: application/json' \
  -d '{"code":"WH-MUM","name":"Mumbai warehouse"}'
```

Receive two physical units. `quantity` must exactly equal the `units` array size:

```bash
curl -sS -X POST http://localhost:8082/api/v1/inventory/<SKU>/receive \
  -H 'Content-Type: application/json' \
  -d '{
    "locationId":"<location-id>",
    "quantity":2,
    "referenceId":"PO-1001",
    "units":[
      {"serialNumber":"SERIAL-001","barcode":"BAR-001"},
      {"serialNumber":"SERIAL-002","barcode":"BAR-002"}
    ]
  }'
```

Read aggregate inventory, optionally for one location:

```bash
curl -sS 'http://localhost:8082/api/v1/inventory/<SKU>'
curl -sS 'http://localhost:8082/api/v1/inventory/<SKU>?locationId=<location-id>'
```

Read the global dashboard totals. This endpoint requires a user JWT with
`INVENTORY_READ`; it does not call Catalog and is calculated from physical unit
records:

```bash
curl -sS 'http://localhost:8082/api/v1/inventory/summary' \
  -H 'Authorization: Bearer <access-token>'
```

List physical units with filters and pagination:

```bash
curl -sS 'http://localhost:8082/api/v1/inventory/<SKU>/units?page=0&size=20&status=AVAILABLE&sort=createdAt,asc'
```

Reserve, then confirm or release:

```bash
curl -sS -X POST http://localhost:8082/api/v1/inventory/<SKU>/reservations \
  -H 'Content-Type: application/json' \
  -d '{"locationId":"<location-id>","quantity":1,"referenceId":"CART-123","expiresAt":"2030-01-01T00:00:00Z"}'

curl -sS -X POST http://localhost:8082/api/v1/inventory/reservations/<reservation-id>/confirm
curl -sS -X POST http://localhost:8082/api/v1/inventory/reservations/<reservation-id>/release
```

Shipping transitions use its dedicated service credential:

```bash
curl -sS -X POST http://localhost:8082/api/v1/inventory/reservations/<reservation-id>/shipping-transition \
  -H 'X-Shipping-Service-Token: <shipping-to-inventory-secret>' \
  -H 'Content-Type: application/json' \
  -d '{"transition":"ALLOCATE","shippingReference":"SHP-2026-09-22-000001","unitIds":["<unit-id>"]}'
```

List reservations with optional SKU, location, and status filters:

```bash
curl -sS 'http://localhost:8082/api/v1/inventory/reservations?page=0&size=20&sort=createdAt,desc&status=ACTIVE&sku=<SKU>&locationId=<location-id>'
```

Move explicit physical units and read the grouped transfer history:

```bash
curl -sS -X POST http://localhost:8082/api/v1/inventory/transfers \
  -H 'Content-Type: application/json' \
  -d '{"sku":"<SKU>","fromLocationId":"<source-location-id>","toLocationId":"<destination-location-id>","unitIds":["<unit-id>"],"referenceId":"MOVE-1001"}'

curl -sS 'http://localhost:8082/api/v1/inventory/transfers?page=0&size=20&sku=<SKU>&locationId=<location-id>'
```

Transfer history is built from the per-unit movement records and is available to
the internal/admin UI. The response groups all units sharing one transfer
reference into one auditable transfer row.

Apply an explicit loss adjustment:

```bash
curl -sS -X POST http://localhost:8082/api/v1/inventory/<SKU>/adjustments \
  -H 'Content-Type: application/json' \
  -d '{"locationId":"<location-id>","quantity":-1,"reason":"LOSS","referenceId":"LOSS-1001","unitIds":["<unit-id>"]}'
```

Reconciliation is report-only and never silently changes stock:

```bash
curl -sS -X POST 'http://localhost:8082/api/v1/inventory/<SKU>/reconcile?locationId=<location-id>'
```

Validation and business errors return structured HTTP 400, 404, 409, or 503
responses without stack traces.

## Database schema

`src/main/resources/db/migration/V1__init_inventory.sql` creates locations,
inventory items, physical units, reservations, reservation-unit links,
adjustments, and unit movement history. The `(sku, location_id)` item key and
reservation reference key are database constraints; application checks provide
friendly errors before the database constraint is reached.
