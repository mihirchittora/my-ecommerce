import assert from "node:assert/strict";
import test from "node:test";
import { activeVariants, availabilityLabel, parseDiscoveryState, productIsOutOfStock, selectedVariant, variantIsUnavailable, wrapGalleryIndex } from "../lib/storefront-logic.ts";

const variants = [
  { id: "1", sku: "M-BLK", price: 10, currency: "INR", attributes: { Color: "Black" }, status: "ACTIVE" },
  { id: "2", sku: "M-WHT", price: 11, currency: "INR", attributes: { Color: "White" }, status: "INACTIVE" },
];

test("only active variants can be selected", () => {
  assert.equal(activeVariants(variants).length, 1);
  assert.equal(selectedVariant(variants, "M-WHT"), null);
  assert.equal(selectedVariant(variants, "M-BLK")?.sku, "M-BLK");
});

test("availability labels stay customer friendly", () => {
  assert.equal(availabilityLabel(undefined), "Availability checked at checkout");
  assert.equal(availabilityLabel({ sku: "M-BLK", available: true, message: "Only a few left" }), "Only a few left");
  assert.equal(availabilityLabel({ sku: "M-BLK", available: false, message: "Out of stock" }), "Out of stock");
});

test("out-of-stock logic only marks a product unavailable when every active SKU is unavailable", () => {
  const product = { id: "p1", categoryId: "c1", name: "Coffee", slug: "coffee", description: null, brand: "Brand", expiryDate: null, status: "ACTIVE", variants: [
    variants[0],
    { id: "3", sku: "M-GRN", price: 12, currency: "INR", attributes: { Color: "Green" }, status: "ACTIVE" },
  ], images: [] };
  assert.equal(productIsOutOfStock(product, { "M-BLK": { sku: "M-BLK", available: false, message: "Out of stock" }, "M-GRN": { sku: "M-GRN", available: true, message: "In stock" } }), false);
  assert.equal(productIsOutOfStock(product, { "M-BLK": { sku: "M-BLK", available: false, message: "Out of stock" }, "M-GRN": { sku: "M-GRN", available: false, message: "Out of stock" } }), true);
});

test("unavailable variants are not purchasable and remain understandable", () => {
  assert.equal(variantIsUnavailable(variants[1]), true);
  assert.equal(variantIsUnavailable(variants[0], { sku: "M-BLK", available: false, message: "Out of stock" }), true);
  assert.equal(variantIsUnavailable(variants[0], { sku: "M-BLK", available: true, message: "In stock" }), false);
});

test("discovery state round-trips URL filters and supported sort values", () => {
  const state = parseDiscoveryState(new URLSearchParams("category=phones&availability=in_stock&priceMin=10000&priceMax=50000&brand=Acme&brand=Nova&attribute=color:Black&sort=createdAt,desc&page=2"));
  assert.deepEqual(state, {
    page: 2,
    search: "",
    categoryId: "phones",
    availability: "in_stock",
    priceMin: 10000,
    priceMax: 50000,
    brands: ["Acme", "Nova"],
    attributes: ["color:Black"],
    sort: "createdAt,desc",
  });
});

test("gallery navigation wraps in both directions", () => {
  assert.equal(wrapGalleryIndex(0, -1, 4), 3);
  assert.equal(wrapGalleryIndex(3, 1, 4), 0);
  assert.equal(wrapGalleryIndex(1, 1, 0), 0);
});
