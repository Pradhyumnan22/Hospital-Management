import { FormEvent, useEffect, useState } from "react";
import { ApiError, api } from "../api/client";
import type { Ward, WardType } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Banner, Empty, Field, PageHeader, inputClass } from "../components/ui";

const types: WardType[] = ["GENERAL", "ICU", "ISOLATION", "PEDIATRIC", "MATERNITY"];

export function WardsPage() {
  const { user } = useAuth();
  const [rows, setRows] = useState<Ward[] | null>(null);
  const [error, setError] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState({ code: "", name: "", wardType: "GENERAL" as WardType, floorNumber: "1" });

  function load() {
    api<Ward[]>("/wards").then(setRows).catch(() => setError("Could not load wards."));
  }
  useEffect(() => { load(); }, []);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      const body = JSON.stringify({ ...form, floorNumber: Number(form.floorNumber) });
      if (editingId === null) {
        await api("/wards", { method: "POST", body });
      } else {
        await api(`/wards/${editingId}`, { method: "PUT", body });
      }
      setEditingId(null);
      setForm({ code: "", name: "", wardType: "GENERAL", floorNumber: "1" });
      setError("");
      load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not save the ward.");
    }
  }

  async function remove(ward: Ward) {
    if (!window.confirm(`Delete ward ${ward.code}? This is allowed only when it has no beds.`)) return;
    try {
      await api(`/wards/${ward.id}`, { method: "DELETE" });
      setError("");
      load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not delete the ward.");
    }
  }

  return (
    <div>
      <PageHeader title="Wards" subtitle="Ward type is a hard constraint when a recommendation requires it." />
      {error && <Banner tone="error">{error}</Banner>}
      {user?.role === "ADMIN" && (
        <form onSubmit={onSubmit} className="mb-6 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:grid-cols-4">
          <Field label="Code"><input className={inputClass} value={form.code} onChange={(event) => setForm({ ...form, code: event.target.value })} /></Field>
          <Field label="Name"><input className={inputClass} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></Field>
          <Field label="Type">
            <select className={inputClass} value={form.wardType} onChange={(event) => setForm({ ...form, wardType: event.target.value as WardType })}>
              {types.map((type) => <option key={type}>{type}</option>)}
            </select>
          </Field>
          <Field label="Floor"><input className={inputClass} type="number" value={form.floorNumber} onChange={(event) => setForm({ ...form, floorNumber: event.target.value })} /></Field>
          <div className="md:col-span-4 flex gap-2">
            <button className="rounded-lg bg-teal-700 px-4 py-2 text-sm text-white" type="submit">{editingId === null ? "Add ward" : "Save ward"}</button>
            {editingId !== null && <button className="rounded-lg border border-slate-300 px-4 py-2 text-sm" type="button" onClick={() => { setEditingId(null); setForm({ code: "", name: "", wardType: "GENERAL", floorNumber: "1" }); }}>Cancel</button>}
          </div>
        </form>
      )}
      {!rows ? <Empty>Loading wards…</Empty> : rows.length === 0 ? <Empty>No wards yet.</Empty> : (
        <div className="grid gap-3 md:grid-cols-2">
          {rows.map((ward) => (
            <article key={ward.id} className="rounded-xl border border-slate-200 bg-white p-4">
              <p className="text-lg font-semibold">{ward.code}</p>
              <p className="text-sm text-slate-600">{ward.name}</p>
              <p className="mt-2 text-xs uppercase text-slate-500">{ward.wardType} · floor {ward.floorNumber}</p>
              {user?.role === "ADMIN" && (
                <div className="mt-3 flex gap-3 text-sm">
                  <button className="text-teal-700" type="button" onClick={() => { setEditingId(ward.id); setForm({ code: ward.code, name: ward.name, wardType: ward.wardType, floorNumber: String(ward.floorNumber) }); }}>Edit</button>
                  <button className="text-rose-700" type="button" onClick={() => void remove(ward)}>Delete</button>
                </div>
              )}
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
