export type AnalyticsEvent = "page_view" | "product_view" | "search" | "add_to_cart" | "remove_from_cart" | "begin_checkout" | "purchase" | "refund";

export function track(event: AnalyticsEvent, properties: Record<string, string | number | boolean | undefined> = {}) {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new CustomEvent("storefront:analytics", { detail: { event, properties } }));
}
