import { ProfileForm } from "@/components/profile-form";
export const metadata = { title: "Profile" };
export default function ProfilePage() { return <div><h2 className="font-display text-2xl font-bold">Profile details</h2><p className="mt-2 text-sm text-ink/60">Update the details we use to personalize your account.</p><div className="mt-7"><ProfileForm /></div></div>; }
