import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Fingerprint, ShieldCheck } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { PdfSignaturePlacementEditor } from "@/components/signing/PdfSignaturePlacementEditor";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { formatDateTime } from "@/lib/utils";
import { getWebAuthnAssertion, isWebAuthnSupported } from "@/lib/webauthn";
import { authService } from "@/services/auth-service";
import { certificatesService } from "@/services/certificates-service";
import { documentsService } from "@/services/documents-service";
import { signingService } from "@/services/signing-service";
import { stepUpService } from "@/services/step-up-service";
import { useAuthStore } from "@/store/auth-store";
import type { SignatureAppearanceOverride } from "@/types/signing";

function getSigningAppearanceDraftKey(requestId: string) {
  return `ct556-signing-appearance-draft:${requestId}`;
}

export function SignRequestDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const currentUser = useAuthStore((state) => state.user);
  const [approveCertificateId, setApproveCertificateId] = useState("");
  const [approveNote, setApproveNote] = useState("");
  const [rejectNote, setRejectNote] = useState("");
  const [registrationId, setRegistrationId] = useState("");
  const [verificationToken, setVerificationToken] = useState("");
  const [webauthnMessage, setWebauthnMessage] = useState<string | null>(null);
  const [signatureAppearance, setSignatureAppearance] = useState<SignatureAppearanceOverride>({});
  const [signaturePreviewUrl, setSignaturePreviewUrl] = useState<string | null>(null);

  const detailQuery = useQuery({
    queryKey: ["signing", "detail", id],
    queryFn: () => signingService.get(id!),
    enabled: Boolean(id),
  });
  const documentQuery = useQuery({
    queryKey: ["documents", "detail", detailQuery.data?.documentId],
    queryFn: () => documentsService.get(detailQuery.data!.documentId),
    enabled: Boolean(detailQuery.data?.documentId),
  });
  const activeUsersQuery = useQuery({
    queryKey: ["auth", "active-users"],
    queryFn: authService.listActiveUsers,
  });
  const certificatesQuery = useQuery({
    queryKey: ["certificates", "mine"],
    queryFn: certificatesService.listMine,
  });
  const registrationsQuery = useQuery({
    queryKey: ["step-up", "registrations"],
    queryFn: stepUpService.listRegistrations,
  });
  const profileQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => authService.getMe(),
  });
  const signatureImageQuery = useQuery({
    queryKey: ["auth", "signature-image"],
    queryFn: authService.getMySignatureImage,
    enabled: Boolean(profileQuery.data?.signatureImageConfigured),
  });
  const sourceDocumentQuery = useQuery({
    queryKey: ["signing", "source-document", id],
    queryFn: () => signingService.downloadSourceDocument(id!),
    enabled: Boolean(id) && detailQuery.data?.signerAccountId === currentUser?.accountId,
  });

  useEffect(() => {
    if (!id) {
      setSignatureAppearance({});
      return;
    }

    try {
      const rawDraft = window.sessionStorage.getItem(getSigningAppearanceDraftKey(id));
      if (!rawDraft) {
        setSignatureAppearance({});
        return;
      }

      const parsedDraft = JSON.parse(rawDraft) as SignatureAppearanceOverride;
      setSignatureAppearance(parsedDraft && typeof parsedDraft === "object" ? parsedDraft : {});
    } catch {
      setSignatureAppearance({});
    }
  }, [id]);

  useEffect(() => {
    if (!id) {
      return;
    }

    const hasDraftValue = Object.values(signatureAppearance).some((value) => value !== undefined && value !== null);
    if (!hasDraftValue) {
      window.sessionStorage.removeItem(getSigningAppearanceDraftKey(id));
      return;
    }

    window.sessionStorage.setItem(getSigningAppearanceDraftKey(id), JSON.stringify(signatureAppearance));
  }, [id, signatureAppearance]);

  useEffect(() => {
    const profile = profileQuery.data;
    if (!profile) {
      return;
    }

    setSignatureAppearance((current) => {
      if (
        current.xPercent !== undefined ||
        current.yPercent !== undefined ||
        current.widthPercent !== undefined ||
        current.heightPercent !== undefined ||
        current.imageOnly !== undefined
      ) {
        return current;
      }

      return {
        pageNumber: current.pageNumber,
        imageOnly: profile.signatureImageOnly,
        xPercent: profile.signatureBoxXPercent,
        yPercent: profile.signatureBoxYPercent,
        widthPercent: profile.signatureBoxWidthPercent,
        heightPercent: profile.signatureBoxHeightPercent,
      };
    });
  }, [profileQuery.data]);

  useEffect(() => {
    const source = signatureImageQuery.data;
    if (!source) {
      setSignaturePreviewUrl(null);
      return;
    }

    const objectUrl = URL.createObjectURL(source);
    setSignaturePreviewUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [signatureImageQuery.data]);

  useEffect(() => {
    if (!id || !detailQuery.data?.status || detailQuery.data.status === "APPROVED") {
      return;
    }

    window.sessionStorage.removeItem(getSigningAppearanceDraftKey(id));
  }, [detailQuery.data?.status, id]);

  const invalidateDetail = async () => {
    await queryClient.invalidateQueries({ queryKey: ["signing"] });
    await queryClient.invalidateQueries({ queryKey: ["signing", "detail", id] });
  };

  const approveMutation = useMutation({
    mutationFn: (payload: { certificateId: string; note?: string }) => signingService.approve(id!, payload),
    onSuccess: invalidateDetail,
  });
  const rejectMutation = useMutation({
    mutationFn: (note: string) => signingService.reject(id!, note),
    onSuccess: invalidateDetail,
  });
  const cancelMutation = useMutation({
    mutationFn: () => signingService.cancel(id!),
    onSuccess: async () => {
      await invalidateDetail();
      navigate("/app/sign-requests/mine");
    },
  });
  const beginWebAuthnAssertionMutation = useMutation({
    mutationFn: async () => {
      const options = await stepUpService.beginWebAuthnAssertion({
        registrationId: registrationId || undefined,
        action: "SIGN_DOCUMENT",
        targetType: "SIGNING_REQUEST",
        targetId: id,
      });
      const credentialJson = await getWebAuthnAssertion(options.optionsJson);
      return stepUpService.finishWebAuthnAssertion({
        ceremonyId: options.ceremonyId,
        credentialJson,
      });
    },
    onSuccess: (verification) => {
      setVerificationToken(verification.verificationToken);
      setWebauthnMessage("Đã xác minh giao dịch ký bằng WebAuthn trên thiết bị của bạn.");
    },
    onError: (error) => setWebauthnMessage(error.message),
  });
  const executeMutation = useMutation({
    mutationFn: () =>
      signingService.executeWithAppearance(id!, {
        stepUpVerificationToken: verificationToken,
        signatureAppearance: isPdfSourceDocument ? signatureAppearance : undefined,
      }),
    onSuccess: async () => {
      setVerificationToken("");
      setWebauthnMessage("Đã ký thành công và lưu signed output lên MinIO.");
      if (id) {
        window.sessionStorage.removeItem(getSigningAppearanceDraftKey(id));
      }
      await invalidateDetail();
    },
  });
  const downloadSignedOutputMutation = useMutation({
    mutationFn: () => signingService.downloadSignedOutput(id!),
    onSuccess: ({ blob, fileName }) => {
      const objectUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(objectUrl);
    },
  });

  const request = detailQuery.data;
  const isSigner = request?.signerAccountId === currentUser?.accountId;
  const isRequester = request?.requesterAccountId === currentUser?.accountId;
  const activeCertificates = (certificatesQuery.data || []).filter((certificate) => certificate.status === "ACTIVE");
  const activeRegistrations = (registrationsQuery.data || []).filter((registration) => registration.registrationStatus === "ACTIVE");
  const webauthnRegistrations = activeRegistrations.filter((registration) => registration.mfaType === "WEBAUTHN");
  const webauthnSupported = useMemo(() => isWebAuthnSupported(), []);

  const userMap = useMemo(() => {
    return new Map((activeUsersQuery.data ?? []).map((user) => [user.id, user] as const));
  }, [activeUsersQuery.data]);
  const requesterUser = request ? userMap.get(request.requesterAccountId) : null;
  const signerUser = request ? userMap.get(request.signerAccountId) : null;

  const isPdfSourceDocument =
    Boolean(sourceDocumentQuery.data) &&
    (sourceDocumentQuery.data?.contentType?.toLowerCase().includes("pdf") ||
      sourceDocumentQuery.data?.fileName?.toLowerCase().endsWith(".pdf"));

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow="Workflow ky"
        title="Chi tiết signing request"
        
        actions={request ? <StatusBadge status={request.status} /> : undefined}
      />

      {detailQuery.isLoading ? <Card><CardContent className="pt-8 text-sm text-muted-foreground">Đang tải request...</CardContent></Card> : null}
      {detailQuery.isError ? <Card><CardContent className="pt-8 text-sm text-red-600">{detailQuery.error.message}</CardContent></Card> : null}
      {!request && !detailQuery.isLoading && !detailQuery.isError ? <EmptyState title="Không tìm thấy request" description="Kiểm tra lại requestId." /> : null}

      {request ? (
        <>
          <section className="grid gap-5 xl:grid-cols-[1.25fr_0.95fr]">
            <Card>
              <CardHeader>
                <CardTitle className="text-[1.9rem]">Thông tin tổng quan</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-4 md:grid-cols-2">
                {[
                  // ["Document ID", request.documentId],
                  // ["Requester", request.requesterAccountId],
                  // ["Signer", request.signerAccountId],
                  // ["Tài liệu", documentQuery.data?.title || request.documentId],
                  ["Người yêu cầu", requesterUser ? `${requesterUser.username} (${requesterUser.email})` : request.requesterAccountId],
                  ["Người ký", signerUser ? `${signerUser.username} (${signerUser.email})` : request.signerAccountId],
                  ["Certificate", request.certificateId || "-"],
                  ["Requested at", formatDateTime(request.requestedAt)],
                  ["Signed at", formatDateTime(request.signedAt)],
                ].map(([label, value]) => (
                  <div key={label} className="rounded-[22px] border border-slate-200/80 bg-white/80 p-4">
                    <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{label}</p>
                    <p className="mt-2 break-all text-sm font-medium text-slate-900">{value}</p>
                  </div>
                ))}
              </CardContent>
            </Card>

            <Card className="bg-[linear-gradient(180deg,rgba(255,252,247,0.96),rgba(249,244,236,0.92))]">
              <CardHeader>
                <CardTitle className="text-[1.9rem]">Trạng thái giao dịch</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="rounded-[22px] border border-emerald-200/70 bg-emerald-50/70 p-4 text-sm leading-6 text-emerald-950">
                  Request hiện ở trạng thái <span className="font-semibold">{request.status}</span>. Signer cần vượt qua step-up trước khi execute-signing.
                </div>
                <div className="rounded-[22px] border border-slate-200/80 bg-white/80 p-4 text-sm leading-6 text-slate-600">
                  Bạn nên dùng <span className="font-semibold text-slate-900">WebAuthn platform authenticator</span> để ràng buộc việc ký với sự hiện diện thật sự của người dùng trên thiết bị.
                </div>
                {webauthnMessage ? (
                  <div className="rounded-[22px] border border-sky-200/70 bg-sky-50/80 p-4 text-sm leading-6 text-sky-950">
                    {webauthnMessage}
                  </div>
                ) : null}
              </CardContent>
            </Card>
          </section>
          {isSigner && request.status === "PENDING" && sourceDocumentQuery.data ? (
            <Card>
              <CardHeader>
                <CardTitle className="text-[1.6rem]">Xem trước tài liệu cần ký</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                
                {sourceDocumentQuery.data.contentType?.toLowerCase().includes("pdf") ? (
                  <iframe
                    src={URL.createObjectURL(sourceDocumentQuery.data.blob)}
                    className="w-full h-[500px] rounded-xl border"
                  />
                ) : (
                  <div className="text-sm text-slate-600">
                    Không hỗ trợ preview trực tiếp. Bạn có thể tải file để xem.
                  </div>
                )}

                <Button
                  variant="outline"
                  onClick={() => {
                    const url = URL.createObjectURL(sourceDocumentQuery.data.blob);
                    const link = document.createElement("a");
                    link.href = url;
                    link.download = sourceDocumentQuery.data.fileName;
                    link.click();
                    URL.revokeObjectURL(url);
                  }}
                >
                  Tải tài liệu gốc
                </Button>
              </CardContent>
            </Card>
          ) : null}

          {isSigner && request.status === "PENDING" ? (
            <Card>
              <CardHeader>
                <CardTitle className="text-[1.9rem]">Duyệt hoặc từ chối yêu cầu</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-6 xl:grid-cols-2">
                <form
                  className="space-y-4 rounded-[24px] border border-slate-200/80 bg-white/75 p-5"
                  onSubmit={(event) => {
                    event.preventDefault();
                    approveMutation.mutate({ certificateId: approveCertificateId, note: approveNote });
                  }}
                >
                  <h3 className="text-lg font-semibold text-slate-950">Approve request</h3>
                  <div className="space-y-2">
                    <Label htmlFor="certificateId">Chọn certificate ACTIVE</Label>
                    <select
                      id="certificateId"
                      className="h-11 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-900"
                      value={approveCertificateId}
                      onChange={(event) => setApproveCertificateId(event.target.value)}
                      required
                    >
                      <option value="">Chon certificate</option>
                      {activeCertificates.map((certificate) => (
                        <option key={certificate.id} value={certificate.id}>
                          {certificate.commonName} - {certificate.serialNumber}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="approveNote">Ghi chú</Label>
                    <Textarea id="approveNote" value={approveNote} onChange={(event) => setApproveNote(event.target.value)} />
                  </div>
                  {approveMutation.isError ? <p className="text-sm text-red-600">{approveMutation.error.message}</p> : null}
                  <Button type="submit" disabled={approveMutation.isPending || !approveCertificateId}>Approve request</Button>
                </form>

                <form
                  className="space-y-4 rounded-[24px] border border-slate-200/80 bg-white/75 p-5"
                  onSubmit={(event) => {
                    event.preventDefault();
                    rejectMutation.mutate(rejectNote);
                  }}
                >
                  <h3 className="text-lg font-semibold text-slate-950">Reject request</h3>
                  <div className="space-y-2">
                    <Label htmlFor="rejectNote">Lý do từ chối</Label>
                    <Textarea id="rejectNote" value={rejectNote} onChange={(event) => setRejectNote(event.target.value)} required />
                  </div>
                  {rejectMutation.isError ? <p className="text-sm text-red-600">{rejectMutation.error.message}</p> : null}
                  <Button type="submit" variant="outline" disabled={rejectMutation.isPending || !rejectNote.trim()}>Reject request</Button>
                </form>
              </CardContent>
            </Card>
          ) : null}

          {isRequester && request.status === "PENDING" ? (
            <Card>
              <CardHeader>
                <CardTitle className="text-[1.9rem]">Hủy yêu cầu</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-sm leading-6 text-slate-600">Requester có thể hủy request khi nó vẫn còn ở trạng thái PENDING.</p>
                {cancelMutation.isError ? <p className="text-sm text-red-600">{cancelMutation.error.message}</p> : null}
                <Button variant="outline" onClick={() => cancelMutation.mutate()} disabled={cancelMutation.isPending}>
                  {cancelMutation.isPending ? "Dang huy..." : "Huy request"}
                </Button>
              </CardContent>
            </Card>
          ) : null}

          {isSigner && request.status === "APPROVED" ? (
            <Card>
              <CardHeader>
                <CardTitle className="text-[1.9rem]">Step-up va execute signing</CardTitle>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="rounded-[22px] border border-sky-200/70 bg-sky-50/80 p-4 text-sm leading-6 text-sky-950">
                  Neu ban roi khoi trang nay trong luc dat vi tri chu ky, he thong se luu lai draft layout theo request de ban co the quay lai tiep tuc execute-signing sau.
                </div>
                {sourceDocumentQuery.isLoading ? (
                  <div className="rounded-[22px] border border-slate-200/80 bg-white/80 p-4 text-sm text-slate-600">
                    Đăng tỉa preview tài liệu để đặt vị trí chữ ký...
                  </div>
                ) : null}
                {sourceDocumentQuery.isError ? (
                  <div className="rounded-[22px] border border-amber-200/80 bg-amber-50/85 p-4 text-sm leading-6 text-amber-950">
                    Không thể tải preview tài liệu cho signer. Bạn vẫn có thể ký, nhưng hệ thống sẽ quay về cấu hình visible signature mặc định trong hồ sơ.
                    <div className="mt-2 text-amber-800">{sourceDocumentQuery.error.message}</div>
                  </div>
                ) : null}
                {isPdfSourceDocument && sourceDocumentQuery.data ? (
                  <PdfSignaturePlacementEditor
                    disabled={executeMutation.isPending}
                    hasSignatureImage={Boolean(profileQuery.data?.signatureImageConfigured)}
                    pdfBlob={sourceDocumentQuery.data.blob}
                    signaturePreviewUrl={signaturePreviewUrl}
                    value={signatureAppearance}
                    onChange={setSignatureAppearance}
                  />
                ) : null}
                {sourceDocumentQuery.data && !isPdfSourceDocument ? (
                  <div className="rounded-[22px] border border-slate-200/80 bg-slate-50/85 p-4 text-sm leading-6 text-slate-600">
                    Tài liệu nguồn hiện không phải PDF, nên không có editor đặt vị trí visible signature. Hệ thống sẽ tạo signed output CMS/CAdES `.p7m`.
                  </div>
                ) : null}

                <section className="grid gap-5 xl:grid-cols-[1.2fr_0.9fr]">
                  <div className="space-y-5 rounded-[24px] border border-emerald-200/70 bg-emerald-50/70 p-5">
                    <div className="flex items-start gap-3">
                      <span className="mt-1 flex h-10 w-10 items-center justify-center rounded-2xl bg-white text-emerald-700 shadow-sm">
                        <Fingerprint className="h-5 w-5" />
                      </span>
                      <div className="space-y-1">
                        <h3 className="text-lg font-semibold text-slate-950">Khuyen nghi: WebAuthn tren thiet bi hien tai</h3>
                        <p className="text-sm leading-6 text-slate-600">
                          Windows Hello, Touch ID, Face ID hoac PIN thiet bi se xac nhan rang chinh nguoi dang giu thiet bi dang cho phep ky giao dich nay.
                        </p>
                      </div>
                    </div>

                    {!webauthnSupported ? (
                      <div className="rounded-[20px] border border-amber-200/70 bg-amber-50/85 p-4 text-sm leading-6 text-amber-950">
                        Trinh duyet hien tai khong ho tro WebAuthn. Ban can dung mot trinh duyet/OS co Windows Hello, Touch ID, Face ID hoac PIN thiet bi.
                      </div>
                    ) : (
                      <>
                        {webauthnRegistrations.length === 0 ? (
                          <div className="space-y-4 rounded-[20px] border border-white/80 bg-white/85 p-4">
                            <p className="text-sm leading-6 text-slate-700">
                              Tài khoản này chưa có authenticator WebAuthn. Theo policy mới, authenticator đầu tiên chỉ được enroll bằng enrollment ticket do admin cấp.
                            </p>
                            <Button asChild variant="secondary">
                              <Link to="/app/step-up">Mo module Bao mat va Step-up</Link>
                            </Button>
                          </div>
                        ) : (
                          <div className="space-y-4 rounded-[20px] border border-white/80 bg-white/85 p-4">
                            <div className="space-y-2">
                              <Label htmlFor="webauthnRegistration">Authenticator WebAuthn</Label>
                              <select
                                id="webauthnRegistration"
                                className="h-11 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-900"
                                value={registrationId}
                                onChange={(event) => setRegistrationId(event.target.value)}
                              >
                                <option value="">Dung primary authenticator</option>
                                {webauthnRegistrations.map((registration) => (
                                  <option key={registration.id} value={registration.id}>
                                    {registration.label} {registration.primary ? "(primary)" : ""}
                                  </option>
                                ))}
                              </select>
                            </div>
                            {beginWebAuthnAssertionMutation.isError ? <p className="text-sm text-red-600">{beginWebAuthnAssertionMutation.error.message}</p> : null}
                            <Button
                              onClick={() => {
                                setWebauthnMessage(null);
                                beginWebAuthnAssertionMutation.mutate();
                              }}
                              disabled={beginWebAuthnAssertionMutation.isPending}
                            >
                              {beginWebAuthnAssertionMutation.isPending ? "Dang xac nhan..." : "Xac nhan bang thiet bi"}
                            </Button>
                          </div>
                        )}
                      </>
                    )}
                  </div>

                  <div className="space-y-4 rounded-[24px] border border-slate-200/80 bg-[linear-gradient(180deg,rgba(255,252,247,0.96),rgba(249,244,236,0.92))] p-5">
                    <div className="flex items-start gap-3">
                      <span className="mt-1 flex h-10 w-10 items-center justify-center rounded-2xl bg-white text-accent shadow-sm">
                        <ShieldCheck className="h-5 w-5" />
                      </span>
                      <div className="space-y-1">
                        <h3 className="text-lg font-semibold text-slate-950">Proof token cho giao dich ky</h3>
                        <p className="text-sm leading-6 text-slate-600">
                          Execute-signing chi duoc mo khi step-up tra ve proof token hop le, ngan han va dung mot lan. Neu ban vua dat vi tri chu ky tren preview PDF, chinh layout do se duoc dung cho lan ky nay.
                        </p>
                      </div>
                    </div>
                    <div className="rounded-[20px] border border-slate-200/80 bg-white/85 p-4">
                      <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Token state</p>
                      <p className="mt-2 break-all text-sm font-medium text-slate-900">{verificationToken || "Chưa có proof token"}</p>
                    </div>
                    {executeMutation.isError ? <p className="text-sm text-red-600">{executeMutation.error.message}</p> : null}
                    <Button onClick={() => executeMutation.mutate()} disabled={executeMutation.isPending || !verificationToken}>
                      {executeMutation.isPending ? "Đăng ký số..." : "Execute signing"}
                    </Button>
                  </div>
                </section>
              </CardContent>
            </Card>
          ) : null}

          {request.artifact ? (
            <Card>
              <CardHeader>
                <CardTitle className="text-[1.9rem]">Signing artifact</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                {[
                  ["File name", request.artifact.fileName],
                  ["Digest algorithm", request.artifact.digestAlgorithm],
                  ["Signature algorithm", request.artifact.signatureAlgorithm],
                  ["Output object key", request.artifact.outputObjectKey || "-"],
                ].map(([label, value]) => (
                  <div key={label} className="rounded-[22px] border border-slate-200/80 bg-white/80 p-4">
                    <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{label}</p>
                    <p className="mt-2 break-all text-sm font-medium text-slate-900">{value}</p>
                  </div>
                ))}
                <div className="md:col-span-2 xl:col-span-4">
                  <div className="flex flex-wrap gap-3">
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => downloadSignedOutputMutation.mutate()}
                      disabled={downloadSignedOutputMutation.isPending}
                    >
                      {downloadSignedOutputMutation.isPending ? "Đang tải file đã ký..." : "Tải signed output"}
                    </Button>
                    <Button asChild size="sm" variant="outline">
                      <Link to={`/app/verify?requestId=${request.id}`}>Xác minh request này</Link>
                    </Button>
                  </div>
                  {downloadSignedOutputMutation.isError ? (
                    <p className="mt-3 text-sm text-red-600">{downloadSignedOutputMutation.error.message}</p>
                  ) : null}
                </div>
              </CardContent>
            </Card>
          ) : null}

          <Card>
            <CardHeader>
              <CardTitle className="text-[1.9rem]">Lịch sử sự kiện</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {request.events.map((event) => (
                <div key={event.id} className="rounded-[22px] border border-slate-200/80 bg-white/80 p-4 text-sm text-slate-600">
                  <p className="font-semibold text-slate-950">{event.eventType}</p>
                  <p className="mt-2">Actor: {event.actorAccountId || "-"}</p>
                  <p>Time: {formatDateTime(event.eventTime)}</p>
                  <p>Detail: {event.detail || "-"}</p>
                </div>
              ))}
            </CardContent>
          </Card>
        </>
      ) : null}
    </div>
  );
}
