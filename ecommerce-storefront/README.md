# ecommerce-storefront

The public customer storefront for `my-ecommerce`, served on port `3001`.

## Run locally

macOS/Linux:

```text
cp .env.example .env.local
npm install
npm run dev
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env.local
npm install
npm run dev
```

The same npm commands work in CMD. The root README documents the full
cross-platform workflow and Docker service startup.

The storefront uses Next rewrites under `/backend/*` so browser requests stay same-origin while each service URL remains configurable through the environment. Auth access tokens live in memory and the refresh token is kept only in `sessionStorage` because the current Auth Service returns token JSON rather than setting an HttpOnly cookie. A future cookie-based Auth contract can remove that browser storage fallback without changing the storefront API modules.

## Service ownership

The UI does not persist catalog, inventory, cart, customer, order, payment, or shipment data locally. It reads and mutates those domains through the owning service. Product prices shown in cart and checkout are estimates; Order Service calculates the authoritative order total. Checkout uses the Cart Service delegation endpoint with a stable `Idempotency-Key`, and payment uses Payment Service with its own stable key.

Category images are read directly from Catalog's typed category response. The
storefront does not persist or duplicate categories. Root-category navigation,
the mobile menu, homepage `Shop by category` cards, category heroes, and child
cards all use the Catalog `image.url` and `image.altText` fields. A branded Morrow
fallback keeps navigation usable when an image is absent or unavailable.

Category images are read directly from Catalog's typed category response. The
storefront does not persist or duplicate categories. Root-category navigation,
the mobile menu, homepage `Shop by category` cards, category heroes, and child
cards all use the Catalog `image.url` and `image.altText` fields. A branded Morrow
fallback keeps navigation usable when an image is absent or unavailable.

## Tests

```bash
npm run typecheck
npm run lint
npm test
npm run build
```

The smoke E2E test runs against `STOREFRONT_E2E_BASE_URL` when set; it is skipped when a running storefront is not available. Run it with `npm run test:e2e` after starting the app.

## Contract additions

The storefront needs public slug lookups and customer-safe availability. The smallest compatible additions are:

- `GET /api/v1/products/slug/{slug}`
- `GET /api/v1/categories/slug/{slug}`
- `GET /api/v1/categories/{id}/image/file` (the stable public image URL returned by Catalog)
- `GET /api/v1/categories/{id}/image/file` (the stable public image URL returned by Catalog)
- `GET /api/v1/inventory/availability/{sku}`

The availability response exposes only `{ sku, available, message }`; operational quantities, locations, reservation identifiers, and inventory units remain private.

Discovery additions use the catalog-owned contract:

- `GET /api/v1/products/facets?status=ACTIVE&categoryId={id}&search={term}` returns the live price, brand, and variant-attribute facets.
- `GET /api/v1/products` accepts repeated `brand` and `attribute=key:value` parameters plus `priceMin` and `priceMax`.
- The catalog currently has no cross-service server-side `availability=in_stock` listing filter. The storefront keeps that state in the URL and applies the customer-safe Inventory availability response to the loaded page while showing the limitation in the filter UI; a future bulk Inventory/catalog discovery contract can remove that fallback.
- Catalog currently supports `name`, `brand`, `slug`, `createdAt`, and `updatedAt` sorting. The storefront exposes only the customer-facing options backed by those capabilities (`Name A–Z`, `Name Z–A`, and `Newest`).
