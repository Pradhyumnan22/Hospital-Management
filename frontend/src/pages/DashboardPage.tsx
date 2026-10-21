import { useCallback, useEffect, useState } from "react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "../api/client";
import type { Notice, Overview } from "../api/types";
import { PageHeader, Empty } from "../components/ui";
import { usePolling } from "../hooks/usePolling";

export function DashboardPage() {
  const [overview, setOverview] = useState<Overview | null>(null);
  const [alerts, setAlerts] = useState<Notice[]>([]);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    Promise.all([api<Overview>("/analytics/overview"), api<Notice[]>("/notifications")])
      .then(([nextOverview, nextAlerts]) => {
        setOverview(nextOverview);
        setAlerts(nextAlerts.filter((item) => !item.acknowledged).slice(0, 4));
        setError("");
      })
      .catch(() => setError("Could not refresh occupancy."));
  }, []);

  useEffect(() => { load(); }, [load]);
  usePolling(load, 20000);

  return (
    <div>
      <PageHeader title="Overview" subtitle="Measured occupancy refreshes every 20 seconds while this tab is visible." />
      {error && <p className="mb-4 text-sm text-rose-700">{error}</p>}
      {!overview ? <Empty>Loading occupancy…</Empty> : (
        <>
          <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <Kpi label="Operational beds" value={overview.operationalBeds} />
            <Kpi label="Occupied" value={overview.occupiedBeds} />
            <Kpi label="Available" value={overview.availableBeds} />
            <Kpi label="Occupancy" value={`${overview.occupancyPercent}%`} />
          </section>
          <section className="mt-6 grid gap-4 lg:grid-cols-[2fr_1fr]">
            <div className="h-80 rounded-2xl border border-slate-200 bg-white p-4">
              <h2 className="mb-3 text-sm font-semibold">Ward occupancy</h2>
              {overview.wards.length === 0 ? <Empty>No wards yet.</Empty> : (
                <ResponsiveContainer width="100%" height="90%">
                  <BarChart data={overview.wards}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="wardCode" />
                    <YAxis />
                    <Tooltip />
                    <Bar dataKey="occupiedBeds" fill="#0f766e" name="Occupied" />
                    <Bar dataKey="availableBeds" fill="#99f6e4" name="Available" />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
            <div className="rounded-2xl border border-slate-200 bg-white p-4">
              <h2 className="text-sm font-semibold">Open alerts</h2>
              <p className="mt-1 text-xs text-slate-500">Cleaning {overview.cleaningBeds} · Out of service {overview.outOfServiceBeds}</p>
              <ul className="mt-3 space-y-2 text-sm">
                {alerts.length === 0 && <li className="text-slate-500">No open capacity alerts.</li>}
                {alerts.map((alert) => (
                  <li key={alert.id} className="rounded-lg bg-amber-50 px-3 py-2">
                    <p className="font-medium">{alert.title}</p>
                    <p className="text-slate-600">{alert.message}</p>
                  </li>
                ))}
              </ul>
            </div>
          </section>
        </>
      )}
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: number | string }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-semibold">{value}</p>
    </article>
  );
}
