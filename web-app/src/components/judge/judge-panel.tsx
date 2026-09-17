import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import type { Match, MatchProgress } from "@/data/matches";
import type { Athlete } from "@/data/athletes";
import type { RoutineOverview } from "@/data/routines";
import { routineGroups } from "@/utils/exercise-labels";
import { AthleteCard } from "./judge-info-cards";
import { RoutineCard } from "./judge-info-cards";
import { JudgeTimerRow } from "./judge-exercise-cards";
import { CurrentExerciseCard, NextExerciseCard, PendingStartCard, FinishedCard } from "./judge-exercise-cards";
import { sideColor, currentGroupIndexFor, currentItemIndexFor, type Side } from "./judge-utils";

interface JudgePanelProps {
  side: Side;
  match: Match;
  progress: MatchProgress | null;
  athlete?: Athlete;
  clubName?: string | null;
  overview?: RoutineOverview;
  reps: number;
  onAdjust: (delta: number) => void;
  onFinish: () => void;
}

export function JudgePanel({ side, match, progress, athlete, clubName, overview, reps, onAdjust, onFinish }: JudgePanelProps) {
  const [showFinishConfirm, setShowFinishConfirm] = useState(false);
  const accent = sideColor(side);
  const exercises = overview?.exercises ?? [];
  const groups = routineGroups(exercises);

  const currentExerciseId = side === "RED" ? progress?.redCurrentExerciseId : progress?.blueCurrentExerciseId;
  const groupIdx = currentGroupIndexFor(exercises, currentExerciseId);
  const { itemIndex, isSuperset } = currentItemIndexFor(exercises, currentExerciseId);
  const currentGroup = groups[groupIdx];
  const currentItem = currentGroup?.items[itemIndex];
  const currentLabel = currentItem
    ? currentItem.addedWeight
      ? `${currentItem.name} (+${currentItem.addedWeight}KG)`
      : currentItem.name
    : currentGroup?.label ?? "Exercise";
  const nextItem = currentGroup?.items[itemIndex + 1];
  const nextGroup = groups[groupIdx + 1];
  const nextLabel = nextItem
    ? `${nextItem.targetReps} ${nextItem.name}`
    : nextGroup?.label;

  const finished = side === "RED" ? !!progress?.redFinishedAt : !!progress?.blueFinishedAt;
  const finishedIso = side === "RED" ? progress?.redFinishedAt : progress?.blueFinishedAt;
  const running = match.status === "RUNNING";
  const ended = match.status === "FINISHED";

  const finishedElapsed = (() => {
    const end = finishedIso ?? (ended ? match.finishedAt : null);
    const start = progress?.timerStartedAt ?? match.startedAt;
    if (!end || !start) return null;
    return Math.max(0, new Date(end).getTime() - new Date(start).getTime());
  })();

  const targetReps = currentGroup?.items.find((e) => e.id === currentExerciseId)?.targetReps ?? null;

  if (!athlete) return null;

  return (
    <div className="flex flex-col flex-1 min-h-0 gap-3">
      <AthleteCard athlete={athlete} side={side} clubName={clubName} finished={finished} />

      {overview && (
        <RoutineCard routine={overview} currentGroupIndex={groupIdx} accentColor={accent} />
      )}

      <JudgeTimerRow
        startedAt={progress?.timerStartedAt ?? match.startedAt}
        finishedAt={ended ? match.finishedAt : finishedIso}
        timeCapSeconds={overview?.timeCapSeconds}
      />

      {!running && !finished && !ended && <PendingStartCard />}
      {(finished || ended) && <FinishedCard elapsedMs={finishedElapsed} />}
      {running && !finished && (
        <>
          <CurrentExerciseCard
            label={currentLabel ?? "Exercise"}
            reps={reps}
            targetReps={targetReps}
            accentColor={accent}
            enabled={running}
            onDecrement={() => onAdjust(-1)}
            onIncrement={() => onAdjust(1)}
            isSuperset={isSuperset}
            itemIndex={itemIndex}
            totalItems={currentGroup?.items.length ?? 1}
          />
          {nextLabel && (
            <NextExerciseCard nextLabel={nextLabel} />
          )}
        </>
      )}

      {!ended && (
        <Button
          disabled={!running || finished}
          onClick={() => setShowFinishConfirm(true)}
          className="w-full h-12 text-sm font-semibold mt-auto"
          style={{ background: accent, color: "#fff" }}
        >
          Finish
        </Button>
      )}

      <Dialog open={showFinishConfirm} onOpenChange={(v) => setShowFinishConfirm(v)}>
        <DialogContent style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
          <DialogHeader>
            <DialogTitle>Force finish athlete</DialogTitle>
            <DialogDescription>
              Are you sure you want to finish <span className="font-semibold">{athlete.name}</span>? This will end the match and the opponent wins.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowFinishConfirm(false)}>Cancel</Button>
            <Button
              onClick={() => { setShowFinishConfirm(false); onFinish(); }}
              style={{ background: accent, color: "#fff" }}
            >
              Finish
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}