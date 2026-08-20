import { useEffect, useReducer, useRef, useState } from "react";
import type { Athlete, Match, MatchProgress, RoutineOverview, Routine, ScreenRoutine, ScreenState, TournamentState } from "@/types";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { api, ApiError } from "@/api";
import { routineGroups } from "@/lib/exerciseLabels";
import { Eye, EyeOff, Trash2, Plus, ArrowUp, ArrowDown, ExternalLink } from "lucide-react";

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
  screenRoutines: ScreenRoutine[];
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
  | { type: "setScreenRoutines"; screenRoutines: ScreenRoutine[] }
  | { type: "addScreenRoutine"; screenRoutine: ScreenRoutine }
  | { type: "updateScreenRoutine"; screenRoutine: ScreenRoutine }
  | { type: "removeScreenRoutine"; id: number }
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
    case "setScreenRoutines":
      return { ...state, screenRoutines: action.screenRoutines };
    case "addScreenRoutine":
      return { ...state, screenRoutines: [...state.screenRoutines, action.screenRoutine] };
    case "updateScreenRoutine":
      return { ...state, screenRoutines: state.screenRoutines.map((r) => r.id === action.screenRoutine.id ? action.screenRoutine : r) };
    case "removeScreenRoutine":
      return { ...state, screenRoutines: state.screenRoutines.filter((r) => r.id !== action.id) };
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

const matchStatusStyles: Record<Match["status"], { label: string; color: string; bg: string }> = {
  PENDING: { label: "Pending", color: "#6b6560", bg: "rgba(107,101,96,0.12)" },
  READY: { label: "Ready", color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  RUNNING: { label: "Running", color: "#4ade80", bg: "rgba(74,222,128,0.12)" },
  PAUSED: { label: "Paused", color: "#a09a92", bg: "rgba(160,154,146,0.12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
};

function formatTime(ms: number): string {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  const millis = ms % 1000;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}`;
}

function useElapsedMs(startedAt: string | null): number {
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    setElapsed(0);
    if (!startedAt) return;
    const interval = setInterval(() => {
      setElapsed(Date.now() - new Date(startedAt).getTime());
    }, 250);
    return () => clearInterval(interval);
  }, [startedAt]);

  return elapsed;
}

export function ScreenControl(
  { tournamentId, state: tournamentState, matches, athletes, routines, overviews, onUpdated }: Props) {
  const [state, dispatch] = useReducer(reducer, {
    screen: tournamentState?.currentScreen ?? "WAITING",
    matchId: tournamentState?.currentMatchId ?? null,
    currentMatch: null,
    progress: null,
    redReps: 0,
    blueReps: 0,
    screenRoutines: [],
    loading: false,
    error: null,
  });

  const repsRef = useRef({ red: 0, blue: 0 });
  repsRef.current = { red: state.redReps, blue: state.blueReps };

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

  useEffect(() => {
    let cancelled = false;
    async function loadScreenRoutines() {
      try {
        const screenRoutines = await api.getScreenRoutines(tournamentId);
        if (!cancelled) dispatch({ type: "setScreenRoutines", screenRoutines });
      } catch {
        // silently fail
      }
    }
    loadScreenRoutines();
    return () => { cancelled = true; };
  }, [tournamentId]);

  async function handleAddScreenRoutine(routineId: number, label: string) {
    const currentMax = state.screenRoutines.reduce((max, r) => Math.max(max, r.displayOrder), -1);
    try {
      const created = await api.createScreenRoutine(tournamentId, {
        routineId,
        displayOrder: currentMax + 1,
        label: label || undefined,
      });
      dispatch({ type: "addScreenRoutine", screenRoutine: created });
    } catch (err) {
      dispatch({ type: "error", message: err instanceof ApiError ? err.message : "Failed to add routine" });
    }
  }

  async function handleToggleVisibility(sr: ScreenRoutine) {
    try {
      const updated = await api.updateScreenRoutineVisibility(sr.tournamentId, sr.id, !sr.isVisible);
      dispatch({ type: "updateScreenRoutine", screenRoutine: updated });
    } catch (err) {
      dispatch({ type: "error", message: err instanceof ApiError ? err.message : "Failed to update visibility" });
    }
  }

  async function handleMoveScreenRoutine(sr: ScreenRoutine, direction: "up" | "down") {
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
      dispatch({ type: "error", message: err instanceof ApiError ? err.message : "Failed to reorder" });
    }
  }

  async function handleDeleteScreenRoutine(sr: ScreenRoutine) {
    try {
      await api.deleteScreenRoutine(sr.tournamentId, sr.id);
      dispatch({ type: "removeScreenRoutine", id: sr.id });
    } catch (err) {
      dispatch({ type: "error", message: err instanceof ApiError ? err.message : "Failed to delete routine" });
    }
  }

  const elapsed = useElapsedMs(state.progress?.timerStartedAt ?? null);

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
      dispatch({ type: "error", message: err instanceof ApiError ? err.message : "Failed to update screen" });
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
      dispatch({ type: "error", message: err instanceof ApiError ? err.message : "Failed to start match" });
    }
  }

  async function adjustReps(side: "red" | "blue", delta: number) {
    if (!state.matchId) return;
    const base = repsRef.current[side];
    if (base === 0 && delta < 0) return;
    const next = Math.max(0, base + delta);
    if (next === base) return;

    repsRef.current[side] = next;
    dispatch({ type: side === "red" ? "setRedReps" : "setBlueReps", value: next });

    const input =
      side === "red"
        ? { redReps: next, blueReps: repsRef.current.blue }
        : { redReps: repsRef.current.red, blueReps: next };

    try {
      const progress = await api.updateMatchReps(state.matchId, input);
      repsRef.current = { red: progress.redCurrentReps, blue: progress.blueCurrentReps };
      dispatch({ type: "setProgress", progress });
      const match = await api.getMatchById(state.matchId);
      dispatch({ type: "setCurrentMatch", match });
    } catch (err) {
      repsRef.current[side] = base;
      dispatch({ type: side === "red" ? "setRedReps" : "setBlueReps", value: base });
      dispatch({ type: "error", message: err instanceof ApiError ? err.message : "Failed to update reps" });
    }
  }

  async function finishSide(side: "red" | "blue") {
    if (!state.matchId) return;
    const input =
      side === "red"
        ? { redReps: null, blueReps: repsRef.current.blue }
        : { redReps: repsRef.current.red, blueReps: null };
    try {
      const progress = await api.updateMatchReps(state.matchId, input);
      repsRef.current = { red: progress.redCurrentReps, blue: progress.blueCurrentReps };
      dispatch({ type: "setProgress", progress });
      const match = await api.getMatchById(state.matchId);
      dispatch({ type: "setCurrentMatch", match });
    } catch (err) {
      dispatch({ type: "error", message: err instanceof ApiError ? err.message : "Failed to finish side" });
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
  const groups = routineGroups(exercises);
  const redExercise = exercises.find((e) => e.id === state.progress?.redCurrentExerciseId) ?? exercises[0];
  const blueExercise = exercises.find((e) => e.id === state.progress?.blueCurrentExerciseId) ?? exercises[0];

  function finishTime(finishedAt: string): string {
    const start = state.progress?.timerStartedAt;
    if (!start) return "00:00.000";
    return formatTime(Math.max(0, new Date(finishedAt).getTime() - new Date(start).getTime()));
  }

  const matchFinished = state.currentMatch?.status === "FINISHED";

  function displayElapsed(): number {
    if (!matchFinished || !state.progress?.timerStartedAt || !state.currentMatch?.finishedAt) return elapsed;
    return Math.max(0, new Date(state.currentMatch.finishedAt).getTime() - new Date(state.progress.timerStartedAt).getTime());
  }

  const matchStatus = state.currentMatch ? matchStatusStyles[state.currentMatch.status] : null;

  const repButton =
    "px-3 py-1.5 rounded text-sm font-semibold transition-colors disabled:opacity-30 disabled:cursor-not-allowed";
  const repIncrementStyle = { background: "rgba(232,160,32,0.12)", color: "#e8a020", border: "1px solid rgba(232,160,32,0.25)" };
  const repDecrementStyle = { background: "#1e1e22", color: "#a09a92", border: "1px solid #252528" };
  const sortedScreenRoutines = [...state.screenRoutines].sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <div className="rounded-lg overflow-hidden" style={{ background: "#17171a", border: "1px solid #252528" }}>
      <div className="flex items-center justify-between px-5 py-4">
        <div>
          <p className="text-xs tracking-widest uppercase" style={{ color: "#6b6560" }}>
            Screen control
          </p>
          <h3
            className="text-xl leading-tight mt-1"
            style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "#f0ede8" }}
          >
            Spectator screen
          </h3>
        </div>
        <div className="flex items-center gap-3">
          {tournamentState && (
            <span
              className="text-[11px] px-2.5 py-1 rounded-full"
              style={{ background: "rgba(232,160,32,0.12)", color: "#e8a020" }}
            >
              Current: {screenLabels[tournamentState.currentScreen]}
            </span>
          )}
          <a
            href={`/screen/${tournamentId}`}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 px-2.5 py-1 rounded text-[11px] font-medium transition-colors"
            style={{ background: "#1e1e22", color: "#a09a92", border: "1px solid #252528" }}
            onMouseEnter={(e) => (e.currentTarget.style.color = "#e8a020")}
            onMouseLeave={(e) => (e.currentTarget.style.color = "#a09a92")}
          >
            <ExternalLink className="w-3 h-3" />
            Open screen
          </a>
        </div>
      </div>

      <div style={{ borderTop: "1px solid #252528" }} className="px-5 py-4 space-y-5">
        <div className="flex items-end gap-3">
          <div className="space-y-1.5">
            <p className="text-[10px] uppercase tracking-widest" style={{ color: "#6b6560" }}>Screen</p>
            <Select
              value={state.screen}
              onValueChange={(value) => dispatch({ type: "setScreen", value: value as ScreenState })}
            >
              <SelectTrigger className="h-8 text-xs w-44 border-[#252528] focus:ring-[#e8a020]/40" style={{ background: "#0f0f11", color: "#a09a92" }}>
                <SelectValue />
              </SelectTrigger>
              <SelectContent style={{ background: "#17171a", border: "1px solid #252528" }}>
                {Object.entries(screenLabels).map(([value, label]) => (
                  <SelectItem key={value} value={value} className="text-xs" style={{ color: "#a09a92" }}>{label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {state.screen === "BATTLE" && (
            <div className="space-y-1.5">
              <p className="text-[10px] uppercase tracking-widest" style={{ color: "#6b6560" }}>Match</p>
              <Select
                value={state.matchId ? String(state.matchId) : ""}
                onValueChange={(value) => dispatch({ type: "setMatchId", value: Number(value) })}
              >
                <SelectTrigger className="h-8 text-xs w-64 border-[#252528] focus:ring-[#e8a020]/40" style={{ background: "#0f0f11", color: "#a09a92" }}>
                  <SelectValue placeholder="Select match" />
                </SelectTrigger>
                <SelectContent style={{ background: "#17171a", border: "1px solid #252528" }}>
                  {readyMatches.map((m) => {
                    const red = athletes.find((a) => a.id === m.athleteRedId);
                    const blue = athletes.find((a) => a.id === m.athleteBlueId);
                    return (
                      <SelectItem key={m.id} value={String(m.id)} className="text-xs" style={{ color: "#a09a92" }}>
                        #{m.id} — {red?.name ?? "Red"} vs {blue?.name ?? "Blue"}
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>
          )}

          <button
            onClick={handleUpdateScreen}
            disabled={state.loading || (state.screen === "BATTLE" && !state.matchId)}
            className="px-4 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
            style={{ background: "#e8a020", color: "#0f0f11" }}
          >
            {state.loading ? "Updating…" : "Update screen"}
          </button>
        </div>

        {state.error && (
          <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded px-3 py-2">
            {state.error}
          </p>
        )}

        {state.screen === "BATTLE" && state.matchId && (
          <div className="rounded-lg" style={{ background: "#0f0f11", border: "1px solid #252528" }}>
            <div className="flex items-center justify-between px-5 py-4">
              <div className="flex items-center gap-3">
                <span className="text-sm font-bold" style={{ color: "#f0ede8" }}>Match #{state.matchId}</span>
                <span className="text-xs hidden sm:block" style={{ color: "#6b6560" }}>
                  {routine?.name}
                </span>
                {matchStatus && (
                  <span className="text-[11px] px-2 py-0.5 rounded-full" style={{ background: matchStatus.bg, color: matchStatus.color }}>
                    {matchStatus.label}
                  </span>
                )}
              </div>

              <div className="flex items-center gap-4">
                <p
                  className="text-xl font-bold tabular-nums hidden sm:block"
                  style={{ color: redFinished || blueFinished ? "#4ade80" : "#f0ede8", fontFamily: "Geist Variable, monospace" }}
                >
                  {isRunning || matchFinished ? formatTime(displayElapsed()) : "–:–"}
                </p>
                {!isRunning && state.currentMatch?.status !== "FINISHED" && (
                  <button
                    onClick={handleStartMatch}
                    className="px-4 py-1.5 rounded text-sm font-medium"
                    style={{ background: "#e8a020", color: "#0f0f11" }}
                  >
                    Start match
                  </button>
                )}
              </div>
            </div>

            {isRunning && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 px-5 pb-5">
                <RepControl
                  label={redAthlete?.name ?? "Red"}
                  dot="#e05555"
                  finished={redFinished}
                  finishTime={redFinished && state.progress?.redFinishedAt ? finishTime(state.progress.redFinishedAt) : null}
                  exercise={redExercise}
                  reps={state.redReps}
                  onAdjust={(delta) => adjustReps("red", delta)}
                  onFinish={() => finishSide("red")}
                  repButton={repButton}
                  repIncrementStyle={repIncrementStyle}
                  repDecrementStyle={repDecrementStyle}
                />
                <RepControl
                  label={blueAthlete?.name ?? "Blue"}
                  dot="#5588e0"
                  finished={blueFinished}
                  finishTime={blueFinished && state.progress?.blueFinishedAt ? finishTime(state.progress.blueFinishedAt) : null}
                  exercise={blueExercise}
                  reps={state.blueReps}
                  onAdjust={(delta) => adjustReps("blue", delta)}
                  onFinish={() => finishSide("blue")}
                  repButton={repButton}
                  repIncrementStyle={repIncrementStyle}
                  repDecrementStyle={repDecrementStyle}
                />
              </div>
            )}

            {exercises.length > 0 && (
              <div style={{ borderTop: "1px solid #252528" }} className="px-5 py-4">
                <div className="flex flex-wrap gap-2">
                  {groups.map((group, i) => {
                    const hasRed = group.items.some((e) => e.id === redExercise?.id);
                    const hasBlue = group.items.some((e) => e.id === blueExercise?.id);
                    const isCurrent = hasRed || hasBlue;
                    return (
                      <span
                        key={group.order}
                        className="text-xs px-2.5 py-1 rounded-full"
                        style={{
                          background:
                            hasRed && hasBlue
                              ? "linear-gradient(90deg, rgba(224,85,85,0.4) 0%, rgba(224,85,85,0.12) 50%, rgba(85,136,224,0.12) 50%, rgba(85,136,224,0.4) 100%)"
                              : hasRed
                                ? "rgba(224,85,85,0.14)"
                                : hasBlue
                                  ? "rgba(85,136,224,0.14)"
                                  : "#1e1e22",
                          color:
                            hasRed && hasBlue
                              ? "#ffffff"
                              : hasRed
                                ? "#e05555"
                                : hasBlue
                                  ? "#5588e0"
                                  : "#6b6560",
                          border:
                            hasRed && hasBlue
                              ? "1px solid rgba(160,120,220,0.4)"
                              : hasRed
                                ? "1px solid rgba(224,85,85,0.45)"
                                : hasBlue
                                  ? "1px solid rgba(85,136,224,0.45)"
                                  : "1px solid #252528",
                        }}
                      >
                        {group.label}
                        {i === groups.length - 1 ? " — goal" : ""}
                      </span>
                    );
                  })}
                </div>
                {routine?.timeCapSeconds && (
                  <p className="text-[11px] mt-3 tracking-widest uppercase" style={{ color: "#3a3a3d" }}>
                    Time cap — {Math.floor(routine.timeCapSeconds / 60)}m
                    {routine.timeCapSeconds % 60 > 0 ? ` ${routine.timeCapSeconds % 60}s` : ""}
                  </p>
                )}
              </div>
            )}
          </div>
        )}

        {state.screen === "ROUTINES" && (
        <div className="rounded-lg" style={{ background: "#0f0f11", border: "1px solid #252528" }}>
          <div className="flex items-center justify-between px-5 py-3">
            <div className="space-y-0.5">
              <p className="text-[10px] uppercase tracking-widest" style={{ color: "#6b6560" }}>Screen routines</p>
              <p className="text-sm font-medium" style={{ color: "#f0ede8" }}>
                Shown when screen is set to Routines
              </p>
            </div>
            <span
              className="text-[11px] px-2.5 py-1 rounded-full tabular-nums"
              style={{ background: "rgba(232,160,32,0.12)", color: "#e8a020" }}
            >
              {sortedScreenRoutines.filter((r) => r.isVisible).length}/{sortedScreenRoutines.length} visible
            </span>
          </div>

          <div style={{ borderTop: "1px solid #252528" }}>
            {sortedScreenRoutines.length === 0 ? (
              <p className="px-5 py-8 text-center text-sm" style={{ color: "#6b6560" }}>
                No routines added yet.
              </p>
            ) : (
              sortedScreenRoutines.map((sr, idx) => {
                const routineName = routines.find((r) => r.id === sr.routineId)?.name ?? `Routine #${sr.routineId}`;
                return (
                  <div
                    key={sr.id}
                    className={`flex items-center gap-3 px-5 py-2.5 ${sr.isVisible ? "" : "opacity-50"}`}
                    style={{ borderTop: "1px solid #19191c" }}
                  >
                    <div className="flex flex-col gap-0.5 shrink-0">
                      <button
                        onClick={() => handleMoveScreenRoutine(sr, "up")}
                        disabled={idx === 0}
                        className="p-0.5 rounded transition-colors disabled:opacity-20 disabled:cursor-not-allowed"
                        style={{ color: "#6b6560" }}
                        onMouseEnter={(e) => (e.currentTarget.style.color = "#f0ede8")}
                        onMouseLeave={(e) => (e.currentTarget.style.color = "#6b6560")}
                      >
                        <ArrowUp className="w-3 h-3" />
                      </button>
                      <button
                        onClick={() => handleMoveScreenRoutine(sr, "down")}
                        disabled={idx === sortedScreenRoutines.length - 1}
                        className="p-0.5 rounded transition-colors disabled:opacity-20 disabled:cursor-not-allowed"
                        style={{ color: "#6b6560" }}
                        onMouseEnter={(e) => (e.currentTarget.style.color = "#f0ede8")}
                        onMouseLeave={(e) => (e.currentTarget.style.color = "#6b6560")}
                      >
                        <ArrowDown className="w-3 h-3" />
                      </button>
                    </div>

                    <div
                      className="w-6 h-6 rounded-md flex items-center justify-center text-xs font-bold shrink-0"
                      style={{ background: "#1e1e22", color: "#6b6560" }}
                    >
                      {idx + 1}
                    </div>

                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate" style={{ color: "#f0ede8" }}>{routineName}</p>
                      {sr.label && (
                        <p className="text-xs truncate" style={{ color: "#6b6560" }}>{sr.label}</p>
                      )}
                    </div>

                    <button
                      onClick={() => handleToggleVisibility(sr)}
                      className="p-1.5 rounded-md shrink-0 transition-colors"
                      style={{ color: "#6b6560" }}
                      title={sr.isVisible ? "Hide" : "Show"}
                      onMouseEnter={(e) => { e.currentTarget.style.color = "#e8a020"; e.currentTarget.style.background = "#1e1e22"; }}
                      onMouseLeave={(e) => { e.currentTarget.style.color = "#6b6560"; e.currentTarget.style.background = "transparent"; }}
                    >
                      {sr.isVisible ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
                    </button>

                    <button
                      onClick={() => handleDeleteScreenRoutine(sr)}
                      className="p-1.5 rounded-md shrink-0 transition-colors"
                      style={{ color: "#6b6560" }}
                      title="Delete"
                      onMouseEnter={(e) => { e.currentTarget.style.color = "#f16a6a"; e.currentTarget.style.background = "rgba(241,106,106,0.1)"; }}
                      onMouseLeave={(e) => { e.currentTarget.style.color = "#6b6560"; e.currentTarget.style.background = "transparent"; }}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                );
              })
            )}

            <div className="px-5 py-3" style={{ borderTop: "1px solid #252528" }}>
              <AddScreenRoutinePanel routines={routines} onAdd={handleAddScreenRoutine} />
            </div>
          </div>
        </div>
        )}
      </div>
    </div>
  );
}

function RepControl({
  label,
  dot,
  finished,
  finishTime,
  exercise,
  reps,
  onAdjust,
  onFinish,
  repButton,
  repIncrementStyle,
  repDecrementStyle,
}: {
  label: string;
  dot: string;
  finished: boolean;
  finishTime: string | null;
  exercise: { name: string; targetReps: number; addedWeight?: number | null } | undefined;
  reps: number;
  onAdjust: (delta: number) => void;
  onFinish: () => void;
  repButton: string;
  repIncrementStyle: React.CSSProperties;
  repDecrementStyle: React.CSSProperties;
}) {
  return (
    <div className="rounded-lg px-4 py-3" style={{ background: "#17171a", border: "1px solid #252528" }}>
      <div className="flex items-center gap-2">
        <span className="w-2.5 h-2.5 rounded-full" style={{ background: dot }} />
        <p className="text-sm font-semibold" style={{ color: "#f0ede8" }}>{label}</p>
      </div>

      <p className="text-[11px] mt-2 uppercase tracking-widest" style={{ color: finished ? "#4ade80" : "#6b6560" }}>
        {finished ? (finishTime ? `Finished — ${finishTime}` : "Finished") : exercise?.name ?? "—"}
        {!finished && exercise?.addedWeight ? ` (with ${exercise.addedWeight} kg)` : ""}
      </p>

      <p className="text-2xl font-bold tabular-nums mt-1" style={{ color: finished ? "#4ade80" : "#e8a020", fontFamily: "Geist Variable, monospace" }}>
        {reps}
        <span className="text-sm font-medium" style={{ color: "#3a3a3d" }}>/{exercise?.targetReps ?? "—"}</span>
      </p>

      <div className="flex items-center gap-2 mt-3">
        <button className={`${repButton} px-2.5`} disabled={finished} style={repDecrementStyle} onClick={() => onAdjust(-1)}>−1</button>
        <button className={repButton} disabled={finished} style={repIncrementStyle} onClick={() => onAdjust(1)}>+1</button>
        <button className={repButton} disabled={finished} style={repIncrementStyle} onClick={() => onAdjust(2)}>+2</button>
        <button className={repButton} disabled={finished} style={repIncrementStyle} onClick={() => onAdjust(5)}>+5</button>
        <button
          onClick={onFinish}
          disabled={finished}
          className="ml-auto px-3 py-1.5 rounded text-sm font-medium transition-colors disabled:opacity-30"
          style={{ background: "rgba(74,222,128,0.12)", color: "#4ade80", border: "1px solid rgba(74,222,128,0.25)" }}
        >
          Finish
        </button>
      </div>
    </div>
  );
}

function AddScreenRoutinePanel({
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
      <Select value={routineId ? String(routineId) : ""} onValueChange={(v) => setRoutineId(Number(v))}>
        <SelectTrigger className="h-8 text-xs flex-1 min-w-0 border-[#252528] focus:ring-[#e8a020]/40" style={{ background: "#17171a", color: "#a09a92" }}>
          <SelectValue placeholder="Add a routine…" />
        </SelectTrigger>
        <SelectContent style={{ background: "#17171a", border: "1px solid #252528" }}>
          {routines.map((r) => (
            <SelectItem key={r.id} value={String(r.id)} className="text-xs" style={{ color: "#a09a92" }}>{r.name}</SelectItem>
          ))}
        </SelectContent>
      </Select>

      <input
        className="h-8 rounded-md border border-[#252528] bg-transparent px-2.5 text-xs w-28 placeholder:text-[#3a3a3d] focus:outline-none focus:ring-1 focus:ring-[#e8a020]/40 shrink-0"
        style={{ color: "#f0ede8" }}
        placeholder="Label"
        value={label}
        onChange={(e) => setLabel(e.target.value)}
      />

      <button
        disabled={!routineId}
        className="flex items-center gap-1 px-3 h-8 rounded text-xs font-medium transition-opacity disabled:opacity-40 shrink-0"
        style={{ background: "#e8a020", color: "#0f0f11" }}
        onClick={() => routineId && onAdd(routineId, label)}
      >
        <Plus className="w-3.5 h-3.5" />
        Add
      </button>
    </div>
  );
}