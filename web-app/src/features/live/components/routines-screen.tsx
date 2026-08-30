import { routineGroups } from "@/features/routines/lib/exercise-labels";
import type { Routine, RoutineOverview } from "@/features/routines/types";
import type { ScreenRoutine } from "@/features/tournaments/types";
import { screenBackground } from "../lib/screen-background";

export function RoutinesScreen({
  tournamentName,
  screenRoutines,
  routines,
  overviews,
}: {
  tournamentName: string;
  screenRoutines: ScreenRoutine[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
}) {
  const visible = screenRoutines
    .filter((sr) => sr.isVisible)
    .sort((a, b) => a.displayOrder - b.displayOrder);

  const compact = visible.length > 4;
  const rowSize = Math.min(visible.length, 4);
  const rows: ScreenRoutine[][] = [];
  for (let i = 0; i < visible.length; i += rowSize) {
    rows.push(visible.slice(i, i + rowSize));
  }

  const headingClass = compact ? "text-[1.5rem] mb-4" : "text-[2rem] mb-8";
  const groupClass = compact ? "space-y-1 text-[1.2rem]" : "space-y-2 text-[1.5rem]";

  return (
    <div className="min-h-screen flex flex-col" style={{ ...screenBackground, color: "white" }}>
      <div className="text-center pt-20 pb-8 px-16">
        <p className="font-cairo font-semibold leading-tight uppercase bg-gradient-to-r from-[var(--spec-accent)] to-[var(--spec-title-end)] bg-clip-text text-transparent text-6xl">
          {tournamentName}
        </p>
      </div>

      {visible.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <p className="font-cairo text-[var(--spec-text-faint)] uppercase tracking-widest text-sm">No routines configured</p>
        </div>
      ) : (
        <div className={`flex-1 flex flex-col items-center justify-center px-16 ${compact ? "gap-y-10" : "gap-y-14"}`}>
          {rows.map((row, rowIndex) => (
            <div key={rowIndex} className="flex justify-center gap-x-12">
              {row.map((sr) => {
                const routine = routines.find((r) => r.id === sr.routineId);
                const overview = routine ? overviews[routine.name] : null;
                return (
                  <div key={sr.id} className="flex flex-1 max-w-[40rem] flex-col items-center text-center px-4">
                    <h2 className={`w-full whitespace-nowrap font-cairo font-bold uppercase tracking-widest text-white ${headingClass}`}>
                      {sr.label ?? routine?.name ?? `Routine #${sr.routineId}`}
                    </h2>
                    <div className={groupClass}>
                      {routineGroups(overview?.exercises ?? []).map((group) => (
                        <p key={group.order} className="font-cairo font-semibold text-white">
                          {group.label}
                        </p>
                      ))}
                    </div>
                    {routine?.timeCapSeconds && (
                      <div className={`mx-auto mt-auto w-full ${compact ? "pt-6" : "pt-8"}`}>
                        <div className={`mx-auto w-max rounded-[20px] ${compact ? "px-4 py-1.5" : "px-5 py-2"}`} style={{ background: "var(--spec-surface)" }}>
                          <p className={`font-cairo font-semibold uppercase tracking-[0.2em] ${compact ? "text-xs" : "text-sm"}`} style={{ color: "var(--spec-accent-75)" }}>
                            Time Cap — {Math.floor(routine.timeCapSeconds / 60)}M
                            {routine.timeCapSeconds % 60 > 0 ? ` ${routine.timeCapSeconds % 60}S` : ""}
                          </p>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}