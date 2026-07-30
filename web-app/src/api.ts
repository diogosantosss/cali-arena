import { getErrorDescription } from "./errorDescriptions";
import type { AssignAthletesInput, Athlete, Bracket, BracketOverview, Club, CreateAthleteInput, CreateBracketInput, CreateClubInput, CreateExerciseInput, CreateMatchInput, CreateRoutineInput, CreateTournamentInput, CreateUserInput, Exercise, LoginInput, LoginOutput, Match, MatchProgress, Routine, RoutineOverview, Tournament, TournamentState, UpdateRepsInput, UpdateScreenInput, User } from "./types";

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
  await delay(1000) // simulate network latency
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
  async getUsers(): Promise<User[]> {
    return fetchApi<User[]>(`/users`, {
      method: "GET",
    })
  },

  async createUser(
    input: CreateUserInput
  ): Promise<User> {
    return fetchApi<User>(`/users`, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async createToken(
    input: LoginInput
  ): Promise<LoginOutput> {
    return fetchApi<LoginOutput>(`/users/token`, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async logout(): Promise<void> {
    return fetchApi<void>(`/users/logout`, {
      method: "POST",
      headers: getAuthHeaders(),
    });
  },

  async getMe(): Promise<User> {
    return fetchApi<User>(`/users/me`, {
      headers: getAuthHeaders(),
    });
  },

  // athelete-related API calls
  async createAthlete(
    input: CreateAthleteInput
  ): Promise<Athlete> {
    return fetchApi<Athlete>(`/athletes`, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async getAthletes(): Promise<Athlete[]> {
    return fetchApi<Athlete[]>(`/athletes`, {
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
    return fetchApi<Club>(`/clubs`, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async getClubs(): Promise<Club[]> {
    return fetchApi<Club[]>(`/clubs`, {
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
    return fetchApi<Bracket>(`/brackets`, {
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

  // match-related API calls
  async createMatch(
    input: CreateMatchInput
  ): Promise<Match> {
    return fetchApi<Match>(`matches`, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async assignAthletesToMatch(
    matchId: number,
    input: AssignAthletesInput
  ): Promise<Match> {
    return fetchApi<Match>(`matches/${matchId}/athletes`, {
      method: "PUT",
      body: JSON.stringify(input),
    });
  },

  async startMatch(
    matchId: number
  ): Promise<MatchProgress> {
    return fetchApi<MatchProgress>(`matches/${matchId}/start`, {
      method: "PUT",
    });
  },

  async updateMatchReps(
    matchId: number,
    input: UpdateRepsInput
  ): Promise<MatchProgress> {
    return fetchApi<MatchProgress>(`matches/${matchId}/reps`, {
      method: "PUT",
      body: JSON.stringify(input),
    });
  },

  async getMatchById(
    matchId: number
  ): Promise<Match> {
    return fetchApi<Match>(`matches/${matchId}`, {
      method: "GET"
    });
  },

  async getProgressByMatchId(
    matchId: number
  ): Promise<MatchProgress> {
    return fetchApi<MatchProgress>(`matches/${matchId}/progress`, {
      method: "GET"
    });
  },

  async getMatchesByBracketId(
    bracketId: number
  ): Promise<Match[]> {
    return fetchApi<Match[]>(`matches/bracket/${bracketId}`, {
      method: "GET"
    });
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
