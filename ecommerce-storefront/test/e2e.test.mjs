import assert from "node:assert/strict";
import test from "node:test";

const baseUrl = process.env.STOREFRONT_E2E_BASE_URL;

test("public storefront shell responds", { skip: !baseUrl }, async () => {
  const response = await fetch(baseUrl);
  assert.equal(response.ok, true);
  const html = await response.text();
  assert.match(html, /Morrow|Good things for the way you live/);
});

test("catalog route is reachable", { skip: !baseUrl }, async () => {
  const response = await fetch(`${baseUrl}/products`);
  assert.equal(response.ok, true);
});
