import { apiClient } from "@/lib/api/client";

import type {
  CreateMatchInput,
  Match,
  MatchProgress,
  UpdateRepsInput,
} from "../types";

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

  getMatchById(matchId: number): Promise<Match> {
    return apiClient.get(`/matches/${matchId}`);
  },

  getProgressByMatchId(matchId: number): Promise<MatchProgress> {
    return apiClient.get(`/matches/${matchId}/progress`);
  },

  getMatchesByBracketId(bracketId: number): Promise<Match[]> {
    return apiClient.get(`/matches/bracket/${bracketId}`);
  },
};
