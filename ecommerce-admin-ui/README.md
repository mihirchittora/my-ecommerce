# Ecommerce Admin UI

Production-oriented Next.js admin workspace for catalog and inventory operations in the `my-ecommerce` repository.

The UI is deliberately split by backend responsibility:

- Catalog screens consume the Spring Boot `catalog-service` service.
- Inventory screens consume the separate `inventory-service` adapter.
- The browser only talks to the Next.js same-origin rewrites; page components never call `fetch` directly.
- No production inventory responses are mocked. If the inventory service is unavailable or an endpoint is not implemented, the UI shows an explicit unavailable state.

## Run locally

```bash
cd ecommerce-admin-ui
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

The checked-in local configuration is:

```dotenv
NEXT_PUBLIC_AUTH_API_URL=http://localhost:8085
NEXT_PUBLIC_CATALOG_API_URL=http://localhost:8081
NEXT_PUBLIC_INVENTORY_API_URL=http://localhost:8082
```

Copy `.env.example` to `.env.local` when setting up a fresh checkout. `.env.local` is ignored by Git.

The current catalog service defaults to port 8080 in its Spring configuration. Either start it on 8081 to match this frontend configuration, or set `NEXT_PUBLIC_CATALOG_API_URL=http://localhost:8080` locally. The inventory service is configured for port 8082.

## Authentication and authorization

This application is restricted to internal users. Open `/login` and sign in with an Auth Service account assigned a catalog, inventory or security-administration role. Customer accounts are rejected even when their credentials are valid.

The UI communicates with Auth Service through the same-origin `/backend/auth` rewrite and uses:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`

Access and refresh tokens are kept for the current browser tab. Catalog and Inventory requests receive the access token automatically; a `401` triggers one refresh attempt and retries the original request. Route access and navigation are filtered from the permission codes returned by `/me`, while the backend services remain the authoritative authorization boundary.

## Application routes

### Catalog

- `/dashboard` — catalog and inventory health overview
- `/login` — internal user sign-in
- `/categories` — hierarchical category tree with create-child, edit and delete flows
- `/products` — searchable, filterable, sortable, paginated product table
- `/products/new` — validated product and multi-variant creation form
- `/products/[id]` — product detail/edit, SKU inventory panel, image management and delete confirmation

### Inventory

- `/inventory` — stock overview and summary cards
- `/inventory/locations` — location CRUD
- `/inventory/stock` — SKU-level stock filters and pagination
- `/inventory/stock/[sku]` — location balances and physical units for one SKU
- `/inventory/units` — itemized inventory unit search by SKU, location, status, serial, IMEI and barcode
- `/inventory/units/[id]` — unit identity and movement history
- `/inventory/receive` — quantity-based receiving with unit identity fields
- `/inventory/reservations` and `/inventory/reservations/[id]` — reservation creation and lifecycle actions
- `/inventory/adjustments` — audited correction creation and history
- `/inventory/transfers` — explicit-unit transfers between locations
- `/inventory/reconciliation` — read-only expected-versus-actual discrepancy view

`/orders` and `/customers` are navigation placeholders and intentionally do not invent service data.

## API layer

The central service-aware client is in `lib/api/client.ts`. It provides typed JSON requests, multipart-safe headers, response parsing, status mapping, field-error extraction and service-specific unavailable messages.

Catalog modules:

- `lib/api/categories.ts`
- `lib/api/products.ts`
- `lib/api/images.ts`

Inventory module:

- `lib/api/inventory.ts`

Shared domain contracts live in `lib/types.ts`. Query caching, invalidation and mutation handling live in `lib/queries.ts`.

The frontend uses the following existing catalog endpoints:

- `GET /api/v1/categories/tree`
- `POST /api/v1/categories`
- `PUT /api/v1/categories/{id}`
- `DELETE /api/v1/categories/{id}`
- `GET /api/v1/products?page=&size=&sort=name,asc&search=&categoryId=`
- `GET /api/v1/products/{id}`
- `POST /api/v1/products`
- `PUT /api/v1/products/{id}`
- `DELETE /api/v1/products/{id}`
- `POST /api/v1/products/{productId}/images` as multipart/form-data
- `DELETE /api/v1/products/{productId}/images/{imageId}`

The product list sends Spring pagination as `page`, `size` and a string sort such as `sort=name,asc`; it does not send Swagger's invalid `sort=["string"]` shape.

The frontend adapter now targets the Inventory service's actual REST surface:

- `GET /api/v1/inventory/{sku}` and `POST /api/v1/inventory/{sku}/receive`
- `GET /api/v1/inventory/{sku}/units` and `GET /api/v1/inventory/units/{unitId}`
- `POST /api/v1/inventory/{sku}/adjustments`
- `GET /api/v1/inventory/adjustments?page=0&size=20&sku=&reason=`
- `POST /api/v1/inventory/{sku}/reservations`
- `GET /api/v1/inventory/reservations?page=0&size=20&sort=createdAt,desc&status=&sku=&locationId=`
- `GET /api/v1/inventory/reservations/{reservationId}` plus confirm/release/cancel actions
- `POST /api/v1/inventory/transfers`
- `POST /api/v1/inventory/{sku}/reconcile?locationId=`
- `GET|POST|PUT|DELETE /api/v1/inventory/locations`

## Inventory API gaps discovered

The Inventory service exposes SKU-scoped detail endpoints and a paginated global aggregate stock summary. It does not currently expose physical-unit-without-SKU, transfer-history or global reconciliation-list endpoints. Adjustment and reservation history are available through paginated list endpoints above. The frontend therefore:

- Uses the global aggregate endpoint for the Inventory overview and Stock list, then uses SKU-scoped endpoints for itemized units and detail views.
- Shows real API data in product/SKU detail and itemized unit views.
- Keeps creation and lifecycle forms available for receiving, reserving, adjusting and transferring.
- Displays a clear unavailable/unsupported state only for history or global-list screens where the backend has no corresponding endpoint.

The unit detail DTO also does not include movement history, so that panel reports the absence of history instead of inventing events. The Reservations screen supports SKU, location, status, and pagination filters against the real reservation-list endpoint.

The inventory model keeps these concepts separate:

- SKU — sellable catalog configuration.
- Inventory Unit ID — one physical item created by receiving.
- Serial number / IMEI — optional identity fields on a unit.
- Inventory item — aggregate SKU/location quantity and reservation balance.

## UX and validation

- TanStack Query owns server state and refetches after mutations.
- React Hook Form + Zod validate locations, products, variants, receipts, reservations, adjustments and transfers.
- Destructive product, category and location actions use confirmation dialogs.
- Toasts communicate successful mutations.
- Loading skeletons, empty states and service-specific error states are present throughout the workspace.
- Backend errors are sanitized into useful messages. Examples include highlighted-field guidance for 400s, resource-not-found messages for 404s, conflict messages for 409s and a generic retry message for 500s.
- Product images use multipart uploads for JPEG, PNG and WEBP with a 5 MB client-side limit; images are never converted to base64 for normal submission.

## Verification

```bash
npm run typecheck
npm run lint
npm run build
```

Verified in this workspace:

- TypeScript check passes.
- ESLint passes with three existing non-blocking `no-img-element` warnings in the catalog image table/gallery.
- `npm run build` passes using the Webpack builder. Next 16's Turbopack builder hit an environment-level worker process-binding panic during CSS processing, so the build script explicitly uses Webpack for reproducible local verification.
- Local route smoke checks returned HTTP 200 for all static routes listed above.

## Backend startup

Start the backend services separately. For the catalog service, align its port with the frontend environment variable. For inventory, use port 8082 once its REST layer is available. The frontend does not connect directly to Postgres or any other backend database.
