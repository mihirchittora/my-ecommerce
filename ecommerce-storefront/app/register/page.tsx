import { Suspense } from "react";
import { AuthForm } from "@/components/auth-form";
import { LoadingBlock } from "@/components/feedback";

export const metadata = { title: "Create an account" };
export default function RegisterPage() { return <Suspense fallback={<LoadingBlock label="Loading registration" />}><AuthForm mode="register" /></Suspense>; }
