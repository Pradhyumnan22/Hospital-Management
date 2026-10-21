import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AuthProvider } from "../auth/AuthContext";
import { LoginPage } from "../pages/LoginPage";

describe("login form", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/auth/me")) {
        return new Response(JSON.stringify({ message: "Authentication is required" }), { status: 401 });
      }
      return new Response("{}", { status: 200 });
    }));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("asks for credentials before calling login", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </MemoryRouter>
    );
    expect(await screen.findByRole("heading", { name: "Hospital bed desk" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    expect(await screen.findByText("Enter a username and password.")).toBeInTheDocument();
  });
});
