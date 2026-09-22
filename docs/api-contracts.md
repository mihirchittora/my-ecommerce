# API contracts

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
- Validation: JWT `sub` must be a UUID. First access lazily creates an `ACTIVE` profile.
- Errors: `401` missing/invalid JWT; `403` `INACTIVE`/`BLOCKED`; `500` unexpected.
- Side effects: one idempotent profile insert on first access.
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

The current Payment API does not expose a transition-history timeline, a human
readable order-number/customer-name search, or a dashboard summary endpoint.
Those values are not reconstructed in the browser.

## Authorization

`CART_READ` is seeded by Auth Service migration `V4__add_cart_read_permission.sql` and granted to `SUPER_ADMIN`. The Cart staff endpoints enforce the permission server-side. The UI uses the same permission for navigation and route gating, but those checks are only a UX layer.
