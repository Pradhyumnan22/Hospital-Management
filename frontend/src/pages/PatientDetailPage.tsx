import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import type { Admission, Patient } from "../api/types";
import { Empty, PageHeader } from "../components/ui";
import { formatWhen } from "../lib/labels";

export function PatientDetailPage() {
  const params = useParams();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [history, setHistory] = useState<Admission[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    const id = params.id;
    if (!id) return;
    Promise.all([api<Patient>(`/patients/${id}`), api<Admission[]>(`/patients/${id}/admissions`)])
      .then(([nextPatient, nextHistory]) => {
        setPatient(nextPatient);
        setHistory(nextHistory);
      })
      .catch(() => setError("Patient could not be loaded."));
  }, [params.id]);

  return (
    <div>
      <Link className="text-sm text-teal-700" to="/patients">Back to patients</Link>
      {error && <p className="mt-4 text-sm text-rose-700">{error}</p>}
      {!patient ? <div className="mt-4"><Empty>Loading patient…</Empty></div> : (
        <>
          <PageHeader title={`${patient.firstName} ${patient.lastName}`} subtitle={`${patient.medicalRecordNumber} · ${patient.sex} · born ${patient.dateOfBirth}`} />
          {history.length === 0 ? <Empty>No admission history.</Empty> : (
            <ul className="space-y-3">
              {history.map((item) => (
                <li key={item.id} className="rounded-xl border border-slate-200 bg-white p-4 text-sm">
                  <p className="font-medium">{item.status} · {item.wardCode} {item.bedCode}</p>
                  <p className="text-slate-600">{item.diagnosis}</p>
                  <p className="text-slate-500">Admitted {formatWhen(item.admittedAt)} · Discharged {formatWhen(item.dischargedAt)}</p>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}
