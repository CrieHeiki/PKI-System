export interface CertificateRecord {
  id: string;
  accountId: string;
  commonName: string;
  subjectDn: string;
  serialNumber: string;
  vaultCertificatePath: string | null;
  certificatePem: string;
  issuingCaPem: string | null;
  status: string;
  renewedFromCertificateId: string | null;
  issuedAt: string;
  expiresAt: string;
  revokedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CertificateRequestRecord {
  id: string;
  accountId: string;
  commonName: string;
  organization: string | null;
  country: string | null;
  ttl: string | null;
  status: string;
  requestType: string;
  sourceCertificateId: string | null;
  renewalReason: string | null;
  revocationReason: string | null;
  certificateId: string | null;
  requestedAt: string;
  reviewedAt: string | null;
  reviewedBy: string | null;
  rejectionReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CertificateApprovalResult {
  message: string;
  request: CertificateRequestRecord;
  certificate: CertificateRecord;
}

export interface RevokeCertificateResult {
  message: string;
  certificateId: string;
  status: string;
  revokedAt: string;
}
