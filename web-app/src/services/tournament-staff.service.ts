import { apiClient } from "@/api/client";

export interface StaffMember {
  id: number;
  username: string;
  role: "ADMIN" | "HOST" | "JUDGE";
}

export interface TournamentStaff {
  tournamentId: number;
  host: StaffMember | null;
  judges: StaffMember[];
}

export interface MyTournaments {
  hosting: Array<{ id: number; name: string }>;
  judging: Array<{ id: number; name: string }>;
}

export const tournamentStaffService = {
  async getStaff(tournamentId: number): Promise<TournamentStaff> {
    return apiClient.get(`/tournaments/${tournamentId}/staff`);
  },

  async assignHost(tournamentId: number, userId: number): Promise<TournamentStaff> {
    return apiClient.put(`/tournaments/${tournamentId}/staff/host`, { userId });
  },

  async addJudge(tournamentId: number, userId: number): Promise<TournamentStaff> {
    return apiClient.post(`/tournaments/${tournamentId}/staff/judges`, { userId });
  },

  async removeJudge(tournamentId: number, userId: number): Promise<void> {
    return apiClient.delete(`/tournaments/${tournamentId}/staff/judges/${userId}`);
  },

  async getMyTournaments(): Promise<MyTournaments> {
    return apiClient.get("/tournaments/mine");
  },

  async getAvailableJudges(): Promise<StaffMember[]> {
    return apiClient.get("/users/judges");
  },

  async getAvailableHosts(): Promise<StaffMember[]> {
    const users = await apiClient.get<StaffMember[]>("/users");
    return users.filter((user) => user.role === "HOST");
  },
};