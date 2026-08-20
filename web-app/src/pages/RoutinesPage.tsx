import { useReducer, useEffect } from "react";
import type { CreateExerciseInput, CreateRoutineInput, ExerciseType, Routine, RoutineOverview } from "@/types";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { RefreshCw, Plus, ChevronRight, Clock, Dumbbell } from "lucide-react";
import { api, ApiError } from "@/api";

const typeStyles: Record<ExerciseType, { color: string; bg: string }> = {
  NORMAL: { color: "#a09a92", bg: "rgba(160,154,146,0.12)" },
  UNBROKEN: { color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  SUPERSET: { color: "#e8a020", bg: "rgba(232,160,32,0.12)" },
};

interface State {
  form: CreateRoutineInput;
  loading: boolean;
  error: string | null;
  routines: Routine[];
  routinesLoading: boolean;
  routinesError: string | null;
  selectedRoutineId: number | null;
  overview: RoutineOverview | null;
  overviewLoading: boolean;
  overviewError: string | null;
  exerciseForm: Omit<CreateExerciseInput, "routineId">;
  exerciseLoading: boolean;
  exerciseError: string | null;
  search: string;
  formOpen: boolean;
}

type Action =
  | { type: "setField"; field: keyof CreateRoutineInput; value: string | number | null }
  | { type: "setExerciseField"; field: keyof Omit<CreateExerciseInput, "routineId">; value: string | number | null }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string }
  | { type: "setRoutines"; routines: Routine[] }
  | { type: "setRoutinesLoading" }
  | { type: "setRoutinesError"; message: string }
  | { type: "selectRoutine"; id: number }
  | { type: "setOverview"; overview: RoutineOverview }
  | { type: "setOverviewLoading" }
  | { type: "setOverviewError"; message: string }
  | { type: "submitExercise" }
  | { type: "exerciseSuccess" }
  | { type: "exerciseError"; message: string }
  | { type: "setSearch"; value: string }
  | { type: "toggleForm" };

const initialRoutineForm: CreateRoutineInput = { name: "", timeCapSeconds: null };

const initialExerciseForm: Omit<CreateExerciseInput, "routineId"> = {
  name: "",
  targetReps: 0,
  addedWeight: null,
  exerciseOrder: 1,
  supersetOrder: null,
  type: "NORMAL",
};

const initialState: State = {
  form: initialRoutineForm,
  loading: false,
  error: null,
  routines: [],
  routinesLoading: false,
  routinesError: null,
  selectedRoutineId: null,
  overview: null,
  overviewLoading: false,
  overviewError: null,
  exerciseForm: initialExerciseForm,
  exerciseLoading: false,
  exerciseError: null,
  search: "",
  formOpen: false,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setField":
      return { ...state, form: { ...state.form, [action.field]: action.value }, error: null };
    case "setExerciseField":
      return { ...state, exerciseForm: { ...state.exerciseForm, [action.field]: action.value }, exerciseError: null };
    case "submit":
      return { ...state, loading: true, error: null };
    case "success":
      return { ...state, loading: false, form: initialRoutineForm, formOpen: false };
    case "error":
      return { ...state, loading: false, error: action.message };
    case "setRoutines":
      return { ...state, routines: action.routines, routinesLoading: false, routinesError: null };
    case "setRoutinesLoading":
      return { ...state, routinesLoading: true, routinesError: null };
    case "setRoutinesError":
      return { ...state, routinesLoading: false, routinesError: action.message };
    case "selectRoutine":
      return { ...state, selectedRoutineId: action.id, overview: null, overviewError: null };
    case "setOverview":
      return { ...state, overview: action.overview, overviewLoading: false, overviewError: null };
    case "setOverviewLoading":
      return { ...state, overviewLoading: true, overviewError: null };
    case "setOverviewError":
      return { ...state, overviewLoading: false, overviewError: action.message };
    case "submitExercise":
      return { ...state, exerciseLoading: true, exerciseError: null };
    case "exerciseSuccess":
      return { ...state, exerciseLoading: false, exerciseForm: initialExerciseForm };
    case "exerciseError":
      return { ...state, exerciseLoading: false, exerciseError: action.message };
    case "setSearch":
      return { ...state, search: action.value };
    case "toggleForm":
      return { ...state, formOpen: !state.formOpen, error: null };
    default:
      throw new Error("Unknown action");
  }
}

export function RoutinesPage() {
  const [state, dispatch] = useReducer(reducer, initialState);

  async function loadRoutines() {
    dispatch({ type: "setRoutinesLoading" });
    try {
      const routines = await api.getRoutines();
      dispatch({ type: "setRoutines", routines });
      if (state.selectedRoutineId !== null) {
        loadOverview(state.selectedRoutineId);
      }
    } catch (err) {
      dispatch({
        type: "setRoutinesError",
        message: err instanceof ApiError ? err.message : "Failed to load routines",
      });
    }
  }

  async function loadOverview(id: number) {
    const routine = state.routines.find((r) => r.id === id);
    if (!routine) return;
    dispatch({ type: "setOverviewLoading" });
    try {
      const overview = await api.getRoutineOverview(routine.name);
      dispatch({ type: "setOverview", overview });
    } catch (err) {
      dispatch({
        type: "setOverviewError",
        message: err instanceof ApiError ? err.message : "Failed to load routine overview",
      });
    }
  }

  useEffect(() => {
    loadRoutines();
  }, []);

  useEffect(() => {
    if (state.selectedRoutineId !== null) {
      loadOverview(state.selectedRoutineId);
    }
  }, [state.selectedRoutineId]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "submit" });
    try {
      await api.createRoutine(state.form);
      dispatch({ type: "success" });
      loadRoutines();
    } catch (err) {
      dispatch({
        type: "error",
        message: err instanceof ApiError ? err.message : "Failed to create routine",
      });
    }
  }

  async function handleExerciseSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!state.selectedRoutineId) return;
    dispatch({ type: "submitExercise" });
    try {
      await api.createExercise({ ...state.exerciseForm, routineId: state.selectedRoutineId });
      dispatch({ type: "exerciseSuccess" });
      loadOverview(state.selectedRoutineId);
    } catch (err) {
      dispatch({
        type: "exerciseError",
        message: err instanceof ApiError ? err.message : "Failed to add exercise",
      });
    }
  }

  const selectedRoutine = state.routines.find((r) => r.id === state.selectedRoutineId);
  const groupedExercises = state.overview?.exercises.reduce((acc, ex) => {
    const key = ex.exerciseOrder;
    if (!acc[key]) acc[key] = [];
    acc[key].push(ex);
    return acc;
  }, {} as Record<number, typeof state.overview.exercises>);

  const query = (state.search ?? "").trim().toLowerCase();
  const filteredRoutines = state.routines.filter(
    (r) => !query || r.name.toLowerCase().includes(query)
  );

  const inputClass =
    "border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60";
  const selectTrigger = "h-8 text-xs border-[#252528] focus:ring-[#e8a020]/40";
  const selectContentStyle = { background: "#17171a", border: "1px solid #252528" };
  const errorBox = {
    background: "rgba(241,106,106,0.1)",
    color: "#f16a6a",
    border: "1px solid rgba(241,106,106,0.25)",
  } as const;

  return (
    <div className="max-w-6xl mx-auto space-y-10">
      <div className="flex items-end justify-between">
        <div>
          <p className="text-xs tracking-widest uppercase mb-1.5" style={{ color: "#6b6560" }}>
            Management
          </p>
          <h1
            className="text-4xl leading-tight"
            style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "#f0ede8" }}
          >
            Routines
          </h1>
        </div>

        <button
          onClick={() => dispatch({ type: "toggleForm" })}
          className="flex items-center gap-2 px-4 py-2 rounded text-sm font-medium transition-colors"
          style={{
            background: state.formOpen ? "rgba(232,160,32,0.1)" : "#e8a020",
            color: state.formOpen ? "#e8a020" : "#0f0f11",
            border: state.formOpen ? "1px solid rgba(232,160,32,0.3)" : "none",
          }}
        >
          <Dumbbell className="w-3.5 h-3.5" />
          New routine
        </button>
      </div>

      {state.formOpen && (
        <div className="rounded-lg p-6 space-y-5" style={{ background: "#17171a", border: "1px solid #252528" }}>
          <p className="text-xs tracking-widest uppercase" style={{ color: "#6b6560" }}>
            New routine
          </p>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Name</Label>
                <Input
                  value={state.form.name}
                  onChange={(e) => dispatch({ type: "setField", field: "name", value: e.target.value })}
                  placeholder="Routine name"
                  required
                  className={inputClass}
                  style={{ background: "#0f0f11" }}
                />
              </div>

              <div className="space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>
                  Time cap <span style={{ color: "#3a3a3d" }}>(seconds, optional)</span>
                </Label>
                <Input
                  type="number"
                  value={state.form.timeCapSeconds ?? ""}
                  onChange={(e) => dispatch({ type: "setField", field: "timeCapSeconds", value: e.target.value ? Number(e.target.value) : null })}
                  placeholder="e.g. 300"
                  min={0}
                  className={inputClass}
                  style={{ background: "#0f0f11" }}
                />
              </div>
            </div>

            {state.error && <p className="text-sm rounded px-3 py-2" style={errorBox}>{state.error}</p>}

            <div className="flex items-center gap-3 pt-1">
              <button
                type="submit"
                disabled={state.loading}
                className="px-5 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
                style={{ background: "#e8a020", color: "#0f0f11" }}
              >
                {state.loading ? "Creating…" : "Create"}
              </button>
              <button
                type="button"
                onClick={() => dispatch({ type: "toggleForm" })}
                className="px-4 py-2 rounded text-sm transition-colors"
                style={{ color: "#6b6560" }}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="grid grid-cols-3 gap-6 items-start">
        <div className="space-y-3">
          <div className="flex items-center justify-between gap-2">
            <Input
              value={state.search}
              onChange={(e) => dispatch({ type: "setSearch", value: e.target.value })}
              placeholder="Search routines…"
              className="h-8 border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
              style={{ background: "#17171a" }}
            />
            <button
              onClick={loadRoutines}
              disabled={state.routinesLoading}
              className="p-1.5 rounded transition-colors shrink-0"
              style={{ color: "#6b6560" }}
            >
              <RefreshCw className={`w-3.5 h-3.5 ${state.routinesLoading ? "animate-spin" : ""}`} />
            </button>
          </div>

          {state.routinesLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="h-14 rounded-lg" style={{ background: "#17171a" }}>
                  <Skeleton className="h-full w-full rounded-lg opacity-40" />
                </div>
              ))}
            </div>
          ) : state.routinesError ? (
            <p className="text-sm py-4" style={{ color: "#f16a6a" }}>{state.routinesError}</p>
          ) : filteredRoutines.length === 0 ? (
            <p className="text-sm py-6 text-center" style={{ color: "#3a3a3d" }}>No routines found</p>
          ) : (
            <div className="space-y-2">
              {filteredRoutines.map((routine, i) => {
                const selected = state.selectedRoutineId === routine.id;
                return (
                  <button
                    key={routine.id}
                    onClick={() => dispatch({ type: "selectRoutine", id: routine.id })}
                    className="group flex items-center gap-3 px-4 py-3 rounded-lg text-left transition-colors animate-fade-up"
                    style={{
                      background: selected ? "rgba(232,160,32,0.08)" : "#17171a",
                      border: "1px solid",
                      borderColor: selected ? "rgba(232,160,32,0.35)" : "#252528",
                      animationDelay: `${i * 0.03}s`,
                      opacity: 0,
                    }}
                    onMouseEnter={(e) => {
                      if (!selected) e.currentTarget.style.borderColor = "#363639";
                    }}
                    onMouseLeave={(e) => {
                      if (!selected) e.currentTarget.style.borderColor = "#252528";
                    }}
                  >
                    <div
                      className="w-1 self-stretch rounded-full shrink-0"
                      style={{ background: selected ? "#e8a020" : "#3a3a3d", opacity: selected ? 1 : 0.5 }}
                    />

                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate" style={{ color: selected ? "#e8a020" : "#f0ede8" }}>
                        {routine.name}
                      </p>
                      {routine.timeCapSeconds && (
                        <p className="text-xs flex items-center gap-1 mt-0.5" style={{ color: "#6b6560" }}>
                          <Clock className="w-3 h-3" />
                          {Math.floor(routine.timeCapSeconds / 60)}m {routine.timeCapSeconds % 60}s
                        </p>
                      )}
                    </div>

                    <ChevronRight className="w-4 h-4 shrink-0" style={{ color: "#3a3a3d" }} />
                  </button>
                );
              })}
            </div>
          )}
        </div>

        <div className="col-span-2 space-y-4">
          {!selectedRoutine ? (
            <div className="flex items-center justify-center h-56 rounded-lg border border-dashed" style={{ borderColor: "#252528" }}>
              <p className="text-sm" style={{ color: "#6b6560" }}>Select a routine to view and add exercises</p>
            </div>
          ) : (
            <>
              <div className="flex items-center justify-between">
                <div>
                  <h2
                    className="text-2xl leading-tight"
                    style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "#f0ede8" }}
                  >
                    {selectedRoutine.name}
                  </h2>
                  {selectedRoutine.timeCapSeconds && (
                    <p className="text-xs flex items-center gap-1 mt-0.5" style={{ color: "#6b6560" }}>
                      <Clock className="w-3 h-3" />
                      Time cap: {Math.floor(selectedRoutine.timeCapSeconds / 60)}m {selectedRoutine.timeCapSeconds % 60}s
                    </p>
                  )}
                </div>
              </div>

              <div className="rounded-lg p-6 space-y-5" style={{ background: "#17171a", border: "1px solid #252528" }}>
                <p className="text-xs tracking-widest uppercase" style={{ color: "#6b6560" }}>Add exercise</p>
                <form onSubmit={handleExerciseSubmit} className="space-y-5">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="col-span-2 space-y-1.5">
                      <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Exercise name</Label>
                      <Input
                        value={state.exerciseForm.name}
                        onChange={(e) => dispatch({ type: "setExerciseField", field: "name", value: e.target.value })}
                        placeholder="e.g. Pull-ups"
                        required
                        className={inputClass}
                        style={{ background: "#0f0f11" }}
                      />
                    </div>

                    <div className="space-y-1.5">
                      <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Type</Label>
                      <Select
                        value={state.exerciseForm.type}
                        onValueChange={(value) => dispatch({ type: "setExerciseField", field: "type", value: value as ExerciseType })}
                      >
                        <SelectTrigger className={`${selectTrigger} w-full`} style={{ background: "#0f0f11", color: "#a09a92" }}>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent style={selectContentStyle}>
                          <SelectItem value="NORMAL" className="text-xs" style={{ color: "#a09a92" }}>Normal</SelectItem>
                          <SelectItem value="UNBROKEN" className="text-xs" style={{ color: "#a09a92" }}>Unbroken</SelectItem>
                          <SelectItem value="SUPERSET" className="text-xs" style={{ color: "#a09a92" }}>Superset</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-1.5">
                      <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Target reps</Label>
                      <Input
                        type="number"
                        value={state.exerciseForm.targetReps}
                        onChange={(e) => dispatch({ type: "setExerciseField", field: "targetReps", value: Number(e.target.value) })}
                        min={1}
                        required
                        className={inputClass}
                        style={{ background: "#0f0f11" }}
                      />
                    </div>

                    <div className="space-y-1.5">
                      <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Order</Label>
                      <Input
                        type="number"
                        value={state.exerciseForm.exerciseOrder}
                        onChange={(e) => dispatch({ type: "setExerciseField", field: "exerciseOrder", value: Number(e.target.value) })}
                        min={1}
                        required
                        className={inputClass}
                        style={{ background: "#0f0f11" }}
                      />
                    </div>

                    <div className="space-y-1.5">
                      <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>
                        Added weight <span style={{ color: "#3a3a3d" }}>(kg, optional)</span>
                      </Label>
                      <Input
                        type="number"
                        value={state.exerciseForm.addedWeight ?? ""}
                        onChange={(e) => dispatch({ type: "setExerciseField", field: "addedWeight", value: e.target.value ? Number(e.target.value) : null })}
                        placeholder="e.g. 10"
                        min={0}
                        className={inputClass}
                        style={{ background: "#0f0f11" }}
                      />
                    </div>

                    {state.exerciseForm.type === "SUPERSET" && (
                      <div className="space-y-1.5">
                        <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Superset order</Label>
                        <Input
                          type="number"
                          value={state.exerciseForm.supersetOrder ?? ""}
                          onChange={(e) => dispatch({ type: "setExerciseField", field: "supersetOrder", value: e.target.value ? Number(e.target.value) : null })}
                          min={1}
                          className={inputClass}
                          style={{ background: "#0f0f11" }}
                        />
                      </div>
                    )}
                  </div>

                  {state.exerciseError && <p className="text-sm rounded px-3 py-2" style={errorBox}>{state.exerciseError}</p>}

                  <div className="flex items-center gap-3 pt-1">
                    <button
                      type="submit"
                      disabled={state.exerciseLoading}
                      className="flex items-center gap-2 px-5 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
                      style={{ background: "#e8a020", color: "#0f0f11" }}
                    >
                      <Plus className="w-3.5 h-3.5" />
                      {state.exerciseLoading ? "Adding…" : "Add exercise"}
                    </button>
                  </div>
                </form>
              </div>

              <div className="space-y-2">
                <h3 className="text-sm font-medium" style={{ color: "#f0ede8" }}>Exercises</h3>
                {state.overviewLoading ? (
                  <div className="space-y-2">
                    {Array.from({ length: 3 }).map((_, i) => (
                      <div key={i} className="h-12 rounded-lg" style={{ background: "#17171a" }}>
                        <Skeleton className="h-full w-full rounded-lg opacity-40" />
                      </div>
                    ))}
                  </div>
                ) : state.overviewError ? (
                  <p className="text-sm" style={{ color: "#f16a6a" }}>{state.overviewError}</p>
                ) : !state.overview || state.overview.exercises.length === 0 ? (
                  <p className="text-sm py-4" style={{ color: "#6b6560" }}>No exercises yet.</p>
                ) : (
                  Object.entries(groupedExercises ?? {})
                    .sort(([a], [b]) => Number(a) - Number(b))
                    .map(([order, exercises]) =>
                      exercises.length > 1 ? (
                        <div key={order} className="rounded-lg overflow-hidden" style={{ background: "#17171a", border: "1px solid #252528" }}>
                          <div
                            className="flex items-center gap-2 px-4 py-2"
                            style={{ background: "rgba(232,160,32,0.06)", borderBottom: "1px solid #252528" }}
                          >
                            <span className="w-5 h-5 rounded-md flex items-center justify-center text-xs font-bold" style={{ background: "rgba(232,160,32,0.15)", color: "#e8a020" }}>
                              {order}
                            </span>
                            <span className="text-xs font-semibold uppercase tracking-widest" style={{ color: "#e8a020" }}>
                              Superset — {exercises.length} exercises
                            </span>
                          </div>
                          {exercises
                            .sort((a, b) => (a.supersetOrder ?? 0) - (b.supersetOrder ?? 0))
                            .map((ex) => (
                              <ExerciseRow key={ex.id} ex={ex} superset />
                            ))}
                        </div>
                      ) : (
                        <ExerciseRow key={exercises[0].id} ex={exercises[0]} order={order} />
                      )
                    )
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function ExerciseRow({ ex, order, superset }: { ex: CreateExerciseInput; order?: string; superset?: boolean }) {
  const t = typeStyles[ex.type];
  return (
    <div
      className="flex items-center gap-3 rounded-lg px-4 py-3"
      style={{ background: superset ? "#17171a" : "transparent", border: superset ? "none" : "1px solid #252528" }}
    >
      {order !== undefined && (
        <span
          className="w-5 h-5 rounded-md flex items-center justify-center text-xs font-bold shrink-0"
          style={{ background: "#1e1e22", color: "#6b6560" }}
        >
          {order}
        </span>
      )}
      {superset && <span className="w-5 shrink-0" />}

      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium truncate" style={{ color: "#f0ede8" }}>
          {ex.name}
        </p>
        <div className="flex items-center gap-2 mt-0.5 text-xs" style={{ color: "#6b6560" }}>
          <span>{ex.targetReps} reps</span>
          {ex.addedWeight ? <span>{ex.addedWeight}kg</span> : null}
        </div>
      </div>

      <span className="text-xs px-2.5 py-1 rounded-full shrink-0 capitalize" style={{ background: t.bg, color: t.color }}>
        {ex.type.toLowerCase()}
      </span>
    </div>
  );
}