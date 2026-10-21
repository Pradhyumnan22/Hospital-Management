import type { BedState } from "../api/types";

export function bedTone(state: BedState): string {
  switch (state) {
    case "AVAILABLE":
      return "bg-emerald-100 text-emerald-900 border-emerald-300";
    case "OCCUPIED":
      return "bg-rose-100 text-rose-900 border-rose-300";
    case "CLEANING":
      return "bg-amber-100 text-amber-900 border-amber-300";
    case "OUT_OF_SERVICE":
      return "bg-slate-200 text-slate-700 border-slate-300";
  }
}

export function formatWhen(value: string | null): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}
