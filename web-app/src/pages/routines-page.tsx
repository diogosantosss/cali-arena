import { useReducer } from "react";
import { ApiError } from "@/api/client";
import { useCollection } from "@/hooks/use-collection";
import { PageHeader } from "@/components/shared/page-header";
import { ToggleButton } from "@/components/shared/toggle-button";
import { CollapsibleFormPanel } from "@/components/shared/collapsible-form-panel";
import { FormError } from "@/components/shared/form-error";
import { FormField, TextField, NumberField } from "@/components/shared/form-fields";
import { DarkSelect } from "@/components/shared/dark-select";
import { SkeletonList } from "@/components/shared/management-list";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { routinesService } from "@/services/routines.service";
import { EXERCISE_NAMES } from "@/data/routines";
import type {
  CreateExerciseInput,
  Exercise,
  ExerciseType,
  Routine,
  RoutineOverview,
} from "@/data/routines";
import {
  RefreshCw,
  Plus,
  ChevronRight,
  Clock,
  Dumbbell,
  Pencil,
  Trash2,
} from "lucide-react";

const typeStyles: Record<ExerciseType, { color: string; bg: string }> = {
  NORMAL: { color: "var(--secondary-foreground)", bg: "rgba(160,154,146,0.12)" },
  UNBROKEN: { color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  SUPERSET: { color: "var(--accent)", bg: "var(--accent-12)" },
};

const inputClass =
  "border-border text-foreground placeholder:text-faint focus-visible:ring-accent/40 focus-visible:border-accent/60";

type ExerciseForm = Omit<CreateExerciseInput, "routineId">;

const initialExerciseForm: ExerciseForm = {
  name: "",
  targetReps: 0,
  addedWeight: null,
  exerciseOrder: 1,
  supersetOrder: null,
  type: "NORMAL",
};

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
  exerciseFormOpen: boolean;
  editingExerciseId: number | null;
  exerciseSaving: boolean;
  exerciseError: string | null;
  deleteTarget: Exercise | null;
  deleteSaving: boolean;
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
  | { type: "toggleExerciseForm" }
  | { type: "startEditExercise"; exercise: Exercise }
  | { type: "cancelEditExercise" }
  | { type: "exerciseSaveStart" }
  | { type: "exerciseSaveSuccess"; added?: Exercise; updated?: Exercise }
  | { type: "exerciseSaveError"; message: string }
  | { type: "openDeleteConfirm"; exercise: Exercise }
  | { type: "closeDeleteConfirm" }
  | { type: "exerciseDeleteStart" }
  | { type: "exerciseDeleteSuccess"; id: number }
  | { type: "exerciseDeleteError"; message: string };

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
  exerciseForm: initialExerciseForm,
  exerciseFormOpen: false,
  editingExerciseId: null,
  exerciseSaving: false,
  exerciseError: null,
  deleteTarget: null,
  deleteSaving: false,
};

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
    case "toggleExerciseForm": {
      const open = !state.exerciseFormOpen;
      const form = open
        ? {
            ...state.exerciseForm,
            exerciseOrder:
              (state.overview?.exercises.reduce((max, ex) => Math.max(max, ex.exerciseOrder), 0) ?? 0) + 1,
          }
        : state.exerciseForm;
      return { ...state, exerciseFormOpen: open, exerciseForm: form, exerciseError: null };
    }
    case "startEditExercise":
      return {
        ...state,
        editingExerciseId: action.exercise.id,
        exerciseForm: {
          name: action.exercise.name,
          targetReps: action.exercise.targetReps,
          addedWeight: action.exercise.addedWeight,
          exerciseOrder: action.exercise.exerciseOrder,
          supersetOrder: action.exercise.supersetOrder,
          type: action.exercise.type,
        },
        exerciseError: null,
      };
    case "cancelEditExercise":
      return { ...state, editingExerciseId: null, exerciseForm: initialExerciseForm, exerciseError: null };
    case "exerciseSaveStart":
      return { ...state, exerciseSaving: true, exerciseError: null };
    case "exerciseSaveSuccess": {
      let exercises = state.overview?.exercises ?? [];
      if (action.added) {
        exercises = [...exercises, action.added];
      }
      if (action.updated) {
        exercises = applyExerciseOrderMove(exercises, action.updated);
      }
      return {
        ...state,
        exerciseSaving: false,
        exerciseError: null,
        editingExerciseId: null,
        exerciseFormOpen: false,
        exerciseForm: initialExerciseForm,
        overview: state.overview ? { ...state.overview, exercises } : state.overview,
      };
    }
    case "exerciseSaveError":
      return { ...state, exerciseSaving: false, exerciseError: action.message };
    case "openDeleteConfirm":
      return { ...state, deleteTarget: action.exercise, deleteSaving: false, exerciseError: null };
    case "closeDeleteConfirm":
      return { ...state, deleteTarget: null, deleteSaving: false, exerciseError: null };
    case "exerciseDeleteStart":
      return { ...state, deleteSaving: true, exerciseError: null };
    case "exerciseDeleteSuccess": {
      const exercises = state.overview?.exercises.filter((ex) => ex.id !== action.id) ?? [];
      return {
        ...state,
        deleteSaving: false,
        deleteTarget: null,
        exerciseError: null,
        editingExerciseId: state.editingExerciseId === action.id ? null : state.editingExerciseId,
        overview: state.overview ? { ...state.overview, exercises } : state.overview,
      };
    }
    case "exerciseDeleteError":
      return { ...state, deleteSaving: false, exerciseError: action.message };
  }
}

function applyExerciseOrderMove(exercises: Exercise[], updated: Exercise): Exercise[] {
  const moved = exercises.find((ex) => ex.id === updated.id);
  if (!moved) return exercises;
  const oldOrder = moved.exerciseOrder;
  return exercises.map((ex) => {
    if (ex.id === updated.id) return updated;
    let order = ex.exerciseOrder;
    if (updated.exerciseOrder < oldOrder && order >= updated.exerciseOrder && order < oldOrder) {
      order += 1;
    }
    if (updated.exerciseOrder > oldOrder && order > oldOrder && order <= updated.exerciseOrder) {
      order -= 1;
    }
    return order === ex.exerciseOrder ? ex : { ...ex, exerciseOrder: order };
  });
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
      const added = await routinesService.createExercise({
        ...ui.exerciseForm,
        routineId: selectedRoutine.id,
      });
      dispatch({ type: "exerciseSaveSuccess", added });
    } catch (err) {
      dispatch({
        type: "exerciseSaveError",
        message: err instanceof ApiError ? err.message : "Failed to add exercise",
      });
    }
  }

  async function handleExerciseUpdate(e: React.FormEvent) {
    e.preventDefault();
    if (ui.editingExerciseId == null) return;
    dispatch({ type: "exerciseSaveStart" });
    try {
      const updated = await routinesService.updateExercise(ui.editingExerciseId, {
        ...ui.exerciseForm,
      });
      dispatch({ type: "exerciseSaveSuccess", updated });
    } catch (err) {
      dispatch({
        type: "exerciseSaveError",
        message: err instanceof ApiError ? err.message : "Failed to update exercise",
      });
    }
  }

  async function handleDeleteExercise() {
    if (!ui.deleteTarget) return;
    dispatch({ type: "exerciseDeleteStart" });
    try {
      await routinesService.deleteExercise(ui.deleteTarget.id);
      dispatch({ type: "exerciseDeleteSuccess", id: ui.deleteTarget.id });
    } catch (err) {
      dispatch({
        type: "exerciseDeleteError",
        message: err instanceof ApiError ? err.message : "Failed to delete exercise",
      });
    }
  }

  const setExerciseField = (field: keyof ExerciseForm, value: string | number | null) =>
    dispatch({ type: "setExerciseField", field, value });

  const exerciseFields = (
    <>
      <div className="col-span-2">
        <FormField label="Exercise name">
          <Input
            list="exercise-names"
            value={ui.exerciseForm.name}
            onChange={(e) => setExerciseField("name", e.target.value)}
            placeholder="Type or pick: Muscle-Up, Pull-Up, Squat…"
            required
            className={inputClass}
            style={{ background: "var(--background)" }}
          />
          <datalist id="exercise-names">
            {EXERCISE_NAMES.map((name) => (
              <option key={name} value={name} />
            ))}
          </datalist>
        </FormField>
      </div>

      <FormField label="Type">
        <DarkSelect
          value={ui.exerciseForm.type}
          onValueChange={(value) => setExerciseField("type", value as ExerciseType)}
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
        onChange={(value) => setExerciseField("targetReps", value ?? 0)}
        min={1}
        required
      />

      <NumberField
        label="Order"
        value={ui.exerciseForm.exerciseOrder}
        onChange={(value) => setExerciseField("exerciseOrder", value ?? 1)}
        min={1}
        required
      />

      <NumberField
        label={<>Added weight <span style={{ color: "var(--faint)" }}>(kg, optional)</span></>}
        value={ui.exerciseForm.addedWeight}
        onChange={(value) => setExerciseField("addedWeight", value)}
        placeholder="e.g. 10"
        min={0}
      />

      {ui.exerciseForm.type === "SUPERSET" && (
        <NumberField
          label="Superset order"
          value={ui.exerciseForm.supersetOrder}
          onChange={(value) => setExerciseField("supersetOrder", value)}
          min={1}
        />
      )}
    </>
  );

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
            label={<>Time cap <span style={{ color: "var(--faint)" }}>(seconds, optional)</span></>}
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
            <Input
              value={ui.search}
              onChange={(e) => dispatch({ type: "setSearch", value: e.target.value })}
              placeholder="Search routines…"
              className={`${inputClass} flex-1 min-w-0`}
              style={{ background: "var(--background)" }}
            />
            <button
              onClick={() => void reloadRoutines()}
              disabled={routinesLoading}
              className="p-1.5 rounded transition-colors shrink-0"
              style={{ color: "var(--muted-foreground)" }}
            >
              <RefreshCw className={`w-3.5 h-3.5 ${routinesLoading ? "animate-spin" : ""}`} />
            </button>
          </div>

          {routinesLoading ? (
            <SkeletonList count={4} rowHeight="h-14" />
          ) : routinesError ? (
            <p className="text-sm py-4" style={{ color: "var(--danger)" }}>{routinesError}</p>
          ) : filteredRoutines.length === 0 ? (
            <p className="text-sm py-6 text-center" style={{ color: "var(--faint)" }}>No routines found</p>
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
                      background: selected ? "var(--accent-08)" : "var(--card)",
                      border: "1px solid",
                      borderColor: selected ? "var(--accent-35)" : "var(--border)",
                      animationDelay: `${i * 0.03}s`,
                      opacity: 0,
                    }}
                    onMouseEnter={(e) => {
                      if (!selected) e.currentTarget.style.borderColor = "var(--border-hover)";
                    }}
                    onMouseLeave={(e) => {
                      if (!selected) e.currentTarget.style.borderColor = "var(--border)";
                    }}
                  >
                    <div
                      className="w-1 self-stretch rounded-full shrink-0"
                      style={{ background: selected ? "var(--accent)" : "var(--faint)", opacity: selected ? 1 : 0.5 }}
                    />

                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate" style={{ color: selected ? "var(--accent)" : "var(--foreground)" }}>
                        {routine.name}
                      </p>
                      {routine.timeCapSeconds && (
                        <p className="text-xs flex items-center gap-1 mt-0.5" style={{ color: "var(--muted-foreground)" }}>
                          <Clock className="w-3 h-3" />
                          {formatTimeCap(routine.timeCapSeconds)}
                        </p>
                      )}
                    </div>

                    <ChevronRight className="w-4 h-4 shrink-0" style={{ color: "var(--faint)" }} />
                  </button>
                );
              })}
            </div>
          )}
        </div>

        <div className="col-span-2 space-y-4">
          {!selectedRoutine ? (
            <div className="flex items-center justify-center h-56 rounded-lg border border-dashed" style={{ borderColor: "var(--border)" }}>
              <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>Select a routine to view and add exercises</p>
            </div>
          ) : (
            <>
              <div className="flex items-center justify-between">
                <div>
                  <h2
                    className="text-2xl leading-tight"
                    style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "var(--foreground)" }}
                  >
                    {selectedRoutine.name}
                  </h2>
                  {selectedRoutine.timeCapSeconds && (
                    <p className="text-xs flex items-center gap-1 mt-0.5" style={{ color: "var(--muted-foreground)" }}>
                      <Clock className="w-3 h-3" />
                      Time cap: {formatTimeCap(selectedRoutine.timeCapSeconds)}
                    </p>
                  )}
                </div>
              </div>

              <div className="space-y-2">
                <h3 className="text-sm font-medium" style={{ color: "var(--foreground)" }}>Exercises</h3>
                {ui.overviewLoading ? (
                  <SkeletonList count={3} rowHeight="h-12" />
                ) : ui.overviewError ? (
                  <p className="text-sm" style={{ color: "var(--danger)" }}>{ui.overviewError}</p>
                ) : !ui.overview || ui.overview.exercises.length === 0 ? (
                  <p className="text-sm py-4" style={{ color: "var(--muted-foreground)" }}>No exercises yet.</p>
                ) : (
                  Object.entries(groupedExercises ?? {})
                    .sort(([a], [b]) => Number(a) - Number(b))
                    .map(([order, exercises]) =>
                      exercises.length > 1 ? (
                        <div key={order} className="rounded-lg overflow-hidden" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
                          <div
                            className="flex items-center gap-2 px-4 py-2"
                            style={{ background: "var(--accent-06)", borderBottom: "1px solid var(--border)" }}
                          >
                            <span className="w-5 h-5 rounded-md flex items-center justify-center text-xs font-bold" style={{ background: "var(--accent-15)", color: "var(--accent)" }}>
                              {order}
                            </span>
                            <span className="text-xs font-semibold uppercase tracking-widest" style={{ color: "var(--accent)" }}>
                              Superset — {exercises.length} exercises
                            </span>
                          </div>
                          {[...exercises]
                            .sort((a, b) => (a.supersetOrder ?? 0) - (b.supersetOrder ?? 0))
                            .map((ex) => (
                              <ExerciseRow
                                key={ex.id}
                                ex={ex}
                                superset
                                editing={ui.editingExerciseId === ex.id}
                                onEdit={() => dispatch({ type: "startEditExercise", exercise: ex })}
                                onDelete={() => dispatch({ type: "openDeleteConfirm", exercise: ex })}
                                expanded={ui.editingExerciseId === ex.id ? exerciseEditForm() : null}
                              />
                            ))}
                        </div>
                      ) : (
                        <ExerciseRow
                          key={exercises[0].id}
                          ex={exercises[0]}
                          order={order}
                          editing={ui.editingExerciseId === exercises[0].id}
                          onEdit={() => dispatch({ type: "startEditExercise", exercise: exercises[0] })}
                          onDelete={() => dispatch({ type: "openDeleteConfirm", exercise: exercises[0] })}
                          expanded={ui.editingExerciseId === exercises[0].id ? exerciseEditForm() : null}
                        />
                      )
                    )
                )}
              </div>

              {ui.editingExerciseId == null && (
                <div className="rounded-lg overflow-hidden" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
                  <button
                    type="button"
                    onClick={() => dispatch({ type: "toggleExerciseForm" })}
                    className="flex items-center gap-3 px-4 py-3 w-full text-left"
                  >
                    <span
                      className="w-5 h-5 rounded-md flex items-center justify-center text-xs font-bold shrink-0"
                      style={{
                        background: ui.exerciseFormOpen ? "var(--accent)" : "var(--secondary)",
                        color: ui.exerciseFormOpen ? "var(--accent-foreground)" : "var(--muted-foreground)",
                      }}
                    >
                      <Plus className="w-3 h-3" />
                    </span>
                    <span className="text-sm font-medium" style={{ color: ui.exerciseFormOpen ? "var(--accent)" : "var(--foreground)" }}>
                      Add exercise
                    </span>
                    <ChevronRight
                      className={`w-4 h-4 ml-auto shrink-0 transition-transform ${ui.exerciseFormOpen ? "rotate-90" : ""}`}
                      style={{ color: "var(--faint)" }}
                    />
                  </button>

                  {ui.exerciseFormOpen && (
                    <form onSubmit={handleExerciseSubmit} className="border-t px-4 py-4 space-y-5" style={{ borderColor: "var(--border)" }}>
                      <div className="grid grid-cols-2 gap-4">
                        {exerciseFields}
                      </div>

                      <FormError message={ui.exerciseError} />

                      <div className="flex items-center gap-3 pt-1">
                        <button
                          type="submit"
                          disabled={ui.exerciseSaving}
                          className="flex items-center gap-2 px-5 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
                          style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
                        >
                          <Plus className="w-3.5 h-3.5" />
                          {ui.exerciseSaving ? "Adding…" : "Add exercise"}
                        </button>
                      </div>
                    </form>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>

      <Dialog
        open={ui.deleteTarget != null}
        onOpenChange={(open) => {
          if (!open) dispatch({ type: "closeDeleteConfirm" });
        }}
      >
        <DialogContent style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
          <DialogHeader>
            <DialogTitle>Delete exercise?</DialogTitle>
            <DialogDescription>
              {ui.deleteTarget ? (
                <>
                  Remove <strong>{ui.deleteTarget.name}</strong> from this routine? This cannot be undone.
                </>
              ) : null}
            </DialogDescription>
          </DialogHeader>
          <FormError message={ui.exerciseError} />
          <DialogFooter>
            <Button variant="outline" disabled={ui.deleteSaving} onClick={() => dispatch({ type: "closeDeleteConfirm" })}>
              Cancel
            </Button>
            <Button
              disabled={ui.deleteSaving}
              onClick={() => void handleDeleteExercise()}
              style={{ background: "var(--danger)", color: "var(--danger-foreground, #fff)" }}
            >
              {ui.deleteSaving ? "Deleting…" : "Delete"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );

  function exerciseEditForm() {
    return (
      <form onSubmit={(e) => void handleExerciseUpdate(e)} className="border-t px-4 py-4 space-y-5" style={{ borderColor: "var(--border)" }}>
        <div className="grid grid-cols-2 gap-4">
          {exerciseFields}
        </div>

        <FormError message={ui.exerciseError} />

        <div className="flex items-center gap-3 pt-1">
          <button
            type="submit"
            disabled={ui.exerciseSaving}
            className="px-5 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
            style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
          >
            {ui.exerciseSaving ? "Saving…" : "Save changes"}
          </button>
          <button
            type="button"
            disabled={ui.exerciseSaving}
            onClick={() => dispatch({ type: "cancelEditExercise" })}
            className="px-5 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
            style={{ background: "var(--secondary)", color: "var(--muted-foreground)" }}
          >
            Cancel
          </button>
        </div>
      </form>
    );
  }
}

function ExerciseRow({
  ex,
  order,
  superset,
  editing,
  onEdit,
  onDelete,
  expanded,
}: {
  ex: Exercise;
  order?: string;
  superset?: boolean;
  editing: boolean;
  onEdit: () => void;
  onDelete: () => void;
  expanded: React.ReactNode | null;
}) {
  const t = typeStyles[ex.type];
  return (
    <div
      className={superset ? "" : "rounded-lg"}
      style={{
        background: superset ? "var(--card)" : "transparent",
        border: superset ? "none" : "1px solid var(--border)",
        borderColor: editing ? "var(--accent-45)" : superset ? "none" : "var(--border)",
      }}
    >
      <div className="flex items-center gap-3 rounded-lg group">
        <div className="flex items-center gap-3 flex-1 min-w-0 px-4 py-3">
          {order !== undefined && (
            <span
              className="w-5 h-5 rounded-md flex items-center justify-center text-xs font-bold shrink-0"
              style={{ background: editing ? "var(--accent)" : "var(--secondary)", color: editing ? "var(--accent-foreground)" : "var(--muted-foreground)" }}
            >
              {order}
            </span>
          )}
          {superset && <span className="w-5 shrink-0" />}

          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium truncate" style={{ color: editing ? "var(--accent)" : "var(--foreground)" }}>
              {ex.name}
            </p>
            <div className="flex items-center gap-2 mt-0.5 text-xs" style={{ color: "var(--muted-foreground)" }}>
              <span>{ex.targetReps} reps</span>
              {ex.addedWeight ? <span>{ex.addedWeight}kg</span> : null}
            </div>
          </div>
        </div>

        <span className={`text-xs px-2.5 py-1 rounded-full capitalize shrink-0 ${editing ? "opacity-40" : ""}`} style={{ background: t.bg, color: t.color }}>
          {ex.type.toLowerCase()}
        </span>

        <div className="flex items-center gap-1 pr-2 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
          <button
            onClick={onEdit}
            title="Edit"
            className="p-1.5 rounded transition-colors"
            style={{ color: "var(--muted-foreground)" }}
            onMouseEnter={(e) => (e.currentTarget.style.color = "var(--accent)")}
            onMouseLeave={(e) => (e.currentTarget.style.color = "var(--muted-foreground)")}
          >
            <Pencil className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={onDelete}
            title="Delete"
            className="p-1.5 rounded transition-colors"
            style={{ color: "var(--muted-foreground)" }}
            onMouseEnter={(e) => (e.currentTarget.style.color = "var(--danger)")}
            onMouseLeave={(e) => (e.currentTarget.style.color = "var(--muted-foreground)")}
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {expanded}
    </div>
  );
}