import { useReducer, useEffect } from "react";
import { api, ApiError } from "@/api";
import type {
  Tournament,
  TournamentState,
  Match,
  Routine,
  ScreenRoutine,
  Athlete,
  RoutineOverview,
} from "@/types";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Eye, EyeOff, Trash2, Plus, ArrowUp, ArrowDown, ExternalLink, ChevronRight } from "lucide-react";
import { ScreenControl } from "./tournament/ScreenControl";

interface State {
  tournaments: Tournament[];
  selectedTournamentId: number | null;
  tournamentState: TournamentState | null;
  matches: Match[];
  routines: Routine[];
  screenRoutines: ScreenRoutine[];
  athletes: Athlete[];
  overviews: Record<string, RoutineOverview>;
  loading: boolean;
  error: string | null;
}

type Action =
  | { type: "setTournaments"; tournaments: Tournament[] }
  | { type: "selectTournament"; id: number }
  | { type: "setTournamentState"; state: TournamentState }
  | { type: "setMatches"; matches: Match[] }
  | { type: "setRoutines"; routines: Routine[] }
  | { type: "setScreenRoutines"; screenRoutines: ScreenRoutine[] }
  | { type: "addScreenRoutine"; screenRoutine: ScreenRoutine }
  | { type: "updateScreenRoutine"; screenRoutine: ScreenRoutine }
  | { type: "removeScreenRoutine"; id: number }
  | { type: "setLoading"; loading: boolean }
  | { type: "setError"; message: string }
  | { type: "setOverview"; routineName: string; overview: RoutineOverview }
  | { type: "setAthletes"; athletes: Athlete[] };

const initialState: State = {
  tournaments: [],
  selectedTournamentId: null,
  tournamentState: null,
  matches: [],
  routines: [],
  screenRoutines: [],
  athletes: [],
  overviews: {},
  loading: false,
  error: null,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setTournaments":
      return { ...state, tournaments: action.tournaments };
    case "selectTournament":
      return { ...state, selectedTournamentId: action.id, tournamentState: null, matches: [], screenRoutines: [] };
    case "setTournamentState":
      return { ...state, tournamentState: action.state };
    case "setMatches":
      return { ...state, matches: action.matches };
    case "setRoutines":
      return { ...state, routines: action.routines };
    case "setScreenRoutines":
      return { ...state, screenRoutines: action.screenRoutines };
    case "addScreenRoutine":
      return { ...state, screenRoutines: [...state.screenRoutines, action.screenRoutine] };
    case "updateScreenRoutine":
      return { ...state, screenRoutines: state.screenRoutines.map((r) => r.id === action.screenRoutine.id ? action.screenRoutine : r) };
    case "removeScreenRoutine":
      return { ...state, screenRoutines: state.screenRoutines.filter((r) => r.id !== action.id) };
    case "setLoading":
      return { ...state, loading: action.loading };
    case "setError":
      return { ...state, loading: false, error: action.message };
    case "setAthletes":
      return { ...state, athletes: action.athletes };
    case "setOverview":
      return { ...state, overviews: { ...state.overviews, [action.routineName]: action.overview } };
    default:
      throw new Error("Unknown action");
  }
}

export function ScreenManagerPage() {
  const [state, dispatch] = useReducer(reducer, initialState);

  useEffect(() => {
    async function load() {
      try {
        const [tournaments, routines] = await Promise.all([
          api.getTournaments(),
          api.getRoutines(),
        ]);
        dispatch({ type: "setTournaments", tournaments });
        dispatch({ type: "setRoutines", routines });
      } catch (err) {
        if (err instanceof ApiError) dispatch({ type: "setError", message: err.message });
      }
    }
    load();
  }, []);

  useEffect(() => {
    if (!state.selectedTournamentId) return;
    const id = state.selectedTournamentId;

    async function loadTournamentData() {
      dispatch({ type: "setLoading", loading: true });
      try {
        const [tournamentState, brackets, screenRoutines, athletes] = await Promise.all([
          api.getTournamentState(id),
          api.getBracketsByTournamentId(id),
          api.getScreenRoutines(id),
          api.getAthletes(),
        ]);
        dispatch({ type: "setTournamentState", state: tournamentState });
        dispatch({ type: "setScreenRoutines", screenRoutines });
        dispatch({ type: "setAthletes", athletes });

        const allMatches = await Promise.all(brackets.map((b) => api.getMatchesByBracketId(b.id)));
        dispatch({ type: "setMatches", matches: allMatches.flat() });

        await Promise.all(
          state.routines.map(async (r) => {
            const overview = await api.getRoutineOverview(r.name);
            dispatch({ type: "setOverview", routineName: r.name, overview });
          })
        );
      } catch (err) {
        if (err instanceof ApiError) dispatch({ type: "setError", message: err.message });
      } finally {
        dispatch({ type: "setLoading", loading: false });
      }
    }
    loadTournamentData();
  }, [state.selectedTournamentId]);

  async function handleAdd(routineId: number, label: string) {
    if (!state.selectedTournamentId) return;
    const currentMax = state.screenRoutines.reduce((max, r) => Math.max(max, r.displayOrder), -1);
    try {
      const created = await api.createScreenRoutine(state.selectedTournamentId, {
        routineId,
        displayOrder: currentMax + 1,
        label: label || undefined,
      });
      dispatch({ type: "addScreenRoutine", screenRoutine: created });
    } catch (err) {
      console.error(err);
    }
  }

  async function handleToggleVisibility(sr: ScreenRoutine) {
    try {
      const updated = await api.updateScreenRoutineVisibility(sr.tournamentId, sr.id, !sr.isVisible);
      dispatch({ type: "updateScreenRoutine", screenRoutine: updated });
    } catch (err) {
      console.error(err);
    }
  }

  async function handleMove(sr: ScreenRoutine, direction: "up" | "down") {
    const sorted = [...state.screenRoutines].sort((a, b) => a.displayOrder - b.displayOrder);
    const idx = sorted.findIndex((r) => r.id === sr.id);
    const swapIdx = direction === "up" ? idx - 1 : idx + 1;
    if (swapIdx < 0 || swapIdx >= sorted.length) return;
    const swap = sorted[swapIdx];
    try {
      const [updatedA, updatedB] = await Promise.all([
        api.updateScreenRoutineDisplayOrder(sr.tournamentId, sr.id, swap.displayOrder),
        api.updateScreenRoutineDisplayOrder(swap.tournamentId, swap.id, sr.displayOrder),
      ]);
      dispatch({ type: "updateScreenRoutine", screenRoutine: updatedA });
      dispatch({ type: "updateScreenRoutine", screenRoutine: updatedB });
    } catch (err) {
      console.error(err);
    }
  }

  async function handleDelete(sr: ScreenRoutine) {
    try {
      await api.deleteScreenRoutine(sr.tournamentId, sr.id);
      dispatch({ type: "removeScreenRoutine", id: sr.id });
    } catch (err) {
      console.error(err);
    }
  }

  const selectedTournament = state.tournaments.find((t) => t.id === state.selectedTournamentId);
  const sorted = [...state.screenRoutines].sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Screen Manager</h1>
          <p className="text-sm text-muted-foreground mt-0.5">Control what spectators see on the public display.</p>
        </div>
        {selectedTournament && (
          <Button
            variant="outline"
            size="sm"
            onClick={() => window.open(`/screen/${selectedTournament.id}`, "_blank")}
          >
            Open screen
            <ExternalLink className="w-3.5 h-3.5 ml-1.5" />
          </Button>
        )}
      </div>

      <div className="flex items-center gap-2">
        <ChevronRight className="w-4 h-4 text-muted-foreground shrink-0" />
        <Select
          value={state.selectedTournamentId ? String(state.selectedTournamentId) : ""}
          onValueChange={(v) => dispatch({ type: "selectTournament", id: Number(v) })}
        >
          <SelectTrigger className="w-72">
            <SelectValue placeholder="Select a tournament" />
          </SelectTrigger>
          <SelectContent>
            {state.tournaments.map((t) => (
              <SelectItem key={t.id} value={String(t.id)}>
                <span className="flex items-center gap-2">
                  {t.name}
                  <span className="text-xs text-muted-foreground">{t.status}</span>
                </span>
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {state.loading && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Skeleton className="h-48 w-full rounded-xl" />
          <Skeleton className="h-64 w-full rounded-xl" />
        </div>
      )}

      {!state.loading && selectedTournament && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-start">
          <ScreenControl
            tournamentId={selectedTournament.id}
            state={state.tournamentState}
            matches={state.matches}
            athletes={state.athletes}
            routines={state.routines}
            overviews={state.overviews}
            onUpdated={(s) => dispatch({ type: "setTournamentState", state: s })}
          />

          <div className="border rounded-xl overflow-hidden">
            <div className="px-4 py-3 border-b bg-muted/30 flex items-center justify-between">
              <div>
                <p className="text-sm font-medium">Screen Routines</p>
                <p className="text-xs text-muted-foreground mt-0.5">Shown when screen is set to Routines</p>
              </div>
              <Badge variant="secondary" className="text-xs tabular-nums">
                {sorted.filter((r) => r.isVisible).length}/{sorted.length} visible
              </Badge>
            </div>

            <div className="divide-y">
              {sorted.length === 0 ? (
                <div className="px-4 py-10 text-center">
                  <p className="text-sm text-muted-foreground">No routines added yet.</p>
                </div>
              ) : (
                sorted.map((sr, idx) => {
                  const routineName = state.routines.find((r) => r.id === sr.routineId)?.name ?? `Routine #${sr.routineId}`;
                  return (
                    <div
                      key={sr.id}
                      className={`flex items-center gap-3 px-4 py-3 transition-colors ${sr.isVisible ? "" : "opacity-50"}`}
                    >
                      <div className="flex flex-col gap-0.5 shrink-0">
                        <button
                          onClick={() => handleMove(sr, "up")}
                          disabled={idx === 0}
                          className="p-0.5 rounded text-muted-foreground hover:text-foreground disabled:opacity-20 disabled:cursor-not-allowed transition-colors"
                        >
                          <ArrowUp className="w-3 h-3" />
                        </button>
                        <button
                          onClick={() => handleMove(sr, "down")}
                          disabled={idx === sorted.length - 1}
                          className="p-0.5 rounded text-muted-foreground hover:text-foreground disabled:opacity-20 disabled:cursor-not-allowed transition-colors"
                        >
                          <ArrowDown className="w-3 h-3" />
                        </button>
                      </div>

                      <div className="w-6 h-6 rounded-md bg-muted flex items-center justify-center text-xs font-bold text-muted-foreground shrink-0">
                        {idx + 1}
                      </div>

                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium truncate">{routineName}</p>
                        {sr.label && (
                          <p className="text-xs text-muted-foreground truncate">{sr.label}</p>
                        )}
                      </div>

                      <button
                        onClick={() => handleToggleVisibility(sr)}
                        className="p-1.5 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors shrink-0"
                        title={sr.isVisible ? "Hide" : "Show"}
                      >
                        {sr.isVisible ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
                      </button>

                      <button
                        onClick={() => handleDelete(sr)}
                        className="p-1.5 rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors shrink-0"
                        title="Delete"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  );
                })
              )}
            </div>

            <div className="px-4 py-3 border-t bg-muted/20">
              <AddScreenRoutineRow routines={state.routines} onAdd={handleAdd} />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function AddScreenRoutineRow({
  routines,
  onAdd,
}: {
  routines: Routine[];
  onAdd: (routineId: number, label: string) => void;
}) {
  const [routineId, setRoutineId] = useReducer((_: number | null, v: number | null) => v, null);
  const [label, setLabel] = useReducer((_: string, v: string) => v, "");

  return (
    <div className="flex items-center gap-2">
      <Select
        value={routineId ? String(routineId) : ""}
        onValueChange={(v) => setRoutineId(Number(v))}
      >
        <SelectTrigger className="h-8 text-xs flex-1 min-w-0">
          <SelectValue placeholder="Add a routine…" />
        </SelectTrigger>
        <SelectContent>
          {routines.map((r) => (
            <SelectItem key={r.id} value={String(r.id)}>{r.name}</SelectItem>
          ))}
        </SelectContent>
      </Select>

      <input
        className="h-8 rounded-md border border-input bg-transparent px-2.5 text-xs w-28 placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring shrink-0"
        placeholder="Label"
        value={label}
        onChange={(e) => setLabel(e.target.value)}
      />

      <Button
        size="sm"
        variant="secondary"
        className="h-8 px-2.5 text-xs shrink-0"
        disabled={!routineId}
        onClick={() => routineId && onAdd(routineId, label)}
      >
        <Plus className="w-3.5 h-3.5 mr-1" />
        Add
      </Button>
    </div>
  );
}
