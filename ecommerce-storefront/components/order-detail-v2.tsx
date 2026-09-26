"use client";

import Link from "next/link";
import { ArrowLeft, CheckCircle2, Clock3, FileText, MapPin, Package, RotateCcw, Truck } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { catalogApi } from "@/lib/api/catalog";
import { orderApi, type ReturnReason } from "@/lib/api/order";
import { paymentApi } from "@/lib/api/payment";
import { shippingApi } from "@/lib/api/shipping";
import { getAuthSession } from "@/lib/auth-session";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ErrorState, LoadingBlock } from "@/components/feedback";
import { formatCurrency, formatDate, getApiBaseUrl, variantLabel } from "@/lib/utils";
import type { OrderDetail as OrderDetailType, OrderItem } from "@/lib/types";

async function downloadInvoice(orderId: string) {
  const session = getAuthSession();
  const response = await fetch(`${getApiBaseUrl("order")}/v1/orders/${encodeURIComponent(orderId)}/invoice`, {
    headers: session?.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : undefined,
  });
  if (!response.ok) throw new Error("Invoice unavailable");
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `invoice-${orderId}.pdf`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function PaymentStatus({ order }: { order: OrderDetailType }) {
  const payments = useQuery({ queryKey: ["payments", order.id], queryFn: () => paymentApi.forOrder(order.id) });
  const payment = payments.data?.[0];
  if (!payment && !payments.isLoading) return null;
  return <Card className="p-6"><p className="eyebrow">Payment</p><div className="mt-3 flex items-center justify-between gap-3"><span className="text-sm font-semibold">{payment?.paymentMethod === "CASH_ON_DELIVERY" ? "Cash on delivery" : payment?.provider ?? "Online payment"}</span>{payment ? <Badge tone={payment.status === "CAPTURED" ? "success" : payment.status === "FAILED" ? "danger" : "warning"}>{payment.status.replaceAll("_", " ")}</Badge> : <span className="text-sm text-ink/50">Loading…</span>}</div>{payment ? <p className="mt-3 text-sm text-ink/60">{formatCurrency(payment.amount, payment.currency)}</p> : null}</Card>;
}

function productIdForItem(item: OrderItem) {
  const productId = item.variantSnapshot?.productId;
  return typeof productId === "string" ? productId : null;
}

function ItemReviewPanel({ order, item }: { order: OrderDetailType; item: OrderItem }) {
  const productId = productIdForItem(item);
  const product = useQuery({ queryKey: ["review-product", productId], queryFn: () => catalogApi.getProductById(productId ?? ""), enabled: Boolean(productId), staleTime: 5 * 60_000 });
  const reviewReady = ["DELIVERED", "COMPLETED"].includes(order.status);
  const queryClient = useQueryClient();
  const myReview = useQuery({ queryKey: ["my-review", productId, item.sku], queryFn: () => catalogApi.getMyReview(productId ?? "", item.sku, item.id), enabled: Boolean(productId && reviewReady), staleTime: 30_000, retry: false });
  const [rating, setRating] = useState(5);
  const [title, setTitle] = useState("");
  const [comment, setComment] = useState("");
  const [editing, setEditing] = useState(false);
  const review = myReview.data;
  const saveReview = useMutation({
    mutationFn: () => review
      ? catalogApi.updateReview(review.id, { rating, title: title.trim() || undefined, comment: comment.trim() })
      : catalogApi.createReview(productId ?? "", { orderId: order.id, orderItemId: item.id, sku: item.sku, rating, title: title.trim() || undefined, comment: comment.trim() }),
    onSuccess: (saved) => {
      queryClient.setQueryData(["my-review", productId, item.sku], saved);
      setEditing(false);
    },
  });
  const editReview = () => {
    if (!review) return;
    setRating(review.rating);
    setTitle(review.title ?? "");
    setComment(review.comment);
    setEditing(true);
  };

  if (item.status === "CANCELLED") return null;
  const productLink = product.data?.slug ? `/products/${encodeURIComponent(product.data.slug)}#reviews-heading` : undefined;
  if (!productId) return <div id={`review-${item.id}`} className="mt-5 rounded-2xl border border-ink/10 bg-mist p-4"><p className="text-sm font-semibold">Review this item</p><p className="mt-2 text-xs leading-5 text-ink/55">This order item is missing its product reference, so a verified review cannot be opened.</p></div>;

  return <div id={`review-${item.id}`} className="mt-5 rounded-2xl bg-mist p-4"><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-sm font-bold">{review ? "Your review" : "Review this item"}</p>{productLink ? <Link href={productLink} className="mt-1 inline-flex text-xs font-semibold text-moss underline">View product details</Link> : null}</div>{reviewReady ? <Badge tone="success">{review ? review.status === "PENDING" ? "Pending moderation" : review.status === "APPROVED" ? "Published review" : "Review needs attention" : "Ready to review"}</Badge> : <Badge>After delivery</Badge>}</div>{!reviewReady ? <p className="mt-3 text-sm leading-6 text-ink/55">The review form will be available after this order is delivered. Current order status: {order.status.replaceAll("_", " ")}.</p> : myReview.isLoading ? <p className="mt-3 text-sm text-ink/55">Checking your review…</p> : review && !editing ? <div className="mt-3 rounded-2xl border border-ink/10 bg-white p-4"><p className="text-lg tracking-wide text-coral">{"★".repeat(review.rating)}{"☆".repeat(5 - review.rating)}</p>{review.title ? <p className="mt-2 font-semibold">{review.title}</p> : null}<p className="mt-2 text-sm leading-6 text-ink/65">{review.comment}</p><div className="mt-4 flex items-center justify-between gap-3"><p className="text-xs text-ink/45">Last updated {formatDate(review.updatedAt, true)}</p><Button variant="secondary" onClick={editReview}>Edit review</Button></div></div> : <div className="mt-3 grid gap-3">{review ? <p className="text-sm text-ink/60">Update your review and submit it again for moderation.</p> : null}<label className="text-sm"><span className="field-label">Rating</span><select className="mt-1 h-10 w-full rounded-xl border border-ink/15 bg-white px-3" value={rating} onChange={(event) => setRating(Number(event.target.value))}>{[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{"★".repeat(value)}{"☆".repeat(5 - value)} ({value}/5)</option>)}</select></label><input className="h-10 rounded-xl border border-ink/15 bg-white px-3 text-sm" placeholder="Title (optional)" value={title} onChange={(event) => setTitle(event.target.value)} /><textarea className="min-h-24 rounded-xl border border-ink/15 bg-white p-3 text-sm" placeholder="Tell us about this SKU" value={comment} onChange={(event) => setComment(event.target.value)} /><div className="flex flex-wrap gap-2"><Button disabled={saveReview.isPending || !comment.trim()} onClick={() => saveReview.mutate()}>{saveReview.isPending ? review ? "Saving…" : "Submitting…" : review ? "Save changes" : "Submit review"}</Button>{review ? <Button variant="ghost" onClick={() => setEditing(false)} disabled={saveReview.isPending}>Cancel</Button> : null}</div>{saveReview.isError ? <p className="text-sm text-red-700">We could not save your review. Please try again.</p> : null}</div>}</div>;
}

function ReturnPanel({ order }: { order: OrderDetailType }) {
  const returnableItems = order.items.filter((item) => item.status !== "CANCELLED");
  const [lines, setLines] = useState<Record<string, { selected: boolean; quantity: number; reason: ReturnReason }>>(() => Object.fromEntries(returnableItems.map((item) => [item.id, { selected: false, quantity: item.quantity, reason: "OTHER" as ReturnReason }])));
  const [comment, setComment] = useState("");
  const selectedItems = returnableItems.filter((item) => lines[item.id]?.selected).map((item) => ({ orderItemId: item.id, quantity: Math.min(item.quantity, Math.max(1, lines[item.id]?.quantity ?? 1)), reason: lines[item.id]?.reason ?? "OTHER" }));
  const create = useMutation({ mutationFn: () => orderApi.createReturn({ orderId: order.id, comment, items: selectedItems }) });
  const updateLine = (itemId: string, changes: Partial<{ selected: boolean; quantity: number; reason: ReturnReason }>) => setLines((current) => ({ ...current, [itemId]: { ...current[itemId], ...changes } }));
  if (!returnableItems.length || !["DELIVERED", "COMPLETED"].includes(order.status)) return null;
  return <Card className="p-6"><p className="eyebrow">Need help?</p><h2 className="mt-2 font-display text-xl font-bold">Request a return</h2><p className="mt-2 text-sm leading-6 text-ink/60">Choose the items you want to send back. Order Service checks the return window and refund amount.</p><div className="mt-5 grid gap-3">{returnableItems.map((item) => { const line = lines[item.id]; return <label key={item.id} className={`rounded-2xl border p-4 ${line?.selected ? "border-moss bg-mist" : "border-ink/10 bg-white"}`}><div className="flex items-start gap-3"><input type="checkbox" checked={Boolean(line?.selected)} onChange={(event) => updateLine(item.id, { selected: event.target.checked })} className="mt-1 h-4 w-4 rounded border-ink/20 text-moss" /><span className="min-w-0 flex-1"><span className="block text-sm font-semibold">{item.productNameSnapshot}</span><span className="mt-1 block text-xs text-ink/55">{item.sku} · Purchased qty {item.quantity}</span></span></div>{line?.selected ? <div className="mt-4 grid gap-3 sm:grid-cols-2"><label className="text-sm"><span className="field-label">Quantity</span><input type="number" min={1} max={item.quantity} value={line.quantity} onChange={(event) => updateLine(item.id, { quantity: Number(event.target.value) || 1 })} className="mt-1 h-10 w-full rounded-xl border border-ink/15 bg-white px-3" /></label><label className="text-sm"><span className="field-label">Reason</span><select className="mt-1 h-10 w-full rounded-xl border border-ink/15 bg-white px-3" value={line.reason} onChange={(event) => updateLine(item.id, { reason: event.target.value as ReturnReason })}><option value="OTHER">Other</option><option value="DAMAGED">Damaged</option><option value="WRONG_ITEM">Wrong item</option><option value="DEFECTIVE">Defective</option><option value="NOT_AS_DESCRIBED">Not as described</option><option value="SIZE_FIT">Size or fit</option></select></label></div> : null}</label>; })}<textarea className="min-h-24 rounded-2xl border border-ink/15 p-3 text-sm" placeholder="Add context (optional)" value={comment} onChange={(event) => setComment(event.target.value)} /><Button disabled={create.isPending || !selectedItems.length} onClick={() => create.mutate()}>{create.isPending ? "Submitting…" : "Request return"}</Button>{create.isSuccess ? <p className="text-sm font-semibold text-moss">Return request submitted. <Link href="/account/returns" className="underline">Track it in returns</Link>.</p> : create.isError ? <p className="text-sm text-red-700">This order is not currently eligible for return, or one of the selected items has already been returned.</p> : null}</div></Card>;
}

export function OrderDetail({ id, isNew }: { id: string; isNew: boolean }) {
  const queryClient = useQueryClient();
  const order = useQuery({ queryKey: ["order", id], queryFn: () => orderApi.get(id) });
  const shipments = useQuery({ queryKey: ["shipments", "mine"], queryFn: shippingApi.listMine, enabled: Boolean(order.data) });
  const cancel = useMutation({ mutationFn: () => orderApi.cancel(id), onSuccess: (data) => { queryClient.setQueryData(["order", id], data); void queryClient.invalidateQueries({ queryKey: ["orders"] }); } });
  const cancelItem = useMutation({ mutationFn: (itemId: string) => orderApi.cancelItem(id, itemId), onSuccess: (data) => { queryClient.setQueryData(["order", id], data); void queryClient.invalidateQueries({ queryKey: ["orders"] }); } });
  const [, setInvoiceError] = useState(false);
  if (order.isLoading) return <LoadingBlock label="Loading order" />;
  if (order.isError || !order.data) return <ErrorState message="This order could not be loaded." retry={() => void order.refetch()} />;
  const current = order.data;
  const shipment = shipments.data?.content.find((item) => item.orderId === current.id);
  const cancellable = ["PENDING_RESERVATION", "RESERVED", "PENDING_PAYMENT"].includes(current.status);
  const itemCancellable = ["PENDING_RESERVATION", "RESERVED", "PENDING_PAYMENT", "PAID", "CONFIRMED"].includes(current.status);
  const reviewReady = ["DELIVERED", "COMPLETED"].includes(current.status);
  return <div><Link href="/orders" className="inline-flex items-center gap-2 text-sm font-bold text-moss"><ArrowLeft className="h-4 w-4" />Back to orders</Link>{isNew ? <div className="mt-7 flex items-start gap-4 rounded-3xl bg-sage p-6"><CheckCircle2 className="h-6 w-6 shrink-0 text-moss" /><div><p className="eyebrow">Order placed</p><h1 className="mt-2 font-display text-2xl font-bold">Thanks — your order is being prepared.</h1></div></div> : <div className="mt-8"><p className="eyebrow">Order details</p><h1 className="mt-3 font-display text-4xl font-bold tracking-tight md:text-6xl">{current.orderNumber}</h1><p className="mt-4 text-sm text-ink/55">Placed {formatDate(current.createdAt, true)}</p></div>}<div className="mt-8 flex flex-wrap items-center gap-3"><Badge tone={current.status === "CANCELLED" || current.status === "FAILED" ? "danger" : ["PAID", "CONFIRMED", "DELIVERED", "COMPLETED"].includes(current.status) ? "success" : "warning"}>{current.status.replaceAll("_", " ")}</Badge>{shipment ? <Link href={`/orders/${encodeURIComponent(id)}/tracking`} className="inline-flex min-h-9 items-center gap-2 rounded-full border border-ink/15 bg-white px-4 text-xs font-bold"><Truck className="h-4 w-4" />Track shipment</Link> : null}{reviewReady ? <Link href="#order-reviews" className="inline-flex min-h-9 items-center rounded-full bg-moss px-4 text-xs font-bold text-white">Write a review</Link> : null}<Button variant="secondary" onClick={() => { setInvoiceError(false); void downloadInvoice(id).catch(() => setInvoiceError(true)); }}><FileText className="h-4 w-4" />Download invoice</Button>{cancellable ? <Button variant="ghost" className="text-red-700" disabled={cancel.isPending} onClick={() => { if (window.confirm("Cancel this order?")) cancel.mutate(); }}><RotateCcw className="mr-2 h-4 w-4" />{cancel.isPending ? "Cancelling…" : "Cancel order"}</Button> : null}</div>{cancel.isError ? <p className="mt-3 text-sm text-red-700">This order could not be cancelled in its current state.</p> : null}<div className="mt-8 grid gap-6 lg:grid-cols-[1fr_340px]"><div className="grid gap-6"><Card id="order-reviews" className="p-6"><div className="flex items-center gap-3"><Package className="h-5 w-5 text-moss" /><h2 className="font-display text-xl font-bold">Items and reviews</h2></div><div className="mt-6 grid gap-5">{current.items.map((item) => <div key={item.id} className="border-b border-ink/10 pb-5 last:border-0 last:pb-0"><div className="flex items-start justify-between gap-4"><div><p className="font-semibold">{item.productNameSnapshot}</p><p className="mt-1 text-sm text-ink/55">{variantLabel((item.variantSnapshot.attributes as Record<string, string> | undefined) ?? {}) || item.sku} · Qty {item.quantity}</p><p className="mt-1 text-xs text-ink/40">SKU {item.sku}</p></div><div className="text-right"><div className="text-right"><p className="font-semibold">{formatCurrency(item.subtotal - item.discountAmount + item.taxAmount, item.currency)}</p><p className="mt-1 text-xs text-ink/45">Base {formatCurrency(item.subtotal, item.currency)} · Tax {formatCurrency(item.taxAmount, item.currency)}</p></div>{item.status === "CANCELLED" ? <Badge tone="danger">Cancelled</Badge> : itemCancellable ? <Button variant="ghost" className="mt-2 px-0 text-xs text-red-700" disabled={cancelItem.isPending} onClick={() => { if (window.confirm(`Cancel ${item.productNameSnapshot}?`)) cancelItem.mutate(item.id); }}>{cancelItem.isPending ? "Cancelling…" : "Cancel item"}</Button> : null}</div></div><ItemReviewPanel order={current} item={item} /></div>)}</div></Card><Card className="p-6"><div className="flex items-center gap-3"><MapPin className="h-5 w-5 text-moss" /><h2 className="font-display text-xl font-bold">Shipping address</h2></div>{current.shippingAddress ? <p className="mt-5 text-sm leading-6 text-ink/65">{current.shippingAddress.recipientName}<br />{current.shippingAddress.line1}{current.shippingAddress.line2 ? `, ${current.shippingAddress.line2}` : ""}<br />{current.shippingAddress.city}, {current.shippingAddress.state} {current.shippingAddress.postalCode}<br />{current.shippingAddress.country}</p> : <p className="mt-5 text-sm text-ink/55">Address snapshot unavailable.</p>}</Card><ReturnPanel order={current} /><Card className="p-6"><div className="flex items-center gap-3"><Clock3 className="h-5 w-5 text-moss" /><h2 className="font-display text-xl font-bold">Order history</h2></div><ol className="mt-6 grid gap-5">{[...current.history].reverse().map((event) => <li key={event.id} className="relative pl-8"><span className="absolute left-0 top-0.5 grid h-5 w-5 place-items-center rounded-full bg-sage"><span className="h-2 w-2 rounded-full bg-moss" /></span><p className="text-sm font-semibold">{event.toStatus.replaceAll("_", " ")}{event.eventType === "ITEM_CANCELLED" ? " · Item cancelled" : ""}</p><p className="mt-1 text-xs text-ink/50">{formatDate(event.createdAt, true)}{event.notes ? ` · ${event.notes}` : ""}</p></li>)}</ol></Card></div><aside className="grid h-fit gap-6 lg:sticky lg:top-28"><PaymentStatus order={current} /><Card className="p-6"><p className="eyebrow">Order summary</p><div className="mt-5 grid gap-3 text-sm"><div className="flex justify-between"><span className="text-ink/60">Subtotal</span><span>{formatCurrency(current.subtotal, current.currency)}</span></div><div className="flex justify-between"><span className="text-ink/60">Discount</span><span>-{formatCurrency(current.discountAmount, current.currency)}</span></div><div className="flex justify-between"><span className="text-ink/60">Shipping</span><span>{formatCurrency(current.shippingAmount, current.currency)}</span></div><div className="flex justify-between"><span className="text-ink/60">Taxable amount</span><span>{formatCurrency(current.taxableAmount, current.currency)}</span></div><div className="flex justify-between"><span className="text-ink/60">Tax ({current.taxRate.toFixed(2)}%)</span><span>{formatCurrency(current.taxAmount, current.currency)}</span></div><div className="mt-2 flex justify-between border-t border-ink/10 pt-4"><span className="font-bold">Total</span><span className="font-display text-xl font-bold">{formatCurrency(current.totalAmount, current.currency)}</span></div></div><p className="mt-5 text-xs leading-5 text-ink/45">Totals are historical snapshots owned by Order Service.</p></Card></aside></div></div>;
}
