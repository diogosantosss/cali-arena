import { useEffect, useRef, useState } from "react";
import { ApiError } from "@/lib/api/client";
import { matchesService } from "../services/matches.service";
import type { Match, MatchProgress } from "../types";

/**
 * Owns everything related to controlling the currently selected match:
 * match + progress loading, rep adjustments, side finishing and starting.
 * The parent must key this consumer by matchId so switching matches
 * starts from a clean slate.
 */
export function useMatchControl(matchId: number) {
  const [currentMatch, setCurrentMatch] = useState<Match | null>(null);
  const [progress, setProgress] = useState<MatchProgress | null>(null);
  const [redReps, setRedReps] = useState(0);
  const [blueReps, setBlueReps] = useState(0);

  const repsRef = useRef({ red: 0, blue: 0 });
  repsRef.current = { red: redReps, blue: blueReps };

  useEffect(() => {
    let cancelled = false;
    async function loadMatch() {
      try {
        const match = await matchesService.getMatchById(matchId);
        if (cancelled) return;
        setCurrentMatch(match);
        const loaded = await matchesService
          .getProgressByMatchId(matchId)
          .catch(() => null);
        if (cancelled) return;
        if (loaded) applyProgress(loaded);
      } catch {
        // silently fail
      }
    }
    loadMatch();
    return () => {
      cancelled = true;
    };
  }, [matchId]);

  function applyProgress(loaded: MatchProgress) {
    setProgress(loaded);
    setRedReps(loaded.redCurrentReps);
    setBlueReps(loaded.blueCurrentReps);
  }

  /**
   * A rep update only changes match-level fields (status/finishedAt/winner)
   * when a side transitions to finished — otherwise the returned
   * MatchProgress already carries everything worth updating.
   */
  function hasNewFinish(before: MatchProgress | null, after: MatchProgress): boolean {
    if (!before) return !!(after.redFinishedAt || after.blueFinishedAt);
    return (
      (!!after.redFinishedAt && !before.redFinishedAt) ||
      (!!after.blueFinishedAt && !before.blueFinishedAt)
    );
  }

  async function syncMatchIfNeeded(before: MatchProgress | null, after: MatchProgress) {
    if (!hasNewFinish(before, after)) return;
    const match = await matchesService.getMatchById(matchId);
    setCurrentMatch(match);
  }

  async function startMatch() {
    if (!matchId) return;
    try {
      const loaded = await matchesService.startMatch(matchId);
      applyProgress(loaded);
      const match = await matchesService.getMatchById(matchId);
      setCurrentMatch(match);
    } catch (err) {
      throw normalize(err, "Failed to start match");
    }
  }

  async function adjustReps(side: "red" | "blue", delta: number) {
    if (!matchId) return;
    const base = repsRef.current[side];
    if (base === 0 && delta < 0) return;
    const next = Math.max(0, base + delta);
    if (next === base) return;

    repsRef.current[side] = next;
    if (side === "red") setRedReps(next);
    else setBlueReps(next);

    const input =
      side === "red"
        ? { redReps: next, blueReps: repsRef.current.blue }
        : { redReps: repsRef.current.red, blueReps: next };

    try {
      const prev = progress;
      const loaded = await matchesService.updateReps(matchId, input);
      repsRef.current = { red: loaded.redCurrentReps, blue: loaded.blueCurrentReps };
      applyProgress(loaded);
      await syncMatchIfNeeded(prev, loaded);
    } catch (err) {
      // rollback the optimistic bump
      repsRef.current[side] = base;
      if (side === "red") setRedReps(base);
      else setBlueReps(base);
      throw normalize(err, "Failed to update reps");
    }
  }

  async function finishSide(side: "red" | "blue") {
    if (!matchId) return;
    const input =
      side === "red"
        ? { redReps: null, blueReps: repsRef.current.blue }
        : { redReps: repsRef.current.red, blueReps: null };
    try {
      const prev = progress;
      const loaded = await matchesService.updateReps(matchId, input);
      repsRef.current = { red: loaded.redCurrentReps, blue: loaded.blueCurrentReps };
      applyProgress(loaded);
      await syncMatchIfNeeded(prev, loaded);
    } catch (err) {
      throw normalize(err, "Failed to finish side");
    }
  }

  return { currentMatch, progress, redReps, blueReps, startMatch, adjustReps, finishSide };
}

function normalize(err: unknown, fallback: string): string {
  return err instanceof ApiError ? err.message : fallback;
}
