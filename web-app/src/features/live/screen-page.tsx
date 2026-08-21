import { useEffect, useReducer, useCallback, useRef, type CSSProperties } from "react";
import { useParams } from "react-router-dom";
import { ApiError } from "@/lib/api/client";
import { useSpectatorSSE } from "./hooks/use-spectator-sse";
import {
  routineGroups,
  nextLabel,
  exerciseProgress,
  type ExerciseProgress,
} from "@/features/routines/lib/exercise-labels";
import { tournamentsService } from "@/features/tournaments/services/tournaments.service";
import type { ScreenRoutine, Tournament, TournamentState } from "@/features/tournaments/types";
import { routinesService } from "@/features/routines/services/routines.service";
import type { Exercise, Routine, RoutineOverview } from "@/features/routines/types";
import type { Athlete } from "@/features/athletes/types";
import { athletesService } from "@/features/athletes/services/athletes.service";
import { matchesService } from "@/features/matches/services/matches.service";
import type { Match, MatchProgress } from "@/features/matches/types";
import type { SpectatorEvent } from "./types";

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

const screenBackground: CSSProperties = {
  background: "#0f0f11",
  backgroundImage: `
    radial-gradient(ellipse 80% 50% at 50% 0%, rgba(232,160,32,0.06) 0%, transparent 60%),
    radial-gradient(ellipse 60% 40% at 100% 100%, rgba(232,160,32,0.04) 0%, transparent 50%)
  `,
};

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
  const currentMatchIdRef = useRef<number | null>(null);

  useEffect(() => {
    currentMatchIdRef.current = state.currentMatch?.id ?? null;
  }, [state.currentMatch]);

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
  }, [id]);

  const handleSSEEvent = useCallback(async (event: SpectatorEvent) => {
    switch (event.action) {
      case "TOURNAMENT_STATE_UPDATED": {
        dispatch({ type: "setTournamentState", state: event.state });
        if (event.state.currentMatchId && event.state.currentMatchId !== currentMatchIdRef.current) {
          const [match, progress] = await Promise.all([
            matchesService.getMatchById(event.state.currentMatchId),
            matchesService.getProgressByMatchId(event.state.currentMatchId).catch(() => null),
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
    return (
      <div className="min-h-screen flex items-center justify-center" style={screenBackground}>
        <div className="text-center space-y-4">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#e8a020] to-[#f0ede8] flex items-center justify-center font-bold text-3xl text-[#0f0f11] mx-auto">
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
      <div className="min-h-screen flex flex-col" style={{ ...screenBackground, color: "white" }}>
        <div className="text-center pt-20 px-16">
          <p className="font-cairo text-6xl font-semibold leading-tight uppercase bg-gradient-to-r from-[#e8a020] to-[#f0ede8] bg-clip-text text-transparent">
            {state.tournament?.name ?? "Cali Arena"}
          </p>
        </div>

        {visible.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <p className="font-cairo text-white/20 uppercase tracking-widest text-sm">No routines configured</p>
          </div>
        ) : (
          <div className="flex-1 flex items-center px-16">
            <div className="w-full grid" style={{ gridTemplateColumns: `repeat(${visible.length}, 1fr)` }}>
            {visible.map((sr) => {
              const routine = state.routines.find((r) => r.id === sr.routineId);
              const overview = routine ? state.overviews[routine.name] : null;
              return (
                <div key={sr.id} className="flex flex-col text-center px-8">
                  <h2 className="font-cairo text-[2.5rem] font-bold uppercase tracking-widest mb-10 text-white">
                    {sr.label ?? routine?.name ?? `Routine #${sr.routineId}`}
                  </h2>
                  <div className="space-y-5">
                    {routineGroups(overview?.exercises ?? []).map((group) => (
                      <p key={group.order} className="font-cairo text-[2rem] font-semibold text-white">
                        {group.label}
                      </p>
                    ))}
                  </div>
                  {routine?.timeCapSeconds && (
                    <div className="mt-10 mx-auto px-6 py-3 rounded-[20px]" style={{ background: "#2D2D2D" }}>
                      <p className="font-cairo text-sm font-semibold uppercase tracking-[0.2em]" style={{ color: "rgba(232,160,32,0.75)" }}>
                        Time Cap — {Math.floor(routine.timeCapSeconds / 60)}M
                        {routine.timeCapSeconds % 60 > 0 ? ` ${routine.timeCapSeconds % 60}S` : ""}
                      </p>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
        )}
      </div>
    );
  }

  if (screen === "BATTLE") {
    if (!state.currentMatch) {
      return (
<div className="min-h-screen flex items-center justify-center" style={screenBackground}>
        <p className="text-white/30 uppercase tracking-widest">Loading match...</p>
      </div>
      );
    }
    return <BattleScreen state={state} match={state.currentMatch} progress={state.matchProgress} athletes={state.athletes} />;
  }

  return (
    <div className="min-h-screen flex items-center justify-center" style={screenBackground}>
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
  const exercises: Exercise[] = routine
    ? (state.overviews[routine.name]?.exercises.sort((a, b) => a.exerciseOrder - b.exerciseOrder) ?? [])
    : [];
  const groups = routineGroups(exercises);

  const redAthlete = athletes.find((a) => a.id === match.athleteRedId);
  const blueAthlete = athletes.find((a) => a.id === match.athleteBlueId);

  const redExercise = exercises.find((e) => e.id === progress?.redCurrentExerciseId) ?? exercises[0];
  const blueExercise = exercises.find((e) => e.id === progress?.blueCurrentExerciseId) ?? exercises[0];
  const redNextLabel = nextLabel(exercises, progress?.redCurrentExerciseId);
  const blueNextLabel = nextLabel(exercises, progress?.blueCurrentExerciseId);
  const redProgress = exerciseProgress(exercises, progress?.redCurrentExerciseId, progress?.redCurrentReps ?? 0);
  const blueProgress = exerciseProgress(exercises, progress?.blueCurrentExerciseId, progress?.blueCurrentReps ?? 0);

  const redFinished = !!progress?.redFinishedAt;
  const blueFinished = !!progress?.blueFinishedAt;

  function finishMs(finishedAt: string) {
    if (!progress?.timerStartedAt) return 0;
    return new Date(finishedAt).getTime() - new Date(progress.timerStartedAt).getTime();
  }

  function finishTime(finishedAt: string) {
    return formatTime(Math.max(0, finishMs(finishedAt)));
  }

  const redWon = redFinished && blueFinished
    ? finishMs(progress!.redFinishedAt!) < finishMs(progress!.blueFinishedAt!)
    : redFinished && !blueFinished;

  const blueWon = redFinished && blueFinished
    ? finishMs(progress!.blueFinishedAt!) < finishMs(progress!.redFinishedAt!)
    : blueFinished && !redFinished;

  const matchFinished = redFinished && blueFinished;
  const finalElapsedMs =
    matchFinished && progress
      ? Math.max(
          progress.redFinishedAt ? finishMs(progress.redFinishedAt) : 0,
          progress.blueFinishedAt ? finishMs(progress.blueFinishedAt) : 0
        )
      : elapsed;

  const timerColor = redWon || blueWon ? "#4ade80" : "#ffffff";

  return (
    <div
      className="min-h-screen flex flex-col overflow-hidden"
      style={screenBackground}
    >
      <div
        className="absolute inset-0 pointer-events-none"
        style={{
          background: "radial-gradient(ellipse 70% 40% at 50% 0%, rgba(232,160,32,0.06) 0%, transparent 70%)",
        }}
      />

      <div className="relative z-10 flex flex-col min-h-screen">
        <div className="text-center pt-20 pb-8 px-16">
          <p className="font-cairo text-6xl font-semibold leading-tight uppercase bg-gradient-to-r from-[#e8a020] to-[#f0ede8] bg-clip-text text-transparent">
            {state.tournament?.name ?? "Cali Arena"}
          </p>
        </div>

        <div className="flex-1 grid grid-cols-3 px-24">
          <AthletePanel
            finishState={{
              finished: redFinished,
              won: redWon,
              lost: blueWon,
              str: redFinished ? finishTime(progress!.redFinishedAt!) : null,
            }}
            name={redAthlete?.name ?? "Red"}
            color="#e05555"
            currentExercise={redExercise}
            currentReps={progress?.redCurrentReps ?? 0}
            progress={redProgress}
            nextExercise={redNextLabel}
          />

          <div className="flex flex-col items-center">
            <p
              className="pt-16 font-cairo text-[6rem] font-bold leading-none tabular-nums transition-colors duration-700"
              style={{ color: timerColor }}
            >
              {formatTime(finalElapsedMs)}
            </p>

            <p className="mt-16 font-cairo text-[2.5rem] font-bold leading-none text-white">
              {routine?.name ?? "—"}
            </p>

            <div className="flex flex-col items-center gap-1 mt-6 font-cairo text-[2rem] font-semibold text-white">
              {groups.map((group) => {
                const hasRed = group.items.some((e) => e.id === progress?.redCurrentExerciseId);
                const hasBlue = group.items.some((e) => e.id === progress?.blueCurrentExerciseId);
                return (
                  <p key={group.order} className="flex items-center gap-3">
                    <span className="w-4 h-4 rounded-full" style={{ background: "#e05555", visibility: hasRed ? "visible" : "hidden" }} />
                    <span>{group.label}</span>
                    <span className="w-4 h-4 rounded-full" style={{ background: "#5588e0", visibility: hasBlue ? "visible" : "hidden" }} />
                  </p>
                );
              })}
            </div>

            {routine && (
              <div className="mt-10 rounded-[20px] bg-[#2D2D2D] px-8 py-3">
                <p className="font-cairo text-[2.25rem] font-bold text-white whitespace-nowrap">
                  Time Cap: {routine.timeCapSeconds ? `${Math.floor(routine.timeCapSeconds / 60)}’${routine.timeCapSeconds % 60 > 0 ? ` ${routine.timeCapSeconds % 60}”` : ""}` : "—"}
                </p>
              </div>
            )}
          </div>

          <AthletePanel
            finishState={{
              finished: blueFinished,
              won: blueWon,
              lost: redWon,
              str: blueFinished ? finishTime(progress!.blueFinishedAt!) : null,
            }}
            name={blueAthlete?.name ?? "Blue"}
            color="#5588e0"
            currentExercise={blueExercise}
            currentReps={progress?.blueCurrentReps ?? 0}
            progress={blueProgress}
            nextExercise={blueNextLabel}
          />
        </div>
      </div>
    </div>
  );
}

function AthletePanel({
  finishState,
  name,
  color,
  currentExercise,
  currentReps,
  progress,
  nextExercise,
}: {
  finishState: { finished: boolean; won: boolean; lost: boolean; str: string | null };
  name: string;
  color: string;
  currentExercise: Exercise | undefined;
  currentReps: number;
  progress: ExerciseProgress;
  nextExercise: string | null | undefined;
}) {
  const finishColor = finishState.won ? "#4ade80" : finishState.lost ? "#e05555" : "#ffffff";
  const isFinished = finishState.finished && finishState.str !== null;
  const pct = isFinished ? 100 : progress.pct;

  return (
    <div className="mt-25 flex flex-col items-center pt-16">
      <div className="flex items-center gap-3">
        <span className="w-5 h-5 rounded-full" style={{ background: color }} />
        <p className="font-cairo text-[3rem] font-bold text-white leading-none">
          {name}
        </p>
      </div>

      <div className="flex flex-col items-center mt-28 gap-2">
        {isFinished ? (
          <>
            <p className="font-cairo text-[2.25rem] font-bold" style={{ color: finishColor }}>
              {finishState.won ? "Winner" : "Finished"}
            </p>
            <p className="font-cairo text-[3.75rem] font-bold tabular-nums leading-none" style={{ color: finishColor }}>
              {finishState.str}
            </p>
          </>
        ) : (
          <>
            <p className="font-cairo text-[3.75rem] font-bold leading-none text-white">
              {currentExercise?.name ?? "—"}
            </p>
            {currentExercise?.addedWeight ? (
              <p className="font-cairo text-[2.25rem] font-bold leading-none bg-gradient-to-r from-[#e8a020] to-[#f0ede8] bg-clip-text text-transparent">
                with {currentExercise.addedWeight} kg
              </p>
            ) : null}
            <p className="font-cairo text-[3.75rem] font-bold leading-none mt-2 tabular-nums text-white">
              <span key={currentReps} className="inline-block animate-rep-pop">
                {currentReps}
              </span>
              /{currentExercise?.targetReps ?? "—"}
            </p>
          </>
        )}
      </div>

      <div className="mt-6 flex w-96 items-center gap-3">
        <div className="flex-1 h-3 rounded-full overflow-hidden" style={{ background: "rgba(255,255,255,0.12)" }}>
          <div
            className="h-full rounded-full transition-all duration-300"
            style={{ width: `${pct}%`, background: color }}
          />
        </div>
        <span className="font-cairo text-[1.75rem] font-bold tabular-nums text-white">
          {pct}%
        </span>
      </div>

      {nextExercise && !isFinished && (
        <p className="mt-42 font-cairo text-[2.5rem] font-bold leading-none bg-gradient-to-r from-[#e8a020] to-[#f0ede8] bg-clip-text text-transparent">
          Next: {nextExercise}
        </p>
      )}
    </div>
  );
}
