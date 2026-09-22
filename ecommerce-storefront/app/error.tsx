"use client";

import { useEffect } from "react";
import { ErrorState } from "@/components/feedback";

export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => { window.dispatchEvent(new CustomEvent("storefront:error", { detail: { message: error.message, digest: error.digest } })); }, [error]);
  return <div className="page-shell py-20"><ErrorState message="This page hit an unexpected issue. Your account and cart are safe." retry={reset} /></div>;
}
