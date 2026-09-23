import { Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import type { MatchStatus } from "@/data/matches";
import { JudgeLogoutButton } from "./judge-logout-button";
import { JudgeThemeToggle } from "./judge-theme-toggle";

const statusStyles: Record<MatchStatus, { label: string; color: string; bg: string }> = {
  PENDING: { label: "Pending", color: "var(--muted-foreground)", bg: "rgba(107,101,96,0.12)" },
  READY: { label: "Ready", color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  RUNNING: { label: "Running", color: "#4ade80", bg: "rgba(74,222,128,0.12)" },
  PAUSED: { label: "Paused", color: "var(--secondary-foreground)", bg: "rgba(160,154,146,0.12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
};

interface JudgeTopBarProps {
  title: string;
  status: MatchStatus | null;
  connectionLost?: boolean;
}

export function JudgeTopBar({ title, status, connectionLost }: JudgeTopBarProps) {
  const s = status ? statusStyles[status] : null;

  return (
    <div
      className="sticky top-0 z-10 flex items-center gap-3 px-3 py-2.5"
      style={{ background: "var(--background)", borderBottom: "1px solid var(--border)" }}
    >
      <Link
        to="/judge"
        aria-label="Back to matches"
        className="p-1.5 rounded-full transition-colors hover:bg-muted"
        style={{ color: "var(--foreground)" }}
      >
        <ArrowLeft className="w-5 h-5" />
      </Link>

      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold leading-tight truncate" style={{ color: "var(--foreground)" }}>
          {title}
        </p>
        {connectionLost && (
          <p className="text-[11px] leading-tight" style={{ color: "var(--muted-foreground)" }}>
            Connection lost — reconnecting…
          </p>
        )}
      </div>

      

      {status && s && status !== "PENDING" && (
        <span className="text-[11px] px-2 py-0.5 rounded-full shrink-0" style={{ background: s.bg, color: s.color }}>
          {s.label}
        </span>
      )}

      <JudgeThemeToggle />
      <JudgeLogoutButton />
    </div>
  );
}
