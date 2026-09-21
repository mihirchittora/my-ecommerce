# API contracts

This document records the service contracts consumed by the admin UI. Spring pagination uses zero-based `page`, `size`, and one string `sort=property,direction`.

## Cart Service

Base URL: `http://localhost:8084`

The customer surface remains `/api/v1/cart`. The staff read-only surface is `/api/v1/carts` and requires an authenticated JWT with `CART_READ`.

### `GET /api/v1/carts`

Query parameters:

- `search` — exact Cart UUID, or a case-insensitive fragment of `customerId`.
- `status` — one of `ACTIVE`, `CHECKOUT_IN_PROGRESS`, `CONVERTED`, `ABANDONED`, `EXPIRED`.
- `sku` — normalized SKU filter.
- `page` — zero-based page, default `0`.
- `size` — `1..100`, default `20`.
- `sort` — supported Cart fields are `createdAt`, `updatedAt`, `expiresAt`, `status`, and `currency`, followed by `asc` or `desc`. Default `updatedAt,desc`.

Response is a Spring `Page<CartSummaryResponse>`:

```json
{
  "content": [
    {
      "id": "8b1f4b52-5d1e-4d9a-a640-5b9aa1cc4b10",
      "customerId": "customer-reference",
      "status": "ACTIVE",
      "currency": "INR",
      "createdAt": "2026-09-21T09:00:00Z",
      "updatedAt": "2026-09-21T10:00:00Z",
      "expiresAt": "2026-10-21T09:00:00Z",
      "convertedOrderId": null,
      "convertedOrderNumber": null,
      "itemCount": 1,
      "totalQuantity": 2
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20,
  "numberOfElements": 1,
  "first": true,
  "last": true,
  "empty": false
}
```

### `GET /api/v1/carts/{cartId}`

Returns `CartResponse` with the Cart summary fields above plus `version`, `enrichmentAvailable`, `warnings`, and `items`:

```json
{
  "id": "8b1f4b52-5d1e-4d9a-a640-5b9aa1cc4b10",
  "customerId": "customer-reference",
  "status": "ACTIVE",
  "currency": "INR",
  "createdAt": "2026-09-21T09:00:00Z",
  "updatedAt": "2026-09-21T10:00:00Z",
  "expiresAt": "2026-10-21T09:00:00Z",
  "convertedOrderId": null,
  "convertedOrderNumber": null,
  "version": 1,
  "itemCount": 1,
  "totalQuantity": 2,
  "enrichmentAvailable": true,
  "warnings": [],
  "items": [
    {
      "id": "1d4bca61-1fdf-4cc0-97b8-c0d8eae7da90",
      "sku": "IP17-BLK-256",
      "quantity": 2,
      "createdAt": "2026-09-21T09:00:00Z",
      "updatedAt": "2026-09-21T09:00:00Z",
      "product": {
        "productId": "2a8a9b83-4ab2-4d15-a65e-2e1a8a9ab111",
        "variantId": "e91b5a67-5a37-4c62-8a34-2b4bc3652222",
        "name": "iPhone 17 Pro",
        "variant": "Black / 256GB",
        "attributes": {"color": "Black", "storage": "256GB"}
      },
      "pricing": {
        "unitPrice": 149900,
        "currency": "INR",
        "subtotalEstimate": 299800
      },
      "availability": {
        "known": false,
        "availableQuantity": null,
        "message": "Availability is checked authoritatively by Order Service at checkout"
      },
      "unavailable": false
    }
  ]
}
```

`product` and `pricing` are current Catalog display data, not persisted Cart data and not historical Order pricing. When Catalog is unavailable, the item still returns its `sku` and `quantity`, with `product` and `pricing` set to `null` and a warning. The Cart API intentionally does not reserve Inventory or return Inventory Unit IDs/reservation IDs. The admin UI reads current availability separately from Inventory.

### Related contracts used by Cart detail

- Catalog product navigation: `GET /api/v1/products/{productId}` through `catalog-service`.
- Inventory availability: `GET /api/v1/inventory/{sku}` through `inventory-service`, requiring `INVENTORY_READ`; its `totalAvailable` is displayed as current availability, never as a Cart reservation.
- Converted Order navigation: `GET /api/v1/orders/{orderId}` through `order-service`, when Cart returns `convertedOrderId` and the operator has `ORDER_READ`.

The current Cart contract does not return customer name/email or lifecycle history events, so the admin UI shows only `customerId` and does not fabricate a timeline or customer route.

## Authorization

`CART_READ` is seeded by Auth Service migration `V4__add_cart_read_permission.sql` and granted to `SUPER_ADMIN`. The Cart staff endpoints enforce the permission server-side. The UI uses the same permission for navigation and route gating, but those checks are only a UX layer.
