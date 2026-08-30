import { Fragment, type ReactNode } from "react";
import { Skeleton } from "@/components/ui/skeleton";

export function SkeletonList({ count = 4, rowHeight = "h-16" }: { count?: number; rowHeight?: string }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className={`${rowHeight} rounded-lg`} style={{ background: "var(--card)" }}>
          <Skeleton className="h-full w-full rounded-lg opacity-40" />
        </div>
      ))}
    </div>
  );
}

interface ManagementListProps<T> {
  loading: boolean;
  error: string | null;
  items: T[];
  emptyLabel: string;
  getKey: (item: T) => React.Key;
  renderRow: (item: T, index: number) => ReactNode;
  skeletonRows?: number;
}

export function ManagementList<T>({
  loading,
  error,
  items,
  emptyLabel,
  getKey,
  renderRow,
  skeletonRows = 4,
}: ManagementListProps<T>) {
  if (loading) {
    return <SkeletonList count={skeletonRows} />;
  }

  if (error) {
    return (
      <p className="text-sm py-8 text-center" style={{ color: "var(--danger)" }}>
        {error}
      </p>
    );
  }

  if (items.length === 0) {
    return (
      <div className="py-20 text-center">
        <p className="text-xs tracking-widest uppercase" style={{ color: "var(--faint)" }}>
          {emptyLabel}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {items.map((item, index) => (
        <Fragment key={getKey(item)}>{renderRow(item, index)}</Fragment>
      ))}
    </div>
  );
}
