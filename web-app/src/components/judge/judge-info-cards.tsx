import { useState } from "react";
import type { Athlete } from "@/data/athletes";
import type { RoutineOverview } from "@/data/routines";
import { routineGroups } from "@/utils/exercise-labels";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
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
      className="rounded-xl px-4 py-3"
      style={{ background: "var(--card)", border: "1px solid var(--border)" }}
    >
      <div className="flex items-center justify-between gap-3">
        <span
          className="text-[11px] px-2.5 py-0.5 rounded-full font-semibold uppercase tracking-wider"
          style={{ background: `${accent}2b`, color: accent }}
        >
          {sideLabel(side)}
        </span>
        {finished && (
          <span
            className="text-[11px] px-2.5 py-0.5 rounded-full font-semibold"
            style={{ background: "rgba(74,222,128,0.12)", color: "#4ade80" }}
          >
            Finished
          </span>
        )}
      </div>
      <div className="flex items-center gap-3 mt-2">
        <JudgeAvatar name={athlete.name} accentColor={accent} size={36} />
        <div className="min-w-0">
          <p className="text-sm font-semibold leading-tight truncate" style={{ color: "var(--foreground)" }}>
            {athlete.name}
          </p>
          <p className="text-xs leading-tight truncate" style={{ color: "var(--muted-foreground)" }}>
            {clubName ?? "No club"}
          </p>
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
  const [open, setOpen] = useState(false);
  const groups = routineGroups(routine.exercises);
  const currentLabel = groups[currentGroupIndex]?.label;

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="w-full text-left rounded-xl px-4 py-3"
        style={{ background: "var(--card)", border: "1px solid var(--border)" }}
      >
        <p className="text-[11px] font-semibold uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
          Routine
        </p>
        <p className="text-sm font-medium leading-tight" style={{ color: "var(--foreground)" }}>
          {routine.name}
        </p>
        {groups.length > 0 && (
          <div className="flex gap-1.5 mt-2.5">
            {groups.map((_, i) => (
              <span
                key={i}
                className="flex-1 h-1.5 rounded-full"
                style={{
                  background: i < currentGroupIndex ? `${accentColor}66` : i === currentGroupIndex ? accentColor : "var(--secondary)",
                }}
              />
            ))}
          </div>
        )}
        {currentLabel && (
          <p className="text-sm font-semibold mt-2 leading-tight" style={{ color: accentColor }}>
            {currentLabel}
          </p>
        )}
        {routine.timeCapSeconds != null && (
          <p className="text-[11px] mt-2 uppercase tracking-widest" style={{ color: "var(--faint)" }}>
            Time cap {formatSeconds(routine.timeCapSeconds)}
          </p>
        )}
      </button>

      <Dialog open={open} onOpenChange={(v) => setOpen(v)}>
        <DialogContent className="sm:max-w-md max-h-[80vh] overflow-y-auto" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
          <DialogHeader>
            <DialogTitle>{routine.name}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 mt-2">
            {routine.timeCapSeconds != null && (
              <span
                className="inline-flex text-xs px-2.5 py-0.5 rounded-full font-semibold"
                style={{ background: "rgba(224,200,80,0.14)", color: "var(--gold)" }}
              >
                Time cap {formatSeconds(routine.timeCapSeconds)}
              </span>
            )}
            {groups.map((group, i) => (
              <div key={group.order}>
                <p className="text-sm font-semibold" style={{ color: "var(--foreground)" }}>
                  <span className="text-base font-bold mr-1.5" style={{ color: "var(--muted-foreground)" }}>
                    {i + 1}
                  </span>
                  {group.label}
                </p>
              </div>
            ))}
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}