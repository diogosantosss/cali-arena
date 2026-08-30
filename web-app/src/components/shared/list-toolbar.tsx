import type { ReactNode } from "react";
import { Input } from "@/components/ui/input";
import { RefreshCw } from "lucide-react";

interface ListToolbarProps {
  search?: string;
  onSearchChange?: (value: string) => void;
  placeholder?: string;
  count: number;
  singular: string;
  plural?: string;
  refreshing?: boolean;
  onRefresh?: () => void;
  filters?: ReactNode;
}

export function ListToolbar({
  search,
  onSearchChange,
  placeholder,
  count,
  singular,
  plural = `${singular}s`,
  refreshing = false,
  onRefresh,
  filters,
}: ListToolbarProps) {
  return (
    <div className="flex items-center justify-between">
      {onSearchChange && (
        <Input
          value={search ?? ""}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder={placeholder}
          className="h-8 w-64 border-border text-foreground placeholder:text-faint focus-visible:ring-accent/40 focus-visible:border-accent/60"
          style={{ background: "var(--card)" }}
        />
      )}
      <div className="flex items-center gap-2">
        <span className="text-sm tabular-nums" style={{ color: "var(--muted-foreground)" }}>
          {count} {count !== 1 ? plural : singular}
        </span>
        {onRefresh && (
          <button
            onClick={onRefresh}
            disabled={refreshing}
            className="p-1.5 rounded transition-colors"
            style={{ color: "var(--muted-foreground)" }}
          >
            <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? "animate-spin" : ""}`} />
          </button>
        )}
        {filters}
      </div>
    </div>
  );
}
