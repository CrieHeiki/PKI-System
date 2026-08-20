import { http } from "@/services/http-client";
import type { ApiEnvelope } from "@/types/common";
import type { VerificationReport } from "@/types/verification";

async function unwrap<T>(promise: Promise<ApiEnvelope<T>>) {
  const payload = await promise;
  return payload.data;
}

export const verificationService = {
  verifyUploadedFile(file: File, payload?: { verifiedBy?: string; trustSource?: string; trustAnchorFiles?: File[] }) {
    const formData = new FormData();
    formData.append("file", file);
    if (payload?.verifiedBy) {
      formData.append("verifiedBy", payload.verifiedBy);
    }
    if (payload?.trustSource) {
      formData.append("trustSource", payload.trustSource);
    }
    payload?.trustAnchorFiles?.forEach((trustAnchorFile) => {
      formData.append("trustAnchors", trustAnchorFile);
    });

    return unwrap(
      http<ApiEnvelope<VerificationReport>>("/api/v1/verifications/files", {
        method: "POST",
        body: formData,
      }),
    );
  },

  verifySigningRequest(requestId: string, payload?: { verifiedBy?: string; trustSource?: string }) {
    return unwrap(
      http<ApiEnvelope<VerificationReport>>(`/api/v1/verifications/signing-requests/${requestId}`, {
        method: "POST",
        body: JSON.stringify(payload || { trustSource: "INTERNAL_CA" }),
      }),
    );
  },

  verifySignedOutput(payload: { objectKey: string; verifiedBy?: string; trustSource?: string }) {
    return unwrap(
      http<ApiEnvelope<VerificationReport>>("/api/v1/verifications/signed-output", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  },

  getLatestByRequest(requestId: string) {
    return unwrap(http<ApiEnvelope<VerificationReport>>(`/api/v1/verifications/signing-requests/${requestId}/latest`));
  },

  getReport(reportId: string) {
    return unwrap(http<ApiEnvelope<VerificationReport>>(`/api/v1/verifications/reports/${reportId}`));
  },

  listRecent() {
    return unwrap(http<ApiEnvelope<VerificationReport[]>>("/api/v1/verifications/reports"));
  },
};
