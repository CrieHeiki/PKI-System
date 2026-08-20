import { cva, type VariantProps } from "class-variance-authority";
import { Slot } from "@radix-ui/react-slot";
import * as React from "react";

import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex cursor-pointer items-center justify-center rounded-2xl text-sm font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/35 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50",
  {
    variants: {
      variant: {
        default:
          "bg-primary px-5 py-2 text-primary-foreground shadow-[0_10px_22px_rgba(37,99,235,0.24)] hover:bg-[hsl(216_92%_54%)] hover:shadow-[0_14px_28px_rgba(37,99,235,0.28)]",
        secondary:
          "border border-slate-200 bg-slate-50 px-5 py-2 text-slate-700 shadow-sm hover:bg-slate-100 hover:text-slate-900",
        outline:
          "border border-slate-300 bg-white px-5 py-2 text-slate-700 shadow-sm hover:border-primary/35 hover:bg-slate-50 hover:text-slate-900",
        ghost: "px-5 py-2 text-slate-700 hover:bg-slate-100 hover:text-slate-950",
        danger:
          "bg-destructive px-5 py-2 text-destructive-foreground shadow-[0_10px_22px_rgba(220,38,38,0.22)] hover:bg-[hsl(0_72%_47%)] hover:shadow-[0_14px_28px_rgba(220,38,38,0.28)]",
      },
      size: {
        default: "h-11",
        sm: "h-9 px-3 text-xs",
        lg: "h-12 px-6 text-sm",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return <Comp className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />;
  },
);

Button.displayName = "Button";
