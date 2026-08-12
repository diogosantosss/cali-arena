import { useEffect, useReducer, useCallback } from "react";
import { useParams } from "react-router-dom";
import { api, ApiError } from "@/api";
import { useSpectatorSSE } from "@/hooks/useSpectatorSSE";
import type {
  Athlete,
  Match,
  MatchProgress,
  Routine,
  RoutineOverview,
  ScreenRoutine,
  SpectatorEvent,
  Tournament,
  TournamentState,
} from "@/types";

interface State {
  tournament: Tournament | null;
  tournamentState: TournamentState | null;
  screenRoutines: ScreenRoutine[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
  matches: Match[];
  currentMatch: Match | null;
  matchProgress: MatchProgress | null;
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
  | { type: "setMatchProgress"; progress: MatchProgress }
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
    case "setAthletes": return { ...state, athletes: action.athletes };
    case "setError": return { ...state, error: action.message };
    default: throw new Error("Unknown action");
  }
}

function formatTime(ms: number): string {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  const millis = ms % 1000;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}`;
}

function useElapsedMs(timerStartedAt: string | null): number {
  const [elapsed, setElapsed] = useReducer((_: number, v: number) => v, 0);

  useEffect(() => {
    if (!timerStartedAt) return;
    const interval = setInterval(() => {
      setElapsed(Date.now() - new Date(timerStartedAt).getTime());
    }, 50);
    return () => clearInterval(interval);
  }, [timerStartedAt]);

  return elapsed;
}

export function ScreenPage() {
  const { tournamentId } = useParams<{ tournamentId: string }>();
  const id = Number(tournamentId);
  const [state, dispatch] = useReducer(reducer, initialState);

  useEffect(() => {
    async function loadBase() {
      try {
        const [tournament, screenRoutines, routines, brackets, athletes] = await Promise.all([
          api.getTournamentById(id),
          api.getScreenRoutines(id),
          api.getRoutines(),
          api.getBracketsByTournamentId(id),
          api.getAthletes(),
        ]);
        dispatch({ type: "setTournament", tournament });
        dispatch({ type: "setScreenRoutines", screenRoutines });
        dispatch({ type: "setRoutines", routines });
        dispatch({ type: "setAthletes", athletes });

        const allMatches = await Promise.all(brackets.map((b) => api.getMatchesByBracketId(b.id)));
        dispatch({ type: "setMatches", matches: allMatches.flat() });

        await Promise.all(
          routines.map(async (r) => {
            const overview = await api.getRoutineOverview(r.name);
            dispatch({ type: "setOverview", routineName: r.name, overview });
          })
        );

        const tournamentState = await api.getTournamentState(id);
        dispatch({ type: "setTournamentState", state: tournamentState });

        if (tournamentState.currentMatchId) {
          const [match, progress] = await Promise.all([
            api.getMatchById(tournamentState.currentMatchId),
            api.getProgressByMatchId(tournamentState.currentMatchId).catch(() => null),
          ]);
          dispatch({ type: "setCurrentMatch", match });
          if (progress) dispatch({ type: "setMatchProgress", progress });
        }
      } catch (err) {
        if (err instanceof ApiError) dispatch({ type: "setError", message: err.message });
      }
    }
    loadBase();
  }, [id]);

  const handleSSEEvent = useCallback(async (event: SpectatorEvent) => {
    switch (event.action) {
      case "TOURNAMENT_STATE_UPDATED": {
        dispatch({ type: "setTournamentState", state: event.state });
        if (event.state.currentMatchId) {
          const [match, progress] = await Promise.all([
            api.getMatchById(event.state.currentMatchId),
            api.getProgressByMatchId(event.state.currentMatchId).catch(() => null),
          ]);
          dispatch({ type: "setCurrentMatch", match });
          if (progress) dispatch({ type: "setMatchProgress", progress });
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
    }
  }, []);

  useSpectatorSSE(id, handleSSEEvent);

  const screen = state.tournamentState?.currentScreen;

  if (!screen || screen === "WAITING") {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ background: "radial-gradient(ellipse at top, #1a1a2e 0%, #0a0a0f 100%)" }}>
        <div className="text-center space-y-4">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#6339db] to-[#a855f7] flex items-center justify-center font-bold text-3xl text-white mx-auto">
            C
          </div>
          <p className="text-white/30 text-sm tracking-widest uppercase">Waiting</p>
        </div>
      </div>
    );
  }

  if (screen === "ROUTINES") {
    const visible = state.screenRoutines
      .filter((sr) => sr.isVisible)
      .sort((a, b) => a.displayOrder - b.displayOrder);

    return (
      <div className="min-h-screen flex flex-col" style={{ background: "radial-gradient(ellipse at top, #1a1a2e 0%, #0a0a0f 100%)", color: "white" }}>
        <div className="text-center pt-14 pb-10">
          <h1 className="text-7xl font-black uppercase tracking-tight mb-3">
            {state.tournament?.name ?? "Cali Arena"}
          </h1>
          <p className="text-xl font-semibold uppercase tracking-[0.3em]" style={{ color: "#6fa3ef" }}>
            Endurance Battles
          </p>
        </div>

        {visible.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <p className="text-white/20 uppercase tracking-widest text-sm">No routines configured</p>
          </div>
        ) : (
          <div className="flex-1 grid px-16" style={{ gridTemplateColumns: `repeat(${visible.length}, 1fr)` }}>
            {visible.map((sr) => {
              const routine = state.routines.find((r) => r.id === sr.routineId);
              const overview = routine ? state.overviews[routine.name] : null;
              return (
                <div key={sr.id} className="flex flex-col text-center px-8">
                  <h2 className="text-2xl font-black uppercase tracking-widest mb-10 text-white">
                    {sr.label ?? routine?.name ?? `Routine #${sr.routineId}`}
                  </h2>
                  <div className="space-y-5">
                    {overview?.exercises
                      .sort((a, b) => a.exerciseOrder - b.exerciseOrder)
                      .map((exercise) => (
                        <p key={exercise.id} className="text-2xl text-white font-medium">
                          {exercise.targetReps} {exercise.name}
                          {exercise.addedWeight ? ` (+${exercise.addedWeight}KG)` : ""}
                        </p>
                      ))}
                  </div>
                  {routine?.timeCapSeconds && (
                    <div className="mt-10 mx-auto px-6 py-3 rounded-xl" style={{ background: "rgba(255,255,255,0.07)" }}>
                      <p className="text-sm font-semibold uppercase tracking-[0.2em] text-white/60">
                        Time Cap — {Math.floor(routine.timeCapSeconds / 60)}M
                        {routine.timeCapSeconds % 60 > 0 ? ` ${routine.timeCapSeconds % 60}S` : ""}
                      </p>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  if (screen === "BATTLE") {
    if (!state.currentMatch) {
      return (
        <div className="min-h-screen flex items-center justify-center" style={{ background: "radial-gradient(ellipse at top, #1a1a2e 0%, #0a0a0f 100%)" }}>
          <p className="text-white/30 uppercase tracking-widest">Loading match...</p>
        </div>
      );
    }
    return <BattleScreen state={state} match={state.currentMatch} progress={state.matchProgress} athletes={state.athletes} />;
  }

  return (
    <div className="min-h-screen flex items-center justify-center" style={{ background: "radial-gradient(ellipse at top, #1a1a2e 0%, #0a0a0f 100%)" }}>
      <p className="text-white/20 text-lg uppercase tracking-widest">{screen}</p>
    </div>
  );
}

function BattleScreen({ state, match, progress, athletes }: {
  state: State;
  match: Match;
  progress: MatchProgress | null;
  athletes: Athlete[];
}) {
  const elapsed = useElapsedMs(progress?.timerStartedAt ?? null);

  const routine = state.routines.find((r) => r.id === match.routineId);
  const exercises = routine ? (state.overviews[routine.name]?.exercises.sort((a, b) => a.exerciseOrder - b.exerciseOrder) ?? []) : [];

  const redExercise = exercises.find((e) => e.id === progress?.redCurrentExerciseId) ?? exercises[0];
  const blueExercise = exercises.find((e) => e.id === progress?.blueCurrentExerciseId) ?? exercises[0];
  const redNext = exercises.find((e) => e.exerciseOrder === (redExercise?.exerciseOrder ?? -1) + 1);
  const blueNext = exercises.find((e) => e.exerciseOrder === (blueExercise?.exerciseOrder ?? -1) + 1);

  const redAthlete = athletes.find((a) => a.id === match.athleteRedId);
  const blueAthlete = athletes.find((a) => a.id === match.athleteBlueId);

  const redFinished = !!progress?.redFinishedAt;
  const blueFinished = !!progress?.blueFinishedAt;

  function finishTime(finishedAt: string) {
    if (!progress?.timerStartedAt) return "00:00.000";
    const ms = new Date(finishedAt).getTime() - new Date(progress.timerStartedAt).getTime();
    return formatTime(Math.max(0, ms));
  }

  return (
    <div
      className="min-h-screen flex flex-col"
      style={{ background: "radial-gradient(ellipse at top, #1a1a2e 0%, #0a0a0f 100%)", color: "white" }}
    >
      <div className="text-center pt-10 pb-6">
        <p className="text-lg font-semibold uppercase tracking-[0.3em]" style={{ color: "#6fa3ef" }}>
          {state.tournament?.name ?? "Cali Arena"}
        </p>
      </div>

      <div className="text-center mb-4">
        <p className="text-8xl font-black tabular-nums tracking-tight">
          {formatTime(elapsed)}
        </p>
      </div>

      <div className="text-center mb-8 space-y-1">
        <p className="text-xl font-bold text-white/80">{routine?.name}</p>
        <div className="space-y-1 mt-2">
          {exercises.map((e) => (
            <p key={e.id} className="text-sm text-white/40">
              {e.targetReps} {e.name}
              {e.addedWeight ? ` (+${e.addedWeight}KG)` : ""}
            </p>
          ))}
        </div>
        {routine?.timeCapSeconds && (
          <div className="inline-block mt-3 px-5 py-2 rounded-xl" style={{ background: "rgba(255,255,255,0.07)" }}>
            <p className="text-sm font-semibold uppercase tracking-widest text-white/50">
              Time Cap: {Math.floor(routine.timeCapSeconds / 60)}m
              {routine.timeCapSeconds % 60 > 0 ? ` ${routine.timeCapSeconds % 60}s` : ""}
            </p>
          </div>
        )}
      </div>

      <div className="flex-1 grid grid-cols-2 px-16 gap-8 pb-12">
        <div className="flex flex-col items-center text-center space-y-4">
          <p className="text-3xl font-black text-white">{redAthlete?.name ?? "Red"}</p>

          {redFinished ? (
            <div className="space-y-1">
              <p className="text-2xl font-bold text-green-400 uppercase tracking-widest">Finished</p>
              <p className="text-4xl font-black tabular-nums">{finishTime(progress.redFinishedAt!)}</p>
            </div>
          ) : (
            <>
              <div className="space-y-1">
                <p className="text-3xl font-bold text-white">{redExercise?.name ?? "—"}</p>
                {redExercise?.addedWeight && (
                  <p className="text-base text-white/40">with {redExercise.addedWeight} kg</p>
                )}
              </div>
              <p className="text-6xl font-black tabular-nums">
                {(progress?.redCurrentReps ?? 0)}/{redExercise?.targetReps ?? "—"}
              </p>
              {redNext && (
                <p className="text-base text-white/50">
                  Next: <span className="text-white font-semibold">{redNext.targetReps} {redNext.name}</span>
                </p>
              )}
            </>
          )}
        </div>

        <div className="flex flex-col items-center text-center space-y-4">
          <p className="text-3xl font-black text-white">{blueAthlete?.name ?? "Blue"}</p>

          {blueFinished ? (
            <div className="space-y-1">
              <p className="text-2xl font-bold text-green-400 uppercase tracking-widest">Finished</p>
              <p className="text-4xl font-black tabular-nums">{finishTime(progress.blueFinishedAt!)}</p>
            </div>
          ) : (
            <>
              <div className="space-y-1">
                <p className="text-3xl font-bold text-white">{blueExercise?.name ?? "—"}</p>
                {blueExercise?.addedWeight && (
                  <p className="text-base text-white/40">with {blueExercise.addedWeight} kg</p>
                )}
              </div>
              <p className="text-6xl font-black tabular-nums">
                {(progress?.blueCurrentReps ?? 0)}/{blueExercise?.targetReps ?? "—"}
              </p>
              {blueNext && (
                <p className="text-base text-white/50">
                  Next: <span className="text-white font-semibold">{blueNext.targetReps} {blueNext.name}</span>
                </p>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
