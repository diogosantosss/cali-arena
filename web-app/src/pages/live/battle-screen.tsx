import type { Exercise, Routine, RoutineOverview } from "@/data/routines";
import {
  routineGroups,
  nextLabel,
  exerciseProgress,
  type ExerciseProgress,
} from "@/utils/exercise-labels";
import type { Athlete } from "@/data/athletes";
import type { Match, MatchProgress } from "@/data/matches";
import { screenBackground } from "@/utils/screen-background";
import { formatTime } from "@/utils/format-time";
import { useElapsedMs } from "@/hooks/use-elapsed-ms";

export function BattleScreen({
  tournamentName,
  match,
  progress,
  athletes,
  routines,
  overviews,
}: {
  tournamentName: string;
  match: Match;
  progress: MatchProgress | null;
  athletes: Athlete[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
}) {
  const elapsed = useElapsedMs(progress?.timerStartedAt ?? null);

  const routine = routines.find((r) => r.id === match.routineId);
  const exercises: Exercise[] = routine
    ? (overviews[routine.name]?.exercises.slice().sort((a, b) => a.exerciseOrder - b.exerciseOrder) ?? [])
    : [];
  const groups = routineGroups(exercises);

  const redAthlete = athletes.find((a) => a.id === match.athleteRedId);
  const blueAthlete = athletes.find((a) => a.id === match.athleteBlueId);

  const redExercise = exercises.find((e) => e.id === progress?.redCurrentExerciseId) ?? exercises[0];
  const blueExercise = exercises.find((e) => e.id === progress?.blueCurrentExerciseId) ?? exercises[0];
  const redNextLabel = nextLabel(exercises, progress?.redCurrentExerciseId);
  const blueNextLabel = nextLabel(exercises, progress?.blueCurrentExerciseId);
  const redProgress = exerciseProgress(exercises, progress?.redCurrentExerciseId, progress?.redCurrentReps ?? 0);
  const blueProgress = exerciseProgress(exercises, progress?.blueCurrentExerciseId, progress?.blueCurrentReps ?? 0);

  const redFinished = !!progress?.redFinishedAt;
  const blueFinished = !!progress?.blueFinishedAt;

  const singleAthlete = (redAthlete != null) !== (blueAthlete != null);
  const loneFinished = redAthlete != null ? redFinished : blueFinished;

  function finishMs(finishedAt: string) {
    if (!progress?.timerStartedAt) return 0;
    return new Date(finishedAt).getTime() - new Date(progress.timerStartedAt).getTime();
  }

  function finishTime(finishedAt: string) {
    return formatTime(Math.max(0, finishMs(finishedAt)));
  }

  const redWon =
    (redFinished && blueFinished &&
      new Date(progress!.redFinishedAt!).getTime() < new Date(progress!.blueFinishedAt!).getTime()) ||
    (singleAthlete && redFinished);
  const blueWon =
    (redFinished && blueFinished &&
      new Date(progress!.blueFinishedAt!).getTime() < new Date(progress!.redFinishedAt!).getTime()) ||
    (singleAthlete && blueFinished);

  const matchFinished = (redFinished && blueFinished) || (singleAthlete && loneFinished);

  const finalElapsedMs = (() => {
    if (matchFinished && progress) {
      return Math.max(
        redFinished && progress.redFinishedAt ? finishMs(progress.redFinishedAt) : 0,
        blueFinished && progress.blueFinishedAt ? finishMs(progress.blueFinishedAt) : 0,
      );
    }
    return elapsed;
  })();

  const timerColor = redWon || blueWon ? "var(--spec-green)" : "var(--spec-text-white)";

  return (
    <div
      className="min-h-screen flex flex-col overflow-hidden"
      style={screenBackground}
    >
      <div
        className="absolute inset-0 pointer-events-none"
        style={{
          background: "radial-gradient(ellipse 70% 40% at 50% 0%, var(--spec-accent-06) 0%, transparent 70%)",
        }}
      />

      <div className="relative z-10 flex flex-col min-h-screen">
        <div className="text-center pt-20 pb-8 px-16">
          <p className="font-cairo text-6xl font-semibold leading-tight uppercase bg-gradient-to-r from-[var(--spec-accent)] to-[var(--spec-title-end)] bg-clip-text text-transparent">
            {tournamentName}
          </p>
        </div>

        <div className="flex-1 grid grid-cols-3 px-24">
          {redAthlete ? (
            <AthletePanel
              finishState={{
                finished: redFinished,
                won: redWon,
                lost: blueWon,
                str: redFinished ? finishTime(progress!.redFinishedAt!) : null,
              }}
              name={redAthlete.name}
              color="var(--spec-red)"
              currentExercise={redExercise}
              currentReps={progress?.redCurrentReps ?? 0}
              progress={redProgress}
              nextExercise={redNextLabel}
            />
          ) : (
            <NoShowPanel color="var(--spec-red)" />
          )}

          <div className="flex flex-col items-center">
            <p
              className="pt-16 font-cairo text-[6rem] font-bold leading-none tabular-nums transition-colors duration-700"
              style={{ color: timerColor }}
            >
              {formatTime(finalElapsedMs)}
            </p>

            <p className="mt-16 font-cairo text-[2.5rem] font-bold leading-none text-white">
              {routine?.name ?? "—"}
            </p>

            <div className="flex flex-col items-center gap-0.1 mt-4 font-cairo text-[2rem] font-semibold text-white">
              {groups.map((group) => {
                const hasRed = group.items.some((e) => e.id === progress?.redCurrentExerciseId);
                const hasBlue = group.items.some((e) => e.id === progress?.blueCurrentExerciseId);
                return (
                  <p key={group.order} className="flex items-center gap-3">
                    <span
                      className="w-4 h-4 rounded-full transition-all duration-300"
                      style={{ background: "var(--spec-red)", opacity: hasRed ? 1 : 0, transform: hasRed ? "scale(1)" : "scale(0.6)" }}
                    />
                    <span>{group.label}</span>
                    <span
                      className="w-4 h-4 rounded-full transition-all duration-300"
                      style={{ background: "var(--spec-blue)", opacity: hasBlue ? 1 : 0, transform: hasBlue ? "scale(1)" : "scale(0.6)" }}
                    />
                  </p>
                );
              })}
            </div>

            {routine && (
              <div className="mt-10 rounded-[20px] bg-[var(--spec-surface)] px-8 py-3">
                <p className="font-cairo text-[2.25rem] font-bold text-white whitespace-nowrap">
                  Time Cap: {routine.timeCapSeconds ? `${Math.floor(routine.timeCapSeconds / 60)}’${routine.timeCapSeconds % 60 > 0 ? ` ${routine.timeCapSeconds % 60}”` : ""}` : "—"}
                </p>
              </div>
            )}
          </div>

          {blueAthlete ? (
            <AthletePanel
              finishState={{
                finished: blueFinished,
                won: blueWon,
                lost: redWon,
                str: blueFinished ? finishTime(progress!.blueFinishedAt!) : null,
              }}
              name={blueAthlete.name}
              color="var(--spec-blue)"
              currentExercise={blueExercise}
              currentReps={progress?.blueCurrentReps ?? 0}
              progress={blueProgress}
              nextExercise={blueNextLabel}
            />
          ) : (
            <NoShowPanel color="var(--spec-blue)" />
          )}
        </div>
      </div>
    </div>
  );
}

function AthletePanel({
  finishState,
  name,
  color,
  currentExercise,
  currentReps,
  progress,
  nextExercise,
}: {
  finishState: { finished: boolean; won: boolean; lost: boolean; str: string | null };
  name: string;
  color: string;
  currentExercise: Exercise | undefined;
  currentReps: number;
  progress: ExerciseProgress;
  nextExercise: string | null | undefined;
}) {
  const finishColor = finishState.won ? "var(--spec-green)" : finishState.lost ? "var(--spec-red)" : "var(--spec-text-white)";
  const isFinished = finishState.finished && finishState.str !== null;
  const pct = isFinished ? 100 : progress.pct;

  return (
    <div className="mt-25 flex flex-col items-center pt-16">
      <div className="flex items-center gap-3">
        <span className="w-5 h-5 rounded-full" style={{ background: color }} />
        <p className="font-cairo text-[3rem] font-bold text-white leading-none">
          {name}
        </p>
      </div>

      <div className="flex flex-col items-center mt-[4.75rem] gap-2">
        {isFinished ? (
          <>
            <p className="font-cairo text-[2.25rem] font-bold" style={{ color: finishColor }}>
              {finishState.won ? "Winner" : "Finished"}
            </p>
            <p className="font-cairo text-[3.75rem] font-bold tabular-nums leading-none" style={{ color: finishColor }}>
              {finishState.str}
            </p>
          </>
        ) : (
          <>
            <div key={currentExercise?.id ?? "none"} className="flex flex-col items-center gap-2 min-h-[6rem] animate-fade-scale">
              <p className="font-cairo text-[3.75rem] font-bold leading-none text-white">
                {currentExercise?.name ?? "—"}
                {currentExercise?.addedWeight ? (
                  <span className="ml-4 align-middle text-[2.25rem] font-bold bg-gradient-to-r from-[var(--spec-accent)] to-[var(--spec-title-end)] bg-clip-text text-transparent">
                    ({currentExercise.addedWeight > 0 ? "+" : ""}
                    {currentExercise.addedWeight}KG)
                  </span>
                ) : null}
              </p>
              <p className="font-cairo text-[1.75rem] font-bold uppercase leading-none tracking-[0.25em] text-[var(--spec-gold)]">
                {currentExercise?.type !== undefined && currentExercise.type !== "NORMAL"
                  ? currentExercise.type === "SUPERSET"
                    ? "Superset"
                    : "Unbroken"
                  : ""}
              </p>
            </div>
            <p className="font-cairo text-[3.75rem] font-bold leading-none mt-2 tabular-nums text-white">
              <span key={currentReps} className="inline-block animate-rep-pop">
                {currentReps}
              </span>
              /{currentExercise?.targetReps ?? "—"}
            </p>
          </>
        )}
      </div>

      <div className="mt-6 flex w-96 items-center gap-3">
        <div className="flex-1 h-3 rounded-full overflow-hidden" style={{ background: "var(--spec-track)" }}>
          <div
            className="h-full rounded-full transition-all duration-300"
            style={{ width: `${pct}%`, background: color }}
          />
        </div>
        <span className="font-cairo text-[1.75rem] font-bold tabular-nums text-white">
          {pct}%
        </span>
      </div>

      {nextExercise && !isFinished && (
        <p
          key={nextExercise}
          className="mt-42 font-cairo text-[2.5rem] font-bold leading-none bg-gradient-to-r from-[var(--spec-accent)] to-[var(--spec-title-end)] bg-clip-text text-transparent animate-fade-scale"
        >
          Next: {nextExercise}
        </p>
      )}
    </div>
  );
}

function NoShowPanel({ color }: { color: string }) {
  return (
    <div className="mt-25 flex flex-col items-center pt-16">
      <div className="flex items-center gap-3">
        <span className="w-5 h-5 rounded-full" style={{ background: color }} />
        <p className="font-cairo text-[3rem] font-bold leading-none text-white">No athlete</p>
      </div>
    </div>
  );
}