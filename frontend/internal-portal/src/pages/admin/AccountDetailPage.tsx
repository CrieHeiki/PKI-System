import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { accountsService } from "@/services/accounts-service";
import { stepUpService } from "@/services/step-up-service";
import { useAuthStore } from "@/store/auth-store";
import { formatDateTime } from "@/lib/utils";

export function AccountDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const currentUser = useAuthStore((state) => state.user);
  const [role, setRole] = useState("USER");
  const [recoveryReviewNote, setRecoveryReviewNote] = useState("");
  const [issuedEnrollmentTicket, setIssuedEnrollmentTicket] = useState<null | {
    enrollmentTicket: string;
    expiresAt: string;
    issuedAt: string;
  }>(null);

  const detailQuery = useQuery({
    queryKey: ["admin", "accounts", "detail", id],
    queryFn: () => accountsService.get(id!),
    enabled: Boolean(id),
  });
  const recoveryRequestsQuery = useQuery({
    queryKey: ["admin", "accounts", "detail", id, "step-up-recovery"],
    queryFn: () => stepUpService.listRecoveryRequestsForAdmin(id!),
    enabled: Boolean(id),
  });

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["admin", "accounts"] });
    await queryClient.invalidateQueries({ queryKey: ["admin", "accounts", "detail", id] });
    await queryClient.invalidateQueries({ queryKey: ["admin", "accounts", "detail", id, "step-up-recovery"] });
  };

  const activateMutation = useMutation({ mutationFn: accountsService.activate, onSuccess: refresh });
  const lockMutation = useMutation({ mutationFn: accountsService.lock, onSuccess: refresh });
  const unlockMutation = useMutation({ mutationFn: accountsService.unlock, onSuccess: refresh });
  const disableMutation = useMutation({ mutationFn: accountsService.disable, onSuccess: refresh });
  const restoreMutation = useMutation({ mutationFn: accountsService.restore, onSuccess: refresh });
  const changeRoleMutation = useMutation({
    mutationFn: (nextRole: string) => accountsService.changeRole(id!, nextRole),
    onSuccess: refresh,
  });
  const issueEnrollmentTicketMutation = useMutation({
    mutationFn: (userId: string) => stepUpService.createFirstEnrollmentTicket(userId),
    onSuccess: (ticket) => {
      setIssuedEnrollmentTicket({
        enrollmentTicket: ticket.enrollmentTicket,
        issuedAt: ticket.issuedAt,
        expiresAt: ticket.expiresAt,
      });
    },
  });
  const approveRecoveryMutation = useMutation({
    mutationFn: (requestId: string) => stepUpService.approveRecoveryRequest(requestId, recoveryReviewNote),
    onSuccess: async () => {
      setRecoveryReviewNote("");
      await refresh();
    },
  });
  const rejectRecoveryMutation = useMutation({
    mutationFn: (requestId: string) => stepUpService.rejectRecoveryRequest(requestId, recoveryReviewNote),
    onSuccess: async () => {
      setRecoveryReviewNote("");
      await refresh();
    },
  });

  const account = detailQuery.data;
  const latestRecoveryRequest = recoveryRequestsQuery.data?.[0] ?? null;
  const isSelfAccount = account?.id === currentUser?.accountId;

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Account detail" title="Chi tiết tài khoản" description="Trang này cho phép admin đổi role và cậ p nhật status tài khoản." />
      {detailQuery.isLoading ? <Card><CardContent className="pt-6 text-sm text-muted-foreground">\u0110ang tải account...</CardContent></Card> : null}
      {detailQuery.isError ? <Card><CardContent className="pt-6 text-sm text-red-600">{detailQuery.error.message}</CardContent></Card> : null}
      {!account && !detailQuery.isLoading && !detailQuery.isError ? <EmptyState title="Không tìm thấy account" description="Kiểm tra lại accountId." /> : null}
      {account ? (
        <>
          <Card>
            <CardHeader><CardTitle>{account.username}</CardTitle></CardHeader>
            <CardContent className="grid gap-4 md:grid-cols-2">
              <div><p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Email</p><p className="mt-1">{account.email}</p></div>
              <div><p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Role</p><p className="mt-1">{account.role}</p></div>
              <div><p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Status</p><div className="mt-2"><StatusBadge status={account.status} /></div></div>
              <div className="md:col-span-2"><p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Account ID</p><p className="mt-1 break-all font-mono text-sm">{account.id}</p></div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Action</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-wrap gap-3">
                {account.status === "PENDING" ? (
                  <Button variant="outline" onClick={() => activateMutation.mutate(account.id)}>Activate</Button>
                ) : null}
                {account.status === "ACTIVE" ? (
                  <>
                    <Button variant="outline" onClick={() => lockMutation.mutate(account.id)} disabled={isSelfAccount}>Lock</Button>
                    <Button variant="outline" onClick={() => disableMutation.mutate(account.id)} disabled={isSelfAccount}>Disable</Button>
                  </>
                ) : null}
                {account.status === "LOCKED" ? (
                  <>
                    <Button variant="outline" onClick={() => unlockMutation.mutate(account.id)}>Unlock</Button>
                    <Button variant="outline" onClick={() => disableMutation.mutate(account.id)} disabled={isSelfAccount}>Disable</Button>
                  </>
                ) : null}
                {account.status === "DISABLED" ? (
                  <Button variant="outline" onClick={() => restoreMutation.mutate(account.id)}>Restore</Button>
                ) : null}
              </div>
              {isSelfAccount ? <p className="text-sm text-amber-700">Admin không được tự lock hoặc disable chính tài khoản của mình.</p> : null}
              <div className="flex flex-col gap-3 md:flex-row md:items-end">
                <div className="space-y-2">
                  <Label htmlFor="role">Doi role</Label>
                  <select id="role" className="h-11 rounded-2xl border border-input bg-white px-4 text-sm" value={role} onChange={(event) => setRole(event.target.value)}>
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </div>
                <Button onClick={() => changeRoleMutation.mutate(role)}>Cập nhật role</Button>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>WebAuthn onboarding ticket</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm leading-6 text-muted-foreground">
                Authenticator WebAuthn đầu tiên phải được admin cấp ticket enroll một lần. Chi cap cho account dang ACTIVE.
              </p>
              <Button
                variant="secondary"
                onClick={() => issueEnrollmentTicketMutation.mutate(account.id)}
                disabled={issueEnrollmentTicketMutation.isPending || account.status !== "ACTIVE"}
              >
                {issueEnrollmentTicketMutation.isPending ? "Dang cap ticket..." : "Cấp ticket enroll đầu tiên"}
              </Button>
              {account.status !== "ACTIVE" ? (
                <p className="text-sm text-amber-700">Chỉ có thể cấp ticket cho account ACTIVE.</p>
              ) : null}
              {issueEnrollmentTicketMutation.isError ? <p className="text-sm text-red-600">{issueEnrollmentTicketMutation.error.message}</p> : null}
              {issuedEnrollmentTicket ? (
                <div className="space-y-3 rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <div>
                    <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Enrollment ticket</p>
                    <p className="mt-2 break-all font-mono text-sm text-slate-900">{issuedEnrollmentTicket.enrollmentTicket}</p>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Issued at</p>
                      <p className="mt-1 text-sm text-slate-900">{formatDateTime(issuedEnrollmentTicket.issuedAt)}</p>
                    </div>
                    <div>
                      <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Expires at</p>
                      <p className="mt-1 text-sm text-slate-900">{formatDateTime(issuedEnrollmentTicket.expiresAt)}</p>
                    </div>
                  </div>
                </div>
              ) : null}
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>WebAuthn recovery flow</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm leading-6 text-muted-foreground">
                Khi user mất thiết bị WebAuthn cuối cùng, admin duyệt recovery request va cấp recovery ticket ngắn hạn để user đăng ký thiết bị thay thế.
                Sau khi recovery hoàn tất, authenticator WebAuthn ACTIVE trước đó sẽ bị disable.
              </p>
              {detailQuery.isLoading ? <Card><CardContent className="pt-6 text-sm text-muted-foreground">Đang tải recovery request...</CardContent></Card> : null}
              {recoveryRequestsQuery.isError ? <p className="text-sm text-red-600">{recoveryRequestsQuery.error.message}</p> : null}
              {!latestRecoveryRequest && !recoveryRequestsQuery.isLoading ? (
                <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-4 text-sm text-slate-600">
                  User này chưa có recovery request nào.
                </div>
              ) : null}
              {latestRecoveryRequest ? (
                <div className="space-y-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <div className="flex flex-wrap items-center gap-3">
                    <StatusBadge status={latestRecoveryRequest.status} />
                    <span className="text-sm text-slate-600">Request ID: {latestRecoveryRequest.id}</span>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Requested at</p>
                      <p className="mt-1 text-sm text-slate-900">{formatDateTime(latestRecoveryRequest.requestedAt)}</p>
                    </div>
                    <div>
                      <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Reviewed at</p>
                      <p className="mt-1 text-sm text-slate-900">
                        {latestRecoveryRequest.reviewedAt ? formatDateTime(latestRecoveryRequest.reviewedAt) : "Chua duyet"}
                      </p>
                    </div>
                  </div>
                  {latestRecoveryRequest.reason ? (
                    <div>
                      <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Ly do user gui</p>
                      <p className="mt-1 text-sm leading-6 text-slate-900">{latestRecoveryRequest.reason}</p>
                    </div>
                  ) : null}
                  {latestRecoveryRequest.decisionNote ? (
                    <div>
                      <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Ghi chu admin</p>
                      <p className="mt-1 text-sm leading-6 text-slate-900">{latestRecoveryRequest.decisionNote}</p>
                    </div>
                  ) : null}
                  {latestRecoveryRequest.recoveryTicket ? (
                    <div className="space-y-2 rounded-2xl border border-slate-200 bg-white p-4">
                      <div>
                        <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Recovery ticket</p>
                        <p className="mt-2 break-all font-mono text-sm text-slate-900">{latestRecoveryRequest.recoveryTicket}</p>
                      </div>
                      <div className="grid gap-3 md:grid-cols-2">
                        <div>
                          <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Issued at</p>
                          <p className="mt-1 text-sm text-slate-900">
                            {latestRecoveryRequest.recoveryTicketIssuedAt ? formatDateTime(latestRecoveryRequest.recoveryTicketIssuedAt) : "Chưa cấp"}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Expires at</p>
                          <p className="mt-1 text-sm text-slate-900">
                            {latestRecoveryRequest.recoveryTicketExpiresAt ? formatDateTime(latestRecoveryRequest.recoveryTicketExpiresAt) : "Chưa có"}
                          </p>
                        </div>
                      </div>
                    </div>
                  ) : null}
                  {latestRecoveryRequest.status === "PENDING" || latestRecoveryRequest.status === "APPROVED" ? (
                    <>
                      <div className="space-y-2">
                        <Label htmlFor="recoveryReviewNote">Ghi chu phe duyet / tu choi</Label>
                        <Textarea
                          id="recoveryReviewNote"
                          value={recoveryReviewNote}
                          onChange={(event) => setRecoveryReviewNote(event.target.value)}
                          placeholder="Ly do phe duyet, huong dan user, hoac ly do tu choi..."
                        />
                      </div>
                      <div className="flex flex-wrap gap-3">
                        <Button
                          variant="secondary"
                          onClick={() => approveRecoveryMutation.mutate(latestRecoveryRequest.id)}
                          disabled={approveRecoveryMutation.isPending}
                        >
                          {approveRecoveryMutation.isPending ? "Dang cap ticket..." : latestRecoveryRequest.status === "APPROVED" ? "Cấp lại recovery ticket" : "Phe duyet va cap ticket"}
                        </Button>
                        <Button
                          variant="outline"
                          onClick={() => rejectRecoveryMutation.mutate(latestRecoveryRequest.id)}
                          disabled={rejectRecoveryMutation.isPending}
                        >
                          {rejectRecoveryMutation.isPending ? "Dang tu choi..." : "Tu choi recovery"}
                        </Button>
                      </div>
                    </>
                  ) : null}
                  {latestRecoveryRequest.status === "COMPLETED" ? (
                    <p className="text-sm text-emerald-700">
                      Recovery da hoan tat. Authenticator moi: {latestRecoveryRequest.completedRegistrationId ?? "Khong co thong tin"}.
                    </p>
                  ) : null}
                </div>
              ) : null}
            </CardContent>
          </Card>
        </>
      ) : null}
    </div>
  );
}
