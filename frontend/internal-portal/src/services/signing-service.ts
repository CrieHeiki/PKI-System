import { http, httpResponse } from "@/services/http-client";
import type { ApiEnvelope } from "@/types/common";
import type { SignatureAppearanceOverride, SigningRequestRecord } from "@/types/signing";

function decodeContentDispositionFileName(contentDisposition: string) {
  const utf8Match = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1].trim());
  }

  const plainMatch = contentDisposition.match(/filename\s*=\s*\"?([^\";]+)\"?/i);
  if (!plainMatch?.[1]) {
    return null;
  }

  const plainValue = plainMatch[1].trim();
  const encodedWordMatch = plainValue.match(/^=\?UTF-8\?Q\?(.+)\?=$/i);
  if (encodedWordMatch?.[1]) {
    return encodedWordMatch[1].replace(/_/g, " ");
  }

  return plainValue;
}

async function unwrap<T>(promise: Promise<ApiEnvelope<T>>) {
  const payload = await promise;
  return payload.data;
}

export const signingService = {
  listMine() {
    return unwrap(http<ApiEnvelope<SigningRequestRecord[]>>("/api/v1/signing-requests/requested-by-me"));
  },

  listPendingForMe() {
    return unwrap(http<ApiEnvelope<SigningRequestRecord[]>>("/api/v1/signing-requests/pending-for-me"));
  },

  listAllAdmin() {
    return unwrap(http<ApiEnvelope<SigningRequestRecord[]>>("/api/v1/admin/signing-requests"));
  },

  get(requestId: string) {
    return unwrap(http<ApiEnvelope<SigningRequestRecord>>(`/api/v1/signing-requests/${requestId}`));
  },

  create(payload: { documentId: string; signerAccountId: string; note?: string }) {
    return unwrap(
      http<ApiEnvelope<SigningRequestRecord>>("/api/v1/signing-requests", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  },

  approve(requestId: string, payload: { certificateId: string; note?: string }) {
    return unwrap(
      http<ApiEnvelope<SigningRequestRecord>>(`/api/v1/signing-requests/${requestId}/approve`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  },

  reject(requestId: string, note: string) {
    return unwrap(
      http<ApiEnvelope<SigningRequestRecord>>(`/api/v1/signing-requests/${requestId}/reject`, {
        method: "POST",
        body: JSON.stringify({ note }),
      }),
    );
  },

  cancel(requestId: string) {
    return unwrap(
      http<ApiEnvelope<SigningRequestRecord>>(`/api/v1/signing-requests/${requestId}/cancel`, {
        method: "POST",
      }),
    );
  },

  execute(requestId: string, stepUpVerificationToken: string) {
    return unwrap(
      http<ApiEnvelope<SigningRequestRecord>>(`/api/v1/signing-requests/${requestId}/execute-signing`, {
        method: "POST",
        body: JSON.stringify({ stepUpVerificationToken }),
      }),
    );
  },

  executeWithAppearance(requestId: string, payload: { stepUpVerificationToken: string; signatureAppearance?: SignatureAppearanceOverride }) {
    return unwrap(
      http<ApiEnvelope<SigningRequestRecord>>(`/api/v1/signing-requests/${requestId}/execute-signing`, {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  },

  async downloadSourceDocument(requestId: string) {
    const response = await httpResponse(`/api/v1/signing-requests/${requestId}/source-document`);
    const disposition = response.headers.get("content-disposition") || "";
    const fileNameMatch = disposition.match(/filename\*=UTF-8''([^;]+)|filename=\"?([^\"]+)\"?/i);
    const fileName = decodeURIComponent(fileNameMatch?.[1] || fileNameMatch?.[2] || `source-document-${requestId}`);
    const blob = await response.blob();
    return { blob, fileName, contentType: response.headers.get("content-type") || blob.type || "application/octet-stream" };
  },

  async downloadSignedOutput(requestId: string) {
    const response = await httpResponse(`/api/v1/signing-requests/${requestId}/signed-output`);
    const blob = await response.blob();
    const contentDisposition = response.headers.get("content-disposition") || "";
    const parsedFileName = decodeContentDispositionFileName(contentDisposition) || `signed-output-${requestId}`;
    return { blob, fileName: parsedFileName };
  },
};
