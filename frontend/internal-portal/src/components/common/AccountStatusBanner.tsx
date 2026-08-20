import { AlertTriangle, CheckCircle2, Lock, ShieldX } from "lucide-react";
import type { ReactNode } from "react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatusBadge } from "@/components/common/StatusBadge";
import type { AccountStatus } from "@/types/auth";

const iconMap = {
  ACTIVE: CheckCircle2,
  PENDING: AlertTriangle,
  LOCKED: Lock,
  DISABLED: ShieldX,
} as const;

export function AccountStatusBanner({
  status,
  title,
  description,
  actions,
}: {
  status: AccountStatus;
  title: string;
  description: string;
  actions?: ReactNode;
}) {
  const Icon = iconMap[status];
  return (
    <Card>
      <CardHeader className="flex flex-row items-center gap-4">
        <div className="rounded-2xl bg-slate-100 p-3">
          <Icon className="h-6 w-6 text-foreground" />
        </div>
        <div className="space-y-2">
          <StatusBadge status={status} />
          <CardTitle>{title}</CardTitle>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm leading-7 text-muted-foreground">{description}</p>
        {actions ? <div className="mt-5 flex flex-wrap gap-3">{actions}</div> : null}
      </CardContent>
    </Card>
  );
}
