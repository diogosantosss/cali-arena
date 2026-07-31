import { useReducer, useEffect } from "react";
import type { CreateExerciseInput, CreateRoutineInput, ExerciseType, Routine, RoutineOverview } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { RefreshCw, Plus, ChevronRight, Clock, Dumbbell } from "lucide-react";
import { api, ApiError } from "@/api";

interface State {
  form: CreateRoutineInput;
  loading: boolean;
  error: string | null;
  success: boolean;
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
  exerciseSuccess: boolean;
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
  | { type: "exerciseError"; message: string };

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
  success: false,
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
  exerciseSuccess: false,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setField":
      return { ...state, form: { ...state.form, [action.field]: action.value }, success: false, error: null };
    case "setExerciseField":
      return { ...state, exerciseForm: { ...state.exerciseForm, [action.field]: action.value }, exerciseSuccess: false, exerciseError: null };
    case "submit":
      return { ...state, loading: true, error: null, success: false };
    case "success":
      return { ...state, loading: false, success: true, form: initialRoutineForm };
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
      return { ...state, exerciseLoading: true, exerciseError: null, exerciseSuccess: false };
    case "exerciseSuccess":
      return { ...state, exerciseLoading: false, exerciseSuccess: true, exerciseForm: initialExerciseForm };
    case "exerciseError":
      return { ...state, exerciseLoading: false, exerciseError: action.message };
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
      if (err instanceof ApiError) {
        dispatch({ type: "setRoutinesError", message: err.message });
      } else {
        dispatch({ type: "setRoutinesError", message: "Failed to load routines" });
      }
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
      if (err instanceof ApiError) {
        dispatch({ type: "setOverviewError", message: err.message });
      } else {
        dispatch({ type: "setOverviewError", message: "Failed to load routine overview" });
      }
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
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({ type: "error", message: "An error occurred while creating routine" });
      }
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
      if (err instanceof ApiError) {
        dispatch({ type: "exerciseError", message: err.message });
      } else {
        dispatch({ type: "exerciseError", message: "An error occurred while creating exercise" });
      }
    }
  }

  const selectedRoutine = state.routines.find((r) => r.id === state.selectedRoutineId);
  const groupedExercises = state.overview?.exercises.reduce((acc, ex) => {
    const key = ex.exerciseOrder;
    if (!acc[key]) acc[key] = [];
    acc[key].push(ex);
    return acc;
  }, {} as Record<number, typeof state.overview.exercises>);

  return (
    <div className="max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Routines</h1>
        <p className="text-sm text-muted-foreground mt-1">Create routines and manage their exercises</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Dumbbell className="w-4 h-4" />
            New routine
          </CardTitle>
          <CardDescription>A routine is a sequence of exercises used in a battle.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1.5 col-span-2">
                <Label>Name</Label>
                <Input
                  value={state.form.name}
                  onChange={(e) => dispatch({ type: "setField", field: "name", value: e.target.value })}
                  placeholder="Routine name"
                  required
                />
              </div>
              <div className="space-y-1.5">
                <Label>Time cap <span className="text-muted-foreground text-xs">(seconds, optional)</span></Label>
                <Input
                  type="number"
                  value={state.form.timeCapSeconds ?? ""}
                  onChange={(e) => dispatch({ type: "setField", field: "timeCapSeconds", value: e.target.value ? Number(e.target.value) : null })}
                  placeholder="e.g. 300"
                  min={0}
                />
              </div>
            </div>

            {state.error && (
              <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2">
                {state.error}
              </p>
            )}
            {state.success && (
              <p className="text-sm text-green-600 bg-green-500/10 border border-green-500/20 rounded-md px-3 py-2">
                Routine created successfully.
              </p>
            )}

            <Button type="submit" disabled={state.loading}>
              {state.loading ? "Creating…" : "Create routine"}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Separator />

      <div className="grid grid-cols-3 gap-6">
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-medium">
              All routines
              {!state.routinesLoading && (
                <span className="text-muted-foreground font-normal ml-2 text-sm">
                  ({state.routines.length})
                </span>
              )}
            </h2>
            <Button variant="outline" size="icon" onClick={loadRoutines} disabled={state.routinesLoading}>
              <RefreshCw className={`w-4 h-4 ${state.routinesLoading ? "animate-spin" : ""}`} />
            </Button>
          </div>

          <div className="space-y-1">
            {state.routinesLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-12 w-full rounded-lg" />
              ))
            ) : state.routinesError ? (
              <p className="text-sm text-destructive py-4">{state.routinesError}</p>
            ) : state.routines.length === 0 ? (
              <p className="text-sm text-muted-foreground py-4">No routines found.</p>
            ) : (
              state.routines.map((routine) => (
                <button
                  key={routine.id}
                  onClick={() => dispatch({ type: "selectRoutine", id: routine.id })}
                  className={`w-full text-left px-3 py-2.5 rounded-lg border text-sm transition-colors flex items-center justify-between gap-2 ${state.selectedRoutineId === routine.id
                    ? "border-primary bg-primary/10 text-primary"
                    : "border-border hover:bg-muted"
                    }`}
                >
                  <div>
                    <p className="font-medium">{routine.name}</p>
                    {routine.timeCapSeconds && (
                      <p className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                        <Clock className="w-3 h-3" />
                        {Math.floor(routine.timeCapSeconds / 60)}m {routine.timeCapSeconds % 60}s
                      </p>
                    )}
                  </div>
                  <ChevronRight className="w-4 h-4 shrink-0 text-muted-foreground" />
                </button>
              ))
            )}
          </div>
        </div>

        <div className="col-span-2 space-y-4">
          {!selectedRoutine ? (
            <div className="flex items-center justify-center h-48 border border-dashed rounded-lg">
              <p className="text-sm text-muted-foreground">Select a routine to view and add exercises</p>
            </div>
          ) : (
            <>
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-base font-medium">{selectedRoutine.name}</h2>
                  {selectedRoutine.timeCapSeconds && (
                    <p className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                      <Clock className="w-3 h-3" />
                      Time cap: {Math.floor(selectedRoutine.timeCapSeconds / 60)}m {selectedRoutine.timeCapSeconds % 60}s
                    </p>
                  )}
                </div>
              </div>

              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-base">
                    <Plus className="w-4 h-4" />
                    Add exercise
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <form onSubmit={handleExerciseSubmit} className="space-y-4">
                    <div className="grid grid-cols-2 gap-3">
                      <div className="space-y-1.5 col-span-2">
                        <Label>Exercise name</Label>
                        <Input
                          value={state.exerciseForm.name}
                          onChange={(e) => dispatch({ type: "setExerciseField", field: "name", value: e.target.value })}
                          placeholder="e.g. Pull-ups"
                          required
                        />
                      </div>
                      <div className="space-y-1.5">
                        <Label>Type</Label>
                        <Select
                          value={state.exerciseForm.type}
                          onValueChange={(value) => dispatch({ type: "setExerciseField", field: "type", value: value as ExerciseType })}
                        >
                          <SelectTrigger className="w-full">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="NORMAL">Normal</SelectItem>
                            <SelectItem value="UNBROKEN">Unbroken</SelectItem>
                            <SelectItem value="SUPERSET">Superset</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-1.5">
                        <Label>Target reps</Label>
                        <Input
                          type="number"
                          value={state.exerciseForm.targetReps}
                          onChange={(e) => dispatch({ type: "setExerciseField", field: "targetReps", value: Number(e.target.value) })}
                          min={1}
                          required
                        />
                      </div>
                      <div className="space-y-1.5">
                        <Label>Order</Label>
                        <Input
                          type="number"
                          value={state.exerciseForm.exerciseOrder}
                          onChange={(e) => dispatch({ type: "setExerciseField", field: "exerciseOrder", value: Number(e.target.value) })}
                          min={1}
                          required
                        />
                      </div>
                      <div className="space-y-1.5">
                        <Label>Added weight <span className="text-muted-foreground text-xs">(kg, optional)</span></Label>
                        <Input
                          type="number"
                          value={state.exerciseForm.addedWeight ?? ""}
                          onChange={(e) => dispatch({ type: "setExerciseField", field: "addedWeight", value: e.target.value ? Number(e.target.value) : null })}
                          placeholder="e.g. 10"
                          min={0}
                        />
                      </div>
                      {state.exerciseForm.type === "SUPERSET" && (
                        <div className="space-y-1.5">
                          <Label>Superset order</Label>
                          <Input
                            type="number"
                            value={state.exerciseForm.supersetOrder ?? ""}
                            onChange={(e) => dispatch({ type: "setExerciseField", field: "supersetOrder", value: e.target.value ? Number(e.target.value) : null })}
                            min={1}
                          />
                        </div>
                      )}
                    </div>

                    {state.exerciseError && (
                      <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2">
                        {state.exerciseError}
                      </p>
                    )}
                    {state.exerciseSuccess && (
                      <p className="text-sm text-green-600 bg-green-500/10 border border-green-500/20 rounded-md px-3 py-2">
                        Exercise added successfully.
                      </p>
                    )}

                    <Button type="submit" disabled={state.exerciseLoading}>
                      {state.exerciseLoading ? "Adding…" : "Add exercise"}
                    </Button>
                  </form>
                </CardContent>
              </Card>

              <div className="space-y-2">
                <h3 className="text-sm font-medium">Exercises</h3>
                {state.overviewLoading ? (
                  Array.from({ length: 3 }).map((_, i) => (
                    <Skeleton key={i} className="h-10 w-full rounded-lg" />
                  ))
                ) : state.overviewError ? (
                  <p className="text-sm text-destructive">{state.overviewError}</p>
                ) : !state.overview || state.overview.exercises.length === 0 ? (
                  <p className="text-sm text-muted-foreground">No exercises yet.</p>
                ) : (
                  <Card>
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead className="w-10">#</TableHead>
                          <TableHead>Name</TableHead>
                          <TableHead>Type</TableHead>
                          <TableHead>Reps</TableHead>
                          <TableHead>Weight</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {Object.entries(groupedExercises ?? {})
                          .sort(([a], [b]) => Number(a) - Number(b))
                          .map(([order, exercises]) => (
                            exercises.length > 1 ? (
                              <>
                                <TableRow key={`superset-header-${order}`} className="bg-muted/30">
                                  <TableCell className="text-muted-foreground text-sm">{order}</TableCell>
                                  <TableCell colSpan={4}>
                                    <Badge variant="outline" className="text-xs">Superset — {exercises.length} exercises</Badge>
                                  </TableCell>
                                </TableRow>
                                {exercises
                                  .sort((a, b) => (a.supersetOrder ?? 0) - (b.supersetOrder ?? 0))
                                  .map((ex) => (
                                    <TableRow key={ex.id} className="border-l-2 border-l-primary/40">
                                      <TableCell />
                                      <TableCell className="font-medium pl-6">{ex.name}</TableCell>
                                      <TableCell>
                                        <Badge variant="outline" className="capitalize text-xs">
                                          {ex.type.toLowerCase()}
                                        </Badge>
                                      </TableCell>
                                      <TableCell>{ex.targetReps}</TableCell>
                                      <TableCell className="text-muted-foreground">
                                        {ex.addedWeight ? `${ex.addedWeight}kg` : "—"}
                                      </TableCell>
                                    </TableRow>
                                  ))}
                              </>
                            ) : (
                              <TableRow key={exercises[0].id}>
                                <TableCell className="text-muted-foreground text-sm">{order}</TableCell>
                                <TableCell className="font-medium">{exercises[0].name}</TableCell>
                                <TableCell>
                                  <Badge variant="outline" className="capitalize text-xs">
                                    {exercises[0].type.toLowerCase()}
                                  </Badge>
                                </TableCell>
                                <TableCell>{exercises[0].targetReps}</TableCell>
                                <TableCell className="text-muted-foreground">
                                  {exercises[0].addedWeight ? `${exercises[0].addedWeight}kg` : "—"}
                                </TableCell>
                              </TableRow>
                            )
                          ))}
                      </TableBody>
                    </Table>
                  </Card>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}