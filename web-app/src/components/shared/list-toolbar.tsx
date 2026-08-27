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
          className="h-8 w-64 border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
          style={{ background: "#17171a" }}
        />
      )}
      <div className="flex items-center gap-2">
        <span className="text-sm tabular-nums" style={{ color: "#6b6560" }}>
          {count} {count !== 1 ? plural : singular}
        </span>
        {onRefresh && (
          <button
            onClick={onRefresh}
            disabled={refreshing}
            className="p-1.5 rounded transition-colors"
            style={{ color: "#6b6560" }}
          >
            <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? "animate-spin" : ""}`} />
          </button>
        )}
        {filters}
      </div>
    </div>
  );
}
