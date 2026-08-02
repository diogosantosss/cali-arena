
// ========== USERS ==========

export type UserRole = "JUDGE" | "ADMIN";

export interface User {
  id: number,
  username: string,
  role: UserRole,
  createdAt: string,
}

export interface CreateUserInput {
  username: string;
  password: string;
}

export interface LoginInput {
  username: string;
  password: string;
}

export interface LoginOutput {
  token: string;
}

// ========== ATHLETES ==========

export type Gender = "MALE" | "FEMALE";

export interface CreateAthleteInput {
  name: string;
  gender: Gender;
  clubId: number;
}

export interface Athlete {
  id: number;
  name: string;
  gender: Gender;
  clubId: number;
  createdAt: string;
}

export interface UpdateAthleteInput {
  name: string;
  gender: Gender;
  clubId: number;
}

// ========== CLUBS ==========

export interface CreateClubInput {
  name: string;
  shortName: string;
}

export interface Club {
  id: number;
  name: string;
  shortName: string;
  createdAt: string;
}

// ========== Routines ==========

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

// ========== Tournaments ==========

export interface CreateTournamentInput {
  name: string;
  location: string | null;
  startDate: string | null;
  endDate: string | null;
}

export type TournamentStatus = "DRAFT" | "READY" | "LIVE" | "FINISHED";

export interface Tournament {
  id: number;
  name: string;
  location: string | null;
  startDate: string | null;
  endDate: string | null;
  status: TournamentStatus;
  createdAt: string;
}

export type ScreenState = "WAITING" | "ROUTINES" | "BATTLE" | "WINNER" | "LEADERBOARD";
export interface TournamentState {
  id: number;
  tournamentId: number;
  currentScreen: ScreenState;
  currentMatchId: number | null;
  updatedAt: string;
}

export interface UpdateScreenInput {
  screen: ScreenState;
  currentMatchId: number | null;
}

export type BracketStage = "QUALIFIERS" | "QUARTERFINALS" | "SEMIFINALS" | "FINALS";

export interface CreateBracketInput {
  tournamentId: number;
  gender: Gender;
  stage: BracketStage;
}

export interface Bracket {
  id: number;
  tournamentId: number;
  gender: Gender;
  stage: BracketStage;
  createdAt: string;
}

export interface BracketOverview {
  bracket: Bracket;
  matches: Match[];
}

// ========== Matches ==========

export type MatchStatus = "PENDING" | "READY" | "RUNNING" | "PAUSED" | "FINISHED";

export interface Match {
  id: number;
  bracketId: number;
  routineId: number;
  judgeId: number;  

  athleteRedId: number | null;
  athleteBlueId: number | null;

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

export interface AssignAthletesInput {
  athleteRedId: number;
  athleteBlueId: number;
}

export interface UpdateRepsInput {
  repReps: number | null;
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

// ========== Screen Routine ==========
export interface ScreenRoutine {
  id: number;
  tournamentId: number;
  routineId: number;
  displayOrder: number;
  isVisible: boolean;
  label: string | null;
  createdAt: number;
  updatedAt: number;
}

export interface CreateScreenRoutineInput {
  routineId: number;
  displayOrder: number;
  label?: string;
}