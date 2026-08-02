import { useEffect, useReducer } from "react";
import { useParams } from "react-router-dom";
import { api, ApiError } from "@/api";
import type { Match, Routine, RoutineOverview, ScreenRoutine, Tournament, TournamentState } from "@/types";

interface State {
  tournament: Tournament | null;
  tournamentState: TournamentState | null;
  screenRoutines: ScreenRoutine[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
  matches: Match[];
  error: string | null;
}

type Action =
  | { type: "setTournament"; tournament: Tournament }
  | { type: "setTournamentState"; state: TournamentState }
  | { type: "setScreenRoutines"; screenRoutines: ScreenRoutine[] }
  | { type: "setRoutines"; routines: Routine[] }
  | { type: "setOverview"; routineName: string; overview: RoutineOverview }
  | { type: "setMatches"; matches: Match[] }
  | { type: "setError"; message: string };

const initialState: State = {
  tournament: null,
  tournamentState: null,
  screenRoutines: [],
  routines: [],
  overviews: {},
  matches: [],
  error: null,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setTournament":
      return { ...state, tournament: action.tournament };
    case "setTournamentState":
      return { ...state, tournamentState: action.state };
    case "setScreenRoutines":
      return { ...state, screenRoutines: action.screenRoutines };
    case "setRoutines":
      return { ...state, routines: action.routines };
    case "setOverview":
      return { ...state, overviews: { ...state.overviews, [action.routineName]: action.overview } };
    case "setMatches":
      return { ...state, matches: action.matches };
    case "setError":
      return { ...state, error: action.message };
    default:
      throw new Error("Unknown action");
  }
}

export function ScreenPage() {
  const { tournamentId } = useParams<{ tournamentId: string }>();
  const id = Number(tournamentId);
  const [state, dispatch] = useReducer(reducer, initialState);

  async function loadBase() {
    try {
      const [tournament, screenRoutines, routines, brackets] = await Promise.all([
        api.getTournamentById(id),
        api.getScreenRoutines(id),
        api.getRoutines(),
        api.getBracketsByTournamentId(id),
      ]);
      dispatch({ type: "setTournament", tournament });
      dispatch({ type: "setScreenRoutines", screenRoutines });
      dispatch({ type: "setRoutines", routines });

      const allMatches = await Promise.all(brackets.map((b) => api.getMatchesByBracketId(b.id)));
      dispatch({ type: "setMatches", matches: allMatches.flat() });

      await Promise.all(
        routines.map(async (r) => {
          const overview = await api.getRoutineOverview(r.name);
          dispatch({ type: "setOverview", routineName: r.name, overview });
        })
      );
    } catch (err) {
      if (err instanceof ApiError) dispatch({ type: "setError", message: err.message });
    }
  }

  async function pollState() {
    try {
      const [tournamentState, screenRoutines] = await Promise.all([
        api.getTournamentState(id),
        api.getScreenRoutines(id),
      ]);
      dispatch({ type: "setTournamentState", state: tournamentState });
      dispatch({ type: "setScreenRoutines", screenRoutines });
    } catch {
      // silently fail
    }
  }

  useEffect(() => { loadBase(); }, [id]);

  useEffect(() => {
    pollState();
    const interval = setInterval(pollState, 1000);
    return () => clearInterval(interval);
  }, [id]);

  const screen = state.tournamentState?.currentScreen;

  if (!screen || screen === "WAITING") {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ background: "#0d0d14" }}>
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
      <div
        className="min-h-screen flex flex-col"
        style={{
          background: "radial-gradient(ellipse at top, #1a1a2e 0%, #0a0a0f 100%)",
          color: "white",
          fontFamily: "sans-serif",
        }}
      >
        <div className="text-center pt-22 pb-30">
          <h1 className="text-7xl font-bold uppercase tracking-tight mb-8">
            {state.tournament?.name ?? "Cali Arena"}
          </h1>
          <p
            className="text-2xl font-semibold uppercase tracking-[0.3em]"
            style={{ color: "#6fa3ef" }}
          >
            Endurance Battles
          </p>
        </div>

        {visible.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <p className="text-white/20 uppercase tracking-widest text-sm">No routines configured</p>
          </div>
        ) : (
          <div
            className="flex-1 grid px-16"
            style={{ gridTemplateColumns: `repeat(${visible.length}, 1fr)` }}
          >
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
                    <div
                      className="mt-10 mx-auto px-6 py-3 rounded-xl"
                      style={{ background: "rgba(255,255,255,0.07)" }}
                    >
                      <p className="text-sm font-semibold uppercase tracking-[0.2em] text-white/70">
                        Time Cap — {Math.floor(routine.timeCapSeconds / 60)}Min
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

  return (
    <div className="min-h-screen flex items-center justify-center" style={{ background: "#13131a" }}>
      <p className="text-white/20 text-lg uppercase tracking-widest">{screen}</p>
    </div>
  );
}