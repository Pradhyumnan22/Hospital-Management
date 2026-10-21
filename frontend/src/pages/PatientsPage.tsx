import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../api/client";
import type { Patient, Sex } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Banner, Empty, Field, PageHeader, inputClass } from "../components/ui";

const sexes: Sex[] = ["FEMALE", "MALE", "OTHER", "UNKNOWN"];

export function PatientsPage() {
  const { user } = useAuth();
  const canEdit = user?.role === "ADMIN" || user?.role === "ADMISSION_STAFF";
  const [rows, setRows] = useState<Patient[] | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [form, setForm] = useState({ medicalRecordNumber: "", firstName: "", lastName: "", dateOfBirth: "", sex: "UNKNOWN" as Sex, phone: "" });

  function load() {
    api<Patient[]>("/patients").then(setRows).catch(() => setError("Could not load patients."));
  }

  useEffect(() => { load(); }, []);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (!form.medicalRecordNumber || !form.firstName || !form.lastName || !form.dateOfBirth) {
      setError("Medical record number, name, and date of birth are required.");
      return;
    }
    setError("");
    try {
      await api("/patients", { method: "POST", body: JSON.stringify({ ...form, phone: form.phone || null }) });
      setMessage("Patient registered.");
      setForm({ medicalRecordNumber: "", firstName: "", lastName: "", dateOfBirth: "", sex: "UNKNOWN", phone: "" });
      load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not register the patient.");
    }
  }

  return (
    <div>
      <PageHeader title="Patients" subtitle="Synthetic records only. Registration is limited to admission staff and administrators." />
      {error && <Banner tone="error">{error}</Banner>}
      {message && <Banner tone="ok">{message}</Banner>}
      {canEdit && (
        <form onSubmit={onSubmit} className="mb-6 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:grid-cols-3">
          <Field label="MRN"><input className={inputClass} value={form.medicalRecordNumber} onChange={(event) => setForm({ ...form, medicalRecordNumber: event.target.value })} /></Field>
          <Field label="First name"><input className={inputClass} value={form.firstName} onChange={(event) => setForm({ ...form, firstName: event.target.value })} /></Field>
          <Field label="Last name"><input className={inputClass} value={form.lastName} onChange={(event) => setForm({ ...form, lastName: event.target.value })} /></Field>
          <Field label="Date of birth"><input className={inputClass} type="date" value={form.dateOfBirth} onChange={(event) => setForm({ ...form, dateOfBirth: event.target.value })} /></Field>
          <Field label="Sex">
            <select className={inputClass} value={form.sex} onChange={(event) => setForm({ ...form, sex: event.target.value as Sex })}>
              {sexes.map((sex) => <option key={sex}>{sex}</option>)}
            </select>
          </Field>
          <Field label="Phone"><input className={inputClass} value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} /></Field>
          <div className="md:col-span-3"><button className="rounded-lg bg-teal-700 px-4 py-2 text-sm text-white" type="submit">Register patient</button></div>
        </form>
      )}
      {!rows ? <Empty>Loading patients…</Empty> : rows.length === 0 ? <Empty>No patients yet.</Empty> : (
        <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-500"><tr><th className="px-4 py-3">MRN</th><th className="px-4 py-3">Name</th><th className="px-4 py-3">Born</th><th className="px-4 py-3"></th></tr></thead>
            <tbody>
              {rows.map((patient) => (
                <tr key={patient.id} className="border-t border-slate-100">
                  <td className="px-4 py-3">{patient.medicalRecordNumber}</td>
                  <td className="px-4 py-3">{patient.firstName} {patient.lastName}</td>
                  <td className="px-4 py-3">{patient.dateOfBirth}</td>
                  <td className="px-4 py-3"><Link className="text-teal-700" to={`/patients/${patient.id}`}>History</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
