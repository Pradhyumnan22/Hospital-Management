import { FormEvent, useState } from "react";
import { Navigate } from "react-router-dom";
import { ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { Banner, Field, inputClass } from "../components/ui";

export function LoginPage() {
  const { user, login } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  if (user) {
    return <Navigate to="/" replace />;
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError("Enter a username and password.");
      return;
    }
    setPending(true);
    setError("");
    try {
      await login(username.trim(), password);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Sign-in failed");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="grid min-h-screen place-items-center px-4">
      <form onSubmit={onSubmit} className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
        <p className="text-xs uppercase tracking-[0.2em] text-teal-700">Hospital operations</p>
        <h1 className="mt-2 text-2xl font-semibold">Hospital bed desk</h1>
        <p className="mt-2 text-sm text-slate-600">Recommendations stay advisory until admission staff confirm a bed.</p>
        {error && <div className="mt-4"><Banner tone="error">{error}</Banner></div>}
        <div className="mt-4 space-y-3">
          <Field label="Username">
            <input className={inputClass} value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" />
          </Field>
          <Field label="Password">
            <input className={inputClass} type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" />
          </Field>
        </div>
        <button className="mt-5 w-full rounded-lg bg-teal-700 px-4 py-2 text-sm font-medium text-white disabled:opacity-60" disabled={pending} type="submit">
          {pending ? "Signing in…" : "Sign in"}
        </button>
      </form>
    </div>
  );
}
