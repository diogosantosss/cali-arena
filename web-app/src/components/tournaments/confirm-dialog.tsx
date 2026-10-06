import { createPortal } from "react-dom";

interface ConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  title: string;
  message: string;
  confirmLabel: React.ReactNode;
  onConfirm: () => void;
  icon?: React.ReactNode;
  destructive?: boolean;
}

export function ConfirmDialog({
  open,
  onClose,
  title,
  message,
  confirmLabel,
  onConfirm,
  icon,
  destructive = false,
}: ConfirmDialogProps) {
  if (!open) return null;

  return createPortal(
    <div className="fixed inset-0 z-[9999] flex items-center justify-center" style={{ background: "rgba(0,0,0,0.5)" }} onClick={onClose}>
      <div className="w-full max-w-sm mx-4" style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: "12px" }} onClick={(e) => e.stopPropagation()}>
        {icon && <div className="flex items-center justify-center gap-2 py-4">{icon}</div>}
        <div className="px-6 pb-2">
          <h3 className="text-lg font-medium text-center" style={{ color: "var(--foreground)" }}>{title}</h3>
          <p className="text-center mt-2 text-sm" style={{ color: "var(--muted-foreground)" }}>{message}</p>
        </div>
        <div className="flex gap-3 px-6 pb-4">
          <button
            onClick={onClose}
            className="flex-1 py-2.5 rounded-lg text-sm font-medium transition-colors"
            style={{ background: "var(--muted)", color: "var(--muted-foreground)" }}
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            className="flex-1 py-2.5 rounded-lg text-sm font-medium transition-colors"
            style={
              destructive
                ? { background: "var(--danger)", color: "var(--danger-foreground)" }
                : { background: "var(--accent)", color: "var(--accent-foreground)" }
            }
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}