import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { Empty, PageHeader } from "../components/ui";
import { formatWhen } from "../lib/labels";

interface AuditEvent {
  id: number;
  actor: string;
  action: string;
  entityType: string;
  entityId: string | null;
  details: string | null;
  createdAt: string;
}

export function AuditPage() {
  const { user } = useAuth();
  const [rows, setRows] = useState<AuditEvent[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    api<AuditEvent[]>("/audit").then(setRows).catch(() => setError("Could not load the audit log."));
  }, [user?.role]);

  if (user && user.role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }

  return (
    <div>
      <PageHeader title="Audit log" subtitle="Administrators can see who registered patients, changed beds, and confirmed admissions. Entries are not deleted." />
      {error && <p className="mb-4 text-sm text-rose-700">{error}</p>}
      {!rows ? <Empty>Loading audit events…</Empty> : rows.length === 0 ? <Empty>No audit events yet.</Empty> : (
        <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-500">
              <tr><th className="px-4 py-3">When</th><th className="px-4 py-3">Actor</th><th className="px-4 py-3">Action</th><th className="px-4 py-3">Details</th></tr>
            </thead>
            <tbody>
              {rows.map((event) => (
                <tr key={event.id} className="border-t border-slate-100">
                  <td className="px-4 py-3 whitespace-nowrap">{formatWhen(event.createdAt)}</td>
                  <td className="px-4 py-3">{event.actor}</td>
                  <td className="px-4 py-3">{event.action}</td>
                  <td className="px-4 py-3">{event.details ?? event.entityType}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
