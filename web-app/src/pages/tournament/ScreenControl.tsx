import { useEffect, useReducer } from "react";
import type { Athlete, Match, MatchProgress, RoutineOverview, Routine, ScreenState, TournamentState } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Monitor, Play, Square } from "lucide-react";
import { api, ApiError } from "@/api";

interface Props {
  tournamentId: number;
  state: TournamentState | null;
  matches: Match[];
  athletes: Athlete[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
  onUpdated: (state: TournamentState) => void;
}

interface LocalState {
  screen: ScreenState;
  matchId: number | null;
  currentMatch: Match | null;
  progress: MatchProgress | null;
  redReps: number;
  blueReps: number;
  loading: boolean;
  error: string | null;
}

type Action =
  | { type: "setScreen"; value: ScreenState }
  | { type: "setMatchId"; value: number | null }
  | { type: "setCurrentMatch"; match: Match }
  | { type: "setProgress"; progress: MatchProgress }
  | { type: "setRedReps"; value: number }
  | { type: "setBlueReps"; value: number }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string };

function reducer(state: LocalState, action: Action): LocalState {
  switch (action.type) {
    case "setScreen":
      return { ...state, screen: action.value, matchId: action.value !== "BATTLE" ? null : state.matchId };
    case "setMatchId":
      return { ...state, matchId: action.value, currentMatch: null, progress: null, redReps: 0, blueReps: 0 };
    case "setCurrentMatch":
      return { ...state, currentMatch: action.match };
    case "setProgress":
      return { ...state, progress: action.progress, redReps: action.progress.redCurrentReps, blueReps: action.progress.blueCurrentReps };
    case "setRedReps":
      return { ...state, redReps: action.value };
    case "setBlueReps":
      return { ...state, blueReps: action.value };
    case "submit":
      return { ...state, loading: true, error: null };
    case "success":
      return { ...state, loading: false };
    case "error":
      return { ...state, loading: false, error: action.message };
    default:
      throw new Error("Unknown action");
  }
}

const screenLabels: Record<ScreenState, string> = {
  WAITING: "Waiting",
  ROUTINES: "Routines",
  BATTLE: "Battle",
  WINNER: "Winner",
  LEADERBOARD: "Leaderboard",
};

export function ScreenControl(
  { tournamentId, state: tournamentState, matches, athletes, routines, overviews, onUpdated }: Props) {
  const [state, dispatch] = useReducer(reducer, {
    screen: tournamentState?.currentScreen ?? "WAITING",
    matchId: tournamentState?.currentMatchId ?? null,
    currentMatch: null,
    progress: null,
    redReps: 0,
    blueReps: 0,
    loading: false,
    error: null,
  });

  useEffect(() => {
    if (!state.matchId) return;
    async function loadMatch() {
      try {
        const match = await api.getMatchById(state.matchId!);
        dispatch({ type: "setCurrentMatch", match });
        const progress = await api.getProgressByMatchId(state.matchId!).catch(() => null);
        if (progress) dispatch({ type: "setProgress", progress });
      } catch {
        // silently fail
      }
    }
    loadMatch();
  }, [state.matchId]);

  async function handleUpdateScreen() {
    dispatch({ type: "submit" });
    try {
      const updated = await api.updateScreen(tournamentId, {
        screen: state.screen,
        currentMatchId: state.screen === "BATTLE" ? state.matchId : null,
      });
      dispatch({ type: "success" });
      onUpdated(updated);
    } catch (err) {
      if (err instanceof ApiError) dispatch({ type: "error", message: err.message });
      else dispatch({ type: "error", message: "Failed to update screen" });
    }
  }

  async function handleStartMatch() {
    if (!state.matchId) return;
    try {
      const progress = await api.startMatch(state.matchId);
      dispatch({ type: "setProgress", progress });
      const match = await api.getMatchById(state.matchId);
      dispatch({ type: "setCurrentMatch", match });
    } catch (err) {
      if (err instanceof ApiError) dispatch({ type: "error", message: err.message });
    }
  }

  async function incrementRed() {
    if (!state.matchId || state.progress?.redFinishedAt) return;
    const newReps = state.redReps + 1;
    dispatch({ type: "setRedReps", value: newReps });
    try {
      const progress = await api.updateMatchReps(state.matchId, { repReps: newReps, blueReps: state.blueReps });
      dispatch({ type: "setProgress", progress });
    } catch (err) {
      if (err instanceof ApiError) dispatch({ type: "error", message: err.message });
    }
  }

  async function decrementRed() {
    if (!state.matchId || state.progress?.redFinishedAt) return;
    const newReps = Math.max(0, state.redReps - 1);
    dispatch({ type: "setRedReps", value: newReps });
    try {
      const progress = await api.updateMatchReps(state.matchId, { repReps: newReps, blueReps: state.blueReps });
      dispatch({ type: "setProgress", progress });
    } catch (err) {
      if (err instanceof ApiError) dispatch({ type: "error", message: err.message });
    }
  }

  async function incrementBlue() {
    if (!state.matchId || state.progress?.blueFinishedAt) return;
    const newReps = state.blueReps + 1;
    dispatch({ type: "setBlueReps", value: newReps });
    try {
      const progress = await api.updateMatchReps(state.matchId, { repReps: state.redReps, blueReps: newReps });
      dispatch({ type: "setProgress", progress });
    } catch (err) {
      if (err instanceof ApiError) dispatch({ type: "error", message: err.message });
    }
  }

  async function decrementBlue() {
    if (!state.matchId || state.progress?.blueFinishedAt) return;
    const newReps = Math.max(0, state.blueReps - 1);
    dispatch({ type: "setBlueReps", value: newReps });
    try {
      const progress = await api.updateMatchReps(state.matchId, { repReps: state.redReps, blueReps: newReps });
      dispatch({ type: "setProgress", progress });
    } catch (err) {
      if (err instanceof ApiError) dispatch({ type: "error", message: err.message });
    }
  }

  async function handleFinishRed() {
    if (!state.matchId) return;
    try {
      const progress = await api.updateMatchReps(state.matchId, { repReps: null, blueReps: state.blueReps });
      dispatch({ type: "setProgress", progress });
    } catch (err) {
      if (err instanceof ApiError) dispatch({ type: "error", message: err.message });
    }
  }

  async function handleFinishBlue() {
    if (!state.matchId) return;
    try {
      const progress = await api.updateMatchReps(state.matchId, { repReps: state.redReps, blueReps: null });
      dispatch({ type: "setProgress", progress });
    } catch (err) {
      if (err instanceof ApiError) dispatch({ type: "error", message: err.message });
    }
  }

  const readyMatches = matches.filter((m) => m.status !== "FINISHED");
  const redAthlete = athletes.find((a) => a.id === state.currentMatch?.athleteRedId);
  const blueAthlete = athletes.find((a) => a.id === state.currentMatch?.athleteBlueId);
  const isRunning = state.currentMatch?.status === "RUNNING";
  const redFinished = !!state.progress?.redFinishedAt;
  const blueFinished = !!state.progress?.blueFinishedAt;

  const routine = routines.find((r) => r.id === state.currentMatch?.routineId);
  const exercises = routine ? (overviews[routine.name]?.exercises.sort((a, b) => a.exerciseOrder - b.exerciseOrder) ?? []) : [];
  const redExercise = exercises.find((e) => e.id === state.progress?.redCurrentExerciseId) ?? exercises[0];
  const blueExercise = exercises.find((e) => e.id === state.progress?.blueCurrentExerciseId) ?? exercises[0];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center justify-between text-base">
          <div className="flex items-center gap-2">
            <Monitor className="w-4 h-4" />
            Screen control
          </div>
          {tournamentState && (
            <Badge variant="outline" className="capitalize">
              Current: {screenLabels[tournamentState.currentScreen]}
            </Badge>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-end gap-3">
          <div className="space-y-1.5">
            <p className="text-xs text-muted-foreground uppercase tracking-wider">Screen</p>
            <Select
              value={state.screen}
              onValueChange={(value) => dispatch({ type: "setScreen", value: value as ScreenState })}
            >
              <SelectTrigger className="w-44">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Object.entries(screenLabels).map(([value, label]) => (
                  <SelectItem key={value} value={value}>{label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {state.screen === "BATTLE" && (
            <div className="space-y-1.5">
              <p className="text-xs text-muted-foreground uppercase tracking-wider">Match</p>
              <Select
                value={state.matchId ? String(state.matchId) : ""}
                onValueChange={(value) => dispatch({ type: "setMatchId", value: Number(value) })}
              >
                <SelectTrigger className="w-56">
                  <SelectValue placeholder="Select match" />
                </SelectTrigger>
                <SelectContent>
                  {readyMatches.map((m) => {
                    const red = athletes.find((a) => a.id === m.athleteRedId);
                    const blue = athletes.find((a) => a.id === m.athleteBlueId);
                    return (
                      <SelectItem key={m.id} value={String(m.id)}>
                        #{m.id} — {red?.name ?? "Red"} vs {blue?.name ?? "Blue"}
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>
          )}

          <Button
            onClick={handleUpdateScreen}
            disabled={state.loading || (state.screen === "BATTLE" && !state.matchId)}
          >
            {state.loading ? "Updating…" : "Update screen"}
          </Button>
        </div>

        {state.screen === "BATTLE" && state.matchId && (
          <>
            <Separator />
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium">
                  Match #{state.matchId}
                  {state.currentMatch && (
                    <Badge className="ml-2 text-xs" variant="outline">{state.currentMatch.status}</Badge>
                  )}
                </p>
                {!isRunning && (
                  <Button size="sm" onClick={handleStartMatch}>
                    <Play className="w-3 h-3 mr-1" />
                    Start
                  </Button>
                )}
              </div>

              {isRunning && (
                <div className="grid grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <p className="text-xs text-muted-foreground uppercase tracking-wider">{redAthlete?.name ?? "Red"}</p>
                    {redFinished ? (
                      <p className="text-sm text-green-500 font-medium">Finished</p>
                    ) : (
                      <>
                        <p className="text-xs text-muted-foreground">
                          {redExercise?.name} — {state.redReps}/{redExercise?.targetReps ?? "—"} reps
                        </p>
                        <div className="flex items-center gap-2">
                          <Button variant="outline" size="icon" className="h-8 w-8" onClick={decrementRed}>−</Button>
                          <span className="text-lg font-bold w-8 text-center">{state.redReps}</span>
                          <Button variant="outline" size="icon" className="h-8 w-8" onClick={incrementRed}>+</Button>
                          <Button variant="ghost" size="sm" className="text-green-600 ml-1" onClick={handleFinishRed}>
                            <Square className="w-3 h-3 mr-1" />
                            Finish
                          </Button>
                        </div>
                      </>
                    )}
                  </div>

                  <div className="space-y-2">
                    <p className="text-xs text-muted-foreground uppercase tracking-wider">{blueAthlete?.name ?? "Blue"}</p>
                    {blueFinished ? (
                      <p className="text-sm text-green-500 font-medium">Finished</p>
                    ) : (
                      <>
                        <p className="text-xs text-muted-foreground">
                          {blueExercise?.name} — {state.blueReps}/{blueExercise?.targetReps ?? "—"} reps
                        </p>
                        <div className="flex items-center gap-2">
                          <Button variant="outline" size="icon" className="h-8 w-8" onClick={decrementBlue}>−</Button>
                          <span className="text-lg font-bold w-8 text-center">{state.blueReps}</span>
                          <Button variant="outline" size="icon" className="h-8 w-8" onClick={incrementBlue}>+</Button>
                          <Button variant="ghost" size="sm" className="text-green-600 ml-1" onClick={handleFinishBlue}>
                            <Square className="w-3 h-3 mr-1" />
                            Finish
                          </Button>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              )}
            </div>
          </>
        )}

        {state.error && (
          <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2">
            {state.error}
          </p>
        )}
      </CardContent>
    </Card>
  );
}