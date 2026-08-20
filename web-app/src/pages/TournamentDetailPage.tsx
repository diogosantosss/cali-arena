import { useReducer, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { api, ApiError } from "@/api";
import type { Athlete, Bracket, BracketStage, Gender, Match, Routine, RoutineOverview, Tournament, TournamentState, User } from "@/types";
import { Skeleton } from "@/components/ui/skeleton";
import { ArrowLeft, MapPin, CalendarDays } from "lucide-react";
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
    case "setTournament": return { ...state, tournament: action.tournament, tournamentLoading: false, tournamentError: null };
    case "setTournamentLoading": return { ...state, tournamentLoading: true, tournamentError: null };
    case "setTournamentError": return { ...state, tournamentLoading: false, tournamentError: action.message };
    case "setTournamentState": return { ...state, tournamentState: action.state };
    case "setBrackets": return { ...state, brackets: action.brackets };
    case "addBracket": return { ...state, brackets: [...state.brackets, action.bracket] };
    case "setMatches": return { ...state, matches: action.matches };
    case "addMatch": return { ...state, matches: [...state.matches, action.match] };
    case "updateMatch": return { ...state, matches: state.matches.map((m) => m.id === action.match.id ? action.match : m) };
    case "setAthletes": return { ...state, athletes: action.athletes };
    case "setRoutines": return { ...state, routines: action.routines };
    case "setJudges": return { ...state, judges: action.judges };
    case "setOverview": return { ...state, overviews: { ...state.overviews, [action.routineName]: action.overview } };
    default: throw new Error("Unknown action");
  }
}

const statusStyles: Record<Tournament["status"], { label: string; color: string; bg: string }> = {
  DRAFT:    { label: "Draft",    color: "#6b6560", bg: "rgba(107,101,96,0.12)" },
  READY:    { label: "Ready",    color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  LIVE:     { label: "Live",     color: "#e8a020", bg: "rgba(232,160,32,0.12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
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

      const allMatches = await Promise.all(brackets.map((b) => api.getMatchesByBracketId(b.id)));
      dispatch({ type: "setMatches", matches: allMatches.flat() });

      await Promise.all(
        routines.map(async (r) => {
          const overview = await api.getRoutineOverview(r.name);
          dispatch({ type: "setOverview", routineName: r.name, overview });
        })
      );
    } catch (err) {
      dispatch({ type: "setTournamentError", message: err instanceof ApiError ? err.message : "Failed to load tournament" });
    }
  }

  useEffect(() => { loadTournament(); }, [tournamentId]);

  async function handleCreateBracket(gender: Gender, stage: BracketStage) {
    try {
      const bracket = await api.createBracket({ tournamentId, gender, stage });
      dispatch({ type: "addBracket", bracket });
    } catch (err) { console.error(err); }
  }

  async function handleStartMatch(match: Match) {
    try {
      await api.startMatch(match.id);
      const updated = await api.getMatchById(match.id);
      dispatch({ type: "updateMatch", match: updated });
    } catch (err) { console.error(err); }
  }

  if (state.tournamentLoading) {
    return (
      <div className="max-w-5xl mx-auto space-y-6">
        <Skeleton className="h-6 w-48" style={{ background: "#252528" }} />
        <Skeleton className="h-24 w-full" style={{ background: "#252528" }} />
        <Skeleton className="h-64 w-full" style={{ background: "#252528" }} />
      </div>
    );
  }

  if (state.tournamentError) {
    return (
      <div className="max-w-5xl mx-auto">
        <p className="text-sm text-destructive">{state.tournamentError}</p>
      </div>
    );
  }

  if (!state.tournament) return null;

  const s = statusStyles[state.tournament.status];

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
              {state.tournament.name}
            </h1>
            <div className="flex items-center gap-4">
              {state.tournament.location && (
                <span className="flex items-center gap-1.5 text-xs" style={{ color: "#6b6560" }}>
                  <MapPin className="w-3 h-3" />
                  {state.tournament.location}
                </span>
              )}
              {state.tournament.startDate && (
                <span className="flex items-center gap-1.5 text-xs" style={{ color: "#6b6560" }}>
                  <CalendarDays className="w-3 h-3" />
                  {new Date(state.tournament.startDate).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}
                  {state.tournament.endDate && (
                    <> — {new Date(state.tournament.endDate).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}</>
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
        state={state.tournamentState}
        matches={state.matches}
        athletes={state.athletes}
        routines={state.routines}
        overviews={state.overviews}
        onUpdated={(s) => dispatch({ type: "setTournamentState", state: s })}
      />

      <div
        className="h-px w-full"
        style={{ background: "#252528" }}
      />

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
