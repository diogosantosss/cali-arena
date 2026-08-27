import { useState } from "react";
import type { Athlete } from "@/features/athletes/types";
import type { Routine } from "@/features/routines/types";
import type { User } from "@/features/users/types";
import type { Match, MatchProgress } from "../types";
import { UserRound, ChevronDown, ChevronUp, Timer, Trophy } from "lucide-react";

interface MatchCardProps {
  match: Match;
  progress?: MatchProgress;
  athletes: Athlete[];
  routines: Routine[];
  judges: User[];
  onAssignAthletes: (match: Match) => void;
  onStartMatch: (match: Match) => void;
}

function formatDuration(ms: number): string {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  const millis = ms % 1000;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}`;
}

const matchStatusStyles: Record<Match["status"], { label: string; color: string; bg: string }> = {
  PENDING:  { label: "Pending",  color: "#6b6560", bg: "rgba(107,101,96,0.12)" },
  READY:    { label: "Ready",    color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  RUNNING:  { label: "Running",  color: "#e8a020", bg: "rgba(232,160,32,0.12)" },
  PAUSED:   { label: "Paused",   color: "#a09a92", bg: "rgba(160,154,146,0.12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
};

export function MatchCard({ match, progress, athletes, routines, judges, onAssignAthletes, onStartMatch }: MatchCardProps) {
  const [expanded, setExpanded] = useState(false);

  const redAthlete = athletes.find((a) => a.id === match.athleteRedId);
  const blueAthlete = athletes.find((a) => a.id === match.athleteBlueId);
  const routine = routines.find((r) => r.id === match.routineId);
  const judge = judges.find((j) => j.id === match.judgeId);
  const s = matchStatusStyles[match.status];

  function sideDuration(finishedAt: string | null): string | null {
    if (!finishedAt || !match.startedAt) return null;
    return formatDuration(Math.max(0, new Date(finishedAt).getTime() - new Date(match.startedAt!).getTime()));
  }
  const redTime = match.status === "FINISHED" ? sideDuration(progress?.redFinishedAt ?? null) : null;
  const blueTime = match.status === "FINISHED" ? sideDuration(progress?.blueFinishedAt ?? null) : null;
  const isRedWinner = match.status === "FINISHED" && match.winnerAthleteId != null && match.winnerAthleteId === match.athleteRedId;
  const isBlueWinner = match.status === "FINISHED" && match.winnerAthleteId != null && match.winnerAthleteId === match.athleteBlueId;

  return (
    <div
      className="rounded-lg overflow-hidden"
      style={{ background: "#17171a", border: "1px solid #252528" }}
    >
      <div className="flex items-center gap-4 px-4 py-3">
        <span
          className="text-[11px] px-2 py-0.5 rounded-full shrink-0"
          style={{ background: s.bg, color: s.color }}
        >
          {s.label}
        </span>

        <div className="flex items-center flex-1 min-w-0 gap-3">
          <div className="flex items-center gap-2 flex-1 min-w-0">
            <div className="w-1.5 h-1.5 rounded-full shrink-0" style={{ background: "#e05555" }} />
            <span className="text-sm truncate" style={{ color: redAthlete ? "#f0ede8" : "#3a3a3d" }}>
              {redAthlete?.name ?? "Not assigned"}
            </span>
          </div>

          <span className="text-xs shrink-0" style={{ color: "#3a3a3d" }}>vs</span>

          <div className="flex items-center gap-2 flex-1 min-w-0 justify-end">
            <span className="text-sm truncate" style={{ color: blueAthlete ? "#f0ede8" : "#3a3a3d" }}>
              {blueAthlete?.name ?? "Not assigned"}
            </span>
            <div className="w-1.5 h-1.5 rounded-full shrink-0" style={{ background: "#5588e0" }} />
          </div>
        </div>

        {routine && (
          <span className="text-xs shrink-0 hidden sm:block" style={{ color: "#6b6560" }}>
            {routine.name}
          </span>
        )}

        <button
          onClick={() => setExpanded((v) => !v)}
          className="p-1 rounded transition-colors shrink-0"
          style={{ color: "#3a3a3d" }}
          onMouseEnter={(e) => (e.currentTarget.style.color = "#6b6560")}
          onMouseLeave={(e) => (e.currentTarget.style.color = "#3a3a3d")}
        >
          {expanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </button>
      </div>

      {match.status === "FINISHED" && (
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 px-4 pb-3 text-xs" style={{ color: "#6b6560" }}>
          {match.startedAt && (
            <span className="flex items-center gap-1.5">
              Started at {new Date(match.startedAt).toLocaleTimeString()}
            </span>
          )}
          {redTime && (
            <span
              className="flex items-center gap-1.5"
              style={{
                color: isRedWinner ? "#e8a020" : "#e05555",
                fontWeight: isRedWinner ? 600 : 400,
                background: isRedWinner ? "rgba(232,160,32,0.1)" : undefined,
                border: isRedWinner ? "1px solid rgba(232,160,32,0.25)" : undefined,
                borderRadius: "9999px",
                padding: "2px 8px",
              }}
            >
              {isRedWinner ? <Trophy className="w-3 h-3" /> : <Timer className="w-3 h-3" />}
              {redAthlete?.name ?? "Red"} — {redTime}
            </span>
          )}
          {blueTime && (
            <span
              className="flex items-center gap-1.5"
              style={{
                color: isBlueWinner ? "#e8a020" : "#5588e0",
                fontWeight: isBlueWinner ? 600 : 400,
                background: isBlueWinner ? "rgba(232,160,32,0.1)" : undefined,
                border: isBlueWinner ? "1px solid rgba(232,160,32,0.25)" : undefined,
                borderRadius: "9999px",
                padding: "2px 8px",
              }}
            >
              {isBlueWinner ? <Trophy className="w-3 h-3" /> : <Timer className="w-3 h-3" />}
              {blueAthlete?.name ?? "Blue"} — {blueTime}
            </span>
          )}
        </div>
      )}

      {expanded && (
        <div style={{ borderTop: "1px solid #252528" }} className="px-4 py-3 space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <p className="text-[10px] uppercase tracking-widest mb-1" style={{ color: "#3a3a3d" }}>Judge</p>
              <div className="flex items-center gap-1.5">
                <UserRound className="w-3 h-3" style={{ color: "#6b6560" }} />
                <span className="text-xs" style={{ color: "#a09a92" }}>
                  {judge?.username ?? `#${match.judgeId}`}
                </span>
              </div>
            </div>
            {match.winnerAthleteId && (
              <div>
                <p className="text-[10px] uppercase tracking-widest mb-1" style={{ color: "#3a3a3d" }}>Winner</p>
                <span className="text-xs font-medium" style={{ color: "#e8a020" }}>
                  {athletes.find((a) => a.id === match.winnerAthleteId)?.name ?? `#${match.winnerAthleteId}`}
                </span>
              </div>
            )}
            {match.startedAt && (
              <div>
                <p className="text-[10px] uppercase tracking-widest mb-1" style={{ color: "#3a3a3d" }}>Started</p>
                <span className="text-xs" style={{ color: "#a09a92" }}>
                  {new Date(match.startedAt).toLocaleTimeString()}
                </span>
              </div>
            )}
            {match.finishedAt && (
              <div>
                <p className="text-[10px] uppercase tracking-widest mb-1" style={{ color: "#3a3a3d" }}>Finished</p>
                <span className="text-xs" style={{ color: "#a09a92" }}>
                  {new Date(match.finishedAt).toLocaleTimeString()}
                </span>
              </div>
            )}
          </div>

          <div className="flex gap-2 pt-1">
            {match.status === "PENDING" && !match.athleteRedId && !match.athleteBlueId && (
              <button
                onClick={() => onAssignAthletes(match)}
                className="px-3 py-1.5 rounded text-xs transition-colors"
                style={{ background: "rgba(232,160,32,0.1)", color: "#e8a020", border: "1px solid rgba(232,160,32,0.2)" }}
              >
                Assign athletes
              </button>
            )}
            {match.status === "READY" && (
              <button
                onClick={() => onStartMatch(match)}
                className="px-3 py-1.5 rounded text-xs font-medium transition-opacity"
                style={{ background: "#e8a020", color: "#0f0f11" }}
              >
                Start match
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
