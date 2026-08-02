import { useReducer, useEffect } from "react";
import { api, ApiError } from "@/api";
import type {
  Tournament,
  TournamentState,
  Match,
  Routine,
  ScreenRoutine,
} from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { Eye, EyeOff, Trash2, Plus, ArrowUp, ArrowDown, Monitor } from "lucide-react";
import { ScreenControl } from "./tournament/ScreenControl";

interface State {
  tournaments: Tournament[];
  selectedTournamentId: number | null;
  tournamentState: TournamentState | null;
  matches: Match[];
  routines: Routine[];
  screenRoutines: ScreenRoutine[];
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
  | { type: "setError"; message: string };

const initialState: State = {
  tournaments: [],
  selectedTournamentId: null,
  tournamentState: null,
  matches: [],
  routines: [],
  screenRoutines: [],
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
        const [tournamentState, brackets, screenRoutines] = await Promise.all([
          api.getTournamentState(id),
          api.getBracketsByTournamentId(id),
          api.getScreenRoutines(id),
        ]);
        dispatch({ type: "setTournamentState", state: tournamentState });
        dispatch({ type: "setScreenRoutines", screenRoutines });

        const allMatches = await Promise.all(brackets.map((b) => api.getMatchesByBracketId(b.id)));
        dispatch({ type: "setMatches", matches: allMatches.flat() });
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
    <div className="max-w-3xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Screen Manager</h1>
          <p className="text-sm text-muted-foreground mt-1">Control what spectators see on the public screen.</p>
        </div>
        {selectedTournament && (
          <Button
            variant="outline"
            size="sm"
            onClick={() => window.open(`/screen/${selectedTournament.id}`, "_blank")}
          >
            <Monitor className="w-4 h-4 mr-2" />
            Open Screen
          </Button>
        )}
      </div>

      <div className="space-y-1.5">
        <p className="text-xs text-muted-foreground uppercase tracking-wider">Tournament</p>
        <Select
          value={state.selectedTournamentId ? String(state.selectedTournamentId) : ""}
          onValueChange={(v) => dispatch({ type: "selectTournament", id: Number(v) })}
        >
          <SelectTrigger className="w-72">
            <SelectValue placeholder="Select a tournament" />
          </SelectTrigger>
          <SelectContent>
            {state.tournaments.map((t) => (
              <SelectItem key={t.id} value={String(t.id)}>{t.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {state.loading && (
        <div className="space-y-3">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-48 w-full" />
        </div>
      )}

      {!state.loading && selectedTournament && (
        <>
          <ScreenControl
            tournamentId={selectedTournament.id}
            state={state.tournamentState}
            matches={state.matches}
            onUpdated={(s) => dispatch({ type: "setTournamentState", state: s })}
          />

          <Separator />

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Screen Routines</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {sorted.length === 0 && (
                <p className="text-sm text-muted-foreground">No routines added yet.</p>
              )}

              {sorted.map((sr, idx) => (
                <div key={sr.id} className="flex items-center gap-3 p-3 border rounded-lg bg-muted/30">
                  <div className="flex flex-col gap-0.5">
                    <Button variant="ghost" size="icon" className="h-5 w-5" onClick={() => handleMove(sr, "up")} disabled={idx === 0}>
                      <ArrowUp className="w-3 h-3" />
                    </Button>
                    <Button variant="ghost" size="icon" className="h-5 w-5" onClick={() => handleMove(sr, "down")} disabled={idx === sorted.length - 1}>
                      <ArrowDown className="w-3 h-3" />
                    </Button>
                  </div>

                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">
                      {state.routines.find((r) => r.id === sr.routineId)?.name ?? `Routine #${sr.routineId}`}
                    </p>
                    {sr.label && (
                      <p className="text-xs text-muted-foreground">{sr.label}</p>
                    )}
                  </div>

                  <Badge variant={sr.isVisible ? "default" : "outline"} className="text-xs">
                    {sr.isVisible ? "Visible" : "Hidden"}
                  </Badge>

                  <Button variant="ghost" size="icon" onClick={() => handleToggleVisibility(sr)}>
                    {sr.isVisible ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </Button>

                  <Button variant="ghost" size="icon" onClick={() => handleDelete(sr)}>
                    <Trash2 className="w-4 h-4 text-destructive" />
                  </Button>
                </div>
              ))}

              <Separator />

              <AddScreenRoutineRow routines={state.routines} onAdd={handleAdd} />
            </CardContent>
          </Card>
        </>
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
        <SelectTrigger className="w-56">
          <SelectValue placeholder="Select routine" />
        </SelectTrigger>
        <SelectContent>
          {routines.map((r) => (
            <SelectItem key={r.id} value={String(r.id)}>{r.name}</SelectItem>
          ))}
        </SelectContent>
      </Select>

      <input
        className="h-9 rounded-md border border-input bg-transparent px-3 text-sm w-36 placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
        placeholder="Label (optional)"
        value={label}
        onChange={(e) => setLabel(e.target.value)}
      />

      <Button
        size="sm"
        variant="outline"
        disabled={!routineId}
        onClick={() => routineId && onAdd(routineId, label)}
      >
        <Plus className="w-4 h-4 mr-1" />
        Add
      </Button>
    </div>
  );
}