import * as React from "react";
import { formatSeconds, formatTimeSmooth } from "./judge-utils";

interface CurrentExerciseCardProps {
  label: string;
  reps: number;
  targetReps: number | null;
  accentColor: string;
  enabled: boolean;
  onDecrement: () => void;
  onIncrement: () => void;
}

export function CurrentExerciseCard({
  label,
  reps,
  targetReps,
  accentColor,
  enabled,
  onDecrement,
  onIncrement,
}: CurrentExerciseCardProps) {
  return (
    <div
      className="rounded-2xl px-5 py-4"
      style={{ background: "var(--card)", border: "1px solid var(--border)" }}
    >
      <p className="text-[11px] font-semibold uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
        Current exercise
      </p>

      <p
        className="mt-1.5 text-base font-semibold leading-tight text-center"
        style={{ color: accentColor }}
      >
        {label}
      </p>

      <div className="flex items-center gap-3 mt-5">
        <button
          disabled={!enabled}
          onClick={onDecrement}
          aria-label="Remove one rep"
          className="w-14 h-14 shrink-0 flex items-center justify-center rounded-full text-3xl font-bold select-none touch-manipulation active:opacity-70 disabled:opacity-30"
          style={{
            background: "var(--secondary)",
            color: "var(--secondary-foreground)",
            border: "1px solid var(--border)",
            WebkitTapHighlightColor: "transparent",
          }}
        >
          −
        </button>

        <div className="flex-1 text-center">
          <span className="text-4xl font-bold tabular-nums" style={{ color: accentColor, fontFamily: "Geist Variable, monospace" }}>
            {reps}
          </span>
          {targetReps != null && (
            <span className="text-sm font-medium" style={{ color: "var(--faint)" }}>
              /{targetReps}
            </span>
          )}
          <p className="text-[11px] uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
            reps
          </p>
        </div>

        <button
          disabled={!enabled}
          onClick={onIncrement}
          aria-label="Add one rep"
          className="w-14 h-14 shrink-0 flex items-center justify-center rounded-full text-3xl font-bold select-none touch-manipulation active:opacity-70 disabled:opacity-30"
          style={{ background: accentColor, color: "#fff", WebkitTapHighlightColor: "transparent" }}
        >
          +
        </button>
      </div>
    </div>
  );
}

export function NextExerciseCard({ nextLabel }: { nextLabel: string }) {
  return (
    <div
      className="rounded-2xl px-5 py-3"
      style={{ background: "var(--card)", border: "1px solid var(--border)" }}
    >
      <p className="text-[11px] font-semibold uppercase tracking-widest" style={{ color: "var(--gold)" }}>
        Next
      </p>
      <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>
        {nextLabel}
      </p>
    </div>
  );
}

export function PendingStartCard() {
  return (
    <div
      className="rounded-2xl px-5 py-6 text-center"
      style={{ background: "rgba(224,200,80,0.08)", color: "var(--gold)" }}
    >
      <p className="font-semibold">Waiting to start</p>
      <p className="text-sm mt-1" style={{ color: "var(--muted-foreground)" }}>
        The match has not started yet.
      </p>
    </div>
  );
}

export function FinishedCard({ elapsedMs }: { elapsedMs: number | null }) {
  return (
    <div
      className="rounded-2xl px-5 py-6 text-center"
      style={{ background: "rgba(74,222,128,0.08)", color: "#4ade80" }}
    >
      <p className="font-semibold">Finished</p>
      {elapsedMs != null && (
        <>
          <p className="text-2xl font-bold tabular-nums mt-2" style={{ fontFamily: "Geist Variable, monospace" }}>
            {formatTimeSmooth(elapsedMs)}
          </p>
          <p className="text-[11px] font-semibold uppercase tracking-widest mt-1" style={{ color: "var(--muted-foreground)" }}>
            Final time
          </p>
        </>
      )}
    </div>
  );
}

interface JudgeTimerRowProps {
  startedAt: string | null | undefined;
  finishedAt?: string | null;
  timeCapSeconds?: number | null;
}

export function JudgeTimerRow({ startedAt, finishedAt, timeCapSeconds }: JudgeTimerRowProps) {
  const elapsed = useTickElapsed(startedAt ?? null, finishedAt);
  return (
    <div
      className="rounded-xl px-4 py-3 flex items-center justify-between"
      style={{ background: "var(--card)", border: "1px solid var(--border)" }}
    >
      <div>
        <p className="text-[11px] font-semibold uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
          Time
        </p>
        <p
          className="text-xl font-bold tabular-nums leading-tight"
          style={{ fontFamily: "Geist Variable, monospace", color: "var(--foreground)" }}
        >
          {formatTimeSmooth(elapsed)}
        </p>
      </div>
      {timeCapSeconds != null && (
        <div className="text-right">
          <p className="text-[11px] font-semibold uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
            Time cap
          </p>
          <p className="text-lg font-semibold" style={{ color: "var(--gold)" }}>
            {formatSeconds(timeCapSeconds)}
          </p>
        </div>
      )}
    </div>
  );
}

function useTickElapsed(startedAt: string | null, fixedEnd?: string | null): number {
  const [elapsed, setElapsed] = React.useState(0);

  React.useEffect(() => {
    if (!startedAt) return;
    const startMs = new Date(startedAt).getTime();
    if (Number.isNaN(startMs)) return;

    const compute = () => {
      if (fixedEnd) {
        const endMs = new Date(fixedEnd).getTime();
        return Number.isNaN(endMs) ? 0 : Math.max(0, endMs - startMs);
      }
      return Math.max(0, Date.now() - startMs);
    };

    let raf = 0;
    const tick = () => {
      setElapsed(compute());
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [startedAt, fixedEnd]);

  return elapsed;
}