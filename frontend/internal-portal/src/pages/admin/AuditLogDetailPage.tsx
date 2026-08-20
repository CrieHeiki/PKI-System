import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatDateTime } from "@/lib/utils";
import { auditService } from "@/services/audit-service";

export function AuditLogDetailPage() {
  const { id } = useParams();
  const detailQuery = useQuery({
    queryKey: ["admin", "audit", "detail", id],
    queryFn: () => auditService.get(id!),
    enabled: Boolean(id),
  });

  const event = detailQuery.data;

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Audit detail" title="Chi tiết audit event" description="Xem raw payload và immudbTxId của sự kiện." />
      {detailQuery.isLoading ? <Card><CardContent className="pt-6 text-sm text-muted-foreground">Đang tải audit event...</CardContent></Card> : null}
      {detailQuery.isError ? <Card><CardContent className="pt-6 text-sm text-red-600">{detailQuery.error.message}</CardContent></Card> : null}
      {!event && !detailQuery.isLoading && !detailQuery.isError ? <EmptyState title="Không tìm thấy event" description="Kiểm tra lại eventId." /> : null}
      {event ? (
        <>
          <Card>
            <CardHeader><CardTitle>{event.eventType}</CardTitle></CardHeader>
            <CardContent className="grid gap-4 md:grid-cols-2 text-sm text-muted-foreground">
              <p>ImmuDB Tx ID: {event.immudbTxId || "-"}</p>
              <p>Actor ID: {event.actorId || "-"}</p>
              <p>Actor Role: {event.actorRole || "-"}</p>
              <p>Target ID: {event.targetId || "-"}</p>
              <p>Resource: {event.resourceType || "-"} / {event.resourceId || "-"}</p>
              <p>Correlation: {event.correlationId || "-"}</p>
              <p>Trace ID: {event.traceId || "-"}</p>
              <p>Event Time: {formatDateTime(event.eventTime || event.createdAt)}</p>
              <p className="md:col-span-2">Summary: {event.summary || "-"}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Raw payload</CardTitle></CardHeader>
            <CardContent>
              <pre className="overflow-auto rounded-2xl bg-slate-950 p-4 text-xs text-slate-100">
                {JSON.stringify(event.payload, null, 2)}
              </pre>
            </CardContent>
          </Card>
        </>
      ) : null}
    </div>
  );
}
