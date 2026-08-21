import { apiClient } from "@/lib/api/client";
import type { Gender } from "@/types/gender";
import type {
  Bracket,
  BracketOverview,
  CreateBracketInput,
  CreateScreenRoutineInput,
  CreateTournamentInput,
  ScreenRoutine,
  Tournament,
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
    return apiClient.post("/tournaments/bracket", input);
  },

  getBracketsByTournamentId(tournamentId: number): Promise<Bracket[]> {
    return apiClient.get(`/tournaments/${tournamentId}/brackets`);
  },

  getBracketOverview(tournamentId: number, gender: Gender): Promise<BracketOverview> {
    return apiClient.get(`/tournaments/${tournamentId}/bracket/${gender}/overview`);
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
