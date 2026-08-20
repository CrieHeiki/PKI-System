import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatDateTime } from "@/lib/utils";
import { signingService } from "@/services/signing-service";

export function MyCreatedSignRequestsPage() {
  const query = useQuery({ queryKey: ["signing", "mine"], queryFn: signingService.listMine });

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Workflow ky"
        title="Yêu cầu tôi đã tạo"
        description="Danh sach request thuc te tu signing-workflow-service."
        actions={
          <Button asChild>
            <Link to="/app/sign-requests/create">Tạo yêu cầu mới</Link>
          </Button>
        }
      />
      <Card>
        <CardHeader><CardTitle>Danh sách</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {query.isLoading ? <p className="text-sm text-muted-foreground">Đang tải dữ liệu...</p> : null}
          {query.isError ? <p className="text-sm text-red-600">{query.error.message}</p> : null}
          {query.data?.length === 0 ? <EmptyState title="Chưa có request nào" description="Tạo request ký mới từ tài liệu của bạn." /> : null}
          {query.data?.map((item) => (
            <div key={item.id} className="rounded-2xl border border-border p-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div className="space-y-2">
                  <Link className="text-lg font-semibold hover:text-primary" to={`/app/sign-requests/${item.id}`}>
                    Request {item.id}
                  </Link>
                  <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                    <span>Document: {item.documentId}</span>
                    <span>Signer: {item.signerAccountId}</span>
                    <span>Requested: {formatDateTime(item.requestedAt)}</span>
                  </div>
                </div>
                <StatusBadge status={item.status} />
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
