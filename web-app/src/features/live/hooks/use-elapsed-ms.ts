import { useEffect, useReducer } from "react";

export function useElapsedMs(timerStartedAt: string | null): number {
  const [elapsed, setElapsed] = useReducer((_: number, v: number) => v, 0);

  useEffect(() => {
    if (!timerStartedAt) return;
    const interval = setInterval(() => {
      setElapsed(Date.now() - new Date(timerStartedAt).getTime());
    }, 50);
    return () => clearInterval(interval);
  }, [timerStartedAt]);

  return elapsed;
}