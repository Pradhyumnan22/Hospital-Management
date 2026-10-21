import type { ReactNode } from "react";

export function PageHeader({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <header className="mb-6">
      <h1 className="text-2xl font-semibold text-slate-900">{title}</h1>
      <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
    </header>
  );
}

export function Banner({ tone, children }: { tone: "error" | "ok"; children: ReactNode }) {
  const color = tone === "error" ? "bg-rose-50 text-rose-800 border-rose-200" : "bg-emerald-50 text-emerald-800 border-emerald-200";
  return <p className={`mb-4 rounded-lg border px-3 py-2 text-sm ${color}`}>{children}</p>;
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium text-slate-700">{label}</span>
      {children}
    </label>
  );
}

export const inputClass = "w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-teal-600";

export function Empty({ children }: { children: ReactNode }) {
  return <p className="rounded-xl border border-dashed border-slate-300 bg-white px-4 py-8 text-center text-sm text-slate-500">{children}</p>;
}
