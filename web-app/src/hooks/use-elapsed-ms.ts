import { useEffect, useReducer } from "react";

export function useElapsedMs(timerStartedAt: string | null): number {
  const [elapsed, setElapsed] = useReducer((_: number, v: number) => v, 0);

  useEffect(() => {
    if (!timerStartedAt) return;

    const start = new Date(timerStartedAt).getTime();

    let frame = 0;
    const tick = () => {
      setElapsed(Date.now() - start);
      frame = requestAnimationFrame(tick);
    };
    tick();
    
    return () => cancelAnimationFrame(frame);
  }, [timerStartedAt]);

  return elapsed;
}