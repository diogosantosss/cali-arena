import { useCollection } from "@/hooks/use-collection";
import { athletesService } from "@/services/athletes.service";
import { clubsService } from "@/services/clubs.service";
import { matchesService } from "@/services/matches.service";
import { routinesService } from "@/services/routines.service";
import { tournamentsService } from "@/services/tournaments.service";
import type { Athlete } from "@/data/athletes";
import type { Match } from "@/data/matches";

export interface JudgeMatchItem {
  match: Match;
  division?: string;
  athleteRed?: Athlete;
  athleteBlue?: Athlete;
  clubRedName?: string;
  clubBlueName?: string;
  routineName?: string;
}

interface JudgeMatchesResult {
  items: JudgeMatchItem[];
  loading: boolean;
  error: string | null;
  reload: () => Promise<void>;
}

/**
 * Judge home: loads the match list plus the metadata needed to render each
 * card (athletes, clubs, routine names and the bracket division).
 */
async function loadJudgeMatches(): Promise<JudgeMatchItem[]> {
  const [matches, athletes, clubs, routines] = await Promise.all([
    matchesService.getMatches(),
    athletesService.getAthletes(),
    clubsService.getClubs(),
    routinesService.getRoutines(),
  ]);

  const bracketIds = Array.from(new Set(matches.map((m) => m.bracketId)));
  const brackets = await Promise.all(
    bracketIds.map((id) => tournamentsService.getBracketById(id).catch(() => null)),
  );
  const divisionByBracket = new Map<number, string>();
  for (const bracket of brackets) {
    if (bracket) divisionByBracket.set(bracket.id, bracket.division);
  }

  return matches.map((match) => {
    const athleteRed = athletes.find((a) => a.id === match.athleteRedId);
    const athleteBlue = athletes.find((a) => a.id === match.athleteBlueId);
    return {
      match,
      division: divisionByBracket.get(match.bracketId),
      athleteRed,
      athleteBlue,
      clubRedName: athleteRed?.clubId != null ? clubs.find((c) => c.id === athleteRed.clubId)?.name : undefined,
      clubBlueName: athleteBlue?.clubId != null ? clubs.find((c) => c.id === athleteBlue.clubId)?.name : undefined,
      routineName: routines.find((r) => r.id === match.routineId)?.name,
    };
  });
}

export function useJudgeMatches(): JudgeMatchesResult {
  const collection = useCollection(loadJudgeMatches, "Failed to load matches");
  return {
    items: collection.items,
    loading: collection.loading,
    error: collection.error,
    reload: collection.reload,
  };
}
