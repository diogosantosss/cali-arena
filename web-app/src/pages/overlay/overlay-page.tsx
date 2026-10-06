import { useEffect, useState } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import { loadTheme } from "@/theme/theme";
import { useSpectatorSSE } from "@/hooks/use-spectator-sse";
import { tournamentsService } from "@/services/tournaments.service";
import { matchesService } from "@/services/matches.service";
import { routinesService } from "@/services/routines.service";
import { athletesService } from "@/services/athletes.service";
import { Overlay } from "@/pages/overlay/overlay-screen";
import { buildOverlay, type OverlayData } from "@/data/overlay";
import type { Athlete } from "@/data/athletes";
import type { Match, MatchProgress } from "@/data/matches";
import type { Exercise, Routine } from "@/data/routines";
import type { BracketStage } from "@/data/tournaments";
import type { SpectatorEvent } from "@/data/live";

const empty: OverlayData = {
  red: null,
  blue: null,
  time: "00:00.000",
  timerStartedAt: null,
  finished: false,
  stage: null,
  timeCap: null,
};

export function OverlayPage() {
  useEffect(() => {
    loadTheme();
    // Lets the base layer drop its opaque background so the OBS browser source
    // composites over the camera instead of covering it.
    document.documentElement.classList.add("overlay-transparent");
    return () => document.documentElement.classList.remove("overlay-transparent");
  }, []);

  const { tournamentId } = useParams<{ tournamentId: string }>();
  const [params] = useSearchParams();

  const [match, setMatch] = useState<Match | null>(null);
  const [progress, setProgress] = useState<MatchProgress | null>(null);
  const [athletes, setAthletes] = useState<Athlete[]>([]);
  const [routine, setRoutine] = useState<Routine | null>(null);
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [stage, setStage] = useState<BracketStage | null>(null);
  const [currentMatchId, setCurrentMatchId] = useState<number | null>(null);

  const id = tournamentId ? Number(tournamentId) : undefined;
  const pinned = params.get("match");
  const pinnedMatchId = pinned ? Number(pinned) : null;

  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    athletesService
      .getAthletes()
      .then((list) => {
        if (!cancelled) setAthletes(list);
      })
      .catch(() => { });

    return () => {
      cancelled = true;
    };
  }, [id]);

  const attachMatch = (matchId: number, isStale: () => boolean) => {
    matchesService
      .getMatchById(matchId)
      .then(async (m) => {
        const routines = await routinesService.getRoutines();
        const r = routines.find((x) => x.id === m.routineId) ?? null;
        const [overview, prog, bracket] = await Promise.all([
          r ? routinesService.getRoutineOverview(r.name).catch(() => null) : null,
          matchesService.getProgressByMatchId(m.id).catch(() => null),
          tournamentsService.getBracketById(m.bracketId).catch(() => null),
        ]);
        return { m, r, overview, prog, bracket };
      })
      .then((res) => {
        if (!res || isStale()) return;
        setMatch(res.m);
        setRoutine(res.r);
        setExercises(res.overview?.exercises ?? []);
        setProgress(res.prog);
        setStage(res.bracket?.stage ?? null);
      })
      .catch(() => { });
  };

  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    if (pinnedMatchId != null) {
      attachMatch(pinnedMatchId, () => cancelled);
      return () => {
        cancelled = true;
      };
    }

    tournamentsService
      .getTournamentState(id)
      .then((state) => {
        if (cancelled) return;
        setCurrentMatchId(state.currentMatchId);
        if (state.currentMatchId != null) attachMatch(state.currentMatchId, () => cancelled);
      })
      .catch(() => { });

    return () => {
      cancelled = true;
    };
  }, [id, pinnedMatchId]);

  const handleEvent = (event: SpectatorEvent) => {
    if (event.action === "MATCH_UPDATED") {
      if (event.matchProgress.matchId === currentMatchId) {
        setProgress(event.matchProgress);
      }
      return;
    }

    if (event.action === "TOURNAMENT_STATE_UPDATED" && pinnedMatchId == null) {
      const next = event.currentMatchId;
      if (next === currentMatchId) return;
      setCurrentMatchId(next);
      setMatch(null);
      setProgress(null);
      setExercises([]);
      setRoutine(null);
      setStage(null);
      if (next != null) attachMatch(next, () => false);
    }
  };

  useSpectatorSSE(id, handleEvent);

  const data =
    match && (pinnedMatchId != null || match.id === currentMatchId)
      ? buildOverlay(match, progress, athletes, routine ? [routine] : [], exercises, stage)
      : empty;

  if (params.get("debug") === "1") {
    return (
      <div className="p-8 font-mono text-sm">
        <pre>
          {JSON.stringify(
            { id, currentMatchId, matchId: match?.id ?? null, stage, data },
            null,
            2
          )}
        </pre>
      </div>
    );
  }

  const variant = (params.get("variant") ?? "band") as
    | "band"
    | "plate"
    | "bare"
    | "bar"
    | "solid"
    | "card";

  return (
    <div className="h-screen w-screen overflow-hidden bg-transparent">
      <Overlay data={data} variant={variant} />
    </div>
  );
}
