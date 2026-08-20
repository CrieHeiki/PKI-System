export interface AuditEvent {
  id: string;
  immudbTxId: number | null;
  eventType: string;
  actorId: string | null;
  actorRole: string | null;
  targetId: string | null;
  resourceType: string | null;
  resourceId: string | null;
  correlationId: string | null;
  traceId: string | null;
  eventTime: string | null;
  summary: string | null;
  payload: unknown;
  createdAt: string;
}
