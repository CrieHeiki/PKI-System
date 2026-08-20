import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { formatDateTime } from "@/lib/utils";
import { certificatesService } from "@/services/certificates-service";

export function CertificateRequestManagementPage() {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState("");
  const [rejectingId, setRejectingId] = useState("");
  const [rejectReason, setRejectReason] = useState("");

  const requestsQuery = useQuery({
    queryKey: ["admin", "certificate-requests", "ISSUE", status],
    queryFn: () => certificatesService.listAdminRequests({ requestType: "ISSUE", status: status || undefined }),
  });

  const approveMutation = useMutation({
    mutationFn: certificatesService.approveRequest,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "certificate-requests"] }),
  });
  const rejectMutation = useMutation({
    mutationFn: ({ requestId, reason }: { requestId: string; reason: string }) => certificatesService.rejectRequest(requestId, reason),
    onSuccess: async () => {
      setRejectingId("");
      setRejectReason("");
      await queryClient.invalidateQueries({ queryKey: ["admin", "certificate-requests"] });
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Certificate issuing" title="Duyệt yêu cầu cấu chứng thư" description="Danh sách này đang gọi certification-service với requestType=ISSUE." />
      <Card>
        <CardHeader><CardTitle>Lọc theo status</CardTitle></CardHeader>
        <CardContent>
          <div className="space-y-2 md:max-w-xs">
            <Label htmlFor="status">Status</Label>
            <select id="status" className="h-11 w-full rounded-2xl border border-input bg-white px-4 text-sm" value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="">Tất cả</option>
              <option value="PENDING">PENDING</option>
              <option value="ISSUED">ISSUED</option>
              <option value="REJECTED">REJECTED</option>
            </select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Issue requests</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {requestsQuery.isLoading ? <p className="text-sm text-muted-foreground">Đang tải certificate requests...</p> : null}
          {requestsQuery.isError ? <p className="text-sm text-red-600">{requestsQuery.error.message}</p> : null}
          {requestsQuery.data?.length === 0 ? <EmptyState title="Không có request" description="Không có issue request nào phù hợp bộ lọc hiện tại." /> : null}
          {requestsQuery.data?.map((request) => (
            <div key={request.id} className="rounded-2xl border border-border p-4">
              <div className="space-y-3">
                <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                  <div className="space-y-1">
                    <p className="text-lg font-semibold">{request.commonName}</p>
                    <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                      <span>Account: {request.accountId}</span>
                      <span>TTL: {request.ttl || "-"}</span>
                      <span>Requested: {formatDateTime(request.requestedAt)}</span>
                    </div>
                  </div>
                  <StatusBadge status={request.status} />
                </div>
                {request.status === "PENDING" ? (
                  <div className="space-y-3 rounded-2xl border border-border/70 p-4">
                    <div className="flex flex-wrap gap-3">
                      <Button onClick={() => approveMutation.mutate(request.id)} disabled={approveMutation.isPending}>Approve</Button>
                      <Button variant="outline" onClick={() => setRejectingId(rejectingId === request.id ? "" : request.id)}>Reject</Button>
                    </div>
                    {rejectingId === request.id ? (
                      <form
                        className="space-y-3"
                        onSubmit={(event) => {
                          event.preventDefault();
                          rejectMutation.mutate({ requestId: request.id, reason: rejectReason });
                        }}
                      >
                        <Textarea value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} placeholder="Nhập lý do từ chối" />
                        <Button type="submit" variant="outline" disabled={!rejectReason.trim() || rejectMutation.isPending}>Xác nhận từ chối</Button>
                      </form>
                    ) : null}
                  </div>
                ) : null}
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
