import * as React from "react";

import { cn } from "@/lib/utils";

export const Textarea = React.forwardRef<HTMLTextAreaElement, React.TextareaHTMLAttributes<HTMLTextAreaElement>>(
  ({ className, ...props }, ref) => (
    <textarea
      ref={ref}
      className={cn(
        "flex min-h-[124px] w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-foreground shadow-sm outline-none ring-offset-background placeholder:text-slate-400 transition focus-visible:border-primary/60 focus-visible:ring-2 focus-visible:ring-ring/25",
        className,
      )}
      {...props}
    />
  ),
);

Textarea.displayName = "Textarea";
