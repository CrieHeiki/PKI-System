import { useMemo } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatDateTime } from "@/lib/utils";
import { accountsService } from "@/services/accounts-service";
import { documentsService } from "@/services/documents-service";
import { signingService } from "@/services/signing-service";

function formatAccountLabel(account?: { username: string; email: string } | null) {
  if (!account) {
    return "Kh\u00f4ng r\u00f5 ng\u01b0\u1eddi d\u00f9ng";
  }
  return `${account.username} (${account.email})`;
}

async function triggerDownload(fileName: string, blob: Blob) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  window.URL.revokeObjectURL(url);
}

export function AdminDocumentDetailPage() {
  const { id } = useParams();

  const detailQuery = useQuery({
    queryKey: ["admin", "documents", "detail", id],
    queryFn: () => documentsService.get(id!),
    enabled: Boolean(id),
  });

  const accountsQuery = useQuery({
    queryKey: ["admin", "accounts", "documents-detail"],
    queryFn: () => accountsService.list(),
  });

  const signingRequestsQuery = useQuery({
    queryKey: ["admin", "documents", "detail", id, "signing-requests"],
    queryFn: async () => {
      const requests = await signingService.listAllAdmin();
      return requests.filter((request) => request.documentId === id);
    },
    enabled: Boolean(id),
  });

  const accountMap = useMemo(() => {
    return new Map((accountsQuery.data ?? []).map((account) => [account.id, account] as const));
  }, [accountsQuery.data]);

  const owner = detailQuery.data ? accountMap.get(detailQuery.data.ownerAccountId) : null;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Document detail"
        title="Chi tiết tài liệu"
        description="Tài liệu upload sẽ hiện người upload. ếu tài liệu đã được ký, màn này sẽ hiện người yêu cầu và người ký."
      />

      {detailQuery.isLoading ? <Card><CardContent className="pt-6 text-sm text-muted-foreground">Dang tai chi tiet tai lieu...</CardContent></Card> : null}
      {detailQuery.isError ? <Card><CardContent className="pt-6 text-sm text-red-600">{detailQuery.error.message}</CardContent></Card> : null}
      {!detailQuery.data && !detailQuery.isLoading && !detailQuery.isError ? (
        <EmptyState title="Không tìm thấy tài liệu" description="Kiểm tra lại documentId hoặc quay về danh sách tài liệu." />
      ) : null}

      {detailQuery.data ? (
        <>
          <Card>
            <CardHeader>
              <CardTitle>{detailQuery.data.title}</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Người upload</p>
                <p className="mt-1 text-sm">{formatAccountLabel(owner)}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Trạng thái</p>
                <div className="mt-2"><StatusBadge status={detailQuery.data.status} /></div>
              </div>
              <div>
                <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Version hiện tại</p>
                <p className="mt-1 text-sm">{detailQuery.data.currentVersion}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Created at</p>
                <p className="mt-1 text-sm">{formatDateTime(detailQuery.data.createdAt)}</p>
              </div>
              <div className="md:col-span-2">
                <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Mô tả</p>
                <p className="mt-1 text-sm">{detailQuery.data.description || "Khoogn có mô tả."}</p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Version hiện tại</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <p>Tên file: {detailQuery.data.currentVersionDetail.fileName}</p>
              <p>Content type: {detailQuery.data.currentVersionDetail.contentType}</p>
              <p>Dung lượng: {detailQuery.data.currentVersionDetail.fileSize} bytes</p>
              <p>SHA-256: <span className="break-all font-mono">{detailQuery.data.currentVersionDetail.sha256Hash}</span></p>
              <p>Created at: {formatDateTime(detailQuery.data.currentVersionDetail.createdAt)}</p>
              <Button
                variant="outline"
                onClick={async () => {
                  if (!id) return;
                  const { fileName, blob } = await documentsService.download(id);
                  await triggerDownload(fileName, blob);
                }}
              >
                Tải về tài liệu hiện tại
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Các yêu cầu ký liên quan</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {signingRequestsQuery.isLoading ? <p className="text-sm text-muted-foreground">Đang tải yêu cầu ký...</p> : null}
              {signingRequestsQuery.isError ? <p className="text-sm text-red-600">{signingRequestsQuery.error.message}</p> : null}

              {signingRequestsQuery.data?.length === 0 ? (
                <EmptyState title="Chưa có yêu cầu ký" description="Tài liệu này chưa được đưa vào signing workflow nào." />
              ) : null}

              {signingRequestsQuery.data?.map((request) => {
                const requester = accountMap.get(request.requesterAccountId);
                const signer = accountMap.get(request.signerAccountId);
                const hasSignedOutput = Boolean(request.artifact?.outputObjectKey);

                return (
                  <div key={request.id} className="rounded-2xl border border-border p-4">
                    <div className="space-y-3">
                      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                        <div className="space-y-1">
                          <p className="text-base font-semibold">Signing request {request.id}</p>
                          <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                            <span>Người yêu cầu: {formatAccountLabel(requester)}</span>
                            <span>Người ký: {formatAccountLabel(signer)}</span>
                            <span>Requested: {formatDateTime(request.requestedAt)}</span>
                          </div>
                        </div>
                        <StatusBadge status={request.status} />
                      </div>

                      {request.note ? <p className="text-sm text-muted-foreground">Ghi chú: {request.note}</p> : null}

                      <div className="grid gap-3 md:grid-cols-2 text-sm text-muted-foreground">
                        <p>Certificate: {request.certificateId || "Chưa chọn"}</p>
                        <p>Signed at: {request.signedAt ? formatDateTime(request.signedAt) : "Chưa ký"}</p>
                      </div>

                      {hasSignedOutput ? (
                        <div className="rounded-2xl border border-border/70 p-4">
                          <div className="grid gap-2 text-sm text-muted-foreground">
                            <p>Signed file: {request.artifact?.outputFileName || request.artifact?.fileName || "Không rõ tên file"}</p>
                            <p>Content type: {request.artifact?.outputContentType || request.artifact?.contentType || "-"}</p>
                            <p>Digest / Signature: {request.artifact?.digestAlgorithm} / {request.artifact?.signatureAlgorithm}</p>
                          </div>
                          <div className="mt-3 flex flex-wrap gap-3">
                            <Button
                              variant="outline"
                              onClick={async () => {
                                const { fileName, blob } = await signingService.downloadSignedOutput(request.id);
                                await triggerDownload(fileName, blob);
                              }}
                            >
                              Tải về tài liệu đã ký
                            </Button>
                            <Button
                              variant="outline"
                              onClick={async () => {
                                const { fileName, blob } = await signingService.downloadSourceDocument(request.id);
                                await triggerDownload(fileName, blob);
                              }}
                            >
                              Tải về tài liệu nguồn
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-muted-foreground">Request này chưa tạo signed output.</p>
                      )}
                    </div>
                  </div>
                );
              })}
            </CardContent>
          </Card>
        </>
      ) : null}
    </div>
  );
}
