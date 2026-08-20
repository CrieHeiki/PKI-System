import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatDateTime } from "@/lib/utils";
import { certificatesService } from "@/services/certificates-service";
import { signingService } from "@/services/signing-service";

export function RequestsToSignPage() {
  const query = useQuery({ queryKey: ["signing", "to-sign"], queryFn: signingService.listPendingForMe });
  const certificatesQuery = useQuery({ queryKey: ["certificates", "mine"], queryFn: certificatesService.listMine });
  const activeCertificates = (certificatesQuery.data ?? []).filter((certificate) => certificate.status === "ACTIVE");

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Quy trình ký"
        title="Yêu cầu cần tôi xử lý"
        description="Danh sách này giữ cả yêu cầu đang chờ duyệt và yêu cầu đã được duyệt nhưng chưa ký xong, để người ký có thể quay lại tiếp tục thực hiện ký."
      />
      <Card className="bg-[linear-gradient(180deg,rgba(255,252,247,0.96),rgba(249,244,236,0.92))]">
        <CardContent className="flex flex-col gap-3 pt-6 text-sm text-slate-600 md:flex-row md:items-center md:justify-between">
          <div className="space-y-1">
            <p className="font-medium text-slate-900">Người ký có thể quay lại yêu cầu đã duyệt để tiếp tục ký số.</p>
            <p>
              Yêu cầu `PENDING` được xử lý duyệt hoặc từ chối trong trang chi tiết. Yêu cầu `APPROVED` vẫn nằm tại đây cho đến khi ký xong.
            </p>
          </div>
          <div className="rounded-2xl border border-slate-200/80 bg-white/80 px-4 py-3 text-sm">
            Chứng thư đang hoạt động: <span className="font-semibold text-slate-900">{activeCertificates.length}</span>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Hàng đợi xử lý</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {query.isLoading ? <p className="text-sm text-muted-foreground">Đang tải danh sách chờ xử lý...</p> : null}
          {query.isError ? <p className="text-sm text-red-600">{query.error.message}</p> : null}
          {query.data?.length === 0 ? <EmptyState title="Không có yêu cầu cần xử lý" description="Khi được giao ký hoặc cần tiếp tục ký số, yêu cầu sẽ xuất hiện tại đây." /> : null}
          {query.data?.map((item) => (
            <div key={item.id} className="rounded-2xl border border-border p-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div className="space-y-2">
                  <Link className="text-lg font-semibold text-slate-950 hover:text-primary" to={`/app/sign-requests/${item.id}`}>
                    Yêu cầu {item.id}
                  </Link>
                  <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                    <span>Người yêu cầu: {item.requesterAccountId}</span>
                    <span>Tài liệu: {item.documentId}</span>
                    <span>Gửi lúc: {formatDateTime(item.requestedAt)}</span>
                    {item.status === "APPROVED" ? <span>Sẵn sàng thực hiện ký số</span> : null}
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-3">
                  <StatusBadge status={item.status} />
                  <Button asChild size="sm">
                    <Link to={`/app/sign-requests/${item.id}`}>{item.status === "APPROVED" ? "Tiếp tục ký số" : "Mở trang duyệt"}</Link>
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
