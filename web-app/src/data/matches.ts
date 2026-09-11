export type MatchStatus = "PENDING" | "READY" | "RUNNING" | "PAUSED" | "FINISHED";

export interface Match {
  id: number;
  bracketId: number;
  routineId: number;
  judgeId: number;

  athleteRedId: number;
  athleteBlueId: number;

  winnerAthleteId: number | null;

  status: MatchStatus;
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
}

export interface CreateMatchInput {
  bracketId: number;
  routineId: number;
  judgeId: number;

  athleteRedId: number;
  athleteBlueId: number;
}

export interface UpdateRepsInput {
  redReps: number | null;
  blueReps: number | null;
}

export interface MatchProgress {
  id: number;
  matchId: number;

  redCurrentExerciseId: number | null;
  blueCurrentExerciseId: number | null;

  redCurrentReps: number;
  blueCurrentReps: number;

  redFinishedAt: string | null;
  blueFinishedAt: string | null;

  timerStartedAt: string | null;
  timerRemainingSeconds: number | null;

  updatedAt: string;
}
