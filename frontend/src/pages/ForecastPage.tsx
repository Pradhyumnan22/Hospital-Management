import { useCallback, useEffect, useState } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api } from "../api/client";
import type { Forecast } from "../api/types";
import { Empty, PageHeader } from "../components/ui";
import { usePolling } from "../hooks/usePolling";

export function ForecastPage() {
  const [forecast, setForecast] = useState<Forecast | null>(null);
  const [error, setError] = useState("");
  const load = useCallback(() => {
    api<Forecast>("/analytics/forecast").then((next) => {
      setForecast(next);
      setError("");
    }).catch(() => setError("Could not load the forecast."));
  }, []);
  useEffect(() => { load(); }, [load]);
  usePolling(load, 20000);

  return (
    <div>
      <PageHeader title="Occupancy forecast" subtitle="Predicted series are a rolling average, not a measurement and not a trained model." />
      {error && <p className="mb-4 text-sm text-rose-700">{error}</p>}
      {!forecast ? <Empty>Loading forecast…</Empty> : (
        <div className="space-y-4">
          <p className="rounded-xl bg-white p-4 text-sm text-slate-700">
            {forecast.method}. Average daily net change {forecast.averageDailyNetChange}.
            {forecast.lowConfidence ? " Confidence is low because the history window has no flow." : " History includes recent admissions and discharges."}
          </p>
          <p className="text-sm text-slate-500">{forecast.limitations}</p>
          <Chart title="Next 7 days (predicted occupied beds)" data={forecast.next7Days.map((point) => ({ label: point.date.slice(5), occupied: point.predictedOccupied }))} />
          <Chart title="Next 24 hours (predicted occupied beds)" data={forecast.next24Hours.map((point) => ({ label: `${point.hourOffset}h`, occupied: point.predictedOccupied }))} />
          <Chart title="Recent admissions and discharges" data={forecast.history.map((point) => ({ label: point.date.slice(5), occupied: point.admissions }))} />
        </div>
      )}
    </div>
  );
}

function Chart({ title, data }: { title: string; data: { label: string; occupied: number }[] }) {
  return (
    <section className="h-72 rounded-2xl border border-slate-200 bg-white p-4">
      <h2 className="mb-2 text-sm font-semibold">{title}</h2>
      <ResponsiveContainer width="100%" height="85%">
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="label" />
          <YAxis allowDecimals={false} />
          <Tooltip />
          <Line type="monotone" dataKey="occupied" stroke="#0f766e" dot={false} name="Count" />
        </LineChart>
      </ResponsiveContainer>
    </section>
  );
}
