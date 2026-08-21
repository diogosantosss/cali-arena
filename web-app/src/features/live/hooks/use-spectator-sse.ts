import { useEffect, useRef } from "react";
import { SPECTATOR_ACTIONS } from "../types";
import type { SpectatorEvent } from "../types";

export function useSpectatorSSE(
  tournamentId: number | undefined,
  onEvent: (event: SpectatorEvent) => void
) {
  const onEventRef = useRef(onEvent);
  useEffect(() => {
    onEventRef.current = onEvent;
  });

  useEffect(() => {
    if (!tournamentId) return;

    // NOTE: the backend's spectator endpoint is unauthenticated on purpose —
    // no token is attached here.
    const eventSource = new EventSource(
      `/api/tournaments/${tournamentId}/screen-routines/listen`
    );

    const handleEvent = (raw: MessageEvent, eventType: string) => {
      try {
        const data = JSON.parse(raw.data);
        onEventRef.current({ ...data, action: eventType });
      } catch (err) {
        console.error("SSE parse error:", err);
      }
    };

    for (const type of SPECTATOR_ACTIONS.filter((a) => a !== "KEEP_ALIVE")) {
      eventSource.addEventListener(type, (e) => handleEvent(e, type));
    }

    eventSource.onerror = (err) => {
      console.error("SSE error:", err);
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [tournamentId]);
}
