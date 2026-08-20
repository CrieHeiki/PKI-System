import { http } from "@/services/http-client";
import type {
  CertificateApprovalResult,
  CertificateRecord,
  CertificateRequestRecord,
} from "@/types/certificate";

export const certificatesService = {
  listMine() {
    return http<CertificateRecord[]>("/api/v1/certificates/my");
  },

  get(certificateId: string) {
    return http<CertificateRecord>(`/api/v1/certificates/${certificateId}`);
  },

  listAll() {
    return http<CertificateRecord[]>("/api/v1/admin/certificates");
  },

  revokeMine(certificateId: string) {
    return http<CertificateRequestRecord>(`/api/v1/certificates/${certificateId}/revoke`, {
      method: "POST",
      body: JSON.stringify({ reason: "Không còn nhu cầu sử dụng chứng thư này." }),
    });
  },

  createRevocationRequest(certificateId: string, payload: { reason: string }) {
    return http<CertificateRequestRecord>(`/api/v1/certificates/${certificateId}/revocation-requests`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  revokeAdmin(certificateId: string) {
    return http<{ message: string; certificateId: string; status: string; revokedAt: string }>(`/api/v1/admin/certificates/${certificateId}/revoke`, {
      method: "POST",
    });
  },

  listMyRequests() {
    return http<CertificateRequestRecord[]>("/api/v1/certificate-requests/my");
  },

  getRequest(requestId: string) {
    return http<CertificateRequestRecord>(`/api/v1/certificate-requests/${requestId}`);
  },

  createRequest(payload: { commonName: string; ttl?: string; organization?: string; country?: string }) {
    return http<CertificateRequestRecord>("/api/v1/certificate-requests", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  createRenewalRequest(
    certificateId: string,
    payload: { ttl?: string; organization?: string; country?: string; reason?: string },
  ) {
    return http<CertificateRequestRecord>(`/api/v1/certificates/${certificateId}/renewal-requests`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },

  listAdminRequests(params?: { status?: string; requestType?: string }) {
    const query = new URLSearchParams();
    if (params?.status) query.set("status", params.status);
    if (params?.requestType) query.set("requestType", params.requestType);
    const suffix = query.toString() ? `?${query.toString()}` : "";
    return http<CertificateRequestRecord[]>(`/api/v1/admin/certificate-requests${suffix}`);
  },

  approveRequest(requestId: string) {
    return http<CertificateApprovalResult>(`/api/v1/admin/certificate-requests/${requestId}/approve`, {
      method: "POST",
    });
  },

  rejectRequest(requestId: string, reason: string) {
    return http<{ message: string; request: CertificateRequestRecord }>(
      `/api/v1/admin/certificate-requests/${requestId}/reject`,
      {
        method: "POST",
        body: JSON.stringify({ reason }),
      },
    );
  },
};
