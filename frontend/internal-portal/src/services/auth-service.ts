import { decodeJwtPayload, normalizeAccountStatus, normalizeRole } from "@/lib/utils";
import { http } from "@/services/http-client";
import type { AccountRecord, SignatureAppearancePayload } from "@/types/account";
import type { AuthUser, LoginPayload, RegisterPayload, RegisterResult } from "@/types/auth";

interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInMinutes: number;
}

export const authService = {
  async login(payload: LoginPayload): Promise<AuthUser> {
    const response = await http<LoginResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(payload),
      skipAuth: true,
    });

    try {
      const profile = await authService.getMe(response.accessToken);
      return {
        accountId: profile.id,
        username: profile.username,
        email: profile.email,
        role: profile.role,
        status: profile.status,
        token: response.accessToken,
        tokenType: response.tokenType,
      };
    } catch {
      const claims = decodeJwtPayload(response.accessToken);
      const role = normalizeRole(claims.role);
      const accountId = String(claims.accountId || "");
      const username = String(claims.username || claims.sub || payload.usernameOrEmail);
      const status = normalizeAccountStatus(claims.status) || "ACTIVE";
      const email = String(claims.email || "");

      if (!role || !accountId) {
        throw new Error("JWT không đủ thông tin role/accountId để khởi tạo session");
      }

      return {
        accountId,
        username,
        email,
        role,
        status,
        token: response.accessToken,
        tokenType: response.tokenType,
      };
    }
  },

  async register(payload: RegisterPayload): Promise<RegisterResult> {
    return http<RegisterResult>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
      skipAuth: true,
    });
  },

  async getMe(token?: string): Promise<AccountRecord> {
    return http<AccountRecord>("/api/v1/auth/me", {
      token,
    });
  },

  async uploadSignatureImage(file: File): Promise<AccountRecord> {
    const formData = new FormData();
    formData.append("file", file);
    return http<AccountRecord>("/api/v1/auth/me/signature-image", {
      method: "POST",
      body: formData,
    });
  },

  async getMySignatureImage(): Promise<Blob> {
    return http<Blob>("/api/v1/auth/me/signature-image", {
      parseAs: "blob",
    });
  },

  async deleteMySignatureImage(): Promise<AccountRecord> {
    return http<AccountRecord>("/api/v1/auth/me/signature-image", {
      method: "DELETE",
    });
  },

  async updateSignatureAppearance(payload: SignatureAppearancePayload): Promise<AccountRecord> {
    return http<AccountRecord>("/api/v1/auth/me/signature-appearance", {
      method: "PATCH",
      body: JSON.stringify(payload),
    });
  },

  async listActiveUsers(): Promise<AccountRecord[]> {
    return http<AccountRecord[]>("/api/v1/auth/active-users");
  },
};
