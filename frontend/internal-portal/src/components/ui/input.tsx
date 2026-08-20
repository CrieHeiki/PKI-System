import * as React from "react";

import { cn } from "@/lib/utils";

export const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        "flex h-11 w-full rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm text-foreground shadow-sm outline-none ring-offset-background placeholder:text-slate-400 transition focus-visible:border-primary/60 focus-visible:ring-2 focus-visible:ring-ring/25",
        className,
      )}
      {...props}
    />
  ),
);

Input.displayName = "Input";
