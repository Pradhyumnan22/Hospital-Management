import { useCallback, useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import type { Notice } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Empty, PageHeader } from "../components/ui";
import { usePolling } from "../hooks/usePolling";
import { formatWhen } from "../lib/labels";

export function AlertsPage() {
  const { user } = useAuth();
  const canAck = user?.role === "ADMIN" || user?.role === "ADMISSION_STAFF";
  const [rows, setRows] = useState<Notice[] | null>(null);
  const [error, setError] = useState("");
  const load = useCallback(() => {
    api<Notice[]>("/notifications").then((next) => {
      setRows(next);
      setError("");
    }).catch(() => setError("Could not load alerts."));
  }, []);
  useEffect(() => { load(); }, [load]);
  usePolling(load, 20000);

  async function acknowledge(id: number) {
    try {
      await api(`/notifications/${id}/acknowledge`, { method: "POST" });
      load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not acknowledge the alert.");
    }
  }

  return (
    <div>
      <PageHeader title="Alerts" subtitle="Capacity warnings are created from measured occupancy at 85% and 90%." />
      {error && <p className="mb-4 text-sm text-rose-700">{error}</p>}
      {!rows ? <Empty>Loading alerts…</Empty> : rows.length === 0 ? <Empty>No alerts yet.</Empty> : (
        <ul className="space-y-3">
          {rows.map((row) => (
            <li key={row.id} className="rounded-xl border border-slate-200 bg-white p-4">
              <p className="text-xs uppercase text-slate-500">{row.severity} · {formatWhen(row.createdAt)}</p>
              <p className="font-medium">{row.title}</p>
              <p className="text-sm text-slate-600">{row.message}</p>
              {canAck && !row.acknowledged && (
                <button className="mt-2 text-sm text-teal-700" type="button" onClick={() => void acknowledge(row.id)}>Acknowledge</button>
              )}
              {row.acknowledged && <p className="mt-2 text-xs text-slate-500">Acknowledged</p>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
