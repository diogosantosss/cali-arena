import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { useMatchControl } from "@/hooks/use-match-control";
import { athletesService } from "@/services/athletes.service";
import { clubsService } from "@/services/clubs.service";
import { routinesService } from "@/services/routines.service";
import { tournamentsService } from "@/services/tournaments.service";
import type { Athlete } from "@/data/athletes";
import type { Club } from "@/data/clubs";
import type { Routine, RoutineOverview } from "@/data/routines";
import { JudgeTopBar } from "@/components/judge/judge-top-bar";
import { JudgeErrorBanner } from "@/components/judge/judge-banners";
import { JudgeLoading, JudgeUnassignedCard } from "@/components/judge/judge-status-views";
import { JudgeSideChooser } from "@/components/judge/judge-side-chooser";
import { JudgePanel } from "@/components/judge/judge-panel";
import type { Side } from "@/components/judge/judge-utils";
import { sideToLower } from "@/components/judge/judge-utils";

interface JudgeSources {
  athletes: Athlete[];
  clubs: Club[];
  routines: Routine[];
}

export function JudgeMatchPage() {
  const { matchId } = useParams<{ matchId: string }>();
  const id = Number(matchId);

  const [selectedSide, setSelectedSide] = useState<Side | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [overview, setOverview] = useState<RoutineOverview | null>(null);
  const [overviewFor, setOverviewFor] = useState<string | null>(null);
  const [division, setDivision] = useState<string | null>(null);
  const [sources, setSources] = useState<JudgeSources>({ athletes: [], clubs: [], routines: [] });

  const onServerError = useCallback((message: string) => {
    setErrorMessage(message);
  }, []);

  const { currentMatch, progress, redReps, blueReps, adjustReps, finishSide } = useMatchControl(
    Number.isFinite(id) ? id : 0,
    onServerError,
  );

  useEffect(() => {
    let cancelled = false;
    Promise.all([athletesService.getAthletes(), clubsService.getClubs(), routinesService.getRoutines()])
      .then(([athletes, clubs, routines]) => {
        if (!cancelled) setSources({ athletes, clubs, routines });
      })
      .catch(() => {
        // metadata is best-effort; the match panel still works without it
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const routineName = sources.routines.find((r) => r.id === currentMatch?.routineId)?.name;

  const bracketId = currentMatch?.bracketId;

  useEffect(() => {
    if (bracketId == null) return;
    let cancelled = false;
    tournamentsService
      .getBracketById(bracketId)
      .then((bracket) => {
        if (!cancelled) setDivision(bracket.division);
      })
      .catch(() => {
        // division is best-effort
      });
    return () => {
      cancelled = true;
    };
  }, [bracketId]);

  useEffect(() => {
    if (!routineName) return;
    let cancelled = false;
    routinesService
      .getRoutineOverview(routineName)
      .then((ov) => {
        if (!cancelled) {
          setOverview(ov);
          setOverviewFor(routineName);
        }
      })
      .catch(() => {
        // overview is best-effort
      });
    return () => {
      cancelled = true;
    };
  }, [routineName]);

  const activeOverview = overviewFor === routineName && overview ? overview : undefined;

  async function run(action: () => Promise<unknown>) {
    try {
      await action();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : "Unexpected error");
    }
  }

  if (!Number.isFinite(id)) {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ background: "var(--background)" }}>
        <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>Invalid match.</p>
      </div>
    );
  }

  const redAthlete = sources.athletes.find((a) => a.id === currentMatch?.athleteRedId);
  const blueAthlete = sources.athletes.find((a) => a.id === currentMatch?.athleteBlueId);
  const clubRedName = redAthlete?.clubId != null ? sources.clubs.find((c) => c.id === redAthlete.clubId)?.name : undefined;
  const clubBlueName = blueAthlete?.clubId != null ? sources.clubs.find((c) => c.id === blueAthlete.clubId)?.name : undefined;

  return (
    <div
      className="min-h-screen flex flex-col"
      style={{ background: "var(--background)" }}
    >
      <JudgeTopBar
        title={division ?? "Match"}
        status={currentMatch?.status ?? null}
      />

      <main className="flex-1 flex flex-col overflow-y-auto">
        <div className="max-w-2xl w-full mx-auto px-4 py-4 flex flex-col flex-1 min-h-0 gap-3">
          {errorMessage && (
            <JudgeErrorBanner message={errorMessage} onDismiss={() => setErrorMessage(null)} />
          )}

          {!currentMatch ? (
            <JudgeLoading />
          ) : selectedSide === null ? (
            <JudgeSideChooser
              choices={[
                { side: "RED", athlete: redAthlete, clubName: clubRedName },
                { side: "BLUE", athlete: blueAthlete, clubName: clubBlueName },
              ]}
              onSelectSide={setSelectedSide}
            />
          ) : !redAthlete && !blueAthlete ? (
            <JudgeUnassignedCard />
          ) : (
            <JudgePanel
              side={selectedSide}
              match={currentMatch}
              progress={progress}
              athlete={selectedSide === "RED" ? redAthlete : blueAthlete}
              clubName={selectedSide === "RED" ? clubRedName : clubBlueName}
              overview={activeOverview}
              reps={selectedSide === "RED" ? redReps : blueReps}
              onAdjust={(delta) => void run(() => adjustReps(sideToLower(selectedSide), delta))}
              onFinish={() => void run(() => finishSide(sideToLower(selectedSide)))}
            />
          )}
        </div>
      </main>
    </div>
  );
}