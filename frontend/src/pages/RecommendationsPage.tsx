import { FormEvent, useEffect, useRef, useState } from "react";
import { ApiError, api } from "../api/client";
import type { Patient, Recommendation, WardType } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Banner, Field, PageHeader, inputClass } from "../components/ui";

interface ChatTurn {
  role: "staff" | "assistant";
  text: string;
}

interface ChatReply {
  reply: string;
  diagnosis: string;
  requiredWardType: WardType | null;
  preferredWardType: WardType | null;
  isolationRequired: boolean;
  ventilatorRequired: boolean;
  monitorRequired: boolean;
  oxygenRequired: boolean;
  recommendation: Recommendation;
}

export function RecommendationsPage() {
  const { user } = useAuth();
  const canAllocate = user?.role === "ADMIN" || user?.role === "ADMISSION_STAFF";
  const [patients, setPatients] = useState<Patient[]>([]);
  const [patientId, setPatientId] = useState(0);
  const [draft, setDraft] = useState("");
  const [turns, setTurns] = useState<ChatTurn[]>([
    { role: "assistant", text: "Hi. What's the problem? A stomach ache, chest pain, a fever, anything like that, and I'll suggest a bed." }
  ]);
  const [latest, setLatest] = useState<ChatReply | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [pending, setPending] = useState(false);
  const threadRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const thread = threadRef.current;
    if (thread) {
      thread.scrollTop = thread.scrollHeight;
    }
  }, [turns, pending]);

  useEffect(() => {
    api<Patient[]>("/patients").then(setPatients).catch(() => setError("Could not load patients."));
  }, []);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (!patientId || !draft.trim()) {
      setError("Choose a patient and describe the bed they need.");
      return;
    }
    const text = draft.trim();
    const history = turns.filter((turn) => turn.role === "staff").map((turn) => turn.text);
    setDraft("");
    setError("");
    setMessage("");
    setTurns((current) => [...current, { role: "staff", text }]);
    setPending(true);
    try {
      const reply = await api<ChatReply>("/assistant", {
        method: "POST",
        body: JSON.stringify({ patientId, message: text, history })
      });
      setLatest(reply);
      setTurns((current) => [...current, { role: "assistant", text: reply.reply }]);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "The assistant could not answer.");
    } finally {
      setPending(false);
    }
  }

  async function allocate(bedId: number) {
    if (!latest) {
      return;
    }
    try {
      await api("/admissions", {
        method: "POST",
        body: JSON.stringify({
          patientId,
          bedId,
          diagnosis: latest.diagnosis,
          requiredWardType: latest.requiredWardType,
          preferredWardType: latest.preferredWardType,
          isolationRequired: latest.isolationRequired,
          ventilatorRequired: latest.ventilatorRequired,
          monitorRequired: latest.monitorRequired,
          oxygenRequired: latest.oxygenRequired
        })
      });
      setMessage("Bed allocated. The assistant suggestion was confirmed by staff.");
      setLatest(null);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Allocation was rejected.");
    }
  }

  const top = latest?.recommendation.candidates[0];

  return (
    <div>
      <PageHeader title="Bed assistant" subtitle="Describe the problem in ordinary words. The assistant suggests a bed, and you confirm it." />
      {error && <div className="mx-auto mb-3 max-w-2xl"><Banner tone="error">{error}</Banner></div>}
      {message && <div className="mx-auto mb-3 max-w-2xl"><Banner tone="ok">{message}</Banner></div>}
      <section className="mx-auto flex h-[calc(100vh-11rem)] max-w-2xl flex-col overflow-hidden rounded-2xl border border-slate-200 bg-slate-50 shadow-sm">
        <div ref={threadRef} className="flex-1 space-y-3 overflow-y-auto px-4 py-4">
          {turns.map((turn, index) => (
            <div key={`${turn.role}-${index}`} className={turn.role === "staff" ? "ml-auto max-w-[80%] rounded-2xl bg-teal-700 px-4 py-3 text-sm text-white" : "mr-auto max-w-[80%] rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-800"}>
              {turn.text}
            </div>
          ))}
          {pending && <p className="text-sm text-slate-500">Looking that up…</p>}
        </div>
        <form onSubmit={onSubmit} className="space-y-3 border-t border-slate-200 bg-white p-4">
          {top && canAllocate && (
            <button className="rounded-lg bg-slate-900 px-4 py-2 text-sm text-white" type="button" onClick={() => void allocate(top.bedId)}>
              Confirm {top.bedCode}
            </button>
          )}
          <Field label="Patient">
            <select className={inputClass} value={patientId || ""} onChange={(event) => setPatientId(Number(event.target.value))}>
              <option value="">Select</option>
              {patients.map((patient) => <option key={patient.id} value={patient.id}>{patient.medicalRecordNumber} · {patient.lastName}</option>)}
            </select>
          </Field>
          <div className="flex gap-2">
            <input className={inputClass} value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="She has a stomach ache" aria-label="Message" />
            <button className="shrink-0 rounded-lg bg-teal-700 px-4 py-2 text-sm text-white disabled:opacity-60" type="submit" disabled={pending}>
              Send
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
