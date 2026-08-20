import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function decodeJwtPayload(token: string): Record<string, unknown> {
  const parts = token.split(".");
  if (parts.length < 2) {
    return {};
  }

  try {
    const base64 = parts[1]
      .replace(/-/g, "+")
      .replace(/_/g, "/")
      .padEnd(Math.ceil(parts[1].length / 4) * 4, "=");
    return JSON.parse(window.atob(base64)) as Record<string, unknown>;
  } catch {
    return {};
  }
}

export function normalizeRole(value: unknown): "ADMIN" | "USER" | null {
  const raw = String(Array.isArray(value) ? value[0] : value || "").trim().toUpperCase();
  if (!raw) {
    return null;
  }
  return raw.startsWith("ROLE_") ? (raw.slice(5) as "ADMIN" | "USER") : (raw as "ADMIN" | "USER");
}

export function normalizeAccountStatus(value: unknown): "PENDING" | "ACTIVE" | "LOCKED" | "DISABLED" | null {
  const raw = String(value || "").trim().toUpperCase();
  if (raw === "PENDING" || raw === "ACTIVE" || raw === "LOCKED" || raw === "DISABLED") {
    return raw;
  }
  return null;
}

export function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }

  const normalized = value.trim().replace(" ", "T");
  const hasExplicitTimezone = /([zZ]|[+-]\d{2}:\d{2})$/.test(normalized);
  const date = new Date(hasExplicitTimezone ? normalized : `${normalized}Z`);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

export function truncateMiddle(value: string, head = 8, tail = 6) {
  if (!value || value.length <= head + tail + 3) {
    return value;
  }
  return `${value.slice(0, head)}...${value.slice(-tail)}`;
}
