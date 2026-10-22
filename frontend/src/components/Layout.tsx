import {
  Activity,
  BedDouble,
  Bell,
  Building2,
  LayoutDashboard,
  LogOut,
  Sparkles,
  Stethoscope,
  ScrollText,
  UserRound,
  Users
} from "lucide-react";
import type { ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const links = [
  { to: "/", label: "Overview", icon: LayoutDashboard, end: true },
  { to: "/patients", label: "Patients", icon: Users, end: false },
  { to: "/wards", label: "Wards", icon: Building2, end: false },
  { to: "/beds", label: "Beds", icon: BedDouble, end: false },
  { to: "/admissions", label: "Admissions", icon: Stethoscope, end: false },
  { to: "/recommendations", label: "Assistant", icon: Sparkles, end: false },
  { to: "/forecast", label: "Forecast", icon: Activity, end: false },
  { to: "/alerts", label: "Alerts", icon: Bell, end: false }
];

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  return (
    <div className="min-h-screen md:grid md:grid-cols-[240px_1fr]">
      <aside className="bg-slate-950 text-slate-100">
        <div className="px-5 py-6">
          <p className="text-xs uppercase tracking-[0.2em] text-teal-300">Ward desk</p>
          <p className="mt-1 text-lg font-semibold">Bed occupancy</p>
        </div>
        <nav className="flex gap-2 overflow-x-auto px-3 pb-4 md:block md:space-y-1 md:px-3">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) =>
                `flex items-center gap-2 rounded-lg px-3 py-2 text-sm whitespace-nowrap ${isActive ? "bg-teal-600 text-white" : "text-slate-300 hover:bg-slate-800"}`
              }
            >
              <link.icon size={16} />
              {link.label}
            </NavLink>
          ))}
          {user?.role === "ADMIN" && (
            <NavLink
              to="/audit"
              className={({ isActive }) =>
                `flex items-center gap-2 rounded-lg px-3 py-2 text-sm ${isActive ? "bg-teal-600 text-white" : "text-slate-300 hover:bg-slate-800"}`
              }
            >
              <ScrollText size={16} />
              Audit
            </NavLink>
          )}
          {user?.role === "ADMIN" && (
            <NavLink
              to="/staff"
              className={({ isActive }) =>
                `flex items-center gap-2 rounded-lg px-3 py-2 text-sm ${isActive ? "bg-teal-600 text-white" : "text-slate-300 hover:bg-slate-800"}`
              }
            >
              <UserRound size={16} />
              Staff
            </NavLink>
          )}
        </nav>
        <div className="hidden border-t border-slate-800 px-5 py-4 text-sm md:block">
          <p className="font-medium">{user?.fullName}</p>
          <p className="text-slate-400">{user?.role.replaceAll("_", " ")}</p>
          <button className="mt-3 inline-flex items-center gap-2 text-teal-300" onClick={() => void logout()} type="button">
            <LogOut size={16} /> Sign out
          </button>
        </div>
      </aside>
      <div>
        <div className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-3 md:hidden">
          <p className="text-sm font-medium">{user?.fullName}</p>
          <button className="text-sm text-teal-700" onClick={() => void logout()} type="button">Sign out</button>
        </div>
        <main className="px-4 py-6 md:px-8">{children}</main>
      </div>
    </div>
  );
}
