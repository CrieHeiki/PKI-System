import { http } from "@/services/http-client";
import type { AccountRecord } from "@/types/account";

export const accountsService = {
  async list(params?: { role?: string; status?: string; q?: string }) {
    const query = new URLSearchParams();
    if (params?.role) query.set("role", params.role);
    if (params?.status) query.set("status", params.status);
    if (params?.q) query.set("q", params.q);
    const suffix = query.toString() ? `?${query.toString()}` : "";
    return http<AccountRecord[]>(`/api/v1/admin/accounts${suffix}`);
  },

  async get(accountId: string) {
    return http<AccountRecord>(`/api/v1/admin/accounts/${accountId}`);
  },

  async activate(accountId: string) {
    return http<AccountRecord>(`/api/v1/admin/accounts/${accountId}/activate`, { method: "PATCH" });
  },

  async lock(accountId: string) {
    return http<AccountRecord>(`/api/v1/admin/accounts/${accountId}/lock`, { method: "PATCH" });
  },

  async unlock(accountId: string) {
    return http<AccountRecord>(`/api/v1/admin/accounts/${accountId}/unlock`, { method: "PATCH" });
  },

  async disable(accountId: string) {
    return http<AccountRecord>(`/api/v1/admin/accounts/${accountId}/disable`, { method: "PATCH" });
  },

  async restore(accountId: string) {
    return http<AccountRecord>(`/api/v1/admin/accounts/${accountId}/restore`, { method: "PATCH" });
  },

  async changeRole(accountId: string, role: string) {
    return http<AccountRecord>(`/api/v1/admin/accounts/${accountId}/role`, {
      method: "PATCH",
      body: JSON.stringify({ role }),
    });
  },
};
