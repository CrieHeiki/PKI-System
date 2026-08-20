export interface DocumentVersion {
  id: string;
  versionNumber: number;
  fileName: string;
  contentType: string;
  fileSize: number;
  sha256Hash: string;
  createdAt: string;
}

export interface DocumentSummary {
  id: string;
  ownerAccountId: string;
  title: string;
  description: string | null;
  currentVersion: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentDetail extends DocumentSummary {
  currentVersionDetail: DocumentVersion;
}
