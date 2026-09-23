import { apiClient } from "@/api/client";

import type {
  CreateMatchInput,
  Match,
  MatchProgress,
  UpdateRepsInput,
} from "@/data/matches";

export const matchesService = {
  createMatch(input: CreateMatchInput): Promise<Match> {
    return apiClient.post("/matches", input);
  },

  startMatch(matchId: number): Promise<MatchProgress> {
    return apiClient.put(`/matches/${matchId}/start`);
  },

  updateReps(matchId: number, input: UpdateRepsInput): Promise<MatchProgress> {
    return apiClient.put(`/matches/${matchId}/reps`, input);
  },

  deleteMatch(matchId: number): Promise<void> {
    return apiClient.delete(`/matches/${matchId}`);
  },

  getMatchById(matchId: number): Promise<Match> {
    return apiClient.get(`/matches/${matchId}`);
  },

  getProgressByMatchId(matchId: number): Promise<MatchProgress> {
    return apiClient.get(`/matches/${matchId}/progress`);
  },

  getMatches(): Promise<Match[]> {
    return apiClient.get("/matches");
  },

  getMatchesByBracketId(bracketId: number): Promise<Match[]> {
    return apiClient.get(`/matches/bracket/${bracketId}`);
  },
};
