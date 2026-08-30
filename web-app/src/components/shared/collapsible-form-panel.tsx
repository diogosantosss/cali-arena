import type { ReactNode } from "react";
import { FormError } from "./form-error";

interface CollapsibleFormPanelProps {
  open: boolean;
  label: string;
  error: string | null;
  saving: boolean;
  disabled?: boolean;
  onSubmit: (e: React.FormEvent) => void;
  onCancel: () => void;
  children: ReactNode;
}

export function CollapsibleFormPanel({
  open,
  label,
  error,
  saving,
  disabled = false,
  onSubmit,
  onCancel,
  children,
}: CollapsibleFormPanelProps) {
  if (!open) return null;

  return (
    <div
      className="rounded-lg p-6 space-y-5 animate-fade-up"
      style={{ background: "var(--card)", border: "1px solid var(--border)" }}
    >
      <p className="text-xs tracking-widest uppercase" style={{ color: "var(--muted-foreground)" }}>
        {label}
      </p>
      <form onSubmit={onSubmit} className="space-y-5">
        {children}

        {error && <FormError message={error} />}

        <div className="flex items-center gap-3 pt-1">
          <button
            type="submit"
            disabled={saving || disabled}
            className="px-5 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
            style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
          >
            {saving ? "Creating…" : "Create"}
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 rounded text-sm transition-colors"
            style={{ color: "var(--muted-foreground)" }}
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
