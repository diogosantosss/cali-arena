import { apiClient } from "@/lib/api/client";
import type { Club, CreateClubInput } from "../types";

export const clubsService = {
  getClubs(): Promise<Club[]> {
    return apiClient.get("/clubs");
  },

  getClubById(id: number): Promise<Club> {
    return apiClient.get(`/clubs/${id}`);
  },

  createClub(input: CreateClubInput): Promise<Club> {
    return apiClient.post("/clubs", input);
  },
};
