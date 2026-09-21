import test from "node:test";
import assert from "node:assert/strict";
import { effectivePermissions, serviceAccessSummary } from "../lib/access-model.ts";

test("effective permissions are the de-duplicated union of assigned role permissions", () => {
  const roles = [
    { name: "CATALOG_ADMIN", permissions: [{ code: "PRODUCT_READ", description: "Read products" }, { code: "PRODUCT_UPDATE", description: "Update products" }] },
    { name: "SUPPORT", permissions: [{ code: "PRODUCT_READ", description: "Read products" }, { code: "CUSTOMER_READ", description: "Read customers" }] },
  ];
  assert.deepEqual(effectivePermissions(["CATALOG_ADMIN", "SUPPORT"], roles).map((permission) => permission.code), ["CUSTOMER_READ", "PRODUCT_READ", "PRODUCT_UPDATE"]);
});

test("service access is derived from permission domains", () => {
  const access = serviceAccessSummary([
    { code: "INVENTORY_READ", description: null },
    { code: "CUSTOMER_READ", description: null },
    { code: "USER_READ", description: null },
  ]);
  assert.deepEqual(access.map((entry) => entry.service), ["Customer", "Inventory", "Users & Access"]);
  assert.equal(access.find((entry) => entry.service === "Inventory")?.permissions[0].code, "INVENTORY_READ");
});

