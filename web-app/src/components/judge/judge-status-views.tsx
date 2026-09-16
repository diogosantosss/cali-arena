import { Loader2 } from "lucide-react";

export function JudgeLoading() {
  return (
    <div className="flex flex-col items-center justify-center py-24 gap-4">
      <Loader2 className="w-6 h-6 animate-spin" style={{ color: "var(--muted-foreground)" }} />
      <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>
        Loading match…
      </p>
    </div>
  );
}

export function JudgeUnassignedCard() {
  return (
    <div
      className="w-full rounded-xl px-5 py-6 text-center"
      style={{
        background: "var(--card)",
        border: "1px solid var(--border)",
        color: "var(--muted-foreground)",
      }}
    >
      <p className="text-sm">Athletes have not been assigned to this match yet.</p>
    </div>
  );
}