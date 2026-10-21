import type { ReactElement } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { Layout } from "./components/Layout";
import { AdmissionsPage } from "./pages/AdmissionsPage";
import { AuditPage } from "./pages/AuditPage";
import { AlertsPage } from "./pages/AlertsPage";
import { BedsPage } from "./pages/BedsPage";
import { DashboardPage } from "./pages/DashboardPage";
import { ForecastPage } from "./pages/ForecastPage";
import { LoginPage } from "./pages/LoginPage";
import { PatientDetailPage } from "./pages/PatientDetailPage";
import { PatientsPage } from "./pages/PatientsPage";
import { RecommendationsPage } from "./pages/RecommendationsPage";
import { StaffPage } from "./pages/StaffPage";
import { WardsPage } from "./pages/WardsPage";

function Protected({ children }: { children: ReactElement }) {
  const { user, ready } = useAuth();
  if (!ready) {
    return <p className="p-8 text-sm text-slate-500">Checking session…</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <Layout>{children}</Layout>;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<Protected><DashboardPage /></Protected>} />
      <Route path="/patients" element={<Protected><PatientsPage /></Protected>} />
      <Route path="/patients/:id" element={<Protected><PatientDetailPage /></Protected>} />
      <Route path="/wards" element={<Protected><WardsPage /></Protected>} />
      <Route path="/beds" element={<Protected><BedsPage /></Protected>} />
      <Route path="/admissions" element={<Protected><AdmissionsPage /></Protected>} />
      <Route path="/recommendations" element={<Protected><RecommendationsPage /></Protected>} />
      <Route path="/forecast" element={<Protected><ForecastPage /></Protected>} />
      <Route path="/alerts" element={<Protected><AlertsPage /></Protected>} />
      <Route path="/audit" element={<Protected><AuditPage /></Protected>} />
      <Route path="/staff" element={<Protected><StaffPage /></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
