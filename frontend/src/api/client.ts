export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

interface ErrorBody {
  message?: string;
  fieldErrors?: Record<string, string>;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function readError(value: unknown, fallback: string): ErrorBody {
  if (!isRecord(value)) {
    return { message: fallback };
  }
  const fieldErrors: Record<string, string> = {};
  if (isRecord(value.fieldErrors)) {
    for (const [key, item] of Object.entries(value.fieldErrors)) {
      if (typeof item === "string") {
        fieldErrors[key] = item;
      }
    }
  }
  return {
    message: typeof value.message === "string" ? value.message : fallback,
    fieldErrors
  };
}

export function readCookie(name: string): string | null {
  const pieces = document.cookie.split(";");
  for (const piece of pieces) {
    const [key, ...rest] = piece.trim().split("=");
    if (key === name) {
      return decodeURIComponent(rest.join("="));
    }
  }
  return null;
}

export async function ensureCsrf(): Promise<void> {
  await fetch("/api/auth/csrf", { credentials: "include" });
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const csrf = readCookie("XSRF-TOKEN");
  if (csrf) {
    headers.set("X-XSRF-TOKEN", csrf);
  }
  const response = await fetch(`/api${path}`, { ...options, headers, credentials: "include" });
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  const payload: unknown = text ? JSON.parse(text) as unknown : null;
  if (!response.ok) {
    const error = readError(payload, response.statusText);
    throw new ApiError(response.status, error.message ?? response.statusText, error.fieldErrors ?? {});
  }
  return payload as T;
}
