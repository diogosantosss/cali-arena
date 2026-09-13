import type { LucideIcon } from "lucide-react";

interface ToggleButtonProps {
  open: boolean;
  onClick: () => void;
  icon: LucideIcon;
  label: string;
}

export function ToggleButton({ open, onClick, icon: Icon, label }: ToggleButtonProps) {
  return (
    <button
      onClick={onClick}
      className="flex items-center gap-2 px-4 py-2 rounded text-sm font-medium transition-colors"
      style={
        open
          ? {
              background: "var(--accent-10)",
              color: "var(--accent)",
              border: "1px solid var(--accent-30)",
            }
          : { background: "var(--accent)", color: "var(--accent-foreground)", border: "none" }
      }
    >
      <Icon className="w-3.5 h-3.5" />
      {label}
    </button>
  );
}
