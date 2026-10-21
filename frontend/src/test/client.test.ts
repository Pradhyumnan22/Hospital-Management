import { ApiError, api, readCookie } from "../api/client";

describe("api client", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = "XSRF-TOKEN=; Max-Age=0";
  });

  it("reads the csrf cookie", () => {
    document.cookie = "XSRF-TOKEN=abc%20123";
    expect(readCookie("XSRF-TOKEN")).toBe("abc 123");
  });

  it("throws the server message for a failed response", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify({ message: "Bed is not available", fieldErrors: {} }), { status: 409 })));
    await expect(api("/admissions", { method: "POST", body: "{}" })).rejects.toBeInstanceOf(ApiError);
    try {
      await api("/admissions", { method: "POST", body: "{}" });
    } catch (error) {
      expect(error).toMatchObject({ status: 409, message: "Bed is not available" });
    }
  });
});
