import { useReducer } from "react";
import { ApiError } from "@/lib/api/client";
import { useCollection } from "@/hooks/use-collection";
import { PageHeader } from "@/components/shared/page-header";
import { ToggleButton } from "@/components/shared/toggle-button";
import { CollapsibleFormPanel } from "@/components/shared/collapsible-form-panel";
import { FormError } from "@/components/shared/form-error";
import { FormField, TextField, NumberField } from "@/components/shared/form-fields";
import { DarkSelect } from "@/components/shared/dark-select";
import { SkeletonList } from "@/components/shared/management-list";
import { routinesService } from "./services/routines.service";
import type {
  CreateExerciseInput,
  Exercise,
  ExerciseType,
  Routine,
  RoutineOverview,
} from "./types";
import {
  RefreshCw,
  Plus,
  ChevronRight,
  Clock,
  Dumbbell,
} from "lucide-react";

const typeStyles: Record<ExerciseType, { color: string; bg: string }> = {
  NORMAL: { color: "#a09a92", bg: "rgba(160,154,146,0.12)" },
  UNBROKEN: { color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  SUPERSET: { color: "#e8a020", bg: "rgba(232,160,32,0.12)" },
};

type ExerciseForm = Omit<CreateExerciseInput, "routineId">;

interface RoutineUiState {
  search: string;
  formOpen: boolean;
  form: { name: string; timeCapSeconds: number | null };
  saving: boolean;
  saveError: string | null;
  selectedRoutineId: number | null;
  overview: RoutineOverview | null;
  overviewLoading: boolean;
  overviewError: string | null;
  exerciseForm: ExerciseForm;
  exerciseSaving: boolean;
  exerciseError: string | null;
}

type Action =
  | { type: "setSearch"; value: string }
  | { type: "toggleForm" }
  | { type: "closeForm" }
  | { type: "setFormField"; field: keyof RoutineUiState["form"]; value: string | number | null }
  | { type: "saveStart" }
  | { type: "saveSuccess" }
  | { type: "saveError"; message: string }
  | { type: "selectRoutine"; id: number }
  | { type: "overviewStart" }
  | { type: "overviewSuccess"; overview: RoutineOverview }
  | { type: "overviewError"; message: string }
  | { type: "setExerciseField"; field: keyof ExerciseForm; value: string | number | null }
  | { type: "exerciseSaveStart" }
  | { type: "exerciseSaveSuccess" }
  | { type: "exerciseSaveError"; message: string };

const initialUiState: RoutineUiState = {
  search: "",
  formOpen: false,
  form: { name: "", timeCapSeconds: null },
  saving: false,
  saveError: null,
  selectedRoutineId: null,
  overview: null,
  overviewLoading: false,
  overviewError: null,
  exerciseForm: {
    name: "",
    targetReps: 0,
    addedWeight: null,
    exerciseOrder: 1,
    supersetOrder: null,
    type: "NORMAL",
  },
  exerciseSaving: false,
  exerciseError: null,
};

const initialExerciseForm: ExerciseForm = initialUiState.exerciseForm;

function reducer(state: RoutineUiState, action: Action): RoutineUiState {
  switch (action.type) {
    case "setSearch":
      return { ...state, search: action.value };
    case "toggleForm":
      return { ...state, formOpen: !state.formOpen, saveError: null };
    case "closeForm":
      return { ...state, formOpen: false, saveError: null };
    case "setFormField":
      return {
        ...state,
        form: { ...state.form, [action.field]: action.value } as RoutineUiState["form"],
        saveError: null,
      };
    case "saveStart":
      return { ...state, saving: true, saveError: null };
    case "saveSuccess":
      return { ...state, saving: false, saveError: null, formOpen: false, form: { name: "", timeCapSeconds: null } };
    case "saveError":
      return { ...state, saving: false, saveError: action.message };
    case "selectRoutine":
      return { ...state, selectedRoutineId: action.id, overview: null, overviewError: null };
    case "overviewStart":
      return { ...state, overviewLoading: true, overviewError: null };
    case "overviewSuccess":
      return { ...state, overviewLoading: false, overviewError: null, overview: action.overview };
    case "overviewError":
      return { ...state, overviewLoading: false, overviewError: action.message, overview: null };
    case "setExerciseField":
      return {
        ...state,
        exerciseForm: { ...state.exerciseForm, [action.field]: action.value } as ExerciseForm,
        exerciseError: null,
      };
    case "exerciseSaveStart":
      return { ...state, exerciseSaving: true, exerciseError: null };
    case "exerciseSaveSuccess":
      return { ...state, exerciseSaving: false, exerciseError: null, exerciseForm: initialExerciseForm };
    case "exerciseSaveError":
      return { ...state, exerciseSaving: false, exerciseError: action.message };
  }
}

function formatTimeCap(seconds: number): string {
  return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
}

export function RoutinesPage() {
  const loadRoutines = () => routinesService.getRoutines();
  const {
    items: routines,
    loading: routinesLoading,
    error: routinesError,
    reload: reloadRoutines,
  } = useCollection(loadRoutines, "Failed to load routines");

  const [ui, dispatch] = useReducer(reducer, initialUiState);

  const selectedRoutine = routines.find((r) => r.id === ui.selectedRoutineId);

  async function loadOverview(routine: Routine) {
    dispatch({ type: "overviewStart" });
    try {
      const loaded = await routinesService.getRoutineOverview(routine.name);
      dispatch({ type: "overviewSuccess", overview: loaded });
    } catch (err) {
      dispatch({
        type: "overviewError",
        message: err instanceof ApiError ? err.message : "Failed to load routine overview",
      });
    }
  }

  function selectRoutine(routine: Routine) {
    dispatch({ type: "selectRoutine", id: routine.id });
    void loadOverview(routine);
  }

  async function handleRoutineSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "saveStart" });
    try {
      await routinesService.createRoutine(ui.form);
      dispatch({ type: "saveSuccess" });
      await reloadRoutines();
    } catch (err) {
      dispatch({
        type: "saveError",
        message: err instanceof ApiError ? err.message : "Failed to create routine",
      });
    }
  }

  async function handleExerciseSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedRoutine) return;
    dispatch({ type: "exerciseSaveStart" });
    try {
      await routinesService.createExercise({
        ...ui.exerciseForm,
        routineId: selectedRoutine.id,
      });
      dispatch({ type: "exerciseSaveSuccess" });
      await loadOverview(selectedRoutine);
    } catch (err) {
      dispatch({
        type: "exerciseSaveError",
        message: err instanceof ApiError ? err.message : "Failed to add exercise",
      });
    }
  }

  const query = ui.search.trim().toLowerCase();
  const filteredRoutines = routines.filter(
    (routine) => !query || routine.name.toLowerCase().includes(query)
  );

  const groupedExercises = ui.overview?.exercises.reduce((acc, ex) => {
    const key = ex.exerciseOrder;
    if (!acc[key]) acc[key] = [];
    acc[key].push(ex);
    return acc;
  }, {} as Record<number, Exercise[]>);

  return (
    <div className="max-w-6xl mx-auto space-y-10">
      <PageHeader
        title="Routines"
        action={
          <ToggleButton
            open={ui.formOpen}
            onClick={() => dispatch({ type: "toggleForm" })}
            icon={Dumbbell}
            label="New routine"
          />
        }
      />

      <CollapsibleFormPanel
        open={ui.formOpen}
        label="New routine"
        error={ui.saveError}
        saving={ui.saving}
        onSubmit={handleRoutineSubmit}
        onCancel={() => dispatch({ type: "closeForm" })}
      >
        <div className="grid grid-cols-2 gap-4">
          <TextField
            label="Name"
            value={ui.form.name}
            onChange={(value) => dispatch({ type: "setFormField", field: "name", value })}
            placeholder="Routine name"
            required
          />
          <NumberField
            label={<>Time cap <span style={{ color: "#3a3a3d" }}>(seconds, optional)</span></>}
            value={ui.form.timeCapSeconds}
            onChange={(value) => dispatch({ type: "setFormField", field: "timeCapSeconds", value })}
            placeholder="e.g. 300"
            min={0}
          />
        </div>
      </CollapsibleFormPanel>

      <div className="grid grid-cols-3 gap-6 items-start">
        <div className="space-y-3">
          <div className="flex items-center justify-between gap-2">
            <input
              value={ui.search}
              onChange={(e) => dispatch({ type: "setSearch", value: e.target.value })}
              placeholder="Search routines…"
              className="h-8 border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60 flex-1 min-w-0"
              style={{ background: "#17171a" }}
            />
            <button
              onClick={() => void reloadRoutines()}
              disabled={routinesLoading}
              className="p-1.5 rounded transition-colors shrink-0"
              style={{ color: "#6b6560" }}
            >
              <RefreshCw className={`w-3.5 h-3.5 ${routinesLoading ? "animate-spin" : ""}`} />
            </button>
          </div>

          {routinesLoading ? (
            <SkeletonList count={4} rowHeight="h-14" />
          ) : routinesError ? (
            <p className="text-sm py-4" style={{ color: "#f16a6a" }}>{routinesError}</p>
          ) : filteredRoutines.length === 0 ? (
            <p className="text-sm py-6 text-center" style={{ color: "#3a3a3d" }}>No routines found</p>
          ) : (
            <div className="space-y-2">
              {filteredRoutines.map((routine, i) => {
                const selected = ui.selectedRoutineId === routine.id;
                return (
                  <button
                    key={routine.id}
                    onClick={() => selectRoutine(routine)}
                    className="group flex items-center gap-3 px-4 py-3 rounded-lg text-left transition-colors animate-fade-up w-full"
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
                          {formatTimeCap(routine.timeCapSeconds)}
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
                      Time cap: {formatTimeCap(selectedRoutine.timeCapSeconds)}
                    </p>
                  )}
                </div>
              </div>

              <div className="rounded-lg p-6 space-y-5" style={{ background: "#17171a", border: "1px solid #252528" }}>
                <p className="text-xs tracking-widest uppercase" style={{ color: "#6b6560" }}>Add exercise</p>
                <form onSubmit={handleExerciseSubmit} className="space-y-5">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="col-span-2">
                      <TextField
                        label="Exercise name"
                        value={ui.exerciseForm.name}
                        onChange={(value) => dispatch({ type: "setExerciseField", field: "name", value })}
                        placeholder="e.g. Pull-ups"
                        required
                      />
                    </div>

                    <FormField label="Type">
                      <DarkSelect
                        value={ui.exerciseForm.type}
                        onValueChange={(value) => dispatch({ type: "setExerciseField", field: "type", value: value as ExerciseType })}
                        width="w-full"
                        options={[
                          { value: "NORMAL", label: "Normal" },
                          { value: "UNBROKEN", label: "Unbroken" },
                          { value: "SUPERSET", label: "Superset" },
                        ]}
                      />
                    </FormField>

                    <NumberField
                      label="Target reps"
                      value={ui.exerciseForm.targetReps}
                      onChange={(value) => dispatch({ type: "setExerciseField", field: "targetReps", value: value ?? 0 })}
                      min={1}
                      required
                    />

                    <NumberField
                      label="Order"
                      value={ui.exerciseForm.exerciseOrder}
                      onChange={(value) => dispatch({ type: "setExerciseField", field: "exerciseOrder", value: value ?? 1 })}
                      min={1}
                      required
                    />

                    <NumberField
                      label={<>Added weight <span style={{ color: "#3a3a3d" }}>(kg, optional)</span></>}
                      value={ui.exerciseForm.addedWeight}
                      onChange={(value) => dispatch({ type: "setExerciseField", field: "addedWeight", value })}
                      placeholder="e.g. 10"
                      min={0}
                    />

                    {ui.exerciseForm.type === "SUPERSET" && (
                      <NumberField
                        label="Superset order"
                        value={ui.exerciseForm.supersetOrder}
                        onChange={(value) => dispatch({ type: "setExerciseField", field: "supersetOrder", value })}
                        min={1}
                      />
                    )}
                  </div>

                  <FormError message={ui.exerciseError} />

                  <div className="flex items-center gap-3 pt-1">
                    <button
                      type="submit"
                      disabled={ui.exerciseSaving}
                      className="flex items-center gap-2 px-5 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
                      style={{ background: "#e8a020", color: "#0f0f11" }}
                    >
                      <Plus className="w-3.5 h-3.5" />
                      {ui.exerciseSaving ? "Adding…" : "Add exercise"}
                    </button>
                  </div>
                </form>
              </div>

              <div className="space-y-2">
                <h3 className="text-sm font-medium" style={{ color: "#f0ede8" }}>Exercises</h3>
                {ui.overviewLoading ? (
                  <SkeletonList count={3} rowHeight="h-12" />
                ) : ui.overviewError ? (
                  <p className="text-sm" style={{ color: "#f16a6a" }}>{ui.overviewError}</p>
                ) : !ui.overview || ui.overview.exercises.length === 0 ? (
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
                          {[...exercises]
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

function ExerciseRow({ ex, order, superset }: { ex: Exercise; order?: string; superset?: boolean }) {
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
