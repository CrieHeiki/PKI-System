export interface VerificationCheck {
  checkName: string;
  result: boolean;
  detailMessage: string | null;
  rawData: unknown;
}

export interface VerificationReport {
  id: string;
  documentId: string | null;
  documentVersion: number | null;
  signingRequestId: string | null;
  certificateId: string | null;
  verifiedBy: string | null;
  verificationStatus: string;
  signedObjectKey: string | null;
  signedFileName: string | null;
  originalFileName: string | null;
  signerCertificateSerial: string | null;
  signerSubjectDn: string | null;
  issuerDn: string | null;
  trustSource: string | null;
  revocationSource: string | null;
  digestAlgorithm: string | null;
  signatureAlgorithm: string | null;
  integrityValid: boolean;
  signatureValid: boolean;
  trustChainValid: boolean;
  expirationValid: boolean;
  revocationValid: boolean;
  overallMessage: string | null;
  verifiedAt: string;
  checks: VerificationCheck[];
}
