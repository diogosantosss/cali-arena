import { useEffect, useReducer, useCallback, useRef } from "react";
import { useParams } from "react-router-dom";
import { ApiError } from "@/lib/api/client";
import "./spectator-theme.css";
import { useSpectatorSSE } from "./hooks/use-spectator-sse";
import { tournamentsService } from "@/features/tournaments/services/tournaments.service";
import type { BracketLeaderboard, ScreenRoutine, Tournament, TournamentBracketsSummary, TournamentState } from "@/features/tournaments/types";
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
  const leaderboardReqRef = useRef(0);
  const bracketSummaryReqRef = useRef(0);
  const leaderboardBracketRef = useRef<number | null>(null);
  const bracketSummaryDivisionRef = useRef<string | null>(null);

  const refreshLeaderboard = useCallback(async (bracketId: number) => {
    const requestId = ++leaderboardReqRef.current;
    if (leaderboardBracketRef.current !== bracketId) {
      leaderboardBracketRef.current = bracketId;
      dispatch({ type: "setLeaderboard", leaderboard: null });
    }
    try {
      const leaderboard = await tournamentsService.getBracketLeaderboard(bracketId);
      if (requestId === leaderboardReqRef.current) {
        dispatch({ type: "setLeaderboard", leaderboard });
      }
    } catch {
      // keep previous leaderboard if refresh fails
    }
  }, []);

  const refreshBrackets = useCallback(async (division: string) => {
    const requestId = ++bracketSummaryReqRef.current;
    if (bracketSummaryDivisionRef.current !== division) {
      bracketSummaryDivisionRef.current = division;
      dispatch({ type: "setBracketSummary", summary: null });
    }
    try {
      const summary = await tournamentsService.getBracketSummary(id, division);
      if (requestId === bracketSummaryReqRef.current) {
        dispatch({ type: "setBracketSummary", summary });
      }
    } catch {
      // keep previous summary if refresh fails
    }
  }, [id]);

  const loadRoutines = useCallback(async () => {
    try {
      const routines = await routinesService.getRoutines();
      dispatch({ type: "setRoutines", routines });

      await Promise.all(
        routines.map(async (r) => {
          const overview = await routinesService.getRoutineOverview(r.name);
          dispatch({ type: "setOverview", routineName: r.name, overview });
        })
      );
    } catch {
      // keep previous routines if refresh fails
    }
  }, []);

  const loadScreenRoutines = useCallback(async () => {
    try {
      const screenRoutines = await tournamentsService.getScreenRoutines(id);
      dispatch({ type: "setScreenRoutines", screenRoutines });
    } catch {
      // keep previous screen routines if refresh fails
    }
  }, [id]);

  const loadAthletes = useCallback(async () => {
    try {
      const athletes = await athletesService.getAthletes();
      dispatch({ type: "setAthletes", athletes });
    } catch {
      // keep previous athletes if refresh fails
    }
  }, []);

  useEffect(() => {
    async function loadBase() {
      try {
        const [tournament, tournamentState] = await Promise.all([
          tournamentsService.getTournamentById(id),
          tournamentsService.getTournamentState(id).catch(() => null),
        ]);
        dispatch({ type: "setTournament", tournament });
        if (tournamentState) dispatch({ type: "setTournamentState", state: tournamentState });
      } catch (err) {
        if (err instanceof ApiError) dispatch({ type: "setError", message: err.message });
      }
    }
    loadBase();
  }, [id]);

  const currentScreen = state.tournamentState?.currentScreen;
  const currentBracketId = state.tournamentState?.currentBracketId ?? null;
  const currentDivision = state.tournamentState?.currentDivision ?? null;
  const currentMatchId = state.tournamentState?.currentMatchId ?? null;

  useEffect(() => {
    if (currentScreen === "BATTLE") {
      void loadAthletes();
      void loadRoutines();
    } else if (currentScreen === "ROUTINES") {
      void loadScreenRoutines();
      void loadRoutines();
    } else if (currentScreen === "LEADERBOARD") {
      if (currentBracketId) {
        void refreshLeaderboard(currentBracketId);
      } else {
        dispatch({ type: "setLeaderboard", leaderboard: null });
      }
    } else if (currentScreen === "BRACKETS") {
      if (currentDivision) {
        void refreshBrackets(currentDivision);
      } else {
        dispatch({ type: "setBracketSummary", summary: null });
      }
    }
  }, [currentScreen, currentBracketId, currentDivision, loadScreenRoutines, loadRoutines, loadAthletes, refreshLeaderboard, refreshBrackets]);

  useEffect(() => {
    if (currentMatchId === currentMatchIdRef.current) return;
    currentMatchIdRef.current = currentMatchId;
    if (!currentMatchId) return;

    dispatch({ type: "resetMatchData" });
    let cancelled = false;
    (async () => {
      const [match, progress] = await Promise.all([
        matchesService.getMatchById(currentMatchId),
        matchesService.getProgressByMatchId(currentMatchId).catch(() => null),
      ]);
      if (!cancelled) {
        dispatch({ type: "setCurrentMatch", match });
        dispatch({ type: "setMatchProgress", progress });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [currentMatchId]);

  const handleSSEEvent = useCallback((event: SpectatorEvent) => {
    switch (event.action) {
      case "TOURNAMENT_STATE_UPDATED": {
        dispatch({ type: "setTournamentState", state: event.state });
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
        if (event.matchProgress.matchId !== currentMatchIdRef.current) break;
        dispatch({ type: "setMatchProgress", progress: event.matchProgress });
        break;
      }
    }
  }, []);

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
      <p className="text-[var(--spec-text-faint)] text-lg uppercase tracking-widest">{screen}</p>
    </div>
  );
}