import { ChevronRight, Trophy } from "lucide-react";
import type { MatchStatus } from "@/data/matches";
import type { JudgeMatchItem } from "@/hooks/use-judge-matches";
import { JudgeAvatar } from "./judge-side-chooser";

const stripStyles: Record<MatchStatus, string> = {
  PENDING: "var(--border)",
  READY: "#7eb8f7",
  RUNNING: "var(--gold)",
  PAUSED: "var(--secondary-foreground)",
  FINISHED: "#4ade80",
};

const statusStyles: Record<MatchStatus, { label: string; color: string; bg: string }> = {
  PENDING: { label: "Pending", color: "var(--muted-foreground)", bg: "rgba(107,101,96,0.12)" },
  READY: { label: "Ready", color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  RUNNING: { label: "Running", color: "var(--gold)", bg: "rgba(224,200,80,0.14)" },
  PAUSED: { label: "Paused", color: "var(--secondary-foreground)", bg: "rgba(160,154,146,0.12)" },
  FINISHED: { label: "Finished", color: "#4ade80", bg: "rgba(74,222,128,0.12)" },
};

interface JudgeMatchCardProps {
  item: JudgeMatchItem;
  onClick: () => void;
}

export function JudgeMatchCard({ item, onClick }: JudgeMatchCardProps) {
  const { match } = item;
  const clickable = match.status !== "FINISHED";
  const redWon = match.winnerAthleteId != null && match.winnerAthleteId === match.athleteRedId;
  const blueWon = match.winnerAthleteId != null && match.winnerAthleteId === match.athleteBlueId;
  const s = statusStyles[match.status];

  const body = (
    <div
      className="rounded-2xl overflow-hidden"
      style={{ background: "var(--card)", border: "1px solid var(--border)" }}
    >
      <div className="h-1" style={{ background: stripStyles[match.status] }} />

      <div className="px-4 py-3.5 space-y-3">
        <div className="flex items-center gap-2">
          <p className="text-[11px] font-semibold uppercase tracking-widest flex-1 truncate" style={{ color: "var(--muted-foreground)" }}>
            {item.division ?? `Match #${match.id}`}
          </p>
          <span className="text-[11px] px-2 py-0.5 rounded-full" style={{ background: s.bg, color: s.color }}>
            {s.label}
          </span>
          {clickable && <ChevronRight className="w-3.5 h-3.5 shrink-0" style={{ color: "var(--muted-foreground)" }} />}
        </div>

        <p className="text-xs truncate" style={{ color: item.routineName ? "var(--secondary-foreground)" : "var(--faint)" }}>
          {item.routineName ?? "Routine unavailable"}
        </p>

        <div className="flex items-center gap-2">
          <JudgeSide name={item.athleteRed?.name} clubName={item.clubRedName} accent="#e05555" winner={redWon} />
          <span className="text-xs font-bold px-1 shrink-0" style={{ color: "var(--faint)" }}>vs</span>
          <JudgeSide name={item.athleteBlue?.name} clubName={item.clubBlueName} accent="#5588e0" winner={blueWon} align="right" />
        </div>
      </div>
    </div>
  );

  if (!clickable) return body;

  return (
    <button onClick={onClick} className="w-full text-left transition-transform active:scale-[0.99]">
      {body}
    </button>
  );
}

function JudgeSide({
  name,
  clubName,
  accent,
  winner,
  align = "left",
}: {
  name?: string;
  clubName?: string;
  accent: string;
  winner: boolean;
  align?: "left" | "right";
}) {
  return (
    <div className={`flex-1 min-w-0 flex items-center gap-2 ${align === "right" ? "flex-row-reverse" : ""}`}>
      <JudgeAvatar name={name} accentColor={accent} size={30} />
      <div className={`min-w-0 ${align === "right" ? "text-right" : ""}`}>
        <p className="text-sm font-medium truncate flex items-center gap-1" style={{ color: "var(--foreground)" }}>
          {name ?? "Unassigned"}
          {winner && <Trophy className="w-3 h-3 shrink-0" style={{ color: "var(--gold)" }} />}
        </p>
        {clubName && (
          <p className="text-[11px] truncate" style={{ color: "var(--muted-foreground)" }}>
            {clubName}
          </p>
        )}
      </div>
    </div>
  );
}