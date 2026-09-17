import { useEffect, useRef } from "react";
import type { Athlete } from "@/data/athletes";
import type { RoutineOverview } from "@/data/routines";
import { routineGroups, exerciseAbbreviation, type RoutineGroup } from "@/utils/exercise-labels";
import { JudgeAvatar } from "./judge-side-chooser";
import { sideColor, sideLabel, formatSeconds, type Side } from "./judge-utils";

interface AthleteCardProps {
  athlete: Athlete;
  side: Side;
  clubName: string | null | undefined;
  finished: boolean;
}

export function AthleteCard({ athlete, side, clubName, finished }: AthleteCardProps) {
  const accent = sideColor(side);
  return (
    <div
      className="rounded-xl flex items-center gap-3 px-4 py-3 animate-fade-up"
      style={{
        background: "var(--card)",
        border: "1px solid var(--border)",
        borderLeft: `3px solid ${accent}`,
      }}
    >
      <JudgeAvatar name={athlete.name} accentColor={accent} size={40} />

      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-2">
          <p className="text-sm font-bold leading-tight truncate" style={{ color: "var(--foreground)" }}>
            {athlete.name}
          </p>
          <span
            className="text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider shrink-0"
            style={{ background: `${accent}2b`, color: accent }}
          >
            {sideLabel(side)}
          </span>
        </div>
        <div className="flex items-center justify-between gap-2 mt-0.5">
          <p className="text-xs leading-tight truncate" style={{ color: "var(--muted-foreground)" }}>
            {clubName ?? "No club"}
          </p>
          {finished && (
            <span
              className="text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider shrink-0"
              style={{ background: "rgba(74,222,128,0.12)", color: "#4ade80" }}
            >
              Finished
            </span>
          )}
        </div>
      </div>
    </div>
  );
}

interface RoutineCardProps {
  routine: RoutineOverview;
  currentGroupIndex: number;
  accentColor: string;
}

export function RoutineCard({ routine, currentGroupIndex, accentColor }: RoutineCardProps) {
  const groups = routineGroups(routine.exercises);
  const scrollRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<(HTMLDivElement | null)[]>([]);

  useEffect(() => {
    const container = scrollRef.current;
    const el = itemRefs.current[currentGroupIndex];
    if (!container || !el) return;
    const top = el.getBoundingClientRect().top - container.getBoundingClientRect().top + container.scrollTop;
    container.scrollTo({ top: top - 8, behavior: "smooth" });
  }, [currentGroupIndex]);

  function formatGroup(group: RoutineGroup): string {
    const weight = (w: number | null | undefined) => (w ? ` (+${w}KG)` : "");
    if (group.items.length > 1) {
      return group.items
        .map((e) => `${e.targetReps} ${exerciseAbbreviation(e.name)}${weight(e.addedWeight)}`)
        .join(" - ");
    }
    const e = group.items[0];
    return `${e.targetReps} ${e.name}${weight(e.addedWeight)}`;
  }

  return (
    <div
      className="rounded-xl flex flex-col shrink-0 animate-fade-up"
      style={{ background: "var(--card)", border: "1px solid var(--border)", overflow: "hidden" }}
    >
      <div className="px-3 py-2">
        <div className="flex items-center justify-between gap-2">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
            Routine
          </p>
          {routine.timeCapSeconds != null && (
            <span
              className="text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full tabular-nums"
              style={{ background: "rgba(224,200,80,0.14)", color: "var(--gold)" }}
            >
              {formatSeconds(routine.timeCapSeconds)} cap
            </span>
          )}
        </div>
        <p className="text-sm font-semibold mt-0.5 leading-tight" style={{ color: "var(--foreground)" }}>
          {routine.name}
        </p>
      </div>

      <div className="border-t" style={{ borderColor: "var(--border)" }}>
        <div ref={scrollRef} className="px-3 py-2 max-h-40 overflow-y-auto space-y-1">
          {groups.map((group, i) => {
            const isCurrent = i === currentGroupIndex;
            return (
              <div
                key={group.order}
                ref={(el) => {
                  itemRefs.current[i] = el;
                }}
                className="flex items-center gap-2 rounded-md px-1.5 py-1"
                style={{ background: isCurrent ? `${accentColor}14` : "transparent" }}
              >
                <span
                  className="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold shrink-0"
                  style={
                    isCurrent
                      ? { background: accentColor, color: "#fff" }
                      : { background: "var(--secondary)", color: "var(--muted-foreground)" }
                  }
                >
                  {i + 1}
                </span>
                <span
                  className="truncate text-xs font-medium"
                  style={{ color: isCurrent ? accentColor : "var(--foreground)" }}
                >
                  {formatGroup(group)}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}