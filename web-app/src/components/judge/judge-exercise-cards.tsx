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
  isSuperset?: boolean;
  itemIndex?: number;
  totalItems?: number;
}

export function CurrentExerciseCard({
  label,
  reps,
  targetReps,
  accentColor,
  enabled,
  onDecrement,
  onIncrement,
  isSuperset = false,
  itemIndex = 0,
  totalItems = 1,
}: CurrentExerciseCardProps) {
  return (
    <div
      className="rounded-2xl flex flex-col flex-1 min-h-0 animate-fade-up"
      style={{
        background: "var(--card)",
        border: "1px solid var(--border)",
        minHeight: "clamp(180px, 28vw, 300px)",
        width: "100%",
        margin: "0 auto",
        overflow: "auto",
      }}
    >
      {/* Header: Current exercise + superset badge */}
      <header className="flex items-center justify-between px-4 py-2.5 sm:px-5 sm:py-3 border-b" style={{ borderColor: "var(--border)" }}>
        <h2 className="text-[10px] sm:text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
          Current exercise
        </h2>
        {isSuperset && totalItems > 1 && (
          <span
            className="text-[9px] sm:text-[10px] font-medium px-1.5 py-0.5 sm:px-2 rounded-full"
            style={{
              background: "var(--accent-12)",
              color: "var(--accent)",
            }}
          >
            {itemIndex + 1} / {totalItems}
          </span>
        )}
      </header>

      {/* Exercise name */}
      <div className="px-4 py-3 sm:px-5 sm:py-4">
        <h3
          key={label}
          className="text-lg sm:text-xl font-semibold leading-tight text-center animate-fade-scale"
          style={{ color: accentColor }}
        >
          {label}
        </h3>
      </div>

      {/* Main content: centered counter with side buttons */}
      <main className="flex-1 flex flex-col items-center justify-center px-3 py-1.5 sm:px-4 sm:py-2">
        <div className="flex items-center justify-center gap-3 sm:gap-4 w-full max-w-sm">
          {/* Decrement button */}
          <button
            disabled={!enabled}
            onClick={onDecrement}
            aria-label="Remove one rep"
            className="w-14 h-14 shrink-0 flex items-center justify-center rounded-full text-2xl font-bold select-none touch-manipulation transition-all duration-150 active:scale-90 active:opacity-70 disabled:opacity-30 disabled:scale-100"
            style={{
              background: "var(--secondary)",
              color: "var(--secondary-foreground)",
              border: "1px solid var(--border)",
              WebkitTapHighlightColor: "transparent",
            }}
          >
            −
          </button>

          {/* Counter display */}
          <div className="flex-1 flex flex-col items-center justify-center min-w-[100px] sm:min-w-[120px] md:min-w-[140px]">
            <div className="flex items-center baseline gap-1">
              <span
                key={reps}
                className="inline-block text-3xl sm:text-4xl md:text-5xl font-bold tabular-nums animate-rep-roll"
                style={{ color: accentColor, fontFamily: "Geist Variable, monospace", lineHeight: 1 }}
              >
                {reps}
              </span>
              {targetReps != null && (
                <span
                  className="text-lg sm:text-xl font-medium text-faint self-end mb-1"
                  style={{ fontWeight: 500, lineHeight: 1 }}
                >
                  /{targetReps}
                </span>
              )}
            </div>
            <p className="text-[10px] sm:text-[11px] uppercase tracking-widest mt-1 text-muted-foreground">
              reps
            </p>
          </div>

          {/* Increment button */}
          <button
            disabled={!enabled}
            onClick={onIncrement}
            aria-label="Add one rep"
            className="w-14 h-14 shrink-0 flex items-center justify-center rounded-full text-2xl font-bold select-none touch-manipulation transition-all duration-150 active:scale-90 active:opacity-70 disabled:opacity-30 disabled:scale-100"
            style={{ background: accentColor, color: "#fff", WebkitTapHighlightColor: "transparent" }}
          >
            +
          </button>
        </div>
      </main>
    </div>
  );
}

export function NextExerciseCard({ nextLabel }: { nextLabel: string }) {
  return (
    <div
      className="rounded-2xl px-5 py-3 animate-fade-up"
      style={{ background: "var(--card)", border: "1px solid var(--border)" }}
    >
      <div className="flex items-center gap-2">
        <span
          className="text-[11px] font-semibold uppercase tracking-widest px-2 py-0.5 rounded"
          style={{ background: "var(--gold-12)", color: "var(--gold)" }}
        >
          Next
        </span>
        <p className="text-sm font-medium truncate" style={{ color: "var(--foreground)" }}>
          {nextLabel}
        </p>
      </div>
    </div>
  );
}

export function PendingStartCard() {
  return (
    <div
      className="rounded-2xl flex-1 min-h-0 flex flex-col border animate-fade-up"
      style={{
        background: "rgba(224,200,80,0.08)",
        borderColor: "var(--border)",
        color: "var(--gold)",
        minHeight: "clamp(180px, 28vw, 300px)",
      }}
    >
      <div className="flex-1 flex flex-col items-center justify-center px-5 py-6 text-center">
        <p className="font-semibold">Waiting to start</p>
        <p className="text-sm mt-1" style={{ color: "var(--muted-foreground)" }}>
          The match has not started yet.
        </p>
      </div>
    </div>
  );
}

export function FinishedCard({ elapsedMs }: { elapsedMs: number | null }) {
  return (
    <div
      className="rounded-2xl flex flex-col flex-1 min-h-0 animate-fade-up"
      style={{
        background: "rgba(74,222,128,0.08)",
        border: "1px solid var(--border)",
        color: "#4ade80",
        minHeight: "clamp(180px, 28vw, 300px)",
        width: "100%",
        margin: "0 auto",
        overflow: "auto",
      }}
    >
      <div className="flex-1 flex flex-col items-center justify-center px-5 py-6 text-center">
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
      className="rounded-xl px-4 py-3 flex items-center justify-between animate-fade-up"
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