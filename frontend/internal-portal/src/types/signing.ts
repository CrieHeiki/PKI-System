export interface SigningEvent {
  id: string;
  eventType: string;
  actorAccountId: string | null;
  eventTime: string;
  detail: string | null;
}

export interface SigningArtifact {
  id: string;
  signingRequestId: string;
  signerAccountId: string;
  certificateId: string;
  fileName: string;
  contentType: string;
  digestAlgorithm: string;
  signatureAlgorithm: string;
  documentDigestBase64: string;
  signatureValueBase64: string;
  signedPayloadLength: number;
  outputObjectKey: string | null;
  outputFileName: string | null;
  outputContentType: string | null;
  outputFileSize: number | null;
  createdAt: string;
}

export interface SignatureAppearanceOverride {
  pageNumber?: number;
  imageOnly?: boolean;
  xPercent?: number;
  yPercent?: number;
  widthPercent?: number;
  heightPercent?: number;
}

export interface SigningRequestRecord {
  id: string;
  documentId: string;
  requesterAccountId: string;
  signerAccountId: string;
  certificateId: string | null;
  status: string;
  requestedAt: string;
  decidedAt: string | null;
  signedAt: string | null;
  note: string | null;
  createdAt: string;
  updatedAt: string;
  events: SigningEvent[];
  artifact: SigningArtifact | null;
}
