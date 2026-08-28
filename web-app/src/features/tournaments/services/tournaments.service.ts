import { apiClient } from "@/lib/api/client";
import type {
  Bracket,
  BracketLeaderboard,
  BracketOverview,
  CreateBracketInput,
  CreateScreenRoutineInput,
  CreateTournamentInput,
  ScreenRoutine,
  Tournament,
  TournamentBracketsSummary,
  TournamentState,
  UpdateScreenInput,
} from "../types";

export const tournamentsService = {
  createTournament(input: CreateTournamentInput): Promise<Tournament> {
    return apiClient.post("/tournaments", input);
  },

  getTournaments(): Promise<Tournament[]> {
    return apiClient.get("/tournaments");
  },

  getTournamentById(id: number): Promise<Tournament> {
    return apiClient.get(`/tournaments/${id}`);
  },

  getTournamentState(tournamentId: number): Promise<TournamentState> {
    return apiClient.get(`/tournaments/${tournamentId}/state`);
  },

  updateScreen(tournamentId: number, input: UpdateScreenInput): Promise<TournamentState> {
    return apiClient.put(`/tournaments/${tournamentId}/state/screen`, input);
  },

  createBracket(input: CreateBracketInput): Promise<Bracket> {
    return apiClient.post("/brackets", input);
  },

  getBracketsByTournamentId(tournamentId: number): Promise<Bracket[]> {
    return apiClient.get(`/brackets/tournament/${tournamentId}`);
  },

  getBracketOverview(tournamentId: number, division: string): Promise<BracketOverview> {
    return apiClient.get(`/brackets/tournament/${tournamentId}/division/${division}/overview`);
  },

  getBracketLeaderboard(bracketId: number): Promise<BracketLeaderboard> {
    return apiClient.get(`/brackets/${bracketId}/leaderboard`);
  },

  getBracketSummary(tournamentId: number, division: string): Promise<TournamentBracketsSummary> {
    return apiClient.get(`/brackets/tournament/${tournamentId}/summary?division=${division}`);
  },

  createScreenRoutine(tournamentId: number, input: CreateScreenRoutineInput): Promise<ScreenRoutine> {
    return apiClient.post(`/tournaments/${tournamentId}/screen-routines`, input);
  },

  getScreenRoutines(tournamentId: number): Promise<ScreenRoutine[]> {
    return apiClient.get(`/tournaments/${tournamentId}/screen-routines`);
  },

  updateScreenRoutineVisibility(
    tournamentId: number,
    id: number,
    isVisible: boolean
  ): Promise<ScreenRoutine> {
    return apiClient.patch(`/tournaments/${tournamentId}/screen-routines/${id}/visibility`, {
      isVisible,
    });
  },

  updateScreenRoutineDisplayOrder(
    tournamentId: number,
    id: number,
    displayOrder: number
  ): Promise<ScreenRoutine> {
    return apiClient.patch(`/tournaments/${tournamentId}/screen-routines/${id}/order`, {
      displayOrder,
    });
  },

  deleteScreenRoutine(tournamentId: number, id: number): Promise<void> {
    return apiClient.delete(`/tournaments/${tournamentId}/screen-routines/${id}`);
  },
};
