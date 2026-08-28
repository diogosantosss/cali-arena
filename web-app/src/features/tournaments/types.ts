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

export type ScreenState = "WAITING" | "ROUTINES" | "BATTLE" | "WINNER" | "LEADERBOARD" | "BRACKETS";

export interface TournamentState {
  id: number;
  tournamentId: number;
  currentScreen: ScreenState;
  currentMatchId: number | null;
  currentBracketId: number | null;
  currentDivision: string | null;
  updatedAt: string;
}

export interface UpdateScreenInput {
  screen: ScreenState;
  currentMatchId: number | null;
  currentBracketId: number | null;
  currentDivision: string | null;
}

export type BracketStage = "QUALIFIERS" | "QUARTERFINALS" | "SEMIFINALS" | "FINALS";

export interface CreateBracketInput {
  tournamentId: number;
  division: string;
  stage: BracketStage;
}

export interface Bracket {
  id: number;
  tournamentId: number;
  division: string;
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
  division: string;
  stage: BracketStage;
  entries: LeaderboardEntry[];
}

export interface BracketMatchSummary {
  matchId: number;
  startedAt: string | null;
  athleteRed: string;
  athleteBlue: string;
  winner: string;
}

export interface BracketSummary {
  stage: BracketStage;
  matches: BracketMatchSummary[];
}

export interface TournamentBracketsSummary {
  tournamentId: number;
  division: string;
  brackets: BracketSummary[];
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
