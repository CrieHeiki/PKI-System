import { http, httpResponse } from "@/services/http-client";
import type { DocumentDetail, DocumentSummary } from "@/types/document";

async function responseToDownloadedFile(response: Response) {
  const disposition = response.headers.get("content-disposition") || "";
  const fileNameMatch = disposition.match(/filename\*=UTF-8''([^;]+)|filename="?([^"]+)"?/i);
  const fileName = decodeURIComponent(fileNameMatch?.[1] || fileNameMatch?.[2] || "download.bin");
  const blob = await response.blob();
  return { fileName, blob };
}

export const documentsService = {
  async listMine() {
    return http<DocumentSummary[]>("/api/v1/documents/my");
  },

  async listAll() {
    return http<DocumentSummary[]>("/api/v1/admin/documents");
  },

  async get(documentId: string) {
    return http<DocumentDetail>(`/api/v1/documents/${documentId}`);
  },

  async create(payload: { title: string; description?: string; file: File }) {
    const formData = new FormData();
    formData.append("title", payload.title);
    if (payload.description) {
      formData.append("description", payload.description);
    }
    formData.append("file", payload.file);

    return http<DocumentDetail>("/api/v1/documents", {
      method: "POST",
      body: formData,
    });
  },

  async uploadVersion(documentId: string, payload: { file: File }) {
    const formData = new FormData();
    formData.append("file", payload.file);
    return http<DocumentDetail>(`/api/v1/documents/${documentId}/versions`, {
      method: "POST",
      body: formData,
    });
  },

  async download(documentId: string) {
    const response = await httpResponse(`/api/v1/documents/${documentId}/download`);
    return responseToDownloadedFile(response);
  },
};
