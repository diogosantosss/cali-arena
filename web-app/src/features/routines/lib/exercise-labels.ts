import type { Exercise } from "../types";

export function exerciseAbbreviation(name: string): string {
  return name
    .trim()
    .split(/[^A-Za-z0-9]+/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase())
    .join("");
}

function formatFullItem(targetReps: number, name: string, addedWeight?: number | null): string {
  const weight = addedWeight ? ` (+${addedWeight}KG)` : "";
  return `${targetReps} ${name}${weight}`;
}

function formatSupersetItem(targetReps: number, name: string, addedWeight?: number | null): string {
  const weight = addedWeight ? ` (+${addedWeight}KG)` : "";
  return `${targetReps} ${exerciseAbbreviation(name)}${weight}`;
}

export interface RoutineGroup {
  order: number;
  items: Exercise[];
  label: string;
}

export function routineGroups(exercises: Exercise[]): RoutineGroup[] {
  const sorted = [...exercises].sort((a, b) => a.exerciseOrder - b.exerciseOrder);
  const groups = new Map<number, Exercise[]>();
  for (const exercise of sorted) {
    const list = groups.get(exercise.exerciseOrder) ?? [];
    list.push(exercise);
    groups.set(exercise.exerciseOrder, list);
  }
  return [...groups.values()].map((group) => {
    const items = [...group].sort((a, b) => (a.supersetOrder ?? 0) - (b.supersetOrder ?? 0));
    const label =
      items.length > 1
        ? items.map((e) => formatSupersetItem(e.targetReps, e.name, e.addedWeight)).join(" - ")
        : formatFullItem(items[0].targetReps, items[0].name, items[0].addedWeight);
    return { order: items[0].exerciseOrder, items, label };
  });
}

export function nextLabel(
  exercises: Exercise[],
  currentExerciseId: number | null | undefined
): string | null {
  if (currentExerciseId == null) return null;

  const groups = routineGroups(exercises);
  const current = groups.find((g) => g.items.some((e) => e.id === currentExerciseId));
  if (!current) return null;

  const index = current.items.findIndex((e) => e.id === currentExerciseId);
  const remaining = current.items.slice(index + 1);
  if (remaining.length > 0) {
    return remaining.map((e) => formatSupersetItem(e.targetReps, e.name, e.addedWeight)).join(" - ");
  }

  return groups.find((g) => g.order > current.order)?.label ?? null;
}

export interface ExerciseProgress {
  pct: number;
  fraction: string;
}

export function exerciseProgress(
  exercises: Exercise[],
  currentExerciseId: number | null | undefined,
  currentReps: number
): ExerciseProgress {
  const groups = routineGroups(exercises);
  const current = groups.find((g) => g.items.some((e) => e.id === currentExerciseId));
  if (!current) return { pct: 0, fraction: "0/0" };

  if (current.items.length > 1) {
    const index = current.items.findIndex((e) => e.id === currentExerciseId);
    const currentItem = current.items[index];
    const total = current.items.reduce((sum, e) => sum + e.targetReps, 0);
    const doneBefore = current.items.slice(0, index).reduce((sum, e) => sum + e.targetReps, 0);
    const done = doneBefore + Math.min(currentReps, currentItem.targetReps);
    return { pct: Math.round((done / total) * 100), fraction: `${done}/${total}` };
  }

  const target = current.items[0].targetReps;
  const done = Math.min(currentReps, target);
  return { pct: Math.round((done / target) * 100), fraction: `${done}/${target}` };
}
