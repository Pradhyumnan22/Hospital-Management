import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../auth/AuthContext";
import { AuditPage } from "../pages/AuditPage";
import { PatientsPage } from "../pages/PatientsPage";

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

describe("patient and audit screens", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("requires patient fields before registration", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/auth/me")) {
        return json({ id: 1, username: "admin", fullName: "Avery Admin", role: "ADMIN", enabled: true });
      }
      if (url.includes("/patients")) return json([]);
      return json({});
    }));
    render(
      <MemoryRouter>
        <AuthProvider>
          <PatientsPage />
        </AuthProvider>
      </MemoryRouter>
    );
    expect(await screen.findByRole("button", { name: "Register patient" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Register patient" }));
    expect(await screen.findByText("Medical record number, name, and date of birth are required.")).toBeInTheDocument();
    await user.type(screen.getByRole("textbox", { name: "First name" }), "Alex1");
    await user.type(screen.getByRole("textbox", { name: "Last name" }), "Stone");
    await user.type(screen.getByRole("textbox", { name: "MRN" }), "MRN9001");
    await user.type(screen.getByLabelText("Date of birth"), "1990-01-01");
    await user.click(screen.getByRole("button", { name: "Register patient" }));
    expect(await screen.findByText("First and last names can contain letters only.")).toBeInTheDocument();
  });

  it("shows an empty audit log", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/auth/me")) {
        return json({ id: 1, username: "admin", fullName: "Avery Admin", role: "ADMIN", enabled: true });
      }
      if (url.includes("/audit")) return json([]);
      return json({});
    }));
    render(
      <MemoryRouter>
        <AuthProvider>
          <AuditPage />
        </AuthProvider>
      </MemoryRouter>
    );
    expect(await screen.findByText("No audit events yet.")).toBeInTheDocument();
  });
});