import { useReducer, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { api, ApiError } from "@/api";
import type { Athlete, Bracket, BracketStage, Gender, Match, Routine, RoutineOverview, Tournament, TournamentState, User } from "@/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import { ArrowLeft, MapPin, Calendar } from "lucide-react";
import { BracketView } from "./tournament/BracketView";
import { ScreenControl } from "./tournament/ScreenControl";


interface State {
  tournament: Tournament | null;
  tournamentLoading: boolean;
  tournamentError: string | null;
  tournamentState: TournamentState | null;
  brackets: Bracket[];
  matches: Match[];
  athletes: Athlete[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
  judges: User[];
}

type Action =
  | { type: "setTournament"; tournament: Tournament }
  | { type: "setTournamentLoading" }
  | { type: "setTournamentError"; message: string }
  | { type: "setTournamentState"; state: TournamentState }
  | { type: "setBrackets"; brackets: Bracket[] }
  | { type: "addBracket"; bracket: Bracket }
  | { type: "setMatches"; matches: Match[] }
  | { type: "addMatch"; match: Match }
  | { type: "updateMatch"; match: Match }
  | { type: "setAthletes"; athletes: Athlete[] }
  | { type: "setRoutines"; routines: Routine[] }
  | { type: "setOverview"; routineName: string; overview: RoutineOverview }
  | { type: "setJudges"; judges: User[] };

const initialState: State = {
  tournament: null,
  tournamentLoading: false,
  tournamentError: null,
  tournamentState: null,
  brackets: [],
  matches: [],
  athletes: [],
  routines: [],
  overviews: {},
  judges: [],
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setTournament":
      return { ...state, tournament: action.tournament, tournamentLoading: false, tournamentError: null };
    case "setTournamentLoading":
      return { ...state, tournamentLoading: true, tournamentError: null };
    case "setTournamentError":
      return { ...state, tournamentLoading: false, tournamentError: action.message };
    case "setTournamentState":
      return { ...state, tournamentState: action.state };
    case "setBrackets":
      return { ...state, brackets: action.brackets };
    case "addBracket":
      return { ...state, brackets: [...state.brackets, action.bracket] };
    case "setMatches":
      return { ...state, matches: action.matches };
    case "addMatch":
      return { ...state, matches: [...state.matches, action.match] };
    case "updateMatch":
      return { ...state, matches: state.matches.map((m) => m.id === action.match.id ? action.match : m) };
    case "setAthletes":
      return { ...state, athletes: action.athletes };
    case "setRoutines":
      return { ...state, routines: action.routines };
    case "setJudges":
      return { ...state, judges: action.judges };
    case "setOverview":
  return { ...state, overviews: { ...state.overviews, [action.routineName]: action.overview } };
    default:
      throw new Error("Unknown action");
  }
}

const statusVariant: Record<Tournament["status"], "default" | "secondary" | "outline" | "destructive"> = {
  DRAFT: "outline",
  READY: "secondary",
  LIVE: "default",
  FINISHED: "destructive",
};

export function TournamentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [state, dispatch] = useReducer(reducer, initialState);

  const tournamentId = Number(id);

  async function loadTournament() {
    dispatch({ type: "setTournamentLoading" });
    try {
      const [tournament, tournamentState, brackets, athletes, routines, users] = await Promise.all([
        api.getTournamentById(tournamentId),
        api.getTournamentState(tournamentId),
        api.getBracketsByTournamentId(tournamentId),
        api.getAthletes(),
        api.getRoutines(),
        api.getUsers(),
      ]);
      dispatch({ type: "setTournament", tournament });
      dispatch({ type: "setTournamentState", state: tournamentState });
      dispatch({ type: "setBrackets", brackets });
      dispatch({ type: "setAthletes", athletes });
      dispatch({ type: "setRoutines", routines });
      dispatch({ type: "setJudges", judges: users.filter((u) => u.role === "JUDGE") });

      const allMatches = await Promise.all(
        brackets.map((b) => api.getMatchesByBracketId(b.id))
      );
      dispatch({ type: "setMatches", matches: allMatches.flat() });
      
      await Promise.all(
        routines.map(async (r) => {
          const overview = await api.getRoutineOverview(r.name);
          dispatch({ type: "setOverview", routineName: r.name, overview });
        })
      );
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "setTournamentError", message: err.message });
      } else {
        dispatch({ type: "setTournamentError", message: "Failed to load tournament" });
      }
    }
  }

  useEffect(() => {
    loadTournament();
  }, [tournamentId]);

  async function handleCreateBracket(gender: Gender, stage: BracketStage) {
    try {
      const bracket = await api.createBracket({ tournamentId, gender, stage });
      dispatch({ type: "addBracket", bracket });
    } catch (err) {
      console.error(err);
    }
  }

  async function handleStartMatch(match: Match) {
    try {
      await api.startMatch(match.id);
      const updated = await api.getMatchById(match.id);
      dispatch({ type: "updateMatch", match: updated });
    } catch (err) {
      console.error(err);
    }
  }

  if (state.tournamentLoading) {
    return (
      <div className="max-w-4xl space-y-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (state.tournamentError) {
    return (
      <div className="max-w-4xl">
        <p className="text-sm text-destructive">{state.tournamentError}</p>
      </div>
    );
  }

  if (!state.tournament) return null;

  return (
    <div className="max-w-4xl space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate("/dashboard")}>
          <ArrowLeft className="w-4 h-4" />
        </Button>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold">{state.tournament.name}</h1>
            <Badge variant={statusVariant[state.tournament.status]} className="capitalize">
              {state.tournament.status.toLowerCase()}
            </Badge>
          </div>
          <div className="flex items-center gap-4 mt-1">
            {state.tournament.location && (
              <span className="text-sm text-muted-foreground flex items-center gap-1">
                <MapPin className="w-3.5 h-3.5" />
                {state.tournament.location}
              </span>
            )}
            {state.tournament.startDate && (
              <span className="text-sm text-muted-foreground flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5" />
                {new Date(state.tournament.startDate).toLocaleDateString()}
                {state.tournament.endDate && (
                  <> — {new Date(state.tournament.endDate).toLocaleDateString()}</>
                )}
              </span>
            )}
          </div>
        </div>
      </div>

      <ScreenControl
        tournamentId={tournamentId}
        state={state.tournamentState}
        matches={state.matches}
        athletes={state.athletes}
        routines={state.routines}
        overviews={state.overviews}
        onUpdated={(s) => dispatch({ type: "setTournamentState", state: s })}
      />

      <Separator />

      <BracketView
        brackets={state.brackets}
        matches={state.matches}
        athletes={state.athletes}
        routines={state.routines}
        judges={state.judges}
        onCreateBracket={handleCreateBracket}
        onMatchCreated={(match) => dispatch({ type: "addMatch", match })}
        onMatchUpdated={(match) => dispatch({ type: "updateMatch", match })}
        onStartMatch={handleStartMatch}
      />
    </div>
  );
}