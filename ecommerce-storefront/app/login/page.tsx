import { Suspense } from "react";
import { AuthForm } from "@/components/auth-form";
import { LoadingBlock } from "@/components/feedback";

export const metadata = { title: "Sign in" };
export default function LoginPage() { return <Suspense fallback={<LoadingBlock label="Loading sign in" />}><AuthForm mode="login" /></Suspense>; }
