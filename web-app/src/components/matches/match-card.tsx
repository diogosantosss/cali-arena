import type { Athlete } from "@/data/athletes";
import type { Routine } from "@/data/routines";
import type { Match, MatchProgress } from "@/data/matches";
import { Play, Timer, Trophy, Trash2 } from "lucide-react";

interface MatchCardProps {
  match: Match;
  progress?: MatchProgress;
  athletes: Athlete[];
  routines: Routine[];
  onStartMatch: (match: Match) => void;
  onDeleteMatch?: (match: Match) => void;
}

function formatDuration(ms: number): string {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  const millis = ms % 1000;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}`;
}

const matchStatusStyles: Record<Match["status"], { label: string; color: string; bg: string }> = {
  PENDING:  { label: "Pending",  color: "var(--muted-foreground)", bg: "rgba(107,101,96,0.12)" },
  READY:    { label: "Ready",    color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  RUNNING:  { label: "Running",  color: "var(--accent)", bg: "var(--accent-12)" },
  PAUSED:   { label: "Paused",   color: "var(--secondary-foreground)", bg: "rgba(160,154,146,0.12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
};

const RED = "#e05555";
const BLUE = "#5588e0";
const hairline = "1px solid var(--border)";

export function MatchCard({ match, progress, athletes, routines, onStartMatch, onDeleteMatch }: MatchCardProps) {
  const redAthlete = athletes.find((a) => a.id === match.athleteRedId);
  const blueAthlete = athletes.find((a) => a.id === match.athleteBlueId);
  const routine = routines.find((r) => r.id === match.routineId);
  const s = matchStatusStyles[match.status];

  function sideDuration(finishedAt: string | null): string | null {
    if (!finishedAt || !match.startedAt) return null;
    return formatDuration(Math.max(0, new Date(finishedAt).getTime() - new Date(match.startedAt).getTime()));
  }
  const redTime = match.status === "FINISHED" ? sideDuration(progress?.redFinishedAt ?? null) : null;
  const blueTime = match.status === "FINISHED" ? sideDuration(progress?.blueFinishedAt ?? null) : null;
  const isRedWinner = match.status === "FINISHED" && match.winnerAthleteId != null && match.winnerAthleteId === match.athleteRedId;
  const isBlueWinner = match.status === "FINISHED" && match.winnerAthleteId != null && match.winnerAthleteId === match.athleteBlueId;

  return (
    <div className="rounded-lg overflow-hidden mt-3" style={{ background: "var(--card)", border: hairline }}>
      <div className="flex items-center justify-between gap-2 px-3 pt-2.5 pb-2">
        <span
          className="flex items-center gap-1.5 px-2 py-0.5 rounded-full shrink-0 text-[10px] font-medium"
          style={{ background: s.bg, color: s.color }}
        >
          <span className="w-1 h-1 rounded-full" style={{ background: s.color }} />
          {s.label}
        </span>

        <div className="flex items-center gap-2 min-w-0">
          {routine && (
            <span className="text-[11px] truncate max-w-[140px]" style={{ color: "var(--muted-foreground)" }}>
              {routine.name}
            </span>
          )}
          {onDeleteMatch && (
            <button
              onClick={() => onDeleteMatch(match)}
              title="Delete match"
              className="p-1 rounded transition-colors shrink-0"
              style={{ color: "var(--faint)" }}
              onMouseEnter={(e) => (e.currentTarget.style.color = "var(--danger)")}
              onMouseLeave={(e) => (e.currentTarget.style.color = "var(--faint)")}
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>

      <div className="border-t" style={{ borderColor: "var(--border)" }}>
        <AthleteSide name={redAthlete?.name ?? "Not assigned"} color={RED} time={redTime} winner={isRedWinner} />
        <div className="h-px" style={{ background: "var(--border)" }} />
        <AthleteSide name={blueAthlete?.name ?? "Not assigned"} color={BLUE} time={blueTime} winner={isBlueWinner} />
      </div>

      {match.status === "READY" && (
        <div className="px-3 pb-3 pt-2 border-t" style={{ borderColor: "var(--border)" }}>
          <button
            onClick={() => onStartMatch(match)}
            className="w-full flex items-center justify-center gap-1.5 h-8 rounded-md text-xs font-medium transition-colors"
            style={{
              background: "var(--accent-12)",
              color: "var(--accent)",
              border: "1px solid var(--accent-25)",
            }}
            onMouseEnter={(e) => { e.currentTarget.style.background = "var(--accent-20)"; }}
            onMouseLeave={(e) => { e.currentTarget.style.background = "var(--accent-12)"; }}
          >
            <Play className="w-3 h-3" />
            Start match
          </button>
        </div>
      )}
    </div>
  );
}

function AthleteSide({ name, color, time, winner }: { name: string; color: string; time: string | null; winner: boolean }) {
  return (
    <div
      className="flex items-center gap-2 px-3 py-2 min-w-0"
      style={{ background: winner ? "var(--gold-10)" : "transparent" }}
    >
      <span
        className="w-2 h-2 rounded-full shrink-0"
        style={{ background: winner ? "var(--gold)" : color, boxShadow: winner ? "0 0 6px var(--gold)" : undefined }}
      />
      <span
        className="text-sm font-medium truncate"
        style={{ color: winner ? "var(--gold)" : "var(--foreground)" }}
      >
        {name}
      </span>
      {time && (
        <span
          className="ml-auto flex items-center gap-1.5 shrink-0 text-xs tabular-nums font-medium"
          style={{ color: winner ? "var(--gold)" : "var(--secondary-foreground)" }}
        >
          {winner ? <Trophy className="w-3 h-3" /> : <Timer className="w-3 h-3" />}
          {time}
        </span>
      )}
    </div>
  );
}