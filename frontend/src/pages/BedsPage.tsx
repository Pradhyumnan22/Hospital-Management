import { FormEvent, useCallback, useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import type { Bed, BedState, Ward } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Banner, Empty, Field, PageHeader, inputClass } from "../components/ui";
import { usePolling } from "../hooks/usePolling";
import { bedTone } from "../lib/labels";

export function BedsPage() {
  const { user } = useAuth();
  const [beds, setBeds] = useState<Bed[] | null>(null);
  const [wards, setWards] = useState<Ward[]>([]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [form, setForm] = useState({ wardId: "", bedCode: "", isolationCapable: false, ventilator: false, monitor: false, oxygen: false });

  const load = useCallback(() => {
    Promise.all([api<Bed[]>("/beds"), api<Ward[]>("/wards")])
      .then(([nextBeds, nextWards]) => {
        setBeds(nextBeds);
        setWards(nextWards);
        setError("");
      })
      .catch(() => setError("Could not load beds."));
  }, []);

  useEffect(() => { load(); }, [load]);
  usePolling(load, 20000);

  async function createBed(event: FormEvent) {
    event.preventDefault();
    try {
      await api("/beds", { method: "POST", body: JSON.stringify({ ...form, wardId: Number(form.wardId) }) });
      setMessage("Bed added.");
      setForm({ wardId: "", bedCode: "", isolationCapable: false, ventilator: false, monitor: false, oxygen: false });
      load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not add the bed.");
    }
  }

  async function remove(bed: Bed) {
    if (!window.confirm(`Delete bed ${bed.bedCode}? Beds used by an admission cannot be deleted.`)) return;
    try {
      await api(`/beds/${bed.id}`, { method: "DELETE" });
      setMessage(`${bed.bedCode} deleted.`);
      load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not delete the bed.");
    }
  }

  async function changeState(bed: Bed, state: BedState) {
    try {
      await api(`/beds/${bed.id}/state`, { method: "PATCH", body: JSON.stringify({ state }) });
      setMessage(`${bed.bedCode} is now ${state}.`);
      load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "State change was rejected.");
    }
  }

  return (
    <div>
      <PageHeader title="Beds" subtitle="Colors show the current state. Occupied and cleaning change only through admission and discharge." />
      {error && <Banner tone="error">{error}</Banner>}
      {message && <Banner tone="ok">{message}</Banner>}
      {user?.role === "ADMIN" && (
        <form onSubmit={createBed} className="mb-6 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:grid-cols-3">
          <Field label="Ward">
            <select className={inputClass} value={form.wardId} onChange={(event) => setForm({ ...form, wardId: event.target.value })}>
              <option value="">Select</option>
              {wards.map((ward) => <option key={ward.id} value={ward.id}>{ward.code}</option>)}
            </select>
          </Field>
          <Field label="Bed code"><input className={inputClass} value={form.bedCode} onChange={(event) => setForm({ ...form, bedCode: event.target.value })} /></Field>
          <div className="flex flex-wrap items-end gap-3 text-sm">
            {(["isolationCapable", "ventilator", "monitor", "oxygen"] as const).map((key) => (
              <label key={key} className="flex items-center gap-1">
                <input type="checkbox" checked={form[key]} onChange={(event) => setForm({ ...form, [key]: event.target.checked })} />
                {key}
              </label>
            ))}
          </div>
          <div><button className="rounded-lg bg-teal-700 px-4 py-2 text-sm text-white" type="submit">Add bed</button></div>
        </form>
      )}
      {!beds ? <Empty>Loading beds…</Empty> : beds.length === 0 ? <Empty>No beds yet.</Empty> : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {beds.map((bed) => (
            <article key={bed.id} className={`rounded-xl border p-3 ${bedTone(bed.state)}`}>
              <p className="text-xs">{bed.wardCode}</p>
              <p className="text-lg font-semibold">{bed.bedCode}</p>
              <p className="text-xs">{bed.state.replaceAll("_", " ")}</p>
              <p className="mt-2 text-xs">
                {[bed.isolationCapable && "Isolation", bed.ventilator && "Ventilator", bed.monitor && "Monitor", bed.oxygen && "Oxygen"].filter(Boolean).join(" · ") || "No special equipment"}
              </p>
              {user?.role === "ADMIN" && bed.state === "CLEANING" && (
                <button className="mt-2 text-xs underline" type="button" onClick={() => void changeState(bed, "AVAILABLE")}>Mark available</button>
              )}
              {user?.role === "ADMIN" && bed.state === "AVAILABLE" && (
                <button className="mt-2 text-xs underline" type="button" onClick={() => void changeState(bed, "OUT_OF_SERVICE")}>Take out of service</button>
              )}
              {user?.role === "ADMIN" && bed.state === "OUT_OF_SERVICE" && (
                <button className="mt-2 text-xs underline" type="button" onClick={() => void changeState(bed, "AVAILABLE")}>Return to service</button>
              )}
              {user?.role === "ADMIN" && bed.state !== "OCCUPIED" && (
                <button className="mt-2 block text-xs underline" type="button" onClick={() => void remove(bed)}>Delete bed</button>
              )}
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
