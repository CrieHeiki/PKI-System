import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatDateTime } from "@/lib/utils";
import { certificatesService } from "@/services/certificates-service";

export function MyCertificatePage() {
  const certificatesQuery = useQuery({ queryKey: ["certificates", "mine"], queryFn: certificatesService.listMine });
  const requestsQuery = useQuery({ queryKey: ["certificate-requests", "mine"], queryFn: certificatesService.listMyRequests });

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Chứng thư"
        title="Chứng thư và yêu cầu của tôi"
        description="Trang này kết nối trực tiếp với certification-service để hiển thị chứng thư, yêu cầu cấp mới, gia hạn và thu hồi."
        actions={
          <>
            <Button asChild variant="outline">
              <Link to="/app/certificate/request">Yêu cầu cấp chứng thư</Link>
            </Button>
            <Button asChild>
              <Link to="/app/certificate/renew">Yêu cầu gia hạn</Link>
            </Button>
            <Button asChild variant="outline">
              <Link to="/app/certificate/revoke">Yêu cầu thu hồi</Link>
            </Button>
          </>
        }
      />

      <Card>
        <CardHeader><CardTitle>Chứng thư của tôi</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {certificatesQuery.isLoading ? <p className="text-sm text-muted-foreground">Đang tải chứng thư...</p> : null}
          {certificatesQuery.isError ? <p className="text-sm text-red-600">{certificatesQuery.error.message}</p> : null}
          {certificatesQuery.data?.length === 0 ? <EmptyState title="Chưa có chứng thư" description="Gửi yêu cầu cấp chứng thư để bắt đầu." /> : null}
          {certificatesQuery.data?.map((certificate) => (
            <div key={certificate.id} className="rounded-2xl border border-border p-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div className="space-y-2">
                  <p className="text-lg font-semibold">{certificate.commonName}</p>
                  <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                    <span>Serial: {certificate.serialNumber}</span>
                    <span>Đã cấp: {formatDateTime(certificate.issuedAt)}</span>
                    <span>Hết hạn: {formatDateTime(certificate.expiresAt)}</span>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={certificate.status} />
                  {certificate.status !== "REVOKED" ? (
                    <Button asChild size="sm" variant="outline">
                      <Link to={`/app/certificate/revoke?certificateId=${certificate.id}`}>Gửi yêu cầu thu hồi</Link>
                    </Button>
                  ) : null}
                </div>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Lịch sử yêu cầu</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {requestsQuery.isLoading ? <p className="text-sm text-muted-foreground">Đang tải lịch sử yêu cầu...</p> : null}
          {requestsQuery.isError ? <p className="text-sm text-red-600">{requestsQuery.error.message}</p> : null}
          {requestsQuery.data?.length === 0 ? <EmptyState title="Chưa có yêu cầu" description="Yêu cầu cấp mới, gia hạn và thu hồi chứng thư sẽ hiển thị tại đây." /> : null}
          {requestsQuery.data?.map((request) => (
            <div key={request.id} className="rounded-2xl border border-border p-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div className="space-y-2">
                  <p className="font-semibold">{request.requestType} - {request.commonName}</p>
                  <div className="space-y-1 text-sm text-muted-foreground">
                    <p>Gửi lúc: {formatDateTime(request.requestedAt)}</p>
                    {request.sourceCertificateId ? <p>Chứng thư nguồn: {request.sourceCertificateId}</p> : null}
                    {request.renewalReason ? <p>Lý do gia hạn: {request.renewalReason}</p> : null}
                    {request.revocationReason ? <p>Lý do thu hồi: {request.revocationReason}</p> : null}
                    {request.rejectionReason ? <p>Lý do từ chối: {request.rejectionReason}</p> : null}
                  </div>
                </div>
                <StatusBadge status={request.status} />
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
