import type { ReactNode } from "react";

interface ListRowProps {
  index: number;
  accentColor: string;
  title: ReactNode;
  meta?: ReactNode;
  badge?: ReactNode;
  trailing?: ReactNode;
  onClick?: () => void;
}

export function ListRow({
  index,
  accentColor,
  title,
  meta,
  badge,
  trailing,
  onClick,
}: ListRowProps) {
  return (
    <div
      onClick={onClick}
      className="group flex items-center gap-5 px-5 py-4 rounded-lg transition-colors animate-fade-up hover:border-border-hover cursor-default"
      style={{
        background: "var(--card)",
        border: "1px solid var(--border)",
        animationDelay: `${index * 0.03}s`,
        opacity: 0,
        cursor: onClick ? "pointer" : undefined,
      }}
    >
      <div
        className="w-1 self-stretch rounded-full shrink-0"
        style={{ background: accentColor, opacity: 0.5 }}
      />

      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium truncate" style={{ color: "var(--foreground)" }}>
          {title}
        </p>
        {meta && (
          <div className="flex items-center gap-3 mt-0.5 text-xs" style={{ color: "var(--muted-foreground)" }}>
            {meta}
          </div>
        )}
      </div>

      {badge}
      {trailing}
    </div>
  );
}
