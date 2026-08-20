import { http } from "@/services/http-client";
import type { AuditEvent } from "@/types/audit";
import type { ApiEnvelope } from "@/types/common";

async function unwrap<T>(promise: Promise<ApiEnvelope<T>>) {
  const payload = await promise;
  return payload.data;
}

export const auditService = {
  search(params?: {
    eventType?: string;
    actorId?: string;
    resourceType?: string;
    resourceId?: string;
    correlationId?: string;
    from?: string;
    to?: string;
    limit?: number;
  }) {
    const query = new URLSearchParams();
    if (params?.eventType) query.set("eventType", params.eventType);
    if (params?.actorId) query.set("actorId", params.actorId);
    if (params?.resourceType) query.set("resourceType", params.resourceType);
    if (params?.resourceId) query.set("resourceId", params.resourceId);
    if (params?.correlationId) query.set("correlationId", params.correlationId);
    if (params?.from) query.set("from", params.from);
    if (params?.to) query.set("to", params.to);
    if (params?.limit) query.set("limit", String(params.limit));
    const suffix = query.toString() ? `?${query.toString()}` : "";
    return unwrap(http<ApiEnvelope<AuditEvent[]>>(`/api/v1/admin/audit/events${suffix}`));
  },

  get(eventId: string) {
    return unwrap(http<ApiEnvelope<AuditEvent>>(`/api/v1/admin/audit/events/${eventId}`));
  },
};
