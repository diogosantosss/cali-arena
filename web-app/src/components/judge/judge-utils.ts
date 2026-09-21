import type { Exercise } from "@/data/routines";
import { routineGroups } from "@/utils/exercise-labels";

export type Side = "RED" | "BLUE";

export const SIDE_COLORS: Record<Side, string> = {
  RED: "#e05555",
  BLUE: "#5588e0",
};

export function sideColor(side: Side): string {
  return SIDE_COLORS[side];
}

export function sideLabel(side: Side): string {
  return side === "RED" ? "RED" : "BLUE";
}

export function sideToLower(side: Side): "red" | "blue" {
  return side.toLowerCase() as "red" | "blue";
}

export function formatSeconds(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

export function formatTimeSmooth(ms: number): string {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  const millis = ms % 1000;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}`;
}

export function currentGroupIndexFor(
  exercises: Exercise[],
  exerciseId: number | null | undefined,
): number {
  if (exerciseId == null) return 0;
  const groups = routineGroups(exercises);
  const index = groups.findIndex((group) => group.items.some((e) => e.id === exerciseId));
  return index < 0 ? 0 : index;
}

export function currentItemIndexFor(
  exercises: Exercise[],
  exerciseId: number | null | undefined,
): { groupIndex: number; itemIndex: number; isSuperset: boolean } {
  if (exerciseId == null) return { groupIndex: 0, itemIndex: 0, isSuperset: false };
  const groups = routineGroups(exercises);
  const groupIndex = groups.findIndex((group) => group.items.some((e) => e.id === exerciseId));
  if (groupIndex < 0) return { groupIndex: 0, itemIndex: 0, isSuperset: false };
  const group = groups[groupIndex];
  const itemIndex = group.items.findIndex((e) => e.id === exerciseId);
  return { groupIndex, itemIndex: itemIndex < 0 ? 0 : itemIndex, isSuperset: group.items.length > 1 };
}