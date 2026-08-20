import { useMemo } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { accountsService } from "@/services/accounts-service";
import { documentsService } from "@/services/documents-service";
import { formatDateTime } from "@/lib/utils";

function formatAccountLabel(account?: { username: string; email: string } | null) {
  if (!account) {
    return "Khong ro nguoi upload";
  }
  return `${account.username} (${account.email})`;
}

export function AdminDocumentsPage() {
  const documentsQuery = useQuery({
    queryKey: ["admin", "documents"],
    queryFn: documentsService.listAll,
  });

  const accountsQuery = useQuery({
    queryKey: ["admin", "accounts", "documents-directory"],
    queryFn: () => accountsService.list(),
  });

  const accountMap = useMemo(() => {
    return new Map((accountsQuery.data ?? []).map((account) => [account.id, account] as const));
  }, [accountsQuery.data]);

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Document management"
        title="Tài liệu đã được tải lên"
        // description="Admin có thể xem ai đã upload tài liệu và mở chi tiết để theo dõi các signed output liên quan."
      />

      <Card>
        <CardHeader>
          <CardTitle>Danh sách tài liệu</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {documentsQuery.isLoading ? <p className="text-sm text-muted-foreground">Đang tải danh sách tài liệu...</p> : null}
          {documentsQuery.isError ? <p className="text-sm text-red-600">{documentsQuery.error.message}</p> : null}
          {accountsQuery.isError ? <p className="text-sm text-red-600">{accountsQuery.error.message}</p> : null}

          {documentsQuery.data?.length === 0 ? (
            <EmptyState
              title="Chưa có tài liệu nào"
              description="Khi người dùng upload tài liệu, metadata sẽ hiện ở đây để admin theo dõi."
            />
          ) : null}

          {documentsQuery.data?.map((document) => {
            const owner = accountMap.get(document.ownerAccountId);

            return (
              <div key={document.id} className="rounded-2xl border border-border p-4">
                <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                  <div className="space-y-1">
                    <Link
                      className="text-lg font-semibold text-foreground transition hover:text-primary"
                      to={`/admin/documents/${document.id}`}
                    >
                      {document.title}
                    </Link>
                    <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                      <span>Uploader: {formatAccountLabel(owner)}</span>
                      <span>Version hien tai: {document.currentVersion}</span>
                      <span>Uploaded: {formatDateTime(document.createdAt)}</span>
                    </div>
                    {document.description ? <p className="pt-1 text-sm text-muted-foreground">{document.description}</p> : null}
                  </div>

                  <Link className="text-sm font-medium text-primary hover:underline" to={`/admin/documents/${document.id}`}>
                    Xem chi tiết
                  </Link>
                </div>
              </div>
            );
          })}
        </CardContent>
      </Card>
    </div>
  );
}
