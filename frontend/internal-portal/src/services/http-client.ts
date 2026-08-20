import { env } from "@/lib/env";
import { useAuthStore } from "@/store/auth-store";

type ParseMode = "json" | "text" | "blob" | "response";

interface HttpRequestOptions extends RequestInit {
  skipAuth?: boolean;
  token?: string | null;
  parseAs?: ParseMode;
}

function buildHeaders(init?: RequestInit, token?: string | null) {
  const headers = new Headers(init?.headers);
  const body = init?.body;

  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;
  const isBlob = typeof Blob !== "undefined" && body instanceof Blob;
  if (body && !isFormData && !isBlob && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  return headers;
}

async function request(path: string, init?: HttpRequestOptions) {
  const token = init?.skipAuth ? null : (init?.token ?? useAuthStore.getState().user?.token ?? null);
  const response = await fetch(`${env.apiBaseUrl.replace(/\/+$/, "")}${path}`, {
    ...init,
    headers: buildHeaders(init, token),
  });

  if (!response.ok) {
    if (response.status === 413) {
      throw new Error("File vượt quá giới hạn dung lượng cho phép (tối đa 20MB).");
    }

    const contentType = response.headers.get("content-type") || "";
    let errorPayload: unknown = null;

    try {
      errorPayload = contentType.includes("application/json") ? await response.json() : await response.text();
    } catch {
      errorPayload = null;
    }

    const errorMessage =
      typeof errorPayload === "string"
        ? errorPayload
        : (errorPayload as { message?: string; error?: string } | null)?.message
          || (errorPayload as { message?: string; error?: string } | null)?.error
          || `HTTP ${response.status}`;
    throw new Error(String(errorMessage));
  }

  if (init?.parseAs === "blob") {
    return response.blob();
  }
  if (init?.parseAs === "text") {
    return response.text();
  }
  if (init?.parseAs === "response") {
    return response;
  }

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : await response.text();
  return payload;
}

export async function http<T>(path: string, init?: HttpRequestOptions): Promise<T> {
  return request(path, { ...init, parseAs: init?.parseAs ?? "json" }) as Promise<T>;
}

export async function httpResponse(path: string, init?: HttpRequestOptions): Promise<Response> {
  return request(path, { ...init, parseAs: "response" }) as Promise<Response>;
}
