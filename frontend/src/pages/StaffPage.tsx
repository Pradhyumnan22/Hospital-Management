import { FormEvent, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { ApiError, api } from "../api/client";
import type { Role, User } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { Banner, Empty, Field, PageHeader, inputClass } from "../components/ui";

const roles: Role[] = ["ADMIN", "ADMISSION_STAFF", "CLINICAL_STAFF"];

export function StaffPage() {
  const { user } = useAuth();
  const [rows, setRows] = useState<User[] | null>(null);
  const [error, setError] = useState("");
  const [form, setForm] = useState({ username: "", password: "", fullName: "", role: "CLINICAL_STAFF" as Role });

  function load() {
    api<User[]>("/staff").then(setRows).catch(() => setError("Could not load staff."));
  }
  useEffect(() => {
    if (user?.role === "ADMIN") load();
  }, [user?.role]);

  if (user && user.role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (form.password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    try {
      await api("/staff", { method: "POST", body: JSON.stringify(form) });
      setForm({ username: "", password: "", fullName: "", role: "CLINICAL_STAFF" });
      setError("");
      load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Could not create the account.");
    }
  }

  async function toggle(account: User) {
    await api(`/staff/${account.id}`, { method: "PATCH", body: JSON.stringify({ enabled: !account.enabled }) });
    load();
  }

  return (
    <div>
      <PageHeader title="Staff" subtitle="Administrators create accounts. Passwords are hashed by the server." />
      {error && <Banner tone="error">{error}</Banner>}
      <form onSubmit={onSubmit} className="mb-6 grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 md:grid-cols-2">
        <Field label="Username"><input className={inputClass} value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} /></Field>
        <Field label="Full name"><input className={inputClass} value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} /></Field>
        <Field label="Password"><input className={inputClass} type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} /></Field>
        <Field label="Role">
          <select className={inputClass} value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value as Role })}>
            {roles.map((role) => <option key={role}>{role}</option>)}
          </select>
        </Field>
        <div><button className="rounded-lg bg-teal-700 px-4 py-2 text-sm text-white" type="submit">Create account</button></div>
      </form>
      {!rows ? <Empty>Loading staff…</Empty> : (
        <ul className="space-y-2">
          {rows.map((account) => (
            <li key={account.id} className="flex items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm">
              <span>{account.fullName} · {account.username} · {account.role} · {account.enabled ? "enabled" : "disabled"}</span>
              <button className="text-teal-700" type="button" onClick={() => void toggle(account)}>{account.enabled ? "Disable" : "Enable"}</button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
