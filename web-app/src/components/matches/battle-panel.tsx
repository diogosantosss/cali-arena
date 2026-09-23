import { useEffect, useState } from "react";
import { Lock, ShieldCheck } from "lucide-react";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import type { Athlete } from "@/data/athletes";
import type { Exercise, Routine, RoutineOverview } from "@/data/routines";
import { routineGroups } from "@/utils/exercise-labels";
import { useMatchControl } from "@/hooks/use-match-control";
import type { Match } from "@/data/matches";

const matchStatusStyles: Record<Match["status"], { label: string; color: string; bg: string }> = {
  PENDING: { label: "Pending", color: "var(--muted-foreground)", bg: "rgba(107,101,96,0.12)" },
  READY: { label: "Ready", color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  RUNNING: { label: "Running", color: "#4ade80", bg: "rgba(74,222,128,0.12)" },
  PAUSED: { label: "Paused", color: "var(--secondary-foreground)", bg: "rgba(160,154,146,0.12)" },
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
    let frame = 0;
    const tick = () => {
      setElapsed(Date.now() - start);
      frame = requestAnimationFrame(tick);
    };
    tick();
    return () => cancelAnimationFrame(frame);
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
  } = useMatchControl(matchId, onError);

  const [finishTarget, setFinishTarget] = useState<"red" | "blue" | null>(null);
  const [controlActive, setControlActive] = useState<boolean>(() => {
    try {
      return sessionStorage.getItem(`battle-control:${matchId}`) === "1";
    } catch {
      return false;
    }
  });
  const [confirmTakeover, setConfirmTakeover] = useState(false);

  function acceptControl() {
    setControlActive(true);
    try {
      sessionStorage.setItem(`battle-control:${matchId}`, "1");
    } catch {
      // ignore
    }
    setConfirmTakeover(false);
  }

  function releaseControl() {
    setControlActive(false);
    try {
      sessionStorage.setItem(`battle-control:${matchId}`, "0");
    } catch {
      // ignore
    }
  }

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
  const repIncrementStyle = { background: "var(--accent-12)", color: "var(--accent)", border: "1px solid var(--accent-25)" };
  const repDecrementStyle = { background: "var(--secondary)", color: "var(--secondary-foreground)", border: "1px solid var(--border)" };

  return (
    <div className="rounded-lg" style={{ background: "var(--background)", border: "1px solid var(--border)" }}>
      <div className="flex items-center justify-between gap-4 px-5 py-4">
        <div className="flex items-center gap-3 min-w-0">
          {matchStatus && (
            <span className="text-[11px] px-2 py-0.5 rounded-full shrink-0" style={{ background: matchStatus.bg, color: matchStatus.color }}>
              {matchStatus.label}
            </span>
          )}
          <div className="flex items-center gap-2 min-w-0">
            <span className="w-2 h-2 rounded-full shrink-0" style={{ background: "#e05555", boxShadow: "0 0 8px rgba(224,85,85,0.8)" }} />
            <span className="text-sm font-semibold truncate" style={{ color: redAthlete ? "var(--foreground)" : "var(--faint)" }}>
              {redAthlete?.name ?? "Not assigned"}
            </span>
            <span className="text-xs shrink-0" style={{ color: "var(--faint)" }}>vs</span>
            <span className="text-sm font-semibold truncate" style={{ color: blueAthlete ? "var(--foreground)" : "var(--faint)" }}>
              {blueAthlete?.name ?? "Not assigned"}
            </span>
            <span className="w-2 h-2 rounded-full shrink-0" style={{ background: "#5588e0", boxShadow: "0 0 8px rgba(85,136,224,0.8)" }} />
          </div>
          {routine?.name && (
            <span className="text-xs hidden lg:block shrink-0" style={{ color: "var(--muted-foreground)" }}>
              {routine.name}
            </span>
          )}
        </div>

        <div className="flex items-center gap-4 shrink-0">
          <div className="text-right hidden sm:block">
            <p className="text-[10px] uppercase tracking-widest" style={{ color: "var(--faint)" }}>
              {matchFinished ? "Final time" : isRunning ? "Elapsed" : "Clock"}
            </p>
            <p
              className="text-lg font-bold tabular-nums leading-tight"
              style={{
                color: redFinished || blueFinished ? "#4ade80" : "var(--foreground)",
                fontFamily: "Geist Variable, monospace",
              }}
            >
              {isRunning || matchFinished ? formatTime(displayElapsed()) : "–:–"}
            </p>
          </div>
          {!isRunning && currentMatch?.status !== "FINISHED" && (
            <button
              onClick={() => void run(startMatch)}
              className="px-4 py-1.5 rounded text-sm font-medium"
              style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
            >
              Start match
            </button>
          )}
        </div>
      </div>

      {isRunning && (
        <div>
          <div
            className="flex flex-wrap items-center gap-x-3 gap-y-2 px-5 py-3"
            style={{
              background: controlActive ? "rgba(74,222,128,0.08)" : "var(--card)",
              borderTop: "1px solid var(--border)",
            }}
          >
            {controlActive ? (
              <ShieldCheck className="w-4 h-4 shrink-0" style={{ color: "#4ade80" }} />
            ) : (
              <Lock className="w-4 h-4 shrink-0" style={{ color: "var(--muted-foreground)" }} />
            )}
            <p className="text-xs font-medium" style={{ color: "var(--secondary-foreground)" }}>
              {controlActive ? "Rep control — you" : "Rep control — mobile judge"}
            </p>
            <span className="text-[11px]" style={{ color: "var(--muted-foreground)" }}>
              {controlActive ? "Adjustments from this browser are active." : "Adjustments are locked until you take over."}
            </span>
            <div className="ml-auto">
              {controlActive ? (
                <button
                  onClick={releaseControl}
                  className="px-3 py-1 rounded text-xs font-medium transition-colors"
                  style={{ background: "var(--secondary)", color: "var(--secondary-foreground)", border: "1px solid var(--border)" }}
                >
                  Release
                </button>
              ) : (
                <button
                  onClick={() => setConfirmTakeover(true)}
                  className="px-3 py-1 rounded text-xs font-semibold transition-colors"
                  style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
                >
                  Assume control
                </button>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 px-5 pt-4 pb-5">
            <RepControl
              label={redAthlete?.name ?? "Red"}
              dot="#e05555"
              finished={redFinished}
              finishTime={redFinished && progress?.redFinishedAt ? finishTime(progress.redFinishedAt) : null}
              exercise={redExercise}
              reps={redReps}
              disabled={!controlActive}
              onAdjust={(delta) => void run(() => adjustReps("red", delta))}
              onFinish={() => setFinishTarget("red")}
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
              disabled={!controlActive}
              onAdjust={(delta) => void run(() => adjustReps("blue", delta))}
              onFinish={() => setFinishTarget("blue")}
              repButton={repButton}
              repIncrementStyle={repIncrementStyle}
              repDecrementStyle={repDecrementStyle}
            />
          </div>
        </div>
      )}

      {exercises.length > 0 && (
        <div style={{ borderTop: "1px solid var(--border)" }} className="px-5 py-4">
          <div className="flex items-center gap-2 mb-3">
            <p className="text-[10px] uppercase tracking-widest" style={{ color: "var(--faint)" }}>
              Routine
            </p>
            <h4 className="text-sm font-semibold truncate" style={{ color: "var(--foreground)" }}>
              {routine?.name}
            </h4>
            {routine?.timeCapSeconds && (
              <span
                className="ml-auto shrink-0 text-[10px] uppercase tracking-widest px-2 py-0.5 rounded-full"
                style={{ background: "var(--accent-08)", color: "var(--accent)", border: "1px solid var(--accent-18)" }}
              >
                Time cap — {Math.floor(routine.timeCapSeconds / 60)}m
                {routine.timeCapSeconds % 60 > 0 ? ` ${routine.timeCapSeconds % 60}s` : ""}
              </span>
            )}
          </div>
          <div className="space-y-2">
            {groups.map((group, i) => {
              const hasRed = group.items.some((e) => e.id === redExercise?.id);
              const hasBlue = group.items.some((e) => e.id === blueExercise?.id);
              const isGoal = i === groups.length - 1;
              const type = group.items[0]?.type;
              return (
                <div key={group.order} className="flex items-center gap-3 min-w-0">
                  <span
                    className="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold shrink-0"
                    style={{ background: "var(--secondary)", color: "var(--muted-foreground)", border: "1px solid var(--border)" }}
                  >
                    {i + 1}
                  </span>
                  <span
                    className="text-sm truncate"
                    style={{ color: isGoal ? "var(--foreground)" : "var(--secondary-foreground)", fontWeight: isGoal ? 600 : 400 }}
                  >
                    {group.label}
                  </span>
                  {type && type !== "NORMAL" && (
                    <span
                      className="shrink-0 text-[9px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded"
                      style={
                        type === "SUPERSET"
                          ? { background: "rgba(240,196,106,0.12)", color: "#f0c46a", border: "1px solid rgba(240,196,106,0.25)" }
                          : { background: "rgba(126,184,247,0.12)", color: "#7eb8f7", border: "1px solid rgba(126,184,247,0.25)" }
                      }
                    >
                      {type}
                    </span>
                  )}
                  {isGoal && (
                    <span
                      className="shrink-0 text-[9px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded"
                      style={{ background: "var(--accent-12)", color: "var(--accent)" }}
                    >
                      Goal
                    </span>
                  )}
                  <span className="flex items-center gap-1.5 shrink-0 ml-auto">
                    {hasRed && redAthlete && (
                      <span
                        className="text-[10px] font-semibold px-1.5 py-0.5 rounded"
                        style={{ background: "rgba(224,85,85,0.14)", color: "#e05555", border: "1px solid rgba(224,85,85,0.35)" }}
                      >
                        {redAthlete.name}
                      </span>
                    )}
                    {hasBlue && blueAthlete && (
                      <span
                        className="text-[10px] font-semibold px-1.5 py-0.5 rounded"
                        style={{ background: "rgba(85,136,224,0.14)", color: "#5588e0", border: "1px solid rgba(85,136,224,0.35)" }}
                      >
                        {blueAthlete.name}
                      </span>
                    )}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      <Dialog
        open={finishTarget !== null}
        onOpenChange={(open) => {
          if (!open) setFinishTarget(null);
        }}
      >
        <DialogContent style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
          <DialogHeader>
            <DialogTitle>Force finish athlete</DialogTitle>
            <DialogDescription>
              Are you sure you want to finish{" "}
              <span className="font-semibold">
                {(finishTarget === "red" ? redAthlete?.name : blueAthlete?.name) ?? "this athlete"}
              </span>
              ? The opponent must have already finished — this ends the match and the opponent wins.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setFinishTarget(null)}>
              Cancel
            </Button>
            <Button
              onClick={() => {
                const target = finishTarget;
                setFinishTarget(null);
                if (target) void run(() => finishSide(target));
              }}
              style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
            >
              Finish
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={confirmTakeover} onOpenChange={(open) => { if (!open) setConfirmTakeover(false); }}>
        <DialogContent style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
          <DialogHeader>
            <DialogTitle>Take over rep control?</DialogTitle>
            <DialogDescription>
              A judge may be controlling this match from the mobile app. If you take over, rep changes and{" "}
              <span className="font-semibold">finish</span> actions from this browser become active.
              You can release control at any time.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmTakeover(false)}>Cancel</Button>
            <Button onClick={acceptControl} style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}>
              Yes, take over
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
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
  disabled,
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
  exercise: Exercise | undefined;
  reps: number;
  disabled: boolean;
  onAdjust: (delta: number) => void;
  onFinish: () => void;
  repButton: string;
  repIncrementStyle: React.CSSProperties;
  repDecrementStyle: React.CSSProperties;
}) {
  const target = exercise?.targetReps ?? 0;
  const pct = finished ? 100 : target > 0 ? Math.min(100, Math.round((reps / target) * 100)) : 0;

  return (
    <div
      className="rounded-xl px-4 py-4"
      style={{ background: "var(--card)", border: "1px solid var(--border)", borderTop: `2px solid ${dot}` }}
    >
      <div className="flex items-center gap-2 min-w-0">
        <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: dot, boxShadow: `0 0 8px ${dot}` }} />
        <p className="text-sm font-semibold truncate" style={{ color: "var(--foreground)" }}>{label}</p>
      </div>

      {finished ? (
        <p className="text-sm font-semibold mt-2" style={{ color: "#4ade80" }}>
          {finishTime ? `Finished — ${finishTime}` : "Finished"}
        </p>
      ) : (
        <div className="flex items-center gap-2 mt-2 min-w-0">
          <p className="text-lg font-semibold truncate" style={{ color: "var(--secondary-foreground)" }}>
            {exercise?.name ?? "—"}
          </p>
          {exercise && exercise.type !== "NORMAL" && (
            <span
              className="shrink-0 text-[9px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded"
              style={
                exercise.type === "SUPERSET"
                  ? { background: "rgba(240,196,106,0.12)", color: "#f0c46a", border: "1px solid rgba(240,196,106,0.25)" }
                  : { background: "rgba(126,184,247,0.12)", color: "#7eb8f7", border: "1px solid rgba(126,184,247,0.25)" }
              }
            >
              {exercise.type}
            </span>
          )}
        </div>
      )}

      {!finished && exercise?.addedWeight ? (
        <p className="text-[10px] uppercase tracking-widest mt-1.5" style={{ color: "var(--accent)" }}>
          +{exercise.addedWeight} kg
        </p>
      ) : null}

      <div className="flex items-baseline gap-1.5 mt-2.5">
        <p
          className="text-3xl font-bold tabular-nums leading-none"
          style={{ color: finished ? "#4ade80" : "var(--foreground)", fontFamily: "Geist Variable, monospace" }}
        >
          {reps}
        </p>
        {target > 0 && (
          <p className="text-sm font-medium" style={{ color: "var(--faint)" }}>/ {target}</p>
        )}
      </div>

      <div className="h-1.5 rounded-full mt-2.5 overflow-hidden" style={{ background: "var(--secondary)" }}>
        <div
          className="h-full rounded-full transition-all duration-300"
          style={{ width: `${pct}%`, background: dot, boxShadow: `0 0 10px ${dot}` }}
        />
      </div>

      <div className="flex items-center gap-1.5 mt-4">
        <button className={`${repButton} px-2.5`} disabled={disabled || finished} style={repDecrementStyle} onClick={() => onAdjust(-1)}>−1</button>
        <button className={repButton} disabled={disabled || finished} style={repIncrementStyle} onClick={() => onAdjust(1)}>+1</button>
        <button className={repButton} disabled={disabled || finished} style={repIncrementStyle} onClick={() => onAdjust(2)}>+2</button>
        <button className={repButton} disabled={disabled || finished} style={repIncrementStyle} onClick={() => onAdjust(5)}>+5</button>
        <button
          onClick={onFinish}
          disabled={disabled || finished}
          className="ml-auto px-3 py-1.5 rounded text-sm font-medium transition-colors disabled:opacity-30"
          style={{ background: "rgba(74,222,128,0.12)", color: "#4ade80", border: "1px solid rgba(74,222,128,0.25)" }}
        >
          Finish
        </button>
      </div>
    </div>
  );
}
