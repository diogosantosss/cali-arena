import { apiClient } from "@/lib/api/client";
import type { Athlete, CreateAthleteInput } from "../types";

export const athletesService = {
  getAthletes(): Promise<Athlete[]> {
    return apiClient.get("/athletes");
  },

  getAthleteById(id: number): Promise<Athlete> {
    return apiClient.get(`/athletes/${id}`);
  },

  getAthletesByClubId(clubId: number): Promise<Athlete[]> {
    return apiClient.get(`/athletes/club/${clubId}`);
  },

  getAthletesByGender(gender: string): Promise<Athlete[]> {
    return apiClient.get(`/athletes/gender/${gender}`);
  },

  createAthlete(input: CreateAthleteInput): Promise<Athlete> {
    return apiClient.post("/athletes", input);
  },
};
