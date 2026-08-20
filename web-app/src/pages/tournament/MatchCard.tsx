import { useState } from "react";
import type { Athlete, Match, Routine, User } from "@/types";
import { UserRound, ChevronDown, ChevronUp } from "lucide-react";

interface Props {
  match: Match;
  athletes: Athlete[];
  routines: Routine[];
  judges: User[];
  onAssignAthletes: (match: Match) => void;
  onStartMatch: (match: Match) => void;
}

const matchStatusStyles: Record<Match["status"], { label: string; color: string; bg: string }> = {
  PENDING:  { label: "Pending",  color: "#6b6560", bg: "rgba(107,101,96,0.12)" },
  READY:    { label: "Ready",    color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  RUNNING:  { label: "Running",  color: "#e8a020", bg: "rgba(232,160,32,0.12)" },
  PAUSED:   { label: "Paused",   color: "#a09a92", bg: "rgba(160,154,146,0.12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
};

export function MatchCard({ match, athletes, routines, judges, onAssignAthletes, onStartMatch }: Props) {
  const [expanded, setExpanded] = useState(false);

  const redAthlete = athletes.find((a) => a.id === match.athleteRedId);
  const blueAthlete = athletes.find((a) => a.id === match.athleteBlueId);
  const routine = routines.find((r) => r.id === match.routineId);
  const judge = judges.find((j) => j.id === match.judgeId);
  const s = matchStatusStyles[match.status];

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
