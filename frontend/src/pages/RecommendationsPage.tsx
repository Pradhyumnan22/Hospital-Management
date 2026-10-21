import { FormEvent, useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import type { Criteria, Patient, Recommendation, WardType } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { RecommendationList } from "../components/RecommendationList";
import { Banner, Field, PageHeader, inputClass } from "../components/ui";

const types: Array<WardType | ""> = ["", "GENERAL", "ICU", "ISOLATION", "PEDIATRIC", "MATERNITY"];

const empty: Criteria = {
  patientId: 0,
  diagnosis: "",
  requiredWardType: "",
  preferredWardType: "",
  isolationRequired: false,
  ventilatorRequired: false,
  monitorRequired: false,
  oxygenRequired: false
};

export function RecommendationsPage() {
  const { user } = useAuth();
  const canAllocate = user?.role === "ADMIN" || user?.role === "ADMISSION_STAFF";
  const [patients, setPatients] = useState<Patient[]>([]);
  const [form, setForm] = useState<Criteria>(empty);
  const [result, setResult] = useState<Recommendation | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    api<Patient[]>("/patients").then(setPatients).catch(() => setError("Could not load patients."));
  }, []);

  function payload() {
    return {
      ...form,
      requiredWardType: form.requiredWardType || null,
      preferredWardType: form.preferredWardType || null
    };
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (!form.patientId || !form.diagnosis.trim()) {
      setError("Choose a patient and enter a working diagnosis.");
      return;
    }
    setError("");
    setMessage("");
    try {
      setResult(await api<Recommendation>("/recommendations", { method: "POST", body: JSON.stringify(payload()) }));
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Recommendation failed.");
    }
  }

  async function allocate(bedId: number) {
    try {
      await api("/admissions", { method: "POST", body: JSON.stringify({ ...payload(), bedId }) });
      setMessage("Bed allocated after a fresh availability check. This remains a staff decision.");
      setResult(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Allocation was rejected.");
    }
  }

  return (
    <div>
      <PageHeader title="Bed recommendations" subtitle="Hard rules remove beds before scoring. Confirming allocation locks the bed and checks the rules again." />
      {error && <Banner tone="error">{error}</Banner>}
      {message && <Banner tone="ok">{message}</Banner>}
      <form onSubmit={onSubmit} className="mb-6 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:grid-cols-2">
        <Field label="Patient">
          <select className={inputClass} value={form.patientId || ""} onChange={(event) => setForm({ ...form, patientId: Number(event.target.value) })}>
            <option value="">Select</option>
            {patients.map((patient) => <option key={patient.id} value={patient.id}>{patient.medicalRecordNumber} · {patient.lastName}</option>)}
          </select>
        </Field>
        <Field label="Diagnosis"><input className={inputClass} value={form.diagnosis} onChange={(event) => setForm({ ...form, diagnosis: event.target.value })} /></Field>
        <Field label="Required ward type">
          <select className={inputClass} value={form.requiredWardType} onChange={(event) => setForm({ ...form, requiredWardType: event.target.value as WardType | "" })}>
            {types.map((type) => <option key={type || "any"} value={type}>{type || "Any"}</option>)}
          </select>
        </Field>
        <Field label="Preferred ward type">
          <select className={inputClass} value={form.preferredWardType} onChange={(event) => setForm({ ...form, preferredWardType: event.target.value as WardType | "" })}>
            {types.map((type) => <option key={`p-${type || "none"}`} value={type}>{type || "None"}</option>)}
          </select>
        </Field>
        <div className="flex flex-wrap gap-3 text-sm md:col-span-2">
          {(["isolationRequired", "ventilatorRequired", "monitorRequired", "oxygenRequired"] as const).map((key) => (
            <label key={key} className="flex items-center gap-1">
              <input type="checkbox" checked={form[key]} onChange={(event) => setForm({ ...form, [key]: event.target.checked })} />
              {key.replace("Required", "")}
            </label>
          ))}
        </div>
        <div><button className="rounded-lg bg-teal-700 px-4 py-2 text-sm text-white" type="submit">Rank beds</button></div>
      </form>
      {result && (
        <div className="space-y-4">
          <RecommendationList result={result} />
          {canAllocate && result.candidates.map((bed) => (
            <button key={bed.bedId} className="mr-2 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm" type="button" onClick={() => void allocate(bed.bedId)}>
              Confirm {bed.bedCode}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
