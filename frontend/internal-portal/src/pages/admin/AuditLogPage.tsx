import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { formatDateTime } from "@/lib/utils";
import { auditService } from "@/services/audit-service";

const AUDIT_EVENT_TYPE_PRESETS = [
  "ACCOUNT_REGISTERED",
  "ACCOUNT_LOGGED_IN",
  "ACCOUNT_ACTIVATED",
  "ACCOUNT_LOCKED",
  "ACCOUNT_UNLOCKED",
  "ACCOUNT_DISABLED",
  "ACCOUNT_RESTORED",
  "ACCOUNT_ROLE_CHANGED",
  "ACCOUNT_SIGNATURE_IMAGE_UPDATED",
  "ACCOUNT_SIGNATURE_APPEARANCE_UPDATED",
  "ACCOUNT_SIGNATURE_IMAGE_CLEARED",
  "DOCUMENT_CREATED",
  "DOCUMENT_DOWNLOADED",
  "DOCUMENT_VERSION_UPLOADED",
  "SIGNING_REQUEST_CREATED",
  "SIGNING_REQUEST_APPROVED",
  "SIGNING_REQUEST_REJECTED",
  "SIGNING_REQUEST_CANCELLED",
  "SIGNING_REQUEST_EXECUTED",
  "SIGNED_OUTPUT_DOWNLOADED",
  "SIGNING_SOURCE_DOCUMENT_PREVIEWED",
  "CERTIFICATE_REQUEST_CREATED",
  "CERTIFICATE_RENEWAL_REQUEST_CREATED",
  "CERTIFICATE_REQUEST_APPROVED",
  "CERTIFICATE_RENEWAL_APPROVED",
  "CERTIFICATE_REQUEST_REJECTED",
  "CERTIFICATE_REVOKED",
  "WEBAUTHN_FIRST_ENROLLMENT_TICKET_ISSUED",
  "WEBAUTHN_FIRST_ENROLLMENT_AUTHORIZED",
  "WEBAUTHN_REGISTRATION_STARTED",
  "WEBAUTHN_REGISTRATION_AUTHORIZED",
  "WEBAUTHN_REGISTRATION_COMPLETED",
  "WEBAUTHN_ASSERTION_STARTED",
  "WEBAUTHN_ASSERTION_VERIFIED",
  "MFA_PRIMARY_METHOD_UPDATED",
  "MFA_REGISTRATION_DISABLED",
  "STEP_UP_CHALLENGE",
  "STEP_UP_VERIFICATION_CONSUMED",
  "STEP_UP_MFA_REGISTRATION",
  "STEP_UP_WEBAUTHN",
  "DOCUMENT_VERIFIED",
  "SMOKE_TEST",
];

const AUDIT_RESOURCE_TYPE_PRESETS = [
  "ACCOUNT",
  "DOCUMENT",
  "SIGNING_REQUEST",
  "CERTIFICATE",
  "CERTIFICATE_REQUEST",
  "VERIFICATION_REPORT",
  "STEP_UP_VERIFICATION",
  "MFA_REGISTRATION",
];

function uniqueSorted(values: Array<string | null | undefined>) {
  return Array.from(
    new Set(
      values
        .map((value) => value?.trim())
        .filter((value): value is string => Boolean(value)),
    ),
  ).sort((left, right) => left.localeCompare(right));
}

export function AuditLogPage() {
  const [eventType, setEventType] = useState("");
  const [resourceType, setResourceType] = useState("");
  const [actorId, setActorId] = useState("");
  const [resourceId, setResourceId] = useState("");

  const auditFacetQuery = useQuery({
    queryKey: ["admin", "audit", "facets"],
    queryFn: () => auditService.search({ limit: 200 }),
  });

  const auditQuery = useQuery({
    queryKey: ["admin", "audit", { eventType, resourceType, actorId, resourceId }],
    queryFn: () =>
      auditService.search({
        eventType: eventType || undefined,
        resourceType: resourceType || undefined,
        actorId: actorId || undefined,
        resourceId: resourceId || undefined,
        limit: 50,
      }),
  });

  const availableEventTypes = uniqueSorted([
    ...AUDIT_EVENT_TYPE_PRESETS,
    ...(auditFacetQuery.data?.map((event) => event.eventType) ?? []),
  ]);
  const availableResourceTypes = uniqueSorted([
    ...AUDIT_RESOURCE_TYPE_PRESETS,
    ...(auditFacetQuery.data?.map((event) => event.resourceType) ?? []),
  ]);
  const availableActorIds = uniqueSorted(auditFacetQuery.data?.map((event) => event.actorId) ?? []);
  const availableResourceIds = uniqueSorted(auditFacetQuery.data?.map((event) => event.resourceId) ?? []);

  const resetFilters = () => {
    setEventType("");
    setResourceType("");
    setActorId("");
    setResourceId("");
  };

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Audit" title="Nhật ký hệ thống" />
      <Card>
        <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle>Bộ lọc</CardTitle>
            <p className="mt-1 text-sm text-muted-foreground">
              Chọn từ các giá trị có sẵn. Danh sách actor/resource được lấy từ 200 event gần nhất.
            </p>
          </div>
          <Button variant="outline" onClick={resetFilters}>
            Đặt lại bộ lọc
          </Button>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div className="space-y-2">
            <Label htmlFor="eventType">Event type</Label>
            <Select id="eventType" value={eventType} onChange={(event) => setEventType(event.target.value)} disabled={auditFacetQuery.isLoading}>
              <option value="">Tất cả event type</option>
              {availableEventTypes.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </Select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="resourceType">Resource type</Label>
            <Select id="resourceType" value={resourceType} onChange={(event) => setResourceType(event.target.value)} disabled={auditFacetQuery.isLoading}>
              <option value="">Tất cả resource type</option>
              {availableResourceTypes.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </Select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="actorId">Actor ID</Label>
            <Select id="actorId" value={actorId} onChange={(event) => setActorId(event.target.value)} disabled={auditFacetQuery.isLoading}>
              <option value="">Tất cả actor</option>
              {availableActorIds.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </Select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="resourceId">Resource ID</Label>
            <Select id="resourceId" value={resourceId} onChange={(event) => setResourceId(event.target.value)} disabled={auditFacetQuery.isLoading}>
              <option value="">Tất cả resource</option>
              {availableResourceIds.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </Select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Danh sach event</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {auditQuery.isLoading ? <p className="text-sm text-muted-foreground">Đang tải audit events...</p> : null}
          {auditQuery.isError ? <p className="text-sm text-red-600">{auditQuery.error.message}</p> : null}
          {auditQuery.data?.length === 0 ? <EmptyState title="Không có audit event" description="Không có sự kiện nào khớp bộ lọc hiện tại." /> : null}
          {auditQuery.data?.map((event) => (
            <div key={event.id} className="rounded-2xl border border-border p-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <div className="space-y-1">
                  <Link className="text-lg font-semibold hover:text-primary" to={`/admin/audit/${event.id}`}>{event.eventType}</Link>
                  <div className="flex flex-wrap gap-3 text-sm text-muted-foreground">
                    <span>Resource: {event.resourceType || "-"} / {event.resourceId || "-"}</span>
                    <span>Actor: {event.actorId || "-"}</span>
                    <span>At: {formatDateTime(event.eventTime || event.createdAt)}</span>
                  </div>
                  <p className="text-sm text-muted-foreground">{event.summary || "Không có summary."}</p>
                </div>
                <Button asChild variant="outline" size="sm">
                  <Link to={`/admin/audit/${event.id}`}>Chi tiết</Link>
                </Button>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
