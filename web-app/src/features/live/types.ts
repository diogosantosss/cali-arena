import type { ScreenRoutine, TournamentState } from "@/features/tournaments/types";
import type { MatchProgress } from "@/features/matches/types";
import type { RoutineOverview } from "@/features/routines/types";

export const SPECTATOR_ACTIONS = [
  "TOURNAMENT_STATE_UPDATED",
  "SCREEN_ROUTINES_CREATED",
  "SCREEN_ROUTINES_UPDATED",
  "SCREEN_ROUTINES_DELETED",
  "MATCH_UPDATED",
  "KEEP_ALIVE",
] as const;

export type SpectatorAction = typeof SPECTATOR_ACTIONS[number];

interface SpectatorEventBase {
  tournamentId: number;
  action: SpectatorAction;
}

export interface TournamentStateUpdatedEvent extends SpectatorEventBase {
  action: "TOURNAMENT_STATE_UPDATED";
  state: TournamentState;
}

export interface ScreenRoutinesEvent extends SpectatorEventBase {
  action: "SCREEN_ROUTINES_CREATED" | "SCREEN_ROUTINES_UPDATED";
  screenRoutine: ScreenRoutine;
  routineOverview: RoutineOverview | null;
}

export interface ScreenRoutineDeletedEvent extends SpectatorEventBase {
  action: "SCREEN_ROUTINES_DELETED";
  screenRoutineId: number;
}

export interface MatchUpdatedEvent extends SpectatorEventBase {
  action: "MATCH_UPDATED";
  matchProgress: MatchProgress;
}

export type SpectatorEvent =
  | TournamentStateUpdatedEvent
  | ScreenRoutinesEvent
  | ScreenRoutineDeletedEvent
  | MatchUpdatedEvent;
