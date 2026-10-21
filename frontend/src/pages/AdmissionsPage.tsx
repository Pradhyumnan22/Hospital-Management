import { useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import type { Admission } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Banner, Empty, PageHeader } from "../components/ui";
import { formatWhen } from "../lib/labels";

export function AdmissionsPage() {
  const { user } = useAuth();
  const canEdit = user?.role === "ADMIN" || user?.role === "ADMISSION_STAFF";
  const [rows, setRows] = useState<Admission[] | null>(null);
  const [notes, setNotes] = useState<Record<number, string>>({});
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  function load() {
    api<Admission[]>("/admissions?status=ACTIVE").then(setRows).catch(() => setError("Could not load admissions."));
  }
  useEffect(() => { load(); }, []);

  async function discharge(id: number) {
    try {
      await api(`/admissions/${id}/discharge`, { method: "POST", body: JSON.stringify({ notes: notes[id] ?? "" }) });
      setMessage("Patient discharged. The bed is now cleaning.");
      load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Discharge failed.");
    }
  }

  return (
    <div>
      <PageHeader title="Active admissions" subtitle="Discharge keeps the history row and moves the bed to cleaning." />
      {error && <Banner tone="error">{error}</Banner>}
      {message && <Banner tone="ok">{message}</Banner>}
      {!rows ? <Empty>Loading admissions…</Empty> : rows.length === 0 ? <Empty>No active admissions.</Empty> : (
        <ul className="space-y-3">
          {rows.map((row) => (
            <li key={row.id} className="rounded-xl border border-slate-200 bg-white p-4">
              <p className="font-medium">{row.patientName} · {row.medicalRecordNumber}</p>
              <p className="text-sm text-slate-600">{row.wardName} {row.bedCode} · {row.diagnosis}</p>
              <p className="text-xs text-slate-500">Admitted {formatWhen(row.admittedAt)} by {row.admittedBy}</p>
              {canEdit && (
                <div className="mt-3 flex flex-col gap-2 sm:flex-row">
                  <input className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="Discharge notes" value={notes[row.id] ?? ""} onChange={(event) => setNotes({ ...notes, [row.id]: event.target.value })} />
                  <button className="rounded-lg bg-slate-900 px-3 py-2 text-sm text-white" type="button" onClick={() => void discharge(row.id)}>Discharge</button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
