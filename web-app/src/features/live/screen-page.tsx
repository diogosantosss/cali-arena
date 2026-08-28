import { useEffect, useReducer, useCallback, useRef } from "react";
import { useParams } from "react-router-dom";
import { ApiError } from "@/lib/api/client";
import { useSpectatorSSE } from "./hooks/use-spectator-sse";
import { tournamentsService } from "@/features/tournaments/services/tournaments.service";
import type { BracketLeaderboard, ScreenRoutine, ScreenState, Tournament, TournamentBracketsSummary, TournamentState } from "@/features/tournaments/types";
import { routinesService } from "@/features/routines/services/routines.service";
import type { Routine, RoutineOverview } from "@/features/routines/types";
import type { Athlete } from "@/features/athletes/types";
import { athletesService } from "@/features/athletes/services/athletes.service";
import { matchesService } from "@/features/matches/services/matches.service";
import type { Match, MatchProgress } from "@/features/matches/types";
import type { SpectatorEvent } from "./types";
import { screenBackground } from "./lib/screen-background";
import { BattleScreen } from "./components/battle-screen";
import { BracketsScreen } from "./components/brackets-screen";
import { RoutinesScreen } from "./components/routines-screen";
import { LeaderboardScreen } from "./components/leaderboard-screen";
import { ScreenLoading } from "./components/screen-loading";
import { WaitingScreen } from "./components/waiting-screen";

interface State {
  tournament: Tournament | null;
  tournamentState: TournamentState | null;
  screenRoutines: ScreenRoutine[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
  matches: Match[];
  currentMatch: Match | null;
  matchProgress: MatchProgress | null;
  leaderboard: BracketLeaderboard | null;
  bracketSummary: TournamentBracketsSummary | null;
  athletes: Athlete[];
  error: string | null;
}

type Action =
  | { type: "setTournament"; tournament: Tournament }
  | { type: "setTournamentState"; state: TournamentState }
  | { type: "setScreenRoutines"; screenRoutines: ScreenRoutine[] }
  | { type: "upsertScreenRoutine"; screenRoutine: ScreenRoutine }
  | { type: "removeScreenRoutine"; id: number }
  | { type: "setRoutines"; routines: Routine[] }
  | { type: "setOverview"; routineName: string; overview: RoutineOverview }
  | { type: "setMatches"; matches: Match[] }
  | { type: "setCurrentMatch"; match: Match }
  | { type: "setMatchProgress"; progress: MatchProgress | null }
  | { type: "setLeaderboard"; leaderboard: BracketLeaderboard | null }
  | { type: "setBracketSummary"; summary: TournamentBracketsSummary | null }
  | { type: "resetMatchData" }
  | { type: "setAthletes"; athletes: Athlete[] }
  | { type: "setError"; message: string };

const initialState: State = {
  tournament: null,
  tournamentState: null,
  screenRoutines: [],
  routines: [],
  overviews: {},
  matches: [],
  currentMatch: null,
  matchProgress: null,
  leaderboard: null,
  bracketSummary: null,
  athletes: [],
  error: null,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setTournament": return { ...state, tournament: action.tournament };
    case "setTournamentState": return { ...state, tournamentState: action.state };
    case "setScreenRoutines": return { ...state, screenRoutines: action.screenRoutines };
    case "upsertScreenRoutine": {
      const exists = state.screenRoutines.some((r) => r.id === action.screenRoutine.id);
      return {
        ...state,
        screenRoutines: exists
          ? state.screenRoutines.map((r) => r.id === action.screenRoutine.id ? action.screenRoutine : r)
          : [...state.screenRoutines, action.screenRoutine],
      };
    }
    case "removeScreenRoutine":
      return { ...state, screenRoutines: state.screenRoutines.filter((r) => r.id !== action.id) };
    case "setRoutines": return { ...state, routines: action.routines };
    case "setOverview": return { ...state, overviews: { ...state.overviews, [action.routineName]: action.overview } };
    case "setMatches": return { ...state, matches: action.matches };
    case "setCurrentMatch": return { ...state, currentMatch: action.match };
    case "setMatchProgress": return { ...state, matchProgress: action.progress };
    case "setLeaderboard": return { ...state, leaderboard: action.leaderboard };
    case "setBracketSummary": return { ...state, bracketSummary: action.summary };
    case "resetMatchData": return { ...state, currentMatch: null, matchProgress: null };
    case "setAthletes": return { ...state, athletes: action.athletes };
    case "setError": return { ...state, error: action.message };
    default: throw new Error("Unknown action");
  }
}

export function ScreenPage() {
  const { tournamentId } = useParams<{ tournamentId: string }>();
  const id = Number(tournamentId);
  const [state, dispatch] = useReducer(reducer, initialState);
  const currentMatchIdRef = useRef<number | null>(null);
  const screenRef = useRef<ScreenState | null>(null);
  const bracketIdRef = useRef<number | null>(null);
  const divisionRef = useRef<string | null>(null);

  useEffect(() => {
    currentMatchIdRef.current = state.currentMatch?.id ?? null;
  }, [state.currentMatch]);

  useEffect(() => {
    screenRef.current = state.tournamentState?.currentScreen ?? null;
    bracketIdRef.current = state.tournamentState?.currentBracketId ?? null;
    divisionRef.current = state.tournamentState?.currentDivision ?? null;
  }, [state.tournamentState]);

  const refreshLeaderboard = useCallback(async (bracketId: number) => {
    try {
      const leaderboard = await tournamentsService.getBracketLeaderboard(bracketId);
      dispatch({ type: "setLeaderboard", leaderboard });
    } catch {
      // keep previous leaderboard if refresh fails
    }
  }, []);

  const refreshBrackets = useCallback(async (division: string) => {
    try {
      const summary = await tournamentsService.getBracketSummary(id, division);
      dispatch({ type: "setBracketSummary", summary });
    } catch {
      // keep previous summary if refresh fails
    }
  }, [id]);

  useEffect(() => {
    async function loadBase() {
      try {
        const [tournament, screenRoutines, routines, brackets, athletes] = await Promise.all([
          tournamentsService.getTournamentById(id),
          tournamentsService.getScreenRoutines(id),
          routinesService.getRoutines(),
          tournamentsService.getBracketsByTournamentId(id),
          athletesService.getAthletes(),
        ]);
        dispatch({ type: "setTournament", tournament });
        dispatch({ type: "setScreenRoutines", screenRoutines });
        dispatch({ type: "setRoutines", routines });
        dispatch({ type: "setAthletes", athletes });

        const allMatches = await Promise.all(
          brackets.map((b) => matchesService.getMatchesByBracketId(b.id))
        );
        dispatch({ type: "setMatches", matches: allMatches.flat() });

        await Promise.all(
          routines.map(async (r) => {
            const overview = await routinesService.getRoutineOverview(r.name);
            dispatch({ type: "setOverview", routineName: r.name, overview });
          })
        );

        const tournamentState = await tournamentsService.getTournamentState(id);
        dispatch({ type: "setTournamentState", state: tournamentState });

        if (tournamentState.currentScreen === "LEADERBOARD" && tournamentState.currentBracketId) {
          void refreshLeaderboard(tournamentState.currentBracketId);
        }

        if (tournamentState.currentScreen === "BRACKETS" && tournamentState.currentDivision) {
          void refreshBrackets(tournamentState.currentDivision);
        }

        if (tournamentState.currentMatchId) {
          const [match, progress] = await Promise.all([
            matchesService.getMatchById(tournamentState.currentMatchId),
            matchesService.getProgressByMatchId(tournamentState.currentMatchId).catch(() => null),
          ]);
          dispatch({ type: "setCurrentMatch", match });
          if (progress) dispatch({ type: "setMatchProgress", progress });
        }
      } catch (err) {
        if (err instanceof ApiError) dispatch({ type: "setError", message: err.message });
      }
    }
    loadBase();
  }, [id, refreshLeaderboard, refreshBrackets]);

  const handleSSEEvent = useCallback(async (event: SpectatorEvent) => {
    switch (event.action) {
      case "TOURNAMENT_STATE_UPDATED": {
        dispatch({ type: "setTournamentState", state: event.state });
        if (event.state.currentScreen === "LEADERBOARD" && event.state.currentBracketId) {
          void refreshLeaderboard(event.state.currentBracketId);
        } else if (event.state.currentScreen !== "LEADERBOARD") {
          dispatch({ type: "setLeaderboard", leaderboard: null });
        }
        if (event.state.currentScreen === "BRACKETS" && event.state.currentDivision) {
          void refreshBrackets(event.state.currentDivision);
        } else if (event.state.currentScreen !== "BRACKETS") {
          dispatch({ type: "setBracketSummary", summary: null });
        }
        if (event.state.currentMatchId && event.state.currentMatchId !== currentMatchIdRef.current) {
          // limpa os dados do match anterior enquanto o novo carrega
          dispatch({ type: "resetMatchData" });
          const [match, progress] = await Promise.all([
            matchesService.getMatchById(event.state.currentMatchId),
            matchesService.getProgressByMatchId(event.state.currentMatchId).catch(() => null),
          ]);
          dispatch({ type: "setCurrentMatch", match });
          dispatch({ type: "setMatchProgress", progress });
        }
        break;
      }
      case "SCREEN_ROUTINES_CREATED":
      case "SCREEN_ROUTINES_UPDATED": {
        dispatch({ type: "upsertScreenRoutine", screenRoutine: event.screenRoutine });
        break;
      }
      case "SCREEN_ROUTINES_DELETED": {
        dispatch({ type: "removeScreenRoutine", id: event.screenRoutineId });
        break;
      }
      case "MATCH_UPDATED": {
        if (screenRef.current === "LEADERBOARD" && bracketIdRef.current) {
          void refreshLeaderboard(bracketIdRef.current);
        }
        if (screenRef.current === "BRACKETS" && divisionRef.current) {
          void refreshBrackets(divisionRef.current);
        }
        if (event.matchProgress.matchId !== currentMatchIdRef.current) break;
        dispatch({ type: "setMatchProgress", progress: event.matchProgress });
        break;
      }
    }
  }, [refreshLeaderboard, refreshBrackets]);

  useSpectatorSSE(id, handleSSEEvent);

  const screen = state.tournamentState?.currentScreen;

  if (!screen || screen === "WAITING") {
    return <WaitingScreen tournamentName={state.tournament?.name ?? "Cali Arena"} />;
  }

if (screen === "ROUTINES") {
    return (
      <RoutinesScreen
        tournamentName={state.tournament?.name ?? "Cali Arena"}
        screenRoutines={state.screenRoutines}
        routines={state.routines}
        overviews={state.overviews}
      />
    );
  }

if (screen === "BATTLE") {
    if (!state.currentMatch) {
      return <ScreenLoading label="Loading match..." />;
    }
    return (
      <BattleScreen
        tournamentName={state.tournament?.name ?? "Cali Arena"}
        match={state.currentMatch}
        progress={state.matchProgress}
        athletes={state.athletes}
        routines={state.routines}
        overviews={state.overviews}
      />
    );
  }

  if (screen === "LEADERBOARD") {
    if (!state.leaderboard) {
      return <ScreenLoading label="Loading leaderboard..." />;
    }

    return (
      <LeaderboardScreen
        tournamentName={state.tournament?.name ?? "Cali Arena"}
        leaderboard={state.leaderboard}
      />
    );
  }

  if (screen === "BRACKETS") {
    if (!state.bracketSummary) {
      return <ScreenLoading label="Loading brackets..." />;
    }

    return <BracketsScreen tournamentName={state.tournament?.name ?? "Cali Arena"} summary={state.bracketSummary} />;
  }

  return (
    <div className="min-h-screen flex items-center justify-center" style={screenBackground}>
      <p className="text-white/20 text-lg uppercase tracking-widest">{screen}</p>
    </div>
  );
}