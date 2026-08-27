import type { ReactNode } from "react";

interface BadgeProps {
  label: ReactNode;
  color: string;
  bg: string;
  capitalize?: boolean;
}

export function Badge({ label, color, bg, capitalize = true }: BadgeProps) {
  return (
    <span
      className={`text-xs px-2.5 py-1 rounded-full shrink-0 ${capitalize ? "capitalize" : ""}`}
      style={{ background: bg, color }}
    >
      {label}
    </span>
  );
}
