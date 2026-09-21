# Ecommerce Admin UI

Production-oriented Next.js admin workspace for catalog, inventory, and order operations in the `my-ecommerce` repository.

The UI is deliberately split by backend responsibility:

- Catalog screens consume the Spring Boot `catalog-service` service.
- Inventory screens consume the separate `inventory-service` adapter.
- Order screens consume the separate `order-service` and preserve its historical commercial snapshots.
- Cart screens consume the read-only staff surface of `cart-service` and keep customer Cart quantity separate from Inventory availability and Order history.
- The browser only talks to the Next.js same-origin rewrites; page components never call `fetch` directly.
- No production Catalog, Inventory, or Order responses are mocked. If a dependent service is unavailable or an endpoint is not implemented, the UI shows an explicit unavailable state.

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
NEXT_PUBLIC_ORDER_API_URL=http://localhost:8083
NEXT_PUBLIC_CART_API_URL=http://localhost:8084
```

Copy `.env.example` to `.env.local` when setting up a fresh checkout. `.env.local` is ignored by Git.

The Catalog, Inventory, Order, and Cart services are configured for ports 8081, 8082, 8083, and 8084 respectively. Override the public environment variables for another local topology.

## Authentication and authorization

This application is restricted to internal users. Open `/login` and sign in with an Auth Service account assigned a catalog, inventory or security-administration role. Customer accounts are rejected even when their credentials are valid.

The UI communicates with Auth Service through the same-origin `/backend/auth` rewrite and uses:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`

Access and refresh tokens are kept for the current browser tab. Catalog, Inventory, Order, and Cart requests receive the access token automatically; a `401` triggers one refresh attempt and retries the original request. Route access and navigation are filtered from the permission codes returned by `/me`, while the backend services remain the authoritative authorization boundary. Orders require `ORDER_READ`; customer Cart inspection requires `CART_READ`.

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

### Orders

- `/orders` — server-paginated order ledger with order-number, SKU, status, and created-date filters
- `/orders/[id]` — order overview, historical item snapshots, financial totals, Inventory reservation enrichment, and append-only history

### Carts

- `/carts` — server-paginated, searchable Cart ledger with Cart ID/customer-reference, SKU, status, and URL-preserved sorting/filtering
- `/carts/[id]` — read-only Cart detail with customer reference, Cart lifecycle state, SKU/quantity items, current Catalog price estimates, Inventory availability, and related Order navigation when returned and authorized

Cart navigation and routes require `CART_READ`. The current Cart API does not return customer name/email, so the UI shows the customer reference only. The UI intentionally has no add, remove, update, clear, checkout, reserve, confirm, or transfer actions for Cart support users.

Customers remains a navigation placeholder because no customer-management API is in the current admin scope.

## API layer

The central service-aware client is in `lib/api/client.ts`. It provides typed JSON requests, multipart-safe headers, response parsing, status mapping, field-error extraction and service-specific unavailable messages.

Catalog modules:

- `lib/api/categories.ts`
- `lib/api/products.ts`
- `lib/api/images.ts`

Inventory module:

- `lib/api/inventory.ts`

Order module:

- `lib/api/order/types.ts` — Order DTOs and the actual Order status enum
- `lib/api/order/orders.ts` — typed Order list, detail, and cancel calls
- `lib/api/order/queries.ts` — TanStack Query hooks, cancellation invalidation, and best-effort Inventory reservation enrichment

Cart module:

- `lib/api/cart/types.ts` — actual Cart statuses, paginated list DTOs, current Catalog enrichment, and converted Order references
- `lib/api/cart/carts.ts` — typed staff list/detail calls to `cart-service`
- `lib/api/cart/queries.ts` — TanStack Query hooks for Cart data and isolated per-SKU Inventory availability reads

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

The frontend uses these Order endpoints:

- `GET /api/v1/orders?status=&orderNumber=&sku=&createdFrom=&createdTo=&page=&size=&sort=` for staff search
- `GET /api/v1/orders/{orderId}` for the detail view
- `POST /api/v1/orders/{orderId}/cancel` for state-validated cancellation

The frontend uses these read-only Cart administration endpoints:

- `GET /api/v1/carts?search=&status=&sku=&page=&size=&sort=` for staff Cart summaries
- `GET /api/v1/carts/{cartId}` for a staff Cart detail

The Cart list supports an exact Cart UUID or customer-reference fragment through `search`; customer email is not a supported backend filter. Cart status values are the actual Cart enum: `ACTIVE`, `CHECKOUT_IN_PROGRESS`, `CONVERTED`, `ABANDONED`, and `EXPIRED`. The Cart response owns SKU/quantity and current Catalog display estimates. Inventory is queried separately from `GET /api/v1/inventory/{sku}` and is never treated as a Cart reservation.

The list sends Spring pagination as `page`, `size`, and a single sort string such as `sort=createdAt,desc`. Filters are kept in the URL so browser back/forward and deep links retain the current view.

## Order management

The Order service owns the historical commercial record. The UI displays `productNameSnapshot`, `variantSnapshot`, `unitPrice`, item `subtotal`, and order totals returned by Order service. It never replaces those values with current Catalog prices. Each item labels its purchase-time values as a historical snapshot and links its SKU into the existing Inventory stock and unit views.

The detail view composes the services without transferring ownership:

```text
Product → Variant → SKU → Inventory → Reservation → Order
Order → Order Item → SKU → Inventory
Order → Reservation → Inventory Units
```

Order reservation IDs are references owned by Inventory. The detail page fetches each reservation independently. If Inventory is unavailable, the order header, items, totals, stored reservation references, and history still render and the Inventory section offers a retry state. Inventory and unit links use the existing `/inventory/reservations/[id]` and `/inventory/units/[id]` routes.

Cancellation is exposed from both the list and detail views through the same reusable action component. It is shown only when the operator has `ORDER_CANCEL` and the actual state is one of `DRAFT`, `PENDING_RESERVATION`, `RESERVED`, or `PENDING_PAYMENT`. The backend remains authoritative; the UI handles sanitized `403`, `409`, `404`, and `503` responses and invalidates only the affected order, order list, reservation, and inventory query families.

The current Order DTO stores only a customer reference and SKU-based snapshots. There is no safe public customer lookup for this UI and no browser-safe Catalog SKU lookup because Catalog's SKU endpoint is service-to-service protected. Therefore the UI shows the customer reference as secondary technical metadata, keeps the historical product snapshot visible, and links the SKU to Inventory. A reservation reference containing an order number cannot be turned into a deep Order link without a supported order-number lookup endpoint, so the reservation screen does not guess an Order UUID.

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

## Cart Management

Cart is a customer-owned mutable selection, not a customer storefront and not an Order. Cart Service stores SKU plus quantity and does not reserve Inventory when an item is added. Checkout is a separate downstream workflow owned by Cart/Order; once a Cart is converted, the related Order remains authoritative for historical product/pricing snapshots, final totals, and reservation references.

The admin Cart view is for support, investigation, monitoring, and navigation only. It shows Cart quantity as requested quantity, and Inventory availability as a separate current read. A row such as “Requested: 2” and “Currently available: 5” does not mean that two units are reserved. Expired Cart state is also distinct from an expired Inventory reservation.

Catalog enrichment is optional: if Catalog is unavailable, the Cart detail still shows the persisted SKU and quantity and labels product/price information unavailable. Inventory enrichment is optional: if Inventory is unavailable, the Cart detail remains usable and offers Retry. Order navigation is shown only when Cart Service returns a converted Order reference and the operator has `ORDER_READ`. Customer name and email are not fabricated because the current Cart contract returns only the customer reference.

The backend exposes `CART_READ` for this staff surface. Frontend checks improve navigation and UX; Cart Service remains authoritative and rejects missing permission with `403`. No Cart mutation or administrative checkout endpoint is exposed by this UI.

## UX and validation

- TanStack Query owns server state and refetches after mutations.
- React Hook Form + Zod validate locations, products, variants, receipts, reservations, adjustments and transfers.
- Destructive product, category and location actions use confirmation dialogs.
- Toasts communicate successful mutations.
- Loading skeletons, empty states and service-specific error states are present throughout the workspace.
- Backend errors are sanitized into useful messages. Examples include highlighted-field guidance for 400s, resource-not-found messages for 404s, conflict messages for 409s and a generic retry message for 500s.
- Product images use multipart uploads for JPEG, PNG and WEBP with a 5 MB client-side limit; images are never converted to base64 for normal submission.
- Order filters use native accessible controls, server-side pagination, URL state, responsive table/card layouts, skeleton loading states, and service-isolated error states.
- No Payment or Shipping UI is included; `PENDING_PAYMENT` is shown only as an Order status because those services do not exist in this repository.

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
- The production build includes `/orders` as a static route and `/orders/[id]` as a dynamic route.

There is no frontend `test` script or test runner in the existing package, so `npm test` is not configured. TypeScript, ESLint, and the production build are the available automated checks until a test framework is introduced.

## Backend startup

Start the backend services separately. The Catalog, Inventory, and Order services use ports 8081, 8082, and 8083 by default. The frontend does not connect directly to Postgres or any other backend database.
