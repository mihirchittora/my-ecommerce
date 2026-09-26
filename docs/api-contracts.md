# API contracts

## Service base URLs and portability

The published host base URLs are stable across macOS, Windows, and Linux:

| Service | Host base URL |
| --- | --- |
| Catalog | `http://localhost:8081` |
| Inventory | `http://localhost:8082` |
| Order | `http://localhost:8083` |
| Cart | `http://localhost:8084` |
| Auth | `http://localhost:8085` |
| Customer | `http://localhost:8086` |
| Payment | `http://localhost:8087` |
| Shipping | `http://localhost:8088` |

These URLs are for a host-run browser or developer command. The backend Compose
projects are intentionally independent: a container calls a database by its
Compose service name and calls another backend through
`http://host.docker.internal:<port>`. The Compose files add the Linux
`host-gateway` mapping. If several services are placed in one Compose network,
use that network's service DNS names instead. `localhost` inside a container
always means that same container.

### Single-VM gateway routes

The unified deployment publishes only the reverse proxy. Its default public
HTTP origin is `http://localhost:8080`; set `PROXY_HTTP_PORT` when that host
port is occupied. The proxy routes the existing API paths without changing
service contracts:

| Public path prefix | Internal service |
| --- | --- |
| `/.well-known/jwks.json`, `/api/v1/users`, `/api/v1/roles`, `/api/v1/permissions` | `auth-service:8085` |
| `/api/v1/categories`, `/api/v1/products` | `catalog-service:8081` |
| `/api/v1/inventory` | `inventory-service:8082` |
| `/api/v1/cart`, `/api/v1/carts` | `cart-service:8084` |
| `/api/v1/orders` | `order-service:8083` |
| `/api/v1/customers` | `customer-service:8086` |
| `/api/v1/payments` | `payment-service:8087` |
| `/api/v1/fulfillments`, `/api/v1/shipments`, `/api/v1/shipping/webhooks` | `shipping-service:8088` |

Actuator, OpenAPI, Swagger, and `/internal/*` paths return `404` at the public
gateway. Backend containers use private Docker DNS names; their application and
database ports are not host-published in this profile.

## Auth Service — admin contracts

Base URL: `http://localhost:8085`. Auth owns user identity, passwords, roles,
permissions, JWTs, refresh tokens, and account status. Every endpoint below
requires a valid Auth bearer JWT and the listed server-side permission.

### `GET /api/v1/users?page=0&size=20&scope=SERVICE`

- Auth/permission: `USER_READ`.
- Request: zero-based `page`; `size` is capped at 100 by Auth. `scope=SERVICE`
  returns users with at least one non-`CUSTOMER` role and is used by the Admin
  UI's Service users screen. The default `scope=ALL` preserves the full user
  list. Auth sorts by email ascending and accepts no search, status, or role
  query filter.
- Response: Spring `Page<UserResponse>` containing `id`, `email`, `firstName`,
  `lastName`, `status` (`ACTIVE`, `INACTIVE`, `LOCKED`, `PENDING`),
  `emailVerified`, and role names.
- Errors: `401` unauthenticated; `403` missing permission; `500` generic service
  failure.

### `POST /api/v1/users`

- Auth/permission: `USER_CREATE`.
- Request: `{email,password,firstName,lastName,roleIds}`. Password is 12–128
  characters. `roleIds` contains Auth role UUIDs; an empty list receives Auth's
  seeded default role.
- Response: `201 UserResponse`; password/hash/token fields are never returned.
- Errors: `400` validation; `409` duplicate email; `401`/`403`; `500`.

### `GET /api/v1/users/{id}` and `PUT /api/v1/users/{id}`

- Auth/permission: `USER_READ` for GET; `USER_UPDATE` for PUT.
- Request: PUT accepts only `{firstName,lastName}`. Email changes and password
  changes are not part of this admin contract.
- Response: `200 UserResponse`.
- Errors: `400`, `401`, `403`, `404`, `500`.

### `PATCH /api/v1/users/{id}/status`

- Auth/permission: `USER_UPDATE`.
- Request: `{status}` using the actual Auth enum: `ACTIVE`, `INACTIVE`,
  `LOCKED`, or `PENDING`.
- Response: `200 UserResponse`.
- Errors: `400`, `401`, `403`, `404`, `409` for unsupported status, `500`.
- Note: Auth currently exposes no last-super-admin/last-viable-admin safeguard.
  The UI blocks ordinary administrators from SUPER_ADMIN assignment/removal. Auth
  does enforce that users carrying SUPER_ADMIN retain every seeded application
  role; last-super-admin and last-viable-admin status enforcement remains a
  backend requirement.

### `POST|DELETE /api/v1/users/{id}/roles/{roleId}`

- Auth/permission: `USER_ROLE_ASSIGN`.
- Request: role UUID returned by `GET /api/v1/roles`; no raw role text is
  accepted by the UI.
- Response: `200 UserResponse` with updated role names.
- Errors: `401`, `403`, `404`, `500`.
- Invariant: assigning `SUPER_ADMIN` also assigns every currently seeded
  application role. Removing another role while `SUPER_ADMIN` remains assigned
  returns `409`.

### `GET /api/v1/roles`

- Auth/permission: `ROLE_READ`.
- Response: `200 RoleResponse[]`, each with `id`, `name`, `description`, and
  `permissions[]` containing permission `id`, `code`, and `description`.
- Role-to-user counts and assigned-user lookup are not returned.

### `POST /api/v1/roles`

- Auth/permission: `ROLE_CREATE`.
- Request: `{name,description}`. Names may contain letters, numbers, and
  underscores and are normalized to uppercase. New roles start with no
  permissions; update them through the permission endpoint below.
- Response: `201 RoleResponse`.
- Errors: `400` validation, `401`, `403`, `409` duplicate role name, `500`.

### `GET /api/v1/permissions`

- Auth/permission: `PERMISSION_READ`.
- Response: `200 PermissionResponse[]`, each with `id`, `code`, and
  `description`. Permissions are static seeded metadata; no create/edit API is
  exposed.

### `PUT /api/v1/roles/{id}/permissions`

- Auth/permission: `ROLE_PERMISSION_UPDATE`.
- Request: `{permissionIds: UUID[]}`. The submitted list replaces the role's
  complete permission set.
- Response: `200 RoleResponse`.
- Errors: `401`, `403`, `404` for the role or a permission ID, `409` when trying
  to edit the system-managed `SUPER_ADMIN` role, `500`.

Auth admin gaps intentionally not simulated by the UI: server-side user search,
status/role filters, created and last-login timestamps, audit-event reads, role
user counts, and protected last-super-admin/last-viable-admin mutations.

## Customer Service

Base URL: `http://localhost:8086`. Customer Service owns current profiles and
addresses in private `customer_db` (host port `5437`). Auth remains the source
of truth for identity and security. Every endpoint below requires an Auth JWT
with a valid signature, issuer, audience, expiration, and UUID `sub`; identity
is never taken from client-supplied `customerId` or `authUserId`. Admin
endpoints use the `CUSTOMER_READ` and `CUSTOMER_UPDATE` permissions from the
Auth JWT.

### `GET /api/v1/customers/me`

- Auth/permission: Bearer JWT; authenticated active customer.
- Request: no path, query, or body.
- Response: `200 CustomerResponse` (`id`, `authUserId`, `firstName`, `lastName`, `email`, `phone`, `status`, `createdAt`, `updatedAt`).
- Validation: JWT `sub` must be a UUID. First access lazily creates an `ACTIVE` profile. Auth-signed `email`, `firstName`, and `lastName` claims are used to synchronize the display copy; email is refreshed and names only fill missing profile fields.
- Errors: `401` missing/invalid JWT; `403` `INACTIVE`/`BLOCKED`; `500` unexpected.
- Side effects: one idempotent profile insert on first access and identity-copy synchronization on each authenticated read.
- Example: `curl -H 'Authorization: Bearer <jwt>' http://localhost:8086/api/v1/customers/me`

### `PATCH /api/v1/customers/me`

- Auth/permission: Bearer JWT; authenticated active customer.
- Request: optional JSON fields `firstName` (max 80), `lastName` (max 80), and `phone` (max 30). Omitted fields remain unchanged.
- Response: `200 CustomerResponse`.
- Validation: supplied names cannot be blank; phone is normalized to `+` plus 7–15 digits. Unknown fields fail with `400`, preventing changes to `id`, `authUserId`, `status`, `email`, roles, or permissions.
- Errors: `400` validation/unknown field; `401` unauthenticated; `403` inactive/blocked; `500` unexpected.
- Side effects: only mutable profile fields update. Email changes belong to a future verified Auth flow.
- Example request: `{"firstName":"Mihir","lastName":"Chittora","phone":"+91 98765-43210"}`

### `GET /api/v1/customers/me/addresses`

- Auth/permission: Bearer JWT; authenticated active customer.
- Request: no body/query.
- Response: `200 AddressResponse[]`, defaults first, then newest `createdAt`.
- Validation: owner is derived only from JWT `sub`.
- Errors: `401`, `403`, or `500` as above.
- Side effects: may create the empty lazy profile.
- Example: `curl -H 'Authorization: Bearer <jwt>' http://localhost:8086/api/v1/customers/me/addresses`

### `POST /api/v1/customers/me/addresses`

- Auth/permission: Bearer JWT; authenticated active customer.
- Request: `AddressRequest`: required `addressType` (`SHIPPING`/`BILLING`), `recipientName`, `phone`, `line1`, `city`, `state`, `postalCode`, and ISO alpha-2 `country`; optional `line2`, `landmark`, `isDefault`. No customer ID is accepted.
- Response: `201 AddressResponse`.
- Validation: bounded field lengths; phone normalization; country uses ISO 3166-1 alpha-2; postal code is country-neutral and accepts `302001`.
- Errors: `400` validation; `401`; `403`; `409` conflict; `500` unexpected.
- Side effects: first address of a type is default; `isDefault=true` replaces the same-type default under a customer-row lock and database constraint.
- Example request:

```json
{"addressType":"SHIPPING","recipientName":"Mihir Chittora","phone":"+919876543210","line1":"123 Example Street","line2":"Apartment 4B","city":"Udaipur","state":"Rajasthan","postalCode":"313001","country":"IN","landmark":"Near the market","isDefault":true}
```

### `GET /api/v1/customers/me/addresses/{id}`

- Auth/permission: Bearer JWT; authenticated active customer.
- Request: UUID path ID; no body.
- Response: `200 AddressResponse`.
- Validation: address must belong to JWT subject's customer.
- Errors: `401`; `403`; `404` for missing/not-owned; `500`.
- Side effects: none.
- Example: `curl -H 'Authorization: Bearer <jwt>' http://localhost:8086/api/v1/customers/me/addresses/ADDR_UUID`

### `PUT /api/v1/customers/me/addresses/{id}`

- Auth/permission: Bearer JWT; authenticated active customer.
- Request: UUID path ID plus complete `AddressRequest`; no customer ID.
- Response: `200 AddressResponse`.
- Validation: same as create. A default stays default unless another address is explicitly selected with `isDefault=true`; a type change maintains defaults for both types.
- Errors: `400`; `401`; `403`; `404`; `409`; `500`.
- Side effects: replaces validated fields transactionally and maintains the one-default-per-type invariant.
- Example request: same schema as `POST`, addressed to the path UUID.

### `DELETE /api/v1/customers/me/addresses/{id}`

- Auth/permission: Bearer JWT; authenticated active customer.
- Request: UUID path ID; no body.
- Response: `204 No Content`.
- Validation: address must belong to JWT subject's customer.
- Errors: `401`; `403`; `404`; `500`.
- Side effects: deletes the address; if it was default, promotes the newest remaining same-type address, or leaves no default when none remains.
- Example: `curl -X DELETE -H 'Authorization: Bearer <jwt>' http://localhost:8086/api/v1/customers/me/addresses/ADDR_UUID`

### `POST /api/v1/customers/me/addresses/{id}/default`

- Auth/permission: Bearer JWT; authenticated active customer.
- Request: UUID path ID; no body.
- Response: `200 AddressResponse` with `isDefault=true`.
- Validation: address must belong to JWT subject's customer.
- Errors: `401`; `403`; `404`; `409`; `500`.
- Side effects: atomically unsets the prior default of this address type and sets the target. Pessimistic customer locking plus a PostgreSQL partial unique index prevents concurrent duplicates.
- Example: `curl -X POST -H 'Authorization: Bearer <jwt>' http://localhost:8086/api/v1/customers/me/addresses/ADDR_UUID/default`

Customer errors use `{timestamp,status,code,message,path,fieldErrors}`. `400`
is validation/invalid JSON, `401` unauthenticated, `403` inactive/unauthorized,
`404` missing/not-owned, `409` conflict, `503` reserved for future unavailable
dependencies, and `500` is generic without stack traces.

### `GET /api/v1/customers?page=0&size=20&search=&status=`

- Auth/permission: `CUSTOMER_READ`.
- Request: server-side zero-based pagination, optional name/email/Auth-ID
  fragment `search`, and optional `status` (`ACTIVE`, `INACTIVE`, `BLOCKED`).
- Response: Spring `Page<CustomerResponse>`, newest profiles first.

### `GET /api/v1/customers/{id}` and `GET /api/v1/customers/{id}/addresses`

- Auth/permission: `CUSTOMER_READ`.
- Response: customer profile or the current shipping/billing
  `AddressResponse[]`, respectively.
- Errors: `401`, `403`, `404`, `500`.

### `GET /api/v1/customers/by-auth-user/{authUserId}`

- Auth/permission: `CUSTOMER_READ`.
- Request: the Auth user UUID stored by Order, Cart, Payment, and Shipping as
  their `customerId` reference.
- Response: `200 CustomerResponse`, including the Customer profile UUID in
  `id` and the matched Auth UUID in `authUserId`.
- Errors: `401`, `403`, `404` when no profile exists for the Auth user, `500`.

### `PATCH /api/v1/customers/{id}` and `PATCH /api/v1/customers/{id}/status`

- Auth/permission: `CUSTOMER_UPDATE`.
- Request: profile update accepts optional `firstName`, `lastName`, and `phone`;
  status update accepts `ACTIVE`, `INACTIVE`, or `BLOCKED`.
- Response: `200 CustomerResponse`.
- Email, Auth identity, passwords, roles, and permissions remain Auth-owned and
  cannot be changed through Customer Service.

The Admin UI uses these APIs for a separate Customers screen under User
Management. Customer registration remains the public Auth registration flow.

Customer returns current mutable addresses only. During checkout, Order must
copy the selected address into an Order-owned shipping snapshot; later address
edits/deletes must not mutate historical orders. Customer Service never accesses
Order or Auth databases.

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

## Order Service — checkout and Shipping integration

Base URL: `http://localhost:8083`. Order owns the commercial order, pricing
snapshots, Inventory reservation references, lifecycle, and the immutable
shipping-address snapshot.

### `POST /api/v1/orders`

- Authentication: customer Auth JWT; `customerId` is derived from JWT `sub`.
- Header: required `Idempotency-Key`, maximum 200 characters.
- Request: `{currency,items,preferredLocationId,shippingAddress}`. Each item has
  `sku` and positive `quantity`. `shippingAddress` is required and contains
  `sourceAddressId` (optional traceability), `recipientName`, `phone`, `line1`,
  optional `line2`, `city`, `state`, `postalCode`, ISO alpha-2 `country`, and
  optional `landmark`.
- Response: `200 OrderResponse` with the resolved price/product snapshots,
  totals, order status/history, and the same address values under
  `shippingAddress`. The address is copied into Order's
  `order_shipping_addresses` table at checkout; later Customer address changes
  cannot mutate it.
- Errors: `400` validation, `401` unauthenticated, `409` idempotency or state
  conflict/insufficient inventory, `503` Catalog or Inventory dependency
  failure.

### Payment confirmation and automatic fulfillment

Payment Service calls `POST /internal/orders/{orderId}/payment-events` with
`X-Payment-Service-Token` and a server-generated event containing
`paymentId`, `customerId`, `paymentStatus`, and optional amount/currency/provider
references. A captured payment is verified against the Order and moves it from
`PENDING_PAYMENT` through `PAID` to `CONFIRMED`. The same capture event is
idempotent.

After confirmation, Order inserts one unique `fulfillment_outbox` record per
Order. A retrying worker calls Shipping's internal fulfillment creation API;
Shipping then creates or returns the fulfillment for that Order. This keeps
payment confirmation independent of Shipping availability while ensuring the
fulfillment is eventually created.

### Protected Shipping contract

Shipping uses the configured `X-Shipping-Service-Token`, accepted only as
`SERVICE_SHIPPING` authority:

- `GET /internal/orders/{orderId}` returns the Order's complete address
  snapshot, item snapshots, reservation IDs, and authorized InventoryUnit IDs.
- `POST /internal/orders/{orderId}/shipping-events` accepts only
  `{shipmentNumber,status}` where `status` is `FULFILLING`, `SHIPPED`, or
  `DELIVERED`; the transition is idempotent and recorded in Order history.

Shipping exposes `GET /api/v1/fulfillments/order/{orderId}` for operators with
`SHIPPING_READ`, allowing the Admin UI to show the fulfillment and shipment
relationship directly from an Order detail page.

Cart forwards the checkout `shippingAddress` and bearer token to Order; it does
not persist a second address snapshot or reserve Inventory itself.

## Payment Service

Payment Service is the orchestration boundary for external gateway integrations.
It owns payment state, attempts, provider references, verified webhooks, and
refunds. It does not process cards or read Order, Customer, Inventory, or Auth
databases. The browser uses the internal operations surface below; all paths are
under `/api` at the service base URL `http://localhost:8087`.

### `GET /api/v1/admin/payments`

- Authentication: bearer JWT issued by Auth Service.
- Permission: `PAYMENT_READ`.
- Query: `search`, `status`, `provider`, `currency`, `orderId`, `customerId`,
  `createdFrom`, `createdTo`, `page`, `size`, and `sort`.
- Search: exact payment/order/customer UUIDs when a UUID is entered, and
  case-insensitive fragments of `providerPaymentId` or `providerOrderId`.
- Pagination: zero-based `page`, `size` maximum 100, and one Spring sort string
  such as `createdAt,desc`. Supported sortable fields are `createdAt`,
  `updatedAt`, `amount`, `status`, `provider`, and `refundedAmount`.
- Response: Spring `Page<AdminPaymentResponse>` with `content`,
  `totalElements`, `totalPages`, `number`, `size`, `numberOfElements`, `first`,
  `last`, and `empty`.
- Response fields: payment/order/customer UUIDs, amount, currency, actual
  `PaymentStatus`, configured provider, safe provider payment/order references,
  refund total, lifecycle timestamps, all attempts, and all refunds.
- Security: `checkoutUrl` and `checkoutToken` are intentionally omitted from
  this operations DTO. Card numbers, CVV/PINs, gateway secrets, webhook secrets,
  and authorization headers are never returned.
- Errors: `400` for invalid enum/UUID/date/pagination values, `401` for missing
  or invalid JWT, `403` without `PAYMENT_READ`, and `503` when an upstream
  dependency is unavailable (the list itself has no enrichment call).
- Side effects: none.

### `GET /api/v1/admin/payments/{paymentId}`

- Authentication: bearer JWT.
- Permission: `PAYMENT_READ`.
- Request: path `paymentId` UUID; no request body or special headers.
- Response: `AdminPaymentResponse` with the same safe fields as the list,
  including attempts and refunds.
- Errors: `401`, `403`, `404`, or `400` for a malformed UUID.
- Side effects: none.

### `POST /api/v1/admin/payments/{paymentId}/refunds`

- Authentication: bearer JWT.
- Permission: `PAYMENT_REFUND`.
- Headers: required `Idempotency-Key`, at most 200 characters.
- Request: optional JSON `{ "amount": 100.00, "reason": "Customer request" }`.
  Omit `amount` for a full refund. `amount` must be at least `0.01` and have
  no more than two decimal places; `reason` is optional and max 500 characters.
- Response: updated `AdminPaymentResponse`.
- Validation: the payment must be `CAPTURED` or `PARTIALLY_REFUNDED`, and the
  amount must be greater than zero and no greater than amount minus
  `refundedAmount`. The configured gateway decides provider success/failure.
- Errors: `400` validation or missing idempotency key, `401`, `403` without
  `PAYMENT_REFUND`, `404`, `409` for invalid state/amount/idempotency conflict,
  and `5xx`/gateway error when the provider cannot process the request.
- Side effects: creates a Refund record, transitions through `REFUND_PENDING`,
  updates `refundedAmount` and final payment status on success, and sends the
  existing best-effort payment-state notification to Order Service when enabled.

### `POST /api/v1/admin/payments/{paymentId}/retry`

- Authentication: bearer JWT.
- Permission: `PAYMENT_RETRY`.
- Headers: required `Idempotency-Key`, at most 200 characters.
- Request: no body.
- Response: updated `AdminPaymentResponse` with a new `PaymentAttempt`.
- Validation: only `FAILED` payments can be retried.
- Errors: `400`, `401`, `403` without `PAYMENT_RETRY`, `404`, `409` for a
  non-failed payment or invalid idempotency use, and gateway errors when a new
  checkout attempt cannot be created.
- Side effects: appends a new attempt, transitions to `PENDING`, invokes the
  configured gateway, and preserves the previous failed attempt.

### Customer-owned payment endpoints

The existing customer endpoints remain separate and are not used for Admin UI
cross-customer operations: `POST /api/v1/payments`, `GET
/api/v1/payments/{paymentId}`, `GET /api/v1/payments/order/{orderId}`, `POST
/api/v1/payments/{paymentId}/retry`, and `POST /api/v1/payments/{paymentId}/refund`
or `/refunds`. They scope payment access to the JWT customer subject. Webhooks
remain `POST /api/v1/payments/webhooks/{provider}` and require the configured
provider signature rather than a user JWT.

The provider boundary is `PaymentGateway`: the current implementation is
`SANDBOX`, while provider-specific API calls, authentication, response mapping,
webhook verification/parsing, provider references, and refunds belong in
adapters. A real provider requires an adapter, provider/configuration wiring,
runtime secrets, and adapter tests; Order and payment domain code do not import
provider SDKs. Webhook events are atomically claimed by provider/event ID,
deduplicated, and stored as a payload hash rather than a raw body.

The current Payment API does not expose a transition-history timeline, a human
readable order-number/customer-name search, or a dashboard summary endpoint.
Those values are not reconstructed in the browser.

## Shipping & Fulfillment Service

Base URL: `http://localhost:8088`. Shipping owns fulfillment, shipment,
shipment-item, carrier-reference, tracking-event, and append-only history data
in its private `shipping_db`. It never opens Order, Inventory, Payment, Catalog,
Customer, or Auth databases. Shipping does not own Order status or
`InventoryUnit.status`.

### Customer and staff shipment APIs

All APIs below use an Auth-issued bearer JWT. Customer identity is derived from
JWT `sub`; a request cannot supply another `customerId`. Staff endpoints use the
listed permission claim, not only a role name. A customer requesting another
customer's shipment receives a non-disclosing `404`.

| Method | Path | Permission | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/shipments/my?page=0&size=20&sort=createdAt,desc` | Authenticated customer | Own shipment summaries |
| `GET` | `/api/v1/shipments/{id}` | Authenticated owner or `SHIPPING_READ` | Shipment detail; operational IDs are staff-only |
| `GET` | `/api/v1/shipments/{id}/tracking` | Authenticated owner or `SHIPPING_READ` + `SHIPPING_TRACK` for staff | Customer-safe tracking and events |
| `GET` | `/api/v1/shipments` | `SHIPPING_READ` | Staff shipment page |
| `GET` | `/api/v1/fulfillments/{id}` | Authenticated owner or `SHIPPING_READ` | Fulfillment, items, history, and shipments |
| `GET` | `/api/v1/fulfillments` | `SHIPPING_READ` | Staff fulfillment page |
| `POST` | `/api/v1/shipments` | `SHIPPING_CREATE` | Validate and create an idempotent carrier shipment |
| `POST` | `/api/v1/shipments/{id}/cancel` | `SHIPPING_CANCEL` | Cancel before a non-cancellable carrier state |
| `POST` | `/api/v1/shipments/{id}/order-notification/retry` | `SHIPPING_MANAGE` | Retry a persisted Order notification without recreating a carrier shipment |

Shipment creation requires `Idempotency-Key` and a body such as:

```json
{
  "fulfillmentId": "00000000-0000-0000-0000-000000000001",
  "carrier": "EASYPOST",
  "serviceLevel": "Ground",
  "shippingCost": 0.00,
  "currency": "INR",
  "lines": [
    {
      "orderItemId": "00000000-0000-0000-0000-000000000002",
      "quantity": 2,
      "inventoryUnitIds": [
        "00000000-0000-0000-0000-000000000101",
        "00000000-0000-0000-0000-000000000102"
      ]
    }
  ]
}
```

`lines` may be omitted to request every remaining fulfillment item. For an
itemized line, `quantity` must equal the number of supplied authorized unit IDs;
Shipping checks that each ID belongs to the Order's reservation and is returned
by Inventory. It never chooses a replacement unit. The response contains
`shipmentNumber` (`SHP-YYYY-MM-DD-000001`), status, carrier, tracking number,
provider references, package count, item snapshots, and history. Customer
responses omit `inventoryUnitId`, provider shipment IDs, and label references.
For `EASYPOST`, `serviceLevel` must match the provider-returned rate `service`
or rate ID; `Ground` is only an example and the available services depend on
the configured EasyPost account and origin/destination.

The current staff shipment and fulfillment list endpoints do not expose search
or status/carrier/date filter parameters. The Admin UI therefore sends only
the supported pagination and sort fields; it does not download all records to
filter them in the browser.

### Internal integration APIs

These endpoints are not public. They require the configured
`X-Order-Service-Token` and are accepted only with `SERVICE_ORDER` authority.

| Method | Path | Caller | Purpose |
| --- | --- | --- | --- |
| `POST` | `/internal/fulfillments` | Order workflow | Read an eligible Order over HTTP and create its local fulfillment |
| `POST` | `/internal/shipments` | Order workflow | Run the same idempotent shipment workflow for an authenticated internal command |

The `POST` body is `{ "orderId": "..." }`. Shipping calls Order's protected
`GET /internal/orders/{orderId}` contract, accepts only `CONFIRMED` or
`FULFILLING`, and copies only operational snapshots and cross-service IDs.
The internal shipment command uses the same request body and required
`Idempotency-Key` as the public shipment create operation; it is not exposed to
browsers.

### Carrier webhook

`POST /api/v1/shipping/webhooks/{carrier}` is unauthenticated at the JWT layer
but authenticated by the carrier signature. The SANDBOX adapter expects
`X-Sandbox-Signature`, while EasyPost expects the documented
`X-Hmac-Signature-V2`, `x-timestamp`, and `x-path` headers. EasyPost's signature
covers the exact timestamp, method, path, and raw request body.
The body must identify `providerEventId` and `providerShipmentId` or
`trackingNumber`, and may include `eventType`, `description`, `location`, and
`occurredAt`.

The handler verifies the signature, normalizes the event, resolves the shipment,
records `(carrier, providerEventId)` exactly once, applies the state machine,
appends a tracking event and shipment history, and acknowledges duplicate
events without repeating the transition or Order notification. Unknown events,
bad signatures, malformed payloads, wrong shipment references, and out-of-order
events do not change shipment state.

Normalized events are `SHIPMENT_CREATED`, `LABEL_CREATED`, `PICKED_UP`,
`IN_TRANSIT`, `OUT_FOR_DELIVERY`, `DELIVERED`, `DELIVERY_FAILED`, and `RETURNED`.

### Error and pagination contract

Errors are `{timestamp,status,code,message,path,fieldErrors}`. `400` is invalid
input, `401` missing/invalid authentication or webhook signature, `403` missing
permission, `404` missing/not-owned, `409` state/idempotency/unit conflict,
`502` carrier or service dependency failure, and `500` unexpected. A shipment
attempt with an expired/unusable Inventory reservation or an Order without an
immutable address snapshot is a `409` conflict, not a gateway outage. Spring pages
use zero-based `page`, `size` up to 100, and one sort string such as
`sort=createdAt,desc`; the API does not produce a JSON-array sort parameter.

### Order and Inventory boundaries

Order remains authoritative for `CONFIRMED`, `FULFILLING`, `SHIPPED`, and
`DELIVERED`; Shipping sends milestone notifications through the protected Order
HTTP contract. Inventory remains authoritative for reservations and physical
unit status. Shipping reads the reservation and exact unit references through
`GET /api/v1/inventory/reservations/{id}` with its separate
`X-Shipping-Service-Token`. Shipping then calls Inventory's protected
`POST /api/v1/inventory/reservations/{reservationId}/shipping-transition` with
`ALLOCATE`, `IN_TRANSIT`, or `RELEASE` and the exact reserved unit IDs. Inventory
locks and mutates its own units and records the movement; Shipping never writes
Inventory state.

## Authorization

`CART_READ` is seeded by Auth Service migration `V4__add_cart_read_permission.sql` and granted to `SUPER_ADMIN`. The Cart staff endpoints enforce the permission server-side. The UI uses the same permission for navigation and route gating, but those checks are only a UX layer.

## Public Storefront Additions

The customer storefront uses three narrowly scoped read contracts in addition
to the existing catalog and inventory APIs:

- `GET /api/v1/products/slug/{slug}` returns the same public product response
  as the UUID product lookup, using the product's public slug.
- `GET /api/v1/categories/slug/{slug}` returns the same public category
  response as the UUID category lookup, using the category's public slug.
- `GET /api/v1/inventory/availability/{sku}` returns `{sku, available,
  message}`. It exposes only thresholded customer messaging (`In stock` or
  `Out of stock`); quantities, locations, reservations, serials, IMEIs, and
  inventory-unit identifiers remain operational data.

The storefront is served on port `3001` and keeps browser calls same-origin
through its `/backend/{service}` Next rewrites. Service origins are configured
with the `NEXT_PUBLIC_*_API_URL` variables in
`ecommerce-storefront/.env.example`.

## Catalog — category image contracts

Catalog owns category hierarchy, metadata, and the optional single primary
category image. No other service persists category image state.

### `GET /api/v1/categories`, `GET /api/v1/categories/{id}`, `GET /api/v1/categories/slug/{slug}`

- Auth/permission: public read; no bearer token required.
- Response: the existing category fields plus nullable `description` and
  nullable `image` with `{id, url, altText}`. `url` is a stable usable Catalog
  reference; storage keys and provenance are not exposed.
- Errors: `404` for a missing category; `400` for malformed UUID/query input.

### `POST /api/v1/categories/{id}/image`

- Auth/permission: bearer JWT with `CATEGORY_UPDATE`.
- Request: multipart `file` plus optional `altText` form field.
- Validation: JPEG, PNG, or WEBP; maximum 5 MB by default; content signature
  must match the declared media type. If `altText` is omitted, Catalog derives
  customer-friendly text such as `<category name> collection`.
- Behavior: creates or replaces the one primary image. The old Catalog file is
  removed after the replacement metadata is persisted.
- Response: `200 {id,url,altText}`.
- Errors: `400` invalid/empty/oversized image or alt text; `401` unauthenticated;
  `403` missing `CATEGORY_UPDATE`; `404` missing category; `500` storage failure.

### `PUT /api/v1/categories/{id}/image`

- Auth/permission: bearer JWT with `CATEGORY_UPDATE`.
- Request: JSON `{"altText":"Electronics collection"}`.
- Response: `200 {id,url,altText}`.
- Errors: `400` blank or overlong alt text; `401`; `403`; `404` missing category
  or image.

### `GET /api/v1/categories/{id}/image/file`

- Auth/permission: public read for public category browsing; no bearer token
  required.
- Response: inline JPEG, PNG, or WEBP bytes from Catalog's storage abstraction.
- Errors: `404` when the category/image metadata or stored file is missing.

### `DELETE /api/v1/categories/{id}/image`

- Auth/permission: bearer JWT with `CATEGORY_UPDATE`.
- Request: none.
- Response: `204 No Content`.
- Errors: `401`; `403`; `404` missing category/image; `500` storage failure.

Deleting a category through `DELETE /api/v1/categories/{id}` also deletes its
Catalog image metadata and stored file after the existing child/product safety
checks pass.

## Commerce MVP completion contracts

### Order checkout pricing

`POST /api/v1/cart/checkout` and `POST /api/v1/orders` accept optional
`couponCode` and `serviceLevel` (`STANDARD` or `EXPRESS`). Order Service owns
the resulting `discountAmount`, `shippingAmount`, `taxableAmount`, `taxRate`,
`taxAmount`, `couponCode`, and `totalAmount`; Cart estimates are not authoritative.

The current configured model is free standard shipping for INR/IN orders above
INR 500, otherwise INR 79 standard or INR 149 express. Express remains paid
even when standard shipping is free. `POST /api/v1/orders/shipping-preview`
returns the authoritative Standard and Express amounts before order creation.
Admins can read the active values with `GET /api/v1/admin/shipping/settings`
(`SHIPPING_READ`) and update them with `PUT /api/v1/admin/shipping/settings`
(`SHIPPING_MANAGE`). The editable fields are `freeShippingThreshold`,
`standardShippingCharge`, `expressShippingCharge`, and the comma-separated
`freeShippingCountries`. The `ORDER_*` environment variables remain the
fallback defaults until an admin saves a persisted settings record.
INR/IN tax is 18% of net merchandise plus shipping. This is an MVP model, not
GST compliance.

### Coupons

- `POST /api/v1/orders/coupon-preview` — authenticated customer coupon validation and discount preview; it does not consume coupon usage.
- `POST /api/v1/orders/{orderId}/items/{itemId}/cancel` — authenticated order owner can cancel an active item while its order has not reached `SHIPPED`; releasing the inventory reservation is coordinated by Order Service.
- `GET /api/v1/admin/coupons` — `COUPON_READ`.
- `POST /api/v1/admin/coupons` and `PUT /api/v1/admin/coupons/{id}` —
  `COUPON_MANAGE`.
- `POST /api/v1/admin/coupons/{id}/active?active=true|false` —
  `COUPON_MANAGE`.

Coupon usage limits are checked under a row lock. Invalid, expired, minimum
order, total usage, and per-customer usage failures return a controlled API
error.

### Invoices

- `GET /api/v1/orders/{orderId}/invoice` — authenticated customer owner.
- `GET /api/v1/admin/orders/{orderId}/invoice` — `ORDER_READ`.

The response is an `application/pdf` attachment generated from the historical
order, item, shipping-address, payment, and total snapshots. The default
storage adapter keeps the bytes in Order Service's database and can be
replaced through `InvoiceStorage`.

### Returns and refunds

- `POST /api/v1/returns`, `GET /api/v1/returns/my`, and
  `GET /api/v1/returns/{id}` are customer-owned endpoints.
- `GET /api/v1/admin/returns` and `GET /api/v1/admin/returns/{id}` require
  `RETURN_READ`.
- `POST /api/v1/admin/returns/{id}/{action}` requires `RETURN_MANAGE`; actions
  use persisted return states such as `APPROVED`, `RECEIVED`, `COMPLETED`, and
  `REJECTED`.

Completion computes a server-side item refund and calls Payment Service's
protected internal refund endpoint with a stable return-number idempotency key.
Shipping is excluded from the current refundable amount.

### Password recovery, wishlist, and reviews

- `POST /api/v1/auth/forgot-password` always returns a generic success message.
- `POST /api/v1/auth/reset-password` accepts the raw one-time token; only its
  SHA-256 hash is persisted and tokens expire after the configured TTL.
- `GET|POST /api/v1/customers/me/wishlist` and
  `DELETE /api/v1/customers/me/wishlist/{id}` are customer-owned.
- `GET /api/v1/products/{productId}/reviews` and `/reviews/summary` are public;
  both accept an optional `sku` query parameter to scope ratings and review
  lists to a product SKU. `POST /api/v1/products/{productId}/reviews` accepts
  the SKU and requires a delivered/completed order containing that exact SKU.
  Admin review list/moderation endpoints live under `/api/v1/admin/reviews` and
  require `REVIEW_READ` or `REVIEW_MODERATE`.
