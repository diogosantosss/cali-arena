import { useEffect, useState } from "react";
import type { Athlete } from "@/features/athletes/types";
import type { Exercise, Routine, RoutineOverview } from "@/features/routines/types";
import { routineGroups } from "@/features/routines/lib/exercise-labels";
import { useMatchControl } from "../hooks/use-match-control";
import type { Match } from "../types";

const matchStatusStyles: Record<Match["status"], { label: string; color: string; bg: string }> = {
  PENDING: { label: "Pending", color: "#6b6560", bg: "rgba(107,101,96,0.12)" },
  READY: { label: "Ready", color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  RUNNING: { label: "Running", color: "#4ade80", bg: "rgba(74,222,128,0.12)" },
  PAUSED: { label: "Paused", color: "#a09a92", bg: "rgba(160,154,146,0.12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
};

function formatTime(ms: number): string {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  const millis = ms % 1000;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}`;
}

function useElapsedMs(startedAt: string | null): number {
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    if (!startedAt) {
      const reset = setTimeout(() => setElapsed(0), 0);
      return () => clearTimeout(reset);
    }
    const start = new Date(startedAt).getTime();
    const tick = () => setElapsed(Date.now() - start);
    const first = setTimeout(tick, 0);
    const interval = setInterval(tick, 250);
    return () => {
      clearTimeout(first);
      clearInterval(interval);
    };
  }, [startedAt]);

  return elapsed;
}

interface BattlePanelProps {
  matchId: number;
  athletes: Athlete[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
  onError: (message: string) => void;
}

export function BattlePanel({ matchId, athletes, routines, overviews, onError }: BattlePanelProps) {
  const {
    currentMatch,
    progress,
    redReps,
    blueReps,
    startMatch,
    adjustReps,
    finishSide,
  } = useMatchControl(matchId);

  async function run(action: () => Promise<void>) {
    try {
      await action();
    } catch (err) {
      onError(err instanceof Error ? err.message : "Unexpected error");
    }
  }

  const redAthlete = athletes.find((a) => a.id === currentMatch?.athleteRedId);
  const blueAthlete = athletes.find((a) => a.id === currentMatch?.athleteBlueId);
  const isRunning = currentMatch?.status === "RUNNING";
  const redFinished = !!progress?.redFinishedAt;
  const blueFinished = !!progress?.blueFinishedAt;

  const routine = routines.find((r) => r.id === currentMatch?.routineId);
  const exercises: Exercise[] = routine
    ? overviews[routine.name]?.exercises.sort((a, b) => a.exerciseOrder - b.exerciseOrder) ?? []
    : [];
  const groups = routineGroups(exercises);
  const redExercise = exercises.find((e) => e.id === progress?.redCurrentExerciseId) ?? exercises[0];
  const blueExercise = exercises.find((e) => e.id === progress?.blueCurrentExerciseId) ?? exercises[0];

  function finishTime(finishedAt: string): string {
    const start = progress?.timerStartedAt;
    if (!start) return "00:00.000";
    return formatTime(Math.max(0, new Date(finishedAt).getTime() - new Date(start).getTime()));
  }

  const elapsed = useElapsedMs(progress?.timerStartedAt ?? null);
  const matchFinished = currentMatch?.status === "FINISHED";

  function displayElapsed(): number {
    if (!matchFinished || !progress?.timerStartedAt || !currentMatch?.finishedAt) return elapsed;
    return Math.max(0, new Date(currentMatch.finishedAt).getTime() - new Date(progress.timerStartedAt).getTime());
  }

  const matchStatus = currentMatch ? matchStatusStyles[currentMatch.status] : null;

  const repButton =
    "px-3 py-1.5 rounded text-sm font-semibold transition-colors disabled:opacity-30 disabled:cursor-not-allowed";
  const repIncrementStyle = { background: "rgba(232,160,32,0.12)", color: "#e8a020", border: "1px solid rgba(232,160,32,0.25)" };
  const repDecrementStyle = { background: "#1e1e22", color: "#a09a92", border: "1px solid #252528" };

  return (
    <div className="rounded-lg" style={{ background: "#0f0f11", border: "1px solid #252528" }}>
      <div className="flex items-center justify-between px-5 py-4">
        <div className="flex items-center gap-3">
          <span className="text-sm font-bold" style={{ color: "#f0ede8" }}>Match #{matchId}</span>
          <span className="text-xs hidden sm:block" style={{ color: "#6b6560" }}>
            {routine?.name}
          </span>
          {matchStatus && (
            <span className="text-[11px] px-2 py-0.5 rounded-full" style={{ background: matchStatus.bg, color: matchStatus.color }}>
              {matchStatus.label}
            </span>
          )}
        </div>

        <div className="flex items-center gap-4">
          <p
            className="text-xl font-bold tabular-nums hidden sm:block"
            style={{
              color: redFinished || blueFinished ? "#4ade80" : "#f0ede8",
              fontFamily: "Geist Variable, monospace",
            }}
          >
            {isRunning || matchFinished ? formatTime(displayElapsed()) : "–:–"}
          </p>
          {!isRunning && currentMatch?.status !== "FINISHED" && (
            <button
              onClick={() => void run(startMatch)}
              className="px-4 py-1.5 rounded text-sm font-medium"
              style={{ background: "#e8a020", color: "#0f0f11" }}
            >
              Start match
            </button>
          )}
        </div>
      </div>

      {isRunning && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 px-5 pb-5">
          <RepControl
            label={redAthlete?.name ?? "Red"}
            dot="#e05555"
            finished={redFinished}
            finishTime={redFinished && progress?.redFinishedAt ? finishTime(progress.redFinishedAt) : null}
            exercise={redExercise}
            reps={redReps}
            onAdjust={(delta) => void run(() => adjustReps("red", delta))}
            onFinish={() => void run(() => finishSide("red"))}
            repButton={repButton}
            repIncrementStyle={repIncrementStyle}
            repDecrementStyle={repDecrementStyle}
          />
          <RepControl
            label={blueAthlete?.name ?? "Blue"}
            dot="#5588e0"
            finished={blueFinished}
            finishTime={blueFinished && progress?.blueFinishedAt ? finishTime(progress.blueFinishedAt) : null}
            exercise={blueExercise}
            reps={blueReps}
            onAdjust={(delta) => void run(() => adjustReps("blue", delta))}
            onFinish={() => void run(() => finishSide("blue"))}
            repButton={repButton}
            repIncrementStyle={repIncrementStyle}
            repDecrementStyle={repDecrementStyle}
          />
        </div>
      )}

      {exercises.length > 0 && (
        <div style={{ borderTop: "1px solid #252528" }} className="px-5 py-4">
          <div className="flex flex-wrap gap-2">
            {groups.map((group, i) => {
              const hasRed = group.items.some((e) => e.id === redExercise?.id);
              const hasBlue = group.items.some((e) => e.id === blueExercise?.id);
              return (
                <span
                  key={group.order}
                  className="text-xs px-2.5 py-1 rounded-full"
                  style={{
                    background:
                      hasRed && hasBlue
                        ? "linear-gradient(90deg, rgba(224,85,85,0.4) 0%, rgba(224,85,85,0.12) 50%, rgba(85,136,224,0.12) 50%, rgba(85,136,224,0.4) 100%)"
                        : hasRed
                          ? "rgba(224,85,85,0.14)"
                          : hasBlue
                            ? "rgba(85,136,224,0.14)"
                            : "#1e1e22",
                    color:
                      hasRed && hasBlue
                        ? "#ffffff"
                        : hasRed
                          ? "#e05555"
                          : hasBlue
                            ? "#5588e0"
                            : "#6b6560",
                    border:
                      hasRed && hasBlue
                        ? "1px solid rgba(160,120,220,0.4)"
                        : hasRed
                          ? "1px solid rgba(224,85,85,0.45)"
                          : hasBlue
                            ? "1px solid rgba(85,136,224,0.45)"
                            : "1px solid #252528",
                  }}
                >
                  {group.label}
                  {i === groups.length - 1 ? " — goal" : ""}
                </span>
              );
            })}
          </div>
          {routine?.timeCapSeconds && (
            <p className="text-[11px] mt-3 tracking-widest uppercase" style={{ color: "#3a3a3d" }}>
              Time cap — {Math.floor(routine.timeCapSeconds / 60)}m
              {routine.timeCapSeconds % 60 > 0 ? ` ${routine.timeCapSeconds % 60}s` : ""}
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function RepControl({
  label,
  dot,
  finished,
  finishTime,
  exercise,
  reps,
  onAdjust,
  onFinish,
  repButton,
  repIncrementStyle,
  repDecrementStyle,
}: {
  label: string;
  dot: string;
  finished: boolean;
  finishTime: string | null;
  exercise: { name: string; targetReps: number; addedWeight?: number | null } | undefined;
  reps: number;
  onAdjust: (delta: number) => void;
  onFinish: () => void;
  repButton: string;
  repIncrementStyle: React.CSSProperties;
  repDecrementStyle: React.CSSProperties;
}) {
  return (
    <div className="rounded-lg px-4 py-3" style={{ background: "#17171a", border: "1px solid #252528" }}>
      <div className="flex items-center gap-2">
        <span className="w-2.5 h-2.5 rounded-full" style={{ background: dot }} />
        <p className="text-sm font-semibold" style={{ color: "#f0ede8" }}>{label}</p>
      </div>

      <p className="text-[11px] mt-2 uppercase tracking-widest" style={{ color: finished ? "#4ade80" : "#6b6560" }}>
        {finished ? (finishTime ? `Finished — ${finishTime}` : "Finished") : exercise?.name ?? "—"}
        {!finished && exercise?.addedWeight ? ` (with ${exercise.addedWeight} kg)` : ""}
      </p>

      <p className="text-2xl font-bold tabular-nums mt-1" style={{ color: finished ? "#4ade80" : "#e8a020", fontFamily: "Geist Variable, monospace" }}>
        {reps}
        <span className="text-sm font-medium" style={{ color: "#3a3a3d" }}>/{exercise?.targetReps ?? "—"}</span>
      </p>

      <div className="flex items-center gap-2 mt-3">
        <button className={`${repButton} px-2.5`} disabled={finished} style={repDecrementStyle} onClick={() => onAdjust(-1)}>−1</button>
        <button className={repButton} disabled={finished} style={repIncrementStyle} onClick={() => onAdjust(1)}>+1</button>
        <button className={repButton} disabled={finished} style={repIncrementStyle} onClick={() => onAdjust(2)}>+2</button>
        <button className={repButton} disabled={finished} style={repIncrementStyle} onClick={() => onAdjust(5)}>+5</button>
        <button
          onClick={onFinish}
          disabled={finished}
          className="ml-auto px-3 py-1.5 rounded text-sm font-medium transition-colors disabled:opacity-30"
          style={{ background: "rgba(74,222,128,0.12)", color: "#4ade80", border: "1px solid rgba(74,222,128,0.25)" }}
        >
          Finish
        </button>
      </div>
    </div>
  );
}
