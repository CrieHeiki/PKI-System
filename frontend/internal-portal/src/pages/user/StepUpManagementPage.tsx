import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Fingerprint, ShieldAlert, ShieldCheck, ShieldEllipsis, Star } from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { formatDateTime, truncateMiddle } from "@/lib/utils";
import { createWebAuthnCredential, getWebAuthnAssertion, isWebAuthnSupported } from "@/lib/webauthn";
import { stepUpService } from "@/services/step-up-service";

export function StepUpManagementPage() {
  const queryClient = useQueryClient();
  const [webauthnLabel, setWebauthnLabel] = useState("Thiết bị ký chính");
  const [firstEnrollmentTicket, setFirstEnrollmentTicket] = useState("");
  const [verificationToken, setVerificationToken] = useState("");
  const [recoveryReason, setRecoveryReason] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);

  const registrationsQuery = useQuery({
    queryKey: ["step-up", "registrations"],
    queryFn: stepUpService.listRegistrations,
  });
  const recoveryRequestsQuery = useQuery({
    queryKey: ["step-up", "recovery-requests"],
    queryFn: stepUpService.listMyRecoveryRequests,
  });

  const registrations = registrationsQuery.data ?? [];
  const recoveryRequests = recoveryRequestsQuery.data ?? [];
  const webauthnRegistrations = registrations.filter((registration) => registration.mfaType === "WEBAUTHN");
  const activeWebAuthnRegistrations = webauthnRegistrations.filter((registration) => registration.registrationStatus === "ACTIVE");
  const activeRegistration = activeWebAuthnRegistrations.find((registration) => registration.primary) ?? activeWebAuthnRegistrations[0] ?? null;
  const latestRecoveryRequest = recoveryRequests[0] ?? null;
  const openRecoveryRequest =
    recoveryRequests.find((request) => request.status === "PENDING" || request.status === "APPROVED") ?? null;
  const approvedRecoveryRequest =
    openRecoveryRequest?.status === "APPROVED" && openRecoveryRequest.recoveryTicket ? openRecoveryRequest : null;
  const recoveryOpen = Boolean(openRecoveryRequest);
  const webauthnSupported = useMemo(() => isWebAuthnSupported(), []);
  const hasExistingAuthenticator = activeWebAuthnRegistrations.length > 0;
  const showFirstEnrollment = !hasExistingAuthenticator && !approvedRecoveryRequest;

  const refreshStepUpState = async () => {
    await queryClient.invalidateQueries({ queryKey: ["step-up", "registrations"] });
    await queryClient.invalidateQueries({ queryKey: ["step-up", "recovery-requests"] });
  };

  const createWebAuthnMutation = useMutation({
    mutationFn: async () => {
      const options = await stepUpService.beginWebAuthnRegistration({
        label: webauthnLabel,
        markAsPrimary: true,
        enrollmentTicket: approvedRecoveryRequest
          ? approvedRecoveryRequest.recoveryTicket ?? undefined
          : showFirstEnrollment
            ? firstEnrollmentTicket.trim()
            : undefined,
      });
      const credentialJson = await createWebAuthnCredential(options.optionsJson);
      return stepUpService.finishWebAuthnRegistration({
        ceremonyId: options.ceremonyId,
        credentialJson,
      });
    },
    onSuccess: async () => {
      setFirstEnrollmentTicket("");
      setRecoveryReason("");
      setFeedback(
        approvedRecoveryRequest
          ? "Đã đăng ký thành công thiết bị thay thế. Thiết bị WebAuthn cũ đã được thay bằng thiết bị mới theo recovery flow."
          : "Đã đăng ký thành công thiết bị WebAuthn cho tài khoản.",
      );
      await refreshStepUpState();
    },
    onError: (error) => setFeedback(error.message),
  });

  const verifyMutation = useMutation({
    mutationFn: async () => {
      const options = await stepUpService.beginWebAuthnAssertion({
        action: "MANAGE_STEP_UP",
        targetType: "SELF_SERVICE",
      });
      const credentialJson = await getWebAuthnAssertion(options.optionsJson);
      return stepUpService.finishWebAuthnAssertion({
        ceremonyId: options.ceremonyId,
        credentialJson,
      });
    },
    onSuccess: (verification) => {
      setVerificationToken(verification.verificationToken);
      setFeedback("Đã xác minh thành công bằng WebAuthn cho giao dịch từ quản lý step-up.");
    },
    onError: (error) => setFeedback(error.message),
  });

  const createRecoveryMutation = useMutation({
    mutationFn: () => stepUpService.createRecoveryRequest(recoveryReason.trim()),
    onSuccess: async () => {
      setRecoveryReason("");
      setFeedback("Đã tạo yêu cầu recovery. Trong lúc chờ admin duyệt, assertion WebAuthn thông thường sẽ bị tạm khóa.");
      await refreshStepUpState();
    },
    onError: (error) => setFeedback(error.message),
  });

  const cancelRecoveryMutation = useMutation({
    mutationFn: (requestId: string) => stepUpService.cancelRecoveryRequest(requestId),
    onSuccess: async () => {
      setFeedback("Đã hủy recovery request. Bạn có thể tiếp tục sử dụng thiết bị WebAuthn hiện tại.");
      await refreshStepUpState();
    },
    onError: (error) => setFeedback(error.message),
  });

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow="Bao mat va Step-up"
        title="Quản lý WebAuthn cho tài khoản"
        description="Tài khoản chỉ được phép có một thiết bị WebAuthn đang ACTIVE. Nếu cần thay thiết bị, bạn sẽ đi qua recovery flow do admin duyệt."
        actions={
          <Button
            variant="secondary"
            onClick={() => {
              setFeedback(null);
              refreshStepUpState();
            }}
          >
            Lam moi danh sach
          </Button>
        }
      />

      <section className="grid gap-5 xl:grid-cols-[1.2fr_0.9fr]">
        <Card className="bg-slate-950 text-white shadow-[0_18px_45px_rgba(15,23,42,0.18)]">
          <CardHeader className="space-y-4">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-blue-200">WebAuthn-only security</p>
            <CardTitle className="max-w-2xl text-[2.1rem] leading-[1.04] text-white">
              Step-up chỉ chấp nhận một authenticator WebAuthn hoạt động trên mỗi tài khoản.
            </CardTitle>
            <p className="max-w-2xl text-sm leading-7 text-white/70">
              Thiết bị đầu tiên phải do admin cấp ticket để đăng ký. Khi cần đổi thiết bị, user gửi recovery request và đăng ký thiết bị thay thế sau khi admin duyệt.
            </p>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-3">
            <div className="rounded-[22px] border border-white/10 bg-white/5 p-5">
              <ShieldCheck className="h-5 w-5 text-blue-200" />
              <p className="mt-4 text-xs font-semibold uppercase tracking-[0.16em] text-white/58">Active authenticators</p>
              <p className="mt-2 text-3xl font-bold text-white">{activeWebAuthnRegistrations.length}</p>
              <p className="mt-2 text-sm leading-6 text-white/70">Policy mới chỉ cho phép tối đa một thiết bị WebAuthn đang ACTIVE trên mỗi tài khoản.</p>
            </div>
            <div className="rounded-[22px] border border-white/10 bg-white/5 p-5">
              <Star className="h-5 w-5 text-blue-200" />
              <p className="mt-4 text-xs font-semibold uppercase tracking-[0.16em] text-white/58">Current authenticator</p>
              <p className="mt-2 text-lg font-bold text-white">{activeRegistration?.label ?? "Chưa đăng ký"}</p>
              <p className="mt-2 text-sm leading-6 text-white/70">
                {activeRegistration ? "Đây là thiết bị đang được dùng cho các giao dịch ký và thao tác step-up." : "Bạn cần đăng ký WebAuthn để mở khóa các thao tác nhạy cảm."}
              </p>
            </div>
            <div className="rounded-[22px] border border-white/10 bg-white/5 p-5">
              <Fingerprint className="h-5 w-5 text-blue-200" />
              <p className="mt-4 text-xs font-semibold uppercase tracking-[0.16em] text-white/58">Browser support</p>
              <p className="mt-2 text-3xl font-bold text-white">{webauthnSupported ? "YES" : "NO"}</p>
              <p className="mt-2 text-sm leading-6 text-white/70">
                {webauthnSupported ? "Trình duyệt và thiết bị sẵn sàng cho platform authenticator." : "Cần sử dụng Chrome/Edge/Firefox trên thiết bị có WebAuthn."}
              </p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Current proof</p>
            <CardTitle className="text-[1.6rem]">Proof token và recovery state</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-3">
              <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Verification token</p>
              <p className="mt-2 break-all text-sm font-medium text-slate-900">{verificationToken || "Chưa có proof token"}</p>
            </div>
            <div className="rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-3">
              <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Recovery state</p>
              <div className="mt-2 flex items-center gap-2">
                {openRecoveryRequest ? <StatusBadge status={openRecoveryRequest.status} /> : <Badge variant="info">NONE</Badge>}
                <span className="text-sm font-medium text-slate-900">
                  {openRecoveryRequest
                    ? openRecoveryRequest.status === "APPROVED"
                      ? "Admin đã duyệt. Bạn có thể đăng ký thiết bị thay thế."
                      : "Recovery đang mở. Assertion WebAuthn thông thường tạm thời bị khóa."
                    : "Không có recovery request đang mở"}
                </span>
              </div>
            </div>
            {feedback ? <div className="rounded-[18px] border border-blue-200 bg-blue-50 px-4 py-3 text-sm leading-6 text-blue-950">{feedback}</div> : null}
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
        <Card>
          <CardHeader className="flex flex-row items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Enroll WebAuthn</p>
              <CardTitle className="text-[1.45rem]">
                {approvedRecoveryRequest ? "Đăng ký thiết bị thay thế" : hasExistingAuthenticator ? "Thiết bị WebAuthn hiện tại" : "Đăng ký thiết bị đầu tiên"}
              </CardTitle>
            </div>
            <Fingerprint className="h-5 w-5 text-primary" />
          </CardHeader>
          <CardContent className="space-y-4">
            {!webauthnSupported ? (
              <div className="rounded-[18px] border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-6 text-amber-950">
                Trình duyệt hiện tại không hỗ trợ WebAuthn. ạn cần sử dụng một trình duyệt/OS có Windows Hello, Touch ID, Face ID hoặc PIN thiết bị.
              </div>
            ) : null}
            {approvedRecoveryRequest ? (
              <div className="space-y-4 rounded-[20px] border border-emerald-200/80 bg-emerald-50/85 p-4">
                <div className="rounded-[16px] border border-emerald-200/70 bg-white/80 px-4 py-3 text-sm leading-6 text-slate-700">
                  Admin đã duyệt recovery request. Sau khi bạn đăng ký thành công thiết bị thay thế, thiết bị WebAuthn cũ đang ACTIVE sẽ được thay thế bằng thiết bị mới.
                </div>
                <div className="grid gap-3 md:grid-cols-2">
                  <div className="rounded-[16px] border border-white bg-white px-4 py-3">
                    <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Recovery request</p>
                    <p className="mt-1 font-medium text-slate-900">{truncateMiddle(approvedRecoveryRequest.id, 10, 8)}</p>
                  </div>
                  <div className="rounded-[16px] border border-white bg-white px-4 py-3">
                    <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Ticket expires at</p>
                    <p className="mt-1 font-medium text-slate-900">
                      {approvedRecoveryRequest.recoveryTicketExpiresAt ? formatDateTime(approvedRecoveryRequest.recoveryTicketExpiresAt) : "Chưa có"}
                    </p>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="recoveryWebauthnLabel">Nhận thiết bị thay thế</Label>
                  <Input id="recoveryWebauthnLabel" value={webauthnLabel} onChange={(event) => setWebauthnLabel(event.target.value)} />
                </div>
                <Button
                  onClick={() => {
                    setFeedback(null);
                    createWebAuthnMutation.mutate();
                  }}
                  disabled={!webauthnSupported || createWebAuthnMutation.isPending}
                >
                  {createWebAuthnMutation.isPending ? "Đang đăng ký..." : "Đăng ký thiết bị thay thế"}
                </Button>
              </div>
            ) : hasExistingAuthenticator ? (
              <div className="space-y-4 rounded-[20px] border border-slate-200/80 bg-white/85 p-4">
                <div className="rounded-[16px] border border-slate-200/70 bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-700">
                  Theo policy hiện tại, mỗi tài khoản chỉ được giữ một thiết bị WebAuthn đang ACTIVE. Nếu bạn cần đổi sang thiết bị khác, hãy tạo recovery request ở khung bên phải.
                </div>
                <div className="rounded-[16px] border border-white bg-white px-4 py-3">
                  <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Thiết bị hiện tại</p>
                  <p className="mt-1 text-lg font-semibold text-slate-900">{activeRegistration?.label ?? "Không xác định"}</p>
                  <p className="mt-2 text-sm text-slate-600">
                    Dang ACTIVE tu {activeRegistration?.activatedAt ? formatDateTime(activeRegistration.activatedAt) : "khong ro thoi diem"}.
                  </p>
                </div>
              </div>
            ) : (
              <div className="space-y-4 rounded-[20px] border border-slate-200/80 bg-white/85 p-4">
                <div className="rounded-[18px] border border-amber-200/70 bg-amber-50/85 px-4 py-4 text-sm leading-6 text-amber-950">
                  Thiết bị WebAuthn đầu tiên không được self-enroll chỉ bằng password. Bạn cần enrollment ticket ngắn hạn do admin cấp trước khi đăng ký lần đầu.
                </div>
                <div className="space-y-2">
                  <Label htmlFor="firstEnrollmentTicket">Enrollment ticket do admin cap</Label>
                  <Input id="firstEnrollmentTicket" value={firstEnrollmentTicket} onChange={(event) => setFirstEnrollmentTicket(event.target.value)} placeholder="Dan enrollment ticket vao day" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="firstWebauthnLabel">Nhan thiet bi</Label>
                  <Input id="firstWebauthnLabel" value={webauthnLabel} onChange={(event) => setWebauthnLabel(event.target.value)} />
                </div>
                <p className="text-sm leading-6 text-slate-600">Ticket chỉ dùng một lần và có thời hạn ngắn để onboarding thiết bị WebAuthn đầu tiên.</p>
                <Button
                  onClick={() => {
                    setFeedback(null);
                    createWebAuthnMutation.mutate();
                  }}
                  disabled={!webauthnSupported || !firstEnrollmentTicket.trim() || createWebAuthnMutation.isPending}
                >
                  {createWebAuthnMutation.isPending ? "Dang dang ky..." : "Dang ky thiet bi dau tien"}
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Recovery flow</p>
              <CardTitle className="text-[1.45rem]">Khôi phục khi mất hoặc đổi thiết bị</CardTitle>
            </div>
            <ShieldAlert className="h-5 w-5 text-primary" />
          </CardHeader>
          <CardContent className="space-y-4">
            {recoveryRequestsQuery.isError ? <p className="text-sm text-red-600">{recoveryRequestsQuery.error.message}</p> : null}
            {openRecoveryRequest ? (
              <div className="space-y-4 rounded-[20px] border border-slate-200 bg-slate-50 p-4">
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge status={openRecoveryRequest.status} />
                  <span className="text-sm font-medium text-slate-900">
                    {openRecoveryRequest.status === "APPROVED" ? "Recovery đã được admin duyệt" : "Recovery đang chờ admin xử lý"}
                  </span>
                </div>
                <div className="grid gap-3 md:grid-cols-2">
                  <div className="rounded-[16px] border border-white bg-white px-4 py-3">
                    <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Requested at</p>
                    <p className="mt-1 font-medium text-slate-900">{formatDateTime(openRecoveryRequest.requestedAt)}</p>
                  </div>
                  <div className="rounded-[16px] border border-white bg-white px-4 py-3">
                    <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Request ID</p>
                    <p className="mt-1 font-medium text-slate-900">{truncateMiddle(openRecoveryRequest.id, 10, 8)}</p>
                  </div>
                </div>
                {openRecoveryRequest.reason ? (
                  <div className="rounded-[16px] border border-white bg-white px-4 py-3 text-sm leading-6 text-slate-700">
                    <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Lý do</p>
                    <p className="mt-2">{openRecoveryRequest.reason}</p>
                  </div>
                ) : null}
                {openRecoveryRequest.decisionNote ? (
                  <div className="rounded-[16px] border border-white bg-white px-4 py-3 text-sm leading-6 text-slate-700">
                    <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Ghi chú admin</p>
                    <p className="mt-2">{openRecoveryRequest.decisionNote}</p>
                  </div>
                ) : null}
                {approvedRecoveryRequest?.recoveryTicketExpiresAt ? (
                  <div className="rounded-[16px] border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm leading-6 text-emerald-950">
                    Recovery ticket đã sẵn sàng đến <span className="font-semibold">{formatDateTime(approvedRecoveryRequest.recoveryTicketExpiresAt)}</span>. Đăng ký thiết bị thay thế ở khung bên trái để hoàn tất thay thế thiết bị hiện tại.
                  </div>
                ) : null}
                <Button
                  variant="outline"
                  onClick={() => {
                    setFeedback(null);
                    cancelRecoveryMutation.mutate(openRecoveryRequest.id);
                  }}
                  disabled={cancelRecoveryMutation.isPending}
                >
                  {cancelRecoveryMutation.isPending ? "Dang huy..." : "Huy recovery request"}
                </Button>
              </div>
            ) : (
              <div className="space-y-4 rounded-[20px] border border-slate-200 bg-slate-50 p-4">
                <p className="text-sm leading-6 text-slate-600">
                  Khi thiết bị WebAuthn hiện tại bị mất, hỏng, hoặc bạn muốn chuyển sang thiết bị khác, hãy tạo recovery request để admin duyệt. Sau khi duyệt, bạn đăng ký thiết bị thay thế và hệ thống sẽ chuyển sang thiết bị mới.
                </p>
                {!hasExistingAuthenticator ? (
                  <div className="rounded-[16px] border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-6 text-amber-950">
                    Bạn chưa có thiết bị WebAuthn ACTIVE, nên không cần recovery flow. Hãy yêu cầu admin cấp first-enrollment ticket để đăng ký lần đầu.
                  </div>
                ) : (
                  <>
                    <div className="space-y-2">
                      <Label htmlFor="recoveryReason">Ly do recovery</Label>
                      <Textarea id="recoveryReason" value={recoveryReason} onChange={(event) => setRecoveryReason(event.target.value)} placeholder="Vi du: Mat laptop, doi may moi, Windows Hello tren may cu khong con su dung duoc..." />
                    </div>
                    <Button
                      variant="secondary"
                      onClick={() => {
                        setFeedback(null);
                        createRecoveryMutation.mutate();
                      }}
                      disabled={!recoveryReason.trim() || createRecoveryMutation.isPending}
                    >
                      {createRecoveryMutation.isPending ? "Dang gui..." : "Gui recovery request"}
                    </Button>
                  </>
                )}
                {latestRecoveryRequest && ["COMPLETED", "REJECTED", "CANCELLED"].includes(latestRecoveryRequest.status) ? (
                  <div className="rounded-[16px] border border-white bg-white px-4 py-3 text-sm leading-6 text-slate-700">
                    Lần recovery gần nhất: <span className="font-semibold">{latestRecoveryRequest.status}</span> lúc {formatDateTime(latestRecoveryRequest.completedAt || latestRecoveryRequest.reviewedAt || latestRecoveryRequest.requestedAt)}.
                  </div>
                ) : null}
              </div>
            )}
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-5 xl:grid-cols-[0.9fr_1.1fr]">
        <Card>
          <CardHeader className="flex flex-row items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Independent verification</p>
              <CardTitle className="text-[1.45rem]">Test WebAuthn doc lap</CardTitle>
            </div>
            <CheckCircle2 className="h-5 w-5 text-primary" />
          </CardHeader>
          <CardContent className="space-y-4">
            {recoveryOpen ? (
              <div className="rounded-[18px] border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-6 text-amber-950">
                Dang co recovery request mo, nen bai test assertion thong thuong bi khoa cho den khi recovery duoc huy hoac hoan tat.
              </div>
            ) : !activeRegistration ? (
              <div className="rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-600">
                Chưa có thiết bị WebAuthn ACTIVE để chạy bài test assertion.
              </div>
            ) : (
              <>
                <div className="rounded-[16px] border border-slate-200 bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-700">
                  Assertion sẽ được thực hiện bằng thiết bị đang ACTIVE: <span className="font-semibold text-slate-900">{activeRegistration.label}</span>.
                </div>
                <p className="text-sm leading-6 text-slate-600">
                  Bài test này tạo assertion với action <span className="font-semibold text-slate-900">MANAGE_STEP_UP</span> để kiểm tra thiết bị WebAuthn hiện tại vẫn hoạt động tốt trước khi dùng trong flow ký.
                </p>
                {verifyMutation.isError ? <p className="text-sm text-red-600">{verifyMutation.error.message}</p> : null}
                <Button
                  onClick={() => {
                    setFeedback(null);
                    verifyMutation.mutate();
                  }}
                  disabled={!webauthnSupported || verifyMutation.isPending}
                >
                  {verifyMutation.isPending ? "Đang xác minh..." : "Xác minh bằng WebAuthn"}
                </Button>
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Authenticator inventory</p>
              <CardTitle className="text-[1.45rem]">Thiet bi WebAuthn da dang ky</CardTitle>
            </div>
            <ShieldEllipsis className="h-5 w-5 text-primary" />
          </CardHeader>
          <CardContent className="space-y-4">
            {registrationsQuery.isLoading ? <p className="text-sm text-slate-500">Đang tải danh sách thiết bị...</p> : null}
            {registrationsQuery.isError ? <p className="text-sm text-red-600">{registrationsQuery.error.message}</p> : null}
            {!registrationsQuery.isLoading && !webauthnRegistrations.length ? (
              <EmptyState title="Chưa có thiết bị WebAuthn" description="Bạn cần enrollment ticket do admin cấp để đăng ký thiết bị đầu tiên." />
            ) : null}
            {activeWebAuthnRegistrations.length > 1 ? (
              <div className="rounded-[18px] border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-6 text-amber-950">
                Tài khoản này đang có dữ liệu cũ với hơn một thiết bị ACTIVE. Policy mới không cho phép đăng ký thêm, và bạn nên dùng recovery flow để đưa hệ thống về một thiết bị chính xác.
              </div>
            ) : null}

            <div className="grid gap-4 xl:grid-cols-2">
              {webauthnRegistrations.map((registration) => (
                <div key={registration.id} className="rounded-[22px] border border-slate-200 bg-slate-50 p-5">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="space-y-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <Badge variant="info">WEBAUTHN</Badge>
                        <StatusBadge status={registration.registrationStatus} />
                        {registration.primary ? <Badge variant="success">ACTIVE DEVICE</Badge> : null}
                      </div>
                      <p className="text-lg font-semibold text-slate-950">{registration.label}</p>
                      <p className="text-sm text-slate-500">{registration.issuer || "CT556 PKI"}</p>
                    </div>
                    <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-blue-50 text-blue-700">
                      <Fingerprint className="h-5 w-5" />
                    </span>
                  </div>

                  <div className="mt-4 grid gap-3 text-sm text-slate-600 sm:grid-cols-2">
                    <div className="rounded-[16px] border border-white bg-white px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Created</p>
                      <p className="mt-1 font-medium text-slate-900">{formatDateTime(registration.createdAt)}</p>
                    </div>
                    <div className="rounded-[16px] border border-white bg-white px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Registration ID</p>
                      <p className="mt-1 font-medium text-slate-900">{truncateMiddle(registration.id, 10, 8)}</p>
                    </div>
                  </div>

                  {registration.registrationStatus === "ACTIVE" ? (
                    <p className="mt-4 text-sm leading-6 text-slate-600">
                      Đây là thiết bị WebAuthn đang được sử dụng. Nếu cần đổi thiết bị, hay bắt đầu recovery flow thay vì thêm thiết bị mới.
                    </p>
                  ) : (
                    <p className="mt-4 text-sm leading-6 text-slate-600">Bản ghi này đã không còn ACTIVE và chỉ được giữ lại để đối chiếu lịch sử step-up.</p>
                  )}
                </div>
              ))}
            </div>
            {!hasExistingAuthenticator ? (
              <div className="rounded-[20px] border border-dashed border-slate-300 bg-slate-50 px-4 py-4 text-sm leading-6 text-slate-600">
                Sau khi admin cấp enrollment ticket, quay lại đây để enroll WebAuthn đầu tiên. Nếu cần xem thông tin tài khoản hiện tại, mở{" "}
                <Link className="font-semibold text-slate-900 underline-offset-4 hover:underline" to="/app/profile">
                  Hồ sơ cá nhân
                </Link>
                .
              </div>
            ) : null}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
