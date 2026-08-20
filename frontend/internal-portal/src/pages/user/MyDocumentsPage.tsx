import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatDateTime } from "@/lib/utils";
import { documentsService } from "@/services/documents-service";

export function MyDocumentsPage() {
  const documentsQuery = useQuery({
    queryKey: ["documents", "mine"],
    queryFn: documentsService.listMine,
  });

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow="Tài liệu"
        title="Danh sách tài liệu của tôi"
        description="Đây là dữ liệu thật từ document-service. Mỗi tài liệu có thể được mở chi tiết, tải bản hiện tại hoặc cập nhật phiên bản mới."
        actions={
          <Button asChild>
            <Link to="/app/documents/upload">Tải lên tài liệu</Link>
          </Button>
        }
      />

      <Card>
        <CardHeader><CardTitle className="text-[1.35rem]">Tất cả tài liệu</CardTitle></CardHeader>
        <CardContent>
          {documentsQuery.isLoading ? <p className="text-sm text-muted-foreground">Đang tải danh sách tài liệu...</p> : null}
          {documentsQuery.isError ? <p className="text-sm text-red-600">{documentsQuery.error.message}</p> : null}
          {documentsQuery.data?.length === 0 ? (
            <EmptyState title="Chưa có tài liệu" description="Bắt đầu bằng cách tải lên tài liệu đầu tiên của bạn." />
          ) : null}
          <div className="space-y-4">
            {documentsQuery.data?.map((document) => (
              <div key={document.id} className="rounded-[20px] border border-slate-200 bg-slate-50/70 p-5">
                <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                  <div className="space-y-2">
                    <Link className="text-lg font-semibold text-foreground hover:text-primary" to={`/app/documents/${document.id}`}>
                      {document.title}
                    </Link>
                    <p className="text-sm text-muted-foreground">{document.description || "Không có mô tả."}</p>
                    <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                      <span>Phiên bản hiện tại: {document.currentVersion}</span>
                      <span>Tạo lúc: {formatDateTime(document.createdAt)}</span>
                      <span>Cập nhật: {formatDateTime(document.updatedAt)}</span>
                    </div>
                  </div>
                  <div className="flex flex-col items-start gap-3 md:items-end">
                    <StatusBadge status={document.status} />
                    <Button asChild size="sm" variant="secondary">
                      <Link to={`/app/documents/${document.id}`}>Xem chi tiết</Link>
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
