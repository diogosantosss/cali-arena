import { apiClient } from "@/lib/api/client";
import type {
  CreateExerciseInput,
  CreateRoutineInput,
  Exercise,
  Routine,
  RoutineOverview,
} from "../types";

export const routinesService = {
  getRoutines(): Promise<Routine[]> {
    return apiClient.get("/routines");
  },

  createRoutine(input: CreateRoutineInput): Promise<Routine> {
    return apiClient.post("/routines", input);
  },

  createExercise(input: CreateExerciseInput): Promise<Exercise> {
    return apiClient.post("/routines/exercises", input);
  },

  /** NOTE: backend identifies routines by name, not id. */
  getRoutineOverview(routineName: string): Promise<RoutineOverview> {
    return apiClient.get(`/routines/${routineName}/overview`);
  },
};
