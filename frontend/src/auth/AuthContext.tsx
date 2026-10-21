import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, ensureCsrf } from "../api/client";
import type { User } from "../api/types";

interface AuthValue {
  user: User | null;
  ready: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);

  async function refresh() {
    try {
      setUser(await api<User>("/auth/me"));
    } catch {
      setUser(null);
    }
  }

  useEffect(() => {
    refresh().finally(() => setReady(true));
  }, []);

  const value = useMemo<AuthValue>(() => ({
    user,
    ready,
    refresh,
    async login(username: string, password: string) {
      await ensureCsrf();
      const next = await api<User>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password })
      });
      setUser(next);
    },
    async logout() {
      await ensureCsrf();
      await api<void>("/auth/logout", { method: "POST" });
      setUser(null);
    }
  }), [user, ready]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("AuthProvider is missing");
  }
  return value;
}
