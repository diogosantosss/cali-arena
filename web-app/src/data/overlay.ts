import type { Athlete } from "@/data/athletes";
import type { Match, MatchProgress } from "@/data/matches";
import type { BracketStage } from "@/data/tournaments";
import type { Exercise, Routine } from "@/data/routines";
import { overlayExerciseItems, routineGroups, type OverlayExerciseItem } from "@/utils/exercise-labels";

export interface OverlaySide {
  name: string;
  color: string;
  items: OverlayExerciseItem[];
  currentReps: number;
  targetReps: number;
  finished: boolean;
  won: boolean;
  time: string | null;
}

export interface OverlayData {
  red: OverlaySide | null;
  blue: OverlaySide | null;
  time: string;
  timerStartedAt: string | null;
  finished: boolean;
  stage: string | null;
  timeCap: string | null;
}

export interface OverlayProps {
  data: OverlayData;
  mode?: "static" | "live";
  connected?: boolean;
  variant?: "band" | "plate" | "bare" | "bar" | "solid" | "card";
}

const stageLabel: Record<BracketStage, string> = {
  QUALIFIERS: "Qualifiers",
  QUARTERFINALS: "Quarterfinals",
  SEMIFINALS: "Semifinals",
  FINALS: "Finals",
};

function cap(seconds: number | null | undefined): string | null {
  if (!seconds) return null;
  const min = Math.floor(seconds / 60);
  const sec = seconds % 60;
  return sec > 0 ? `${min}' ${sec}"` : `${min}'`;
}

function msToClock(ms: number): string {
  const total = Math.floor(ms / 1000);
  const rest = ms % 1000;
  return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}.${String(rest).padStart(3, "0")}`;
}

function sideOf(
  athlete: Athlete | undefined,
  match: Match,
  progress: MatchProgress | null,
  exercises: Exercise[]
): OverlaySide | null {
  if (!athlete) return null;

  const isRed = match.athleteRedId === athlete.id;
  const finishedAt = isRed ? progress?.redFinishedAt : progress?.blueFinishedAt;
  const finished = !!finishedAt;

  const otherFinishedAt = isRed ? progress?.blueFinishedAt : progress?.redFinishedAt;
  const isHeadToHead = match.athleteRedId != null && match.athleteBlueId != null;
  const won = finished
    ? !isHeadToHead || !otherFinishedAt
      ? true
      : new Date(finishedAt!).getTime() < new Date(otherFinishedAt!).getTime()
    : false;

  const groups = routineGroups(exercises);
  const currentId = isRed ? progress?.redCurrentExerciseId : progress?.blueCurrentExerciseId;
  const currentReps = isRed ? progress?.redCurrentReps : progress?.blueCurrentReps;
  const group = groups.find((g) => g.items.some((e) => e.id === currentId)) ?? groups[0];
  const items = overlayExerciseItems(exercises, currentId);
  const current = group?.items.find((e) => e.id === currentId) ?? group?.items[0];

  const ms =
    finished && progress?.timerStartedAt
      ? Math.max(0, new Date(finishedAt!).getTime() - new Date(progress.timerStartedAt).getTime())
      : null;

  return {
    name: athlete.name,
    color: isRed ? "var(--spec-red)" : "var(--spec-blue)",
    items,
    currentReps: currentReps ?? 0,
    targetReps: current?.targetReps ?? 0,
    finished,
    won,
    time: ms == null ? null : msToClock(ms),
  };
}

export function buildOverlay(
  match: Match,
  progress: MatchProgress | null,
  athletes: Athlete[],
  routines: Routine[],
  exercises: Exercise[],
  stage: BracketStage | null
): OverlayData {
  const sorted = exercises.slice().sort((a, b) => a.exerciseOrder - b.exerciseOrder);
  const routine = routines.find((r) => r.id === match.routineId);

  const red = sideOf(athletes.find((a) => a.id === match.athleteRedId), match, progress, sorted);
  const blue = sideOf(athletes.find((a) => a.id === match.athleteBlueId), match, progress, sorted);

  const redDone = !!progress?.redFinishedAt;
  const blueDone = !!progress?.blueFinishedAt;
  const both = (redDone && blueDone) || (!!match.athleteRedId !== !!match.athleteBlueId);

  const elapsedMs = (() => {
    if (!progress?.timerStartedAt) return null;
    if (both && progress) {
      return Math.max(
        0,
        Math.max(
          redDone && progress.redFinishedAt ? new Date(progress.redFinishedAt).getTime() : 0,
          blueDone && progress.blueFinishedAt ? new Date(progress.blueFinishedAt).getTime() : 0
        ) - new Date(progress.timerStartedAt).getTime()
      );
    }
    return null;
  })();

  return {
    red,
    blue,
    time: elapsedMs == null ? "00:00.000" : msToClock(elapsedMs),
    timerStartedAt: both ? null : (progress?.timerStartedAt ?? null),
    finished: both,
    stage: stage ? stageLabel[stage] : (routine?.name ?? null),
    timeCap: cap(routine?.timeCapSeconds),
  };
}
