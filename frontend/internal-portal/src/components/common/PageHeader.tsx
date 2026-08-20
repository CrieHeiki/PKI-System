import type { ReactNode } from "react";

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="rounded-3xl border border-slate-200 bg-white px-6 py-5 shadow-sm sm:px-7 sm:py-6 lg:flex lg:items-start lg:justify-between lg:gap-6">
      <div className="space-y-3">
        {eyebrow ? <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">{eyebrow}</p> : null}
        <h1 className="font-heading text-[1.85rem] font-semibold tracking-tight text-slate-950 lg:text-[2.15rem]">{title}</h1>
        {description ? <p className="max-w-3xl text-sm leading-7 text-slate-500">{description}</p> : null}
      </div>
      {actions ? <div className="mt-5 flex flex-wrap items-center gap-3 lg:mt-0 lg:justify-end">{actions}</div> : null}
    </div>
  );
}
