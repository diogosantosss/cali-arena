import { X } from "lucide-react";

interface JudgeErrorBannerProps {
  message: string;
  onDismiss: () => void;
}

export function JudgeErrorBanner({ message, onDismiss }: JudgeErrorBannerProps) {
  return (
    <div
      className="flex items-start gap-3 rounded-xl px-4 py-3 text-sm"
      style={{
        background: "rgba(224,85,85,0.12)",
        color: "#e05555",
        border: "1px solid rgba(224,85,85,0.3)",
      }}
    >
      <p className="flex-1 min-w-0">{message}</p>
      <button
        onClick={onDismiss}
        aria-label="Dismiss error"
        className="shrink-0 p-0.5 rounded transition-colors hover:bg-white/10"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}