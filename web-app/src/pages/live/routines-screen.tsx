import type { Routine, RoutineOverview } from "@/data/routines";
import type { Exercise } from "@/data/routines";
import type { ScreenRoutine } from "@/data/tournaments";
import { routineGroups, exerciseAbbreviation } from "@/utils/exercise-labels";
import { screenBackground } from "@/utils/screen-background";
import { ScreenHeader } from "./screen-header";

const ACCENT = "#e98a80";
const DIVIDER = "#262629";

interface RoutineExerciseLine {
  label: string;
  tag: "SUPERSET" | "UNBROKEN" | null;
}

interface RoutineCardData {
  id: number;
  title: string;
  timeCap: string | null;
  exercises: RoutineExerciseLine[];
}

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

  const cards: RoutineCardData[] = visible.map((sr) => {
    const routine = routines.find((r) => r.id === sr.routineId);
    const overview = routine ? overviews[routine.name] : null;
    const exercises = routineGroups(overview?.exercises ?? []).map((group): RoutineExerciseLine => {
      if (group.items.length > 1) {
        const [first, ...rest] = group.items;
        return {
          tag: "SUPERSET",
          label: `${first.targetReps} ${exerciseAbbreviation(first.name)}${weightSuffix(
            first.addedWeight,
          )} - ${rest
            .map((e) => `${e.targetReps} ${exerciseAbbreviation(e.name)}${weightSuffix(e.addedWeight)}`)
            .join(" - ")}`,
        };
      }
      const e = group.items[0];
      return {
        tag: e.type === "UNBROKEN" ? "UNBROKEN" : null,
        label: `${e.targetReps} ${e.name}${weightSuffix(e.addedWeight)}`,
      };
    });
    return {
      id: sr.id,
      title: sr.label ?? routine?.name ?? `Routine #${sr.routineId}`,
      timeCap: routine?.timeCapSeconds != null ? timeCapLabel(routine.timeCapSeconds) : null,
      exercises,
    };
  });

  const compact = cards.length <= 4;

  return (
    <div className="min-h-screen flex flex-col" style={{ ...screenBackground, color: "white" }}>
      <ScreenHeader tournamentName={tournamentName} accent={ACCENT} />

      {cards.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <p className="font-cairo text-[var(--spec-text-faint)] uppercase tracking-widest text-sm">
            No routines configured
          </p>
        </div>
      ) : compact ? (
        <div className="flex-1 flex flex-col justify-center px-5 md:px-10 lg:px-14 pb-6">
          <div className="flex flex-wrap justify-center gap-5 mb-20">
            {cards.map((card, index) => (
              <CardSlot key={card.id} index={index + 1} card={card} large />
            ))}
          </div>
        </div>
      ) : (
        <div className="flex-1 px-5 md:px-10 lg:px-14 pb-10">
          <div className="flex flex-wrap justify-center gap-4">
            {cards.map((card, index) => (
              <CardSlot key={card.id} index={index + 1} card={card} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function CardSlot({ index, card, large = false }: { index: number; card: RoutineCardData; large?: boolean }) {
  return (
    <div className="w-full md:w-[calc(50%-10px)] lg:w-[calc(25%-15px)] shrink-0">
      <RoutineCard index={index} card={card} large={large} />
    </div>
  );
}

function RoutineCard({ index, card, large = false }: { index: number; card: RoutineCardData; large?: boolean }) {
  return (
    <div
      className={`flex flex-col rounded-[12px] border border-[#242428] bg-[#141417] transition-all duration-300 hover:-translate-y-0.5 hover:border-[#3a3a3f] overflow-hidden h-full ${
        large ? "p-5 gap-1" : "p-4"
      }`}
    >
      <div className="flex items-start gap-3">
        <span
          className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full font-cairo font-bold tabular-nums ${
            large ? "h-8 w-8 text-sm" : "text-xs"
          }`}
          style={{ background: "rgba(233, 138, 128, 0.12)", color: ACCENT }}
        >
          {String(index).padStart(2, "0")}
        </span>
        <h3
          className={`font-cairo font-bold uppercase tracking-wider leading-snug text-white ${
            large ? "text-lg" : "text-base"
          }`}
          style={{ overflowWrap: "anywhere" }}
        >
          {card.title}
        </h3>
      </div>

      <div className="mt-2.5 h-px w-full" style={{ background: DIVIDER }} />

      <div className="mt-4 flex-1 space-y-2">
        {card.exercises.map((exercise, i) => (
          <div key={i} className="flex items-baseline gap-3">
            <span
              className={`w-6 shrink-0 text-right font-cairo tabular-nums ${
                large ? "text-sm" : "text-xs"
              }`}
              style={{ color: "var(--spec-text-dim)" }}
            >
              {String(i + 1).padStart(2, "0")}
            </span>
            <span
              className={`min-w-0 flex-1 font-cairo text-white/85 ${
                large ? "text-base" : "text-sm"
              }`}
              style={{ overflowWrap: "anywhere" }}
            >
              {exercise.label}
            </span>
            {exercise.tag && <ExerciseTag tag={exercise.tag} />}
          </div>
        ))}
      </div>

      {card.timeCap && (
        <>
          <div className="mt-4 h-px w-full" style={{ background: DIVIDER }} />
          <div className="mt-3 flex items-center justify-between">
            <span
              className={`font-semibold uppercase tracking-[0.3em] ${
                large ? "text-[0.65rem]" : "text-[0.6rem]"
              }`}
              style={{ color: "var(--spec-text-dim)" }}
            >
              Time Cap
            </span>
            <span
              className={`rounded-full border px-2.5 py-0.5 font-cairo font-semibold tabular-nums ${
                large ? "text-sm" : "text-[0.7rem]"
              }`}
              style={{ borderColor: "rgba(233, 138, 128, 0.4)", color: ACCENT }}
            >
              {card.timeCap}
            </span>
          </div>
        </>
      )}
    </div>
  );
}

function ExerciseTag({ tag }: { tag: Exercise["type"] }) {
  const palette =
    tag === "UNBROKEN"
      ? { color: "#7eb8f7", bg: "rgba(126, 184, 247, 0.12)" }
      : { color: "#f0c46a", bg: "rgba(240, 196, 106, 0.12)" };
  return (
    <span
      className="shrink-0 rounded-full px-1.5 py-0.5 text-[0.55rem] font-semibold uppercase tracking-widest"
      style={{ color: palette.color, background: palette.bg }}
    >
      {tag}
    </span>
  );
}

function weightSuffix(addedWeight: number | null | undefined): string {
  return addedWeight == null ? "" : ` (+${addedWeight}KG)`;
}

function timeCapLabel(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return rest > 0 ? `${minutes}M ${rest}S` : `${minutes} MIN`;
}