export interface CreateRoutineInput {
  name: string;
  timeCapSeconds: number | null;
}

export interface Routine {
  id: number;
  name: string;
  timeCapSeconds: number | null;
  createdAt: string;
}

export type ExerciseType = "NORMAL" | "UNBROKEN" | "SUPERSET";

export interface Exercise {
  id: number;
  routineId: number;
  name: string;
  targetReps: number;
  addedWeight: number | null;
  exerciseOrder: number;
  supersetOrder: number | null;
  type: ExerciseType;
}

export interface CreateExerciseInput {
  routineId: number;
  name: string;
  targetReps: number;
  addedWeight: number | null;
  exerciseOrder: number;
  supersetOrder: number | null;
  type: ExerciseType;
}

export type UpdateExerciseInput = Omit<CreateExerciseInput, "routineId">;

export const EXERCISE_NAMES = [
  "Muscle-Up",
  "Pull-Up",
  "Squat",
  "Straight-Bar-Dip",
  "Push-Up",
  "Low-Bar-Push-Up",
] as const;

export interface RoutineOverview {
  name: string;
  timeCapSeconds: number | null;
  createdAt: string;
  exercises: Exercise[];
}
