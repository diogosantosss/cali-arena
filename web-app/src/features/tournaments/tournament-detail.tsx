import { useCallback, useEffect, useReducer } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ApiError } from "@/lib/api/client";
import { tournamentsService } from "./services/tournaments.service";
import type { Bracket, BracketStage, Tournament, TournamentState } from "./types";
import type { Gender } from "@/types/gender";
import { athletesService } from "@/features/athletes/services/athletes.service";
import type { Athlete } from "@/features/athletes/types";
import { routinesService } from "@/features/routines/services/routines.service";
import type { Routine, RoutineOverview } from "@/features/routines/types";
import { usersService } from "@/features/users/services/users.service";
import type { User } from "@/features/users/types";
import { matchesService } from "@/features/matches/services/matches.service";
import type { Match } from "@/features/matches/types";
import { ScreenControl } from "./components/screen-control";
import { BracketView } from "@/features/matches/components/bracket-view";
import { Skeleton } from "@/components/ui/skeleton";
import { ArrowLeft, MapPin, CalendarDays } from "lucide-react";

const statusStyles: Record<Tournament["status"], { label: string; color: string; bg: string }> = {
  DRAFT:    { label: "Draft",    color: "#6b6560", bg: "rgba(107,101,96,0.12)" },
  READY:    { label: "Ready",    color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  LIVE:     { label: "Live",     color: "#e8a020", bg: "rgba(232,160,32,0.12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
};

interface DetailState {
  loading: boolean;
  error: string | null;
  tournament: Tournament | null;
  state: TournamentState | null;
  brackets: Bracket[];
  matches: Match[];
  athletes: Athlete[];
  routines: Routine[];
  judges: User[];
  overviews: Record<string, RoutineOverview>;
}

type Action =
  | { type: "loadStart" }
  | {
      type: "loadSuccess";
      tournament: Tournament;
      state: TournamentState | null;
      brackets: Bracket[];
      matches: Match[];
      athletes: Athlete[];
      routines: Routine[];
      judges: User[];
      overviews: Record<string, RoutineOverview>;
    }
  | { type: "loadError"; message: string }
  | { type: "stateUpdated"; state: TournamentState }
  | { type: "bracketCreated"; bracket: Bracket }
  | { type: "matchCreated"; match: Match }
  | { type: "matchUpdated"; match: Match };

const initialDetailState: DetailState = {
  loading: false,
  error: null,
  tournament: null,
  state: null,
  brackets: [],
  matches: [],
  athletes: [],
  routines: [],
  judges: [],
  overviews: {},
};

function reducer(state: DetailState, action: Action): DetailState {
  switch (action.type) {
    case "loadStart":
      return { ...initialDetailState, loading: true };
    case "loadSuccess":
      return {
        loading: false,
        error: null,
        tournament: action.tournament,
        state: action.state,
        brackets: action.brackets,
        matches: action.matches,
        athletes: action.athletes,
        routines: action.routines,
        judges: action.judges,
        overviews: action.overviews,
      };
    case "loadError":
      return { ...state, loading: false, error: action.message };
    case "stateUpdated":
      return { ...state, state: action.state };
    case "bracketCreated":
      return { ...state, brackets: [...state.brackets, action.bracket] };
    case "matchCreated":
      return { ...state, matches: [...state.matches, action.match] };
    case "matchUpdated":
      return {
        ...state,
        matches: state.matches.map((m) => (m.id === action.match.id ? action.match : m)),
      };
  }
}

export function TournamentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const tournamentId = Number(id);

  const [data, dispatch] = useReducer(reducer, initialDetailState);
  const {
    loading: tournamentLoading,
    error: tournamentError,
    tournament,
    state: tournamentState,
    brackets,
    matches,
    athletes,
    routines,
    judges,
    overviews,
  } = data;

  const loadTournament = useCallback(async () => {
    dispatch({ type: "loadStart" });
    try {
      const [loadedTournament, loadedState, loadedBrackets, loadedAthletes, loadedRoutines, users] =
        await Promise.all([
          tournamentsService.getTournamentById(tournamentId),
          tournamentsService.getTournamentState(tournamentId),
          tournamentsService.getBracketsByTournamentId(tournamentId),
          athletesService.getAthletes(),
          routinesService.getRoutines(),
          usersService.getUsers(),
        ]);

      const allMatches = await Promise.all(
        loadedBrackets.map((b) => matchesService.getMatchesByBracketId(b.id))
      );

      const loadedOverviews = await Promise.all(
        loadedRoutines.map(async (r) => {
          const overview = await routinesService.getRoutineOverview(r.name);
          return [r.name, overview] as const;
        })
      );

      dispatch({
        type: "loadSuccess",
        tournament: loadedTournament,
        state: loadedState,
        brackets: loadedBrackets,
        matches: allMatches.flat(),
        athletes: loadedAthletes,
        routines: loadedRoutines,
        judges: users.filter((u) => u.role === "JUDGE"),
        overviews: Object.fromEntries(loadedOverviews),
      });
    } catch (err) {
      dispatch({
        type: "loadError",
        message: err instanceof ApiError ? err.message : "Failed to load tournament",
      });
    }
  }, [tournamentId]);

  useEffect(() => {
    // deferred so the loading state flip doesn't happen synchronously in the effect
    const handle = setTimeout(() => {
      void loadTournament();
    }, 0);
    return () => clearTimeout(handle);
  }, [loadTournament]);

  async function handleCreateBracket(gender: Gender, stage: BracketStage) {
    try {
      const bracket = await tournamentsService.createBracket({ tournamentId, gender, stage });
      dispatch({ type: "bracketCreated", bracket });
    } catch (err) {
      console.error(err);
    }
  }

  async function handleStartMatch(match: Match) {
    try {
      await matchesService.startMatch(match.id);
      const updated = await matchesService.getMatchById(match.id);
      dispatch({ type: "matchUpdated", match: updated });
    } catch (err) {
      console.error(err);
    }
  }

  if (tournamentLoading && !tournament) {
    return (
      <div className="max-w-5xl mx-auto space-y-6">
        <Skeleton className="h-6 w-48" style={{ background: "#252528" }} />
        <Skeleton className="h-24 w-full" style={{ background: "#252528" }} />
        <Skeleton className="h-64 w-full" style={{ background: "#252528" }} />
      </div>
    );
  }

  if (tournamentError) {
    return (
      <div className="max-w-5xl mx-auto">
        <p className="text-sm text-destructive">{tournamentError}</p>
      </div>
    );
  }

  if (!tournament) return null;

  const s = statusStyles[tournament.status];

  return (
    <div className="max-w-5xl mx-auto space-y-8 animate-fade-up" style={{ opacity: 0 }}>
      <div className="space-y-4">
        <button
          onClick={() => navigate("/dashboard")}
          className="flex items-center gap-1.5 text-xs transition-colors"
          style={{ color: "#6b6560" }}
          onMouseEnter={(e) => (e.currentTarget.style.color = "#a09a92")}
          onMouseLeave={(e) => (e.currentTarget.style.color = "#6b6560")}
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          All tournaments
        </button>

        <div className="flex items-start justify-between">
          <div className="space-y-2">
            <h1
              className="text-4xl leading-tight"
              style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "#f0ede8" }}
            >
              {tournament.name}
            </h1>
            <div className="flex items-center gap-4">
              {tournament.location && (
                <span className="flex items-center gap-1.5 text-xs" style={{ color: "#6b6560" }}>
                  <MapPin className="w-3 h-3" />
                  {tournament.location}
                </span>
              )}
              {tournament.startDate && (
                <span className="flex items-center gap-1.5 text-xs" style={{ color: "#6b6560" }}>
                  <CalendarDays className="w-3 h-3" />
                  {new Date(tournament.startDate).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}
                  {tournament.endDate && (
                    <> — {new Date(tournament.endDate).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}</>
                  )}
                </span>
              )}
            </div>
          </div>

          <span
            className="text-xs px-2.5 py-1 rounded-full mt-1"
            style={{ background: s.bg, color: s.color }}
          >
            {s.label}
          </span>
        </div>
      </div>

      <div
        className="h-px w-full"
        style={{ background: "#252528" }}
      />

      <ScreenControl
        tournamentId={tournamentId}
        state={tournamentState}
        matches={matches}
        athletes={athletes}
        routines={routines}
        overviews={overviews}
        onUpdated={(updated) => dispatch({ type: "stateUpdated", state: updated })}
      />

      <div
        className="h-px w-full"
        style={{ background: "#252528" }}
      />

      <BracketView
        brackets={brackets}
        matches={matches}
        athletes={athletes}
        routines={routines}
        judges={judges}
        onCreateBracket={handleCreateBracket}
        onMatchCreated={(match) => dispatch({ type: "matchCreated", match })}
        onMatchUpdated={(match) => dispatch({ type: "matchUpdated", match })}
        onStartMatch={handleStartMatch}
      />
    </div>
  );
}
