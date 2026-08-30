import type { ReactNode } from "react";

interface PageHeaderProps {
  title: string;
  eyebrow?: string;
  action?: ReactNode;
}

export function PageHeader({ title, eyebrow = "Management", action }: PageHeaderProps) {
  return (
    <div className="flex items-end justify-between">
      <div>
        <p
          className="text-xs tracking-widest uppercase mb-1.5"
          style={{ color: "var(--muted-foreground)" }}
        >
          {eyebrow}
        </p>
        <h1
          className="text-4xl leading-tight font-heading"
          style={{ color: "var(--foreground)" }}
        >
          {title}
        </h1>
      </div>
      {action}
    </div>
  );
}
