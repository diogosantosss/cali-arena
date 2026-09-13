import { apiClient } from "@/api/client";
import type {
  CreateExerciseInput,
  CreateRoutineInput,
  Exercise,
  Routine,
  RoutineOverview,
  UpdateExerciseInput,
} from "@/data/routines";

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

  updateExercise(id: number, input: UpdateExerciseInput): Promise<Exercise> {
    return apiClient.put(`/routines/exercises/${id}`, input);
  },

  deleteExercise(id: number): Promise<void> {
    return apiClient.delete(`/routines/exercises/${id}`);
  },

  /** NOTE: backend identifies routines by name, not id. */
  getRoutineOverview(routineName: string): Promise<RoutineOverview> {
    return apiClient.get(`/routines/${routineName}/overview`);
  },
};
