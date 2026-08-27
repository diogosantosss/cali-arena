import type { Gender } from "@/types/gender";
import type { Match } from "@/features/matches/types";

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
  currentBracketId: number | null;
  updatedAt: string;
}

export interface UpdateScreenInput {
  screen: ScreenState;
  currentMatchId: number | null;
  currentBracketId: number | null;
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

export interface LeaderboardEntry {
  athleteName: string;
  duration: string;
  matchId: number;
}

export interface BracketLeaderboard {
  bracketId: number;
  gender: Gender;
  stage: BracketStage;
  entries: LeaderboardEntry[];
}

// ========== Screen Routines ==========

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
