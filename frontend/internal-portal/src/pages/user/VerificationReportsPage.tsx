import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { describeVerificationCheck } from "@/lib/verification-presenter";
import { formatDateTime } from "@/lib/utils";
import { verificationService } from "@/services/verification-service";

export function VerificationReportsPage() {
  const queryClient = useQueryClient();
  const [reportId, setReportId] = useState("");
  const recentQuery = useQuery({ queryKey: ["verification", "recent"], queryFn: verificationService.listRecent });
  const selectedQuery = useQuery({
    queryKey: ["verification", "report", reportId],
    queryFn: () => verificationService.getReport(reportId),
    enabled: Boolean(reportId),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Xác minh"
        title="Báo cáo xác minh"
        description="Các báo cáo gần đây được lấy từ verification-service. Bạn có thể nạp chi tiết theo mã báo cáo."
        actions={<Button variant="outline" onClick={() => queryClient.invalidateQueries({ queryKey: ["verification"] })}>Làm mới</Button>}
      />

      <Card>
        <CardHeader><CardTitle>Nạp chi tiết theo mã báo cáo</CardTitle></CardHeader>
        <CardContent className="flex flex-col gap-3 md:flex-row">
          <div className="flex-1 space-y-2">
            <Label htmlFor="reportId">Mã báo cáo</Label>
            <Input id="reportId" value={reportId} onChange={(event) => setReportId(event.target.value)} />
          </div>
        </CardContent>
      </Card>

      {selectedQuery.data ? (
        <Card>
          <CardHeader><CardTitle>Chi tiết báo cáo đang chọn</CardTitle></CardHeader>
          <CardContent className="space-y-4 text-sm text-muted-foreground">
            <div className="flex items-center gap-3">
              <StatusBadge status={selectedQuery.data.verificationStatus} />
              <span>{formatDateTime(selectedQuery.data.verifiedAt)}</span>
            </div>
            <p>Yêu cầu ký: {selectedQuery.data.signingRequestId || "-"}</p>
            <p>Tệp gốc: {selectedQuery.data.originalFileName || "-"}</p>
            <p>Thông điệp tổng quan: {selectedQuery.data.overallMessage || "-"}</p>
            <div className="space-y-3">
              {selectedQuery.data.checks.map((check) => {
                const presentation = describeVerificationCheck(check);
                return (
                  <div key={check.checkName} className="rounded-2xl border border-border bg-white p-4">
                    <div className="flex items-center gap-3">
                      <StatusBadge status={check.result ? "VALID" : "INVALID"} />
                      <p className="font-medium text-slate-900">{presentation.title}</p>
                    </div>
                    <p className="mt-2 text-slate-700">{presentation.summary}</p>
                    {presentation.technicalDetail ? (
                      <p className="mt-2 text-muted-foreground">Chi tiết kỹ thuật: {presentation.technicalDetail}</p>
                    ) : null}
                    <div className="mt-3 rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 p-3 text-slate-700">
                      <p className="font-medium text-slate-900">Thông tin debug</p>
                      <div className="mt-2 space-y-2">
                        <p><span className="font-medium">Điều kiện pass:</span> {presentation.passCondition}</p>
                        <p><span className="font-medium">Điều kiện fail:</span> {presentation.failCondition}</p>
                        <p><span className="font-medium">Dịch vụ quyết định:</span> {presentation.decidingService}</p>
                        <p className="text-xs text-slate-500"><span className="font-medium">Khóa kỹ thuật:</span> {presentation.technicalKey}</p>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>
      ) : null}

      <Card>
        <CardHeader><CardTitle>Báo cáo gần đây</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {recentQuery.isLoading ? <p className="text-sm text-muted-foreground">Đang tải danh sách báo cáo...</p> : null}
          {recentQuery.isError ? <p className="text-sm text-red-600">{recentQuery.error.message}</p> : null}
          {recentQuery.data?.length === 0 ? <EmptyState title="Chưa có báo cáo" description="Tạo báo cáo xác minh ở trang kiểm tra tệp để xem dữ liệu tại đây." /> : null}
          {recentQuery.data?.map((report) => (
            <button
              key={report.id}
              className="w-full rounded-2xl border border-border p-4 text-left transition hover:border-primary/40 hover:bg-muted/40"
              onClick={() => setReportId(report.id)}
              type="button"
            >
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div className="space-y-2">
                  <p className="font-semibold">{report.originalFileName || report.signedFileName || report.id}</p>
                  <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                    <span>Yêu cầu: {report.signingRequestId || "-"}</span>
                    <span>Xác minh lúc: {formatDateTime(report.verifiedAt)}</span>
                  </div>
                </div>
                <StatusBadge status={report.verificationStatus} />
              </div>
            </button>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
