import { getErrorDescription } from "./errorDescriptions";
import type { Athlete, Bracket, BracketOverview, Club, CreateAthleteInput, CreateBracketInput, CreateClubInput, CreateExerciseInput, CreateRoutineInput, CreateTournamentInput, CreateUserInput, Exercise, LoginInput, LoginOutput, Routine, RoutineOverview, Tournament, TournamentState, UpdateScreenInput, User } from "./types";

const API_BASE_URL = "/api";

class ApiError extends Error {
  public status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem("token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  await delay(1000)
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  // Log the upcoming response and req info
  console.log(`Request to ${API_BASE_URL}${endpoint} with response status: ${response.status}`);

  if (!response.ok) {
    const error = await response
      .json()
      .catch(() => ({ title: "Unknown error" }));
    const errorMessage = error.title
      ? getErrorDescription(error.title)
      : response.statusText;
    throw new ApiError(response.status, errorMessage);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

export const api = {
  // user-related API calls
  async createUser(
    input: CreateUserInput
  ): Promise<User> {
    return fetchApi<User>("/users", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async login(
    input: LoginInput
  ): Promise<LoginOutput> {
    return fetchApi<LoginOutput>("/users/token", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  // athelete-related API calls
  async createAthlete(
    input: CreateAthleteInput
  ): Promise<Athlete> {
    return fetchApi<Athlete>("/athletes", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async getAthletes(): Promise<Athlete[]> {
    return fetchApi<Athlete[]>("/athletes", {
      method: "GET",
    });
  },

  async getAthleteById(id: number): Promise<Athlete> {
    return fetchApi<Athlete>(`/athletes/${id}`, {
      method: "GET",
    });
  },

  async getAthletesByClubId(clubId: number): Promise<Athlete[]> {
    return fetchApi<Athlete[]>(`/athletes/club/${clubId}`, {
      method: "GET",
    });
  },

  async getAthletesByGender(gender: string): Promise<Athlete[]> {
    return fetchApi<Athlete[]>(`/athletes/gender/${gender}`, {
      method: "GET",
    });
  },

  // club-related API calls
  async createClub(
    input: CreateClubInput
  ): Promise<Club> {
    return fetchApi<Club>("/clubs", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async getClubs(): Promise<Club[]> {
    return fetchApi<Club[]>("/clubs", {
      method: "GET",
    });
  },

  async getClubById(id: number): Promise<Club> {
    return fetchApi<Club>(`/clubs/${id}`, {
      method: "GET",
    });
  },

  // routines-related API calls
  async createRoutine(
    input: CreateRoutineInput
  ): Promise<Routine> {
    return fetchApi<Routine>("/routines", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async createExercise(
    input: CreateExerciseInput
  ): Promise<Exercise> {
    return fetchApi<Exercise>("/exercises", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async getRoutineOverview(
    routineName: number
  ): Promise<RoutineOverview> {
    return fetchApi<RoutineOverview>(`/routines/${routineName}/overview`, {
      method: "GET",
    });
  },

  // tournament-related API calls
  async createTournament(
    input: CreateTournamentInput
  ): Promise<Tournament> {
    return fetchApi<Tournament>("/tournaments", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async getTournaments(): Promise<Tournament[]> {
    return fetchApi<Tournament[]>("/tournaments", {
      method: "GET",
    });
  },

  async getTournamentById(id: number): Promise<Tournament> {
    return fetchApi<Tournament>(`/tournaments/${id}`, {
      method: "GET",
    });
  },

  async getTournamentState(
    tournamentId: number
  ): Promise<TournamentState> {
    return fetchApi<TournamentState>(`tournaments/${tournamentId}/state`, {
      method: "GET"
    })
  },

  async createBracket(
    input: CreateBracketInput
  ): Promise<Bracket> {
    return fetchApi<Bracket>("/brackets", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async getBracketsByTournamentId(tournamentId: number): Promise<Bracket[]> {
    return fetchApi<Bracket[]>(`/tournaments/${tournamentId}/brackets`, {
      method: "GET",
    });
  },

  async getBracketOverview(
    tournamentId: number,
    gender: string,
  ): Promise<BracketOverview> {
    return fetchApi<BracketOverview>(`/tournaments/${tournamentId}/bracket/${gender}/overview`, {
      method: "GET",
    });
  },

  async updateScreen(
    tournamentId: number,
    input: UpdateScreenInput
  ): Promise<TournamentState> {
    return fetchApi<TournamentState>(`tournaments/${tournamentId}/state/screen`, {
      method: "PUT",
      body: JSON.stringify(input)
    })
  },

};

export { ApiError };

/**
 * Delays execution for a specified number of milliseconds. 
 */
function delay(delayInMs: number) {
  return new Promise((resolve) => {
    setTimeout(() => resolve(undefined), delayInMs);
  });
}
