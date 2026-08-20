import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

export function PageSection({
  title,
  subtitle,
  children,
  className,
  contentClassName,
}: {
  title: string;
  subtitle?: string;
  children: ReactNode;
  className?: string;
  contentClassName?: string;
}) {
  return (
    <section className={cn("overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm", className)}>
      <div className="border-b border-slate-100 px-6 py-4 sm:px-7">
        <h2 className="text-xl font-semibold tracking-tight text-slate-900">{title}</h2>
        {subtitle ? <p className="mt-1 text-sm leading-6 text-slate-500">{subtitle}</p> : null}
      </div>
      <div className={cn("p-6 sm:p-7", contentClassName)}>{children}</div>
    </section>
  );
}
