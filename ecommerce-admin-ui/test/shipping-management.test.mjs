import test from "node:test";
import assert from "node:assert/strict";
import { canCancelShipment, canCreateShipment, canDeliverShipment } from "../lib/api/shipping/action-rules.ts";
import { routeAccess } from "../lib/permissions.ts";
import { serviceAccessSummary } from "../lib/access-model.ts";

test("shipping routes require SHIPPING_READ", () => {
  assert.deepEqual(routeAccess("/shipments"), { allOf: ["SHIPPING_READ"] });
  assert.deepEqual(routeAccess("/shipments/shipment-id"), { allOf: ["SHIPPING_READ"] });
  assert.deepEqual(routeAccess("/fulfillments"), { allOf: ["SHIPPING_READ"] });
  assert.deepEqual(routeAccess("/fulfillments/fulfillment-id"), { allOf: ["SHIPPING_READ"] });
});

test("shipping actions require both state and permission", () => {
  assert.equal(canCancelShipment("READY", ["SHIPPING_CANCEL"]), true);
  assert.equal(canCancelShipment("DELIVERED", ["SHIPPING_CANCEL"]), false);
  assert.equal(canCancelShipment("READY", ["SHIPPING_READ"]), false);
  assert.equal(canDeliverShipment("SHIPPED", ["SHIPPING_MANAGE"]), true);
  assert.equal(canDeliverShipment("DELIVERED", ["SHIPPING_MANAGE"]), false);
  assert.equal(canDeliverShipment("SHIPPED", ["SHIPPING_READ"]), false);
  assert.equal(canCreateShipment("READY", ["SHIPPING_CREATE"]), true);
  assert.equal(canCreateShipment("COMPLETED", ["SHIPPING_CREATE"]), false);
});

test("shipping permissions appear in derived service access", () => {
  const access = serviceAccessSummary([
    { code: "SHIPPING_READ", description: "Read shipments" },
    { code: "SHIPPING_TRACK", description: "Read tracking" },
  ]);
  assert.deepEqual(access.map((entry) => entry.service), ["Shipping"]);
  assert.deepEqual(access[0].permissions.map((permission) => permission.code), ["SHIPPING_READ", "SHIPPING_TRACK"]);
});
