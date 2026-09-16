import { useEffect, useRef, useState } from "react";
import type { Client } from "@stomp/stompjs";
import { ApiError } from "@/api/client";
import { getErrorDescription } from "@/api/error-messages";
import { matchesService } from "../services/matches.service";
import { createJudgeClient, publishJudgeAction, type JudgeEvent } from "../services/matches-ws.service";
import type { Match, MatchProgress } from "@/data/matches";

/**
 * Converts a backend error class name (e.g. "OpponentNotFinished") into the
 * kebab-case problem title used by errorDescriptions.
 */
function wsErrorDescription(raw: string): string {
  const kebab = raw.replace(/([a-z0-9])([A-Z])/g, "$1-$2").toLowerCase();
  return getErrorDescription(kebab);
}

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
export function useMatchControl(
  matchId: number,
  onServerError?: (message: string) => void,
) {
  const [currentMatch, setCurrentMatch] = useState<Match | null>(null);
  const [progress, setProgress] = useState<MatchProgress | null>(null);
  const [redReps, setRedReps] = useState(0);
  const [blueReps, setBlueReps] = useState(0);

  const clientRef = useRef<Client | null>(null);
  const onServerErrorRef = useRef(onServerError);
  onServerErrorRef.current = onServerError;
  const progressRef = useRef<MatchProgress | null>(null);

  const repsRef = useRef({ red: 0, blue: 0 });

  /**
   * Highest rep count this client has published for each side. The tap base
   * is always max(repsRef, lastSentRef) so a stale echo that reverts
   * display cannot make the next tap repeat a value.
   */
  const lastSentRef = useRef({ red: 0, blue: 0 });

  useEffect(() => {
    progressRef.current = progress;
  });

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
      const side = event.side === "RED" ? "red" : "blue";
      const lastSent = lastSentRef.current[side];
      const currentExerciseId =
        side === "red" ? progressRef.current?.redCurrentExerciseId : progressRef.current?.blueCurrentExerciseId;
      const exerciseChanged =
        event.exerciseId != null && event.exerciseId !== currentExerciseId;

      if (exerciseChanged) {
        // Exercise advanced (server reset reps to 0). Apply the new state
        // and reset refs so the next tap starts from the correct base.
        lastSentRef.current[side] = event.reps;
        repsRef.current[side] = event.reps;
        if (event.side === "RED") setRedReps(event.reps);
        else setBlueReps(event.reps);
      } else if (event.reps >= lastSent) {
        // Same exercise, forward movement — apply the confirmed value.
        if (event.side === "RED") setRedReps(event.reps);
        else setBlueReps(event.reps);
      }
      // else: stale echo that would revert the display — silently ignored.

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

    function stampFinished(side: "RED" | "BLUE", finishedAt: string) {
      setProgress((prev) =>
        !prev
          ? prev
          : side === "RED"
            ? { ...prev, redFinishedAt: finishedAt }
            : { ...prev, blueFinishedAt: finishedAt },
      );
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
        case "STARTED":
          // another client started the match — apply the full match + progress
          // broadcast with the event so this viewer is in sync immediately
          setCurrentMatch(event.match);
          applyProgress(event.progress);
          break;
        case "REPS":
          applyReps(event);
          break;
        case "FINISHED":
          stampFinished(event.side, event.finishedAt);
          void refreshMatch();
          break;
        case "ERROR":
          void resyncProgress();
          onServerErrorRef.current?.(wsErrorDescription(event.message));
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
    repsRef.current = { red: loaded.redCurrentReps, blue: loaded.blueCurrentReps };
    lastSentRef.current = { red: loaded.redCurrentReps, blue: loaded.blueCurrentReps };
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

    if ((side === "red" && progressRef.current?.redFinishedAt) || (side === "blue" && progressRef.current?.blueFinishedAt)) {
      return;
    }

    const base = Math.max(repsRef.current[side], lastSentRef.current[side]);
    if (base === 0 && delta < 0) return;
    const next = Math.max(0, base + delta);
    if (next === base) return;

    // optimistic bump; confirmation comes through the topic echo
    repsRef.current[side] = next;
    lastSentRef.current[side] = next;
    if (side === "red") setRedReps(next);
    else setBlueReps(next);

    const sent = publishJudgeAction(clientRef.current, matchId, {
      action: "ADJUST",
      side: side.toUpperCase() as "RED" | "BLUE",
      reps: next,
    });

    if (!sent) {
      repsRef.current[side] = base;
      lastSentRef.current[side] = base;
      if (side === "red") setRedReps(base);
      else setBlueReps(base);
      throw new Error("Connection lost");
    }
  }

  /**
   * Forces the end of the routine for one athlete. Only valid when the
   * opponent has already finished — otherwise the server answers with an
   * ERROR event (OpponentNotFinished) and state is resynced.
   */
  async function finishSide(side: "red" | "blue") {
    if (!matchId || !clientRef.current?.connected) {
      throw new Error("Connection lost");
    }
    
    const sent = publishJudgeAction(clientRef.current, matchId, {
      action: "FINISH",
      side: side.toUpperCase() as "RED" | "BLUE",
    });

    if (!sent) throw new Error("Connection lost");
  }

  return { currentMatch, progress, redReps, blueReps, startMatch, adjustReps, finishSide };
}

function normalize(err: unknown, fallback: string): string {
  return err instanceof ApiError ? err.message : fallback;
}
