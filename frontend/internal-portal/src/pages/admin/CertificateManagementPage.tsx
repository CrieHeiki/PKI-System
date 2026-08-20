import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatDateTime } from "@/lib/utils";
import { certificatesService } from "@/services/certificates-service";

export function CertificateManagementPage() {
  const queryClient = useQueryClient();
  const certificatesQuery = useQuery({ queryKey: ["admin", "certificates"], queryFn: certificatesService.listAll });
  const revokeMutation = useMutation({
    mutationFn: certificatesService.revokeAdmin,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "certificates"] }),
  });

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Certificate management" title="Quản lý chứng thư" description="Danh sách chứng thư đã issue, kèm theo revoke trên certification-service." />
      <Card>
        <CardHeader><CardTitle>All certificates</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {certificatesQuery.isLoading ? <p className="text-sm text-muted-foreground">Đang tải certificate list...</p> : null}
          {certificatesQuery.isError ? <p className="text-sm text-red-600">{certificatesQuery.error.message}</p> : null}
          {certificatesQuery.data?.length === 0 ? <EmptyState title="Không có certificate" description="Hệ thống chưa cấp certificate nào." /> : null}
          {certificatesQuery.data?.map((certificate) => (
            <div key={certificate.id} className="rounded-2xl border border-border p-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div className="space-y-1">
                  <p className="text-lg font-semibold">{certificate.commonName}</p>
                  <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                    <span>Owner: {certificate.accountId}</span>
                    <span>Serial: {certificate.serialNumber}</span>
                    <span>Expires: {formatDateTime(certificate.expiresAt)}</span>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={certificate.status} />
                  <Button variant="outline" onClick={() => revokeMutation.mutate(certificate.id)} disabled={revokeMutation.isPending}>
                    Revoke
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
