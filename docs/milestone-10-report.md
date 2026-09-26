# Milestone 10 commerce MVP report

## Delivered

M10 adds the commerce completion layer while keeping the existing service
boundaries intact:

- Order Service now calculates shipping, coupon discounts, taxable amount,
  tax, and the final total from server-owned snapshots. Cart forwards the
  coupon code and `STANDARD`/`EXPRESS` service level.
- Coupon administration is protected by `COUPON_READ` and `COUPON_MANAGE`.
  Coupon usage and per-customer limits are locked server-side and released for
  cancelled or inventory-failed orders.
- Invoice PDFs are generated from the immutable order snapshot and stored
  behind an `InvoiceStorage` abstraction. The current adapter stores bytes in
  Order Service's database and generates a dependency-free PDF.
- Customer cancellation releases reservations. Returns have a persisted
  state machine, server-calculated refundable item totals, and an idempotent
  handoff to Payment Service for refunds.
- Auth exposes generic forgot-password and single-use, expiring reset-token
  flows. The current notification adapter is a safe console adapter; it never
  logs raw tokens.
- Customer wishlist items are customer-owned and catalog reviews require the
  authenticated customer to own a delivered or completed order containing the
  reviewed product.
- Storefront surfaces include coupon/service-level checkout controls, wishlist
  management, review summaries and verified-review submission, password
  recovery, and authoritative post-order totals. Admin navigation includes
  coupons, returns, and review moderation.
- The API-only development seed creates an idempotent `DEMO10` coupon.

## Tax and shipping assumptions

The configured MVP model is deliberately explicit: INR orders shipped to IN
use a configurable 18% rate over net merchandise plus shipping. Other
currencies/countries receive zero tax. Shipping defaults to INR 79 standard,
INR 149 express, and free standard shipping above INR 500. Express remains
paid above the standard threshold. This is not a
GST compliance engine and does not infer CGST, SGST, IGST, place of supply,
HSN/SAC, exemptions, or rounding rules required for statutory invoices.

## Known limitations and deferred work

- Invoice PDF generation is intentionally minimal and does not yet include a
  production tax registration, GST component breakdown, email delivery, or
  object-storage adapter.
- Order/payment/shipping notification delivery is not a complete event-driven
  subsystem; only the password-reset notification port and console adapter are
  present.
- Returns do not yet call an Inventory return-transition endpoint because the
  existing Inventory contract has no customer-return operation. Refunds are
  item-level and exclude shipping.
- Wishlist responses contain product and SKU references; the storefront
  hydrates current Catalog presentation data and does not snapshot wishlist
  prices.
- Integration tests still require the repository's Docker/Testcontainers
  environment. Browser E2E remains a separate opt-in command.
