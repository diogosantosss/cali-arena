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

export interface RoutineOverview {
  name: string;
  timeCapSeconds: number | null;
  createdAt: string;
  exercises: Exercise[];
}
