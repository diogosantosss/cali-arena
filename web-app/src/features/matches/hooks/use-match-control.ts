import { useEffect, useRef, useState } from "react";
import type { Client } from "@stomp/stompjs";
import { ApiError } from "@/lib/api/client";
import { matchesService } from "../services/matches.service";
import { createJudgeClient, publishAdjust, type JudgeEvent } from "../services/matches-ws.service";
import type { Match, MatchProgress } from "../types";

/**
 * Owns everything related to controlling the currently selected match:
 * match + progress loading, rep adjustments over the judge WebSocket
 * channel, side finishing and starting.
 * The parent must key this consumer by matchId so switching matches
 * starts from a clean slate.
 *
 * Rep adjustments are optimistic: the local count updates immediately and
 * the authoritative value arrives via the match topic. An ERROR event (or a
 * dropped connection) resyncs state from the REST progress endpoint.
 */
export function useMatchControl(matchId: number) {
  const [currentMatch, setCurrentMatch] = useState<Match | null>(null);
  const [progress, setProgress] = useState<MatchProgress | null>(null);
  const [redReps, setRedReps] = useState(0);
  const [blueReps, setBlueReps] = useState(0);

  const clientRef = useRef<Client | null>(null);

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

    function applyReps(event: Extract<JudgeEvent, { type: "REPS" }>) {
      if (event.side === "RED") setRedReps(event.reps);
      else setBlueReps(event.reps);
      setProgress((prev) => {
        if (!prev) return prev;
        return event.side === "RED"
          ? {
              ...prev,
              redCurrentReps: event.reps,
              redCurrentExerciseId: event.exerciseId ?? prev.redCurrentExerciseId,
            }
          : {
              ...prev,
              blueCurrentReps: event.reps,
              blueCurrentExerciseId: event.exerciseId ?? prev.blueCurrentExerciseId,
            };
      });
    }

    async function refreshMatch() {
      try {
        const match = await matchesService.getMatchById(matchId);
        if (!cancelled) setCurrentMatch(match);
      } catch {
        // silently fail — next event will retry
      }
    }

    async function resyncProgress() {
      try {
        const loaded = await matchesService.getProgressByMatchId(matchId);
        if (!cancelled) applyProgress(loaded);
      } catch {
        // silently fail
      }
    }

    function handleEvent(event: JudgeEvent) {
      if (cancelled) return;
      switch (event.type) {
        case "REPS":
          applyReps(event);
          break;
        case "FINISHED":
          void refreshMatch();
          break;
        case "ERROR":
          void resyncProgress();
          break;
      }
    }

    loadMatch();
    const client = createJudgeClient(matchId, handleEvent);
    clientRef.current = client;

    return () => {
      cancelled = true;
      client.deactivate();
      clientRef.current = null;
    };
  }, [matchId]);

  function applyProgress(loaded: MatchProgress) {
    setProgress(loaded);
    setRedReps(loaded.redCurrentReps);
    setBlueReps(loaded.blueCurrentReps);
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

    // optimistic bump; confirmation comes through the topic echo
    repsRef.current[side] = next;
    if (side === "red") setRedReps(next);
    else setBlueReps(next);

    const sent = publishAdjust(clientRef.current, matchId, side.toUpperCase() as "RED" | "BLUE", next);
    if (!sent) {
      repsRef.current[side] = base;
      if (side === "red") setRedReps(base);
      else setBlueReps(base);
      throw new Error("Connection lost");
    }
  }

  async function finishSide(side: "red" | "blue") {
    if (!matchId) return;
    const input =
      side === "red"
        ? { redReps: null, blueReps: repsRef.current.blue }
        : { redReps: repsRef.current.red, blueReps: null };
    try {
      const loaded = await matchesService.updateReps(matchId, input);
      applyProgress(loaded);
      const match = await matchesService.getMatchById(matchId);
      setCurrentMatch(match);
    } catch (err) {
      throw normalize(err, "Failed to finish side");
    }
  }

  return { currentMatch, progress, redReps, blueReps, startMatch, adjustReps, finishSide };
}

function normalize(err: unknown, fallback: string): string {
  return err instanceof ApiError ? err.message : fallback;
}
