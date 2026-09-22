import test from "node:test";
import assert from "node:assert/strict";
import { canRefundPayment, canRetryPayment } from "../lib/api/payment/action-rules.ts";
import { routeAccess } from "../lib/permissions.ts";
import { serviceAccessSummary } from "../lib/access-model.ts";

test("payment routes require PAYMENT_READ", () => {
  assert.deepEqual(routeAccess("/payments"), { allOf: ["PAYMENT_READ"] });
  assert.deepEqual(routeAccess("/payments/payment-id"), { allOf: ["PAYMENT_READ"] });
});

test("payment actions require both state and permission", () => {
  assert.equal(canRefundPayment("CAPTURED", ["PAYMENT_REFUND"]), true);
  assert.equal(canRefundPayment("FAILED", ["PAYMENT_REFUND"]), false);
  assert.equal(canRefundPayment("CAPTURED", ["PAYMENT_READ"]), false);
  assert.equal(canRetryPayment("FAILED", ["PAYMENT_RETRY"]), true);
  assert.equal(canRetryPayment("CAPTURED", ["PAYMENT_RETRY"]), false);
});

test("payment permissions appear in derived service access", () => {
  const access = serviceAccessSummary([
    { code: "PAYMENT_READ", description: "Read payment operations" },
    { code: "PAYMENT_REFUND", description: "Issue refunds" },
  ]);
  assert.deepEqual(access.map((entry) => entry.service), ["Payments"]);
  assert.deepEqual(access[0].permissions.map((permission) => permission.code), ["PAYMENT_READ", "PAYMENT_REFUND"]);
});
