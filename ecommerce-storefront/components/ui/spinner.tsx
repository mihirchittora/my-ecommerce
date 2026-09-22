export function Spinner({ label = "Loading" }: { label?: string }) {
  return <span className="inline-flex items-center gap-2 text-sm text-ink/65" role="status"><span className="h-4 w-4 animate-spin rounded-full border-2 border-ink/20 border-t-moss" />{label}</span>;
}
