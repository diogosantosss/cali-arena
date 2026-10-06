import { useCollection } from "@/hooks/use-collection";
import { tournamentStaffService } from "@/services/tournament-staff.service";

export interface JudgeTournament {
  id: number;
  name: string;
}

interface JudgeTournamentsResult {
  tournaments: JudgeTournament[];
  loading: boolean;
  error: string | null;
  reload: () => Promise<void>;
}

/**
 * Tournaments the current user can judge (or hosts): the picker shown right
 * after the judge signs in, before any match list. Hosting and judging lists
 * are merged because hosts and admins also drive matches from the judge flow.
 */
async function loadJudgeTournaments(): Promise<JudgeTournament[]> {
  const mine = await tournamentStaffService.getMyTournaments();
  const seen = new Set<number>();
  const merged: JudgeTournament[] = [];
  for (const tournament of [...mine.hosting, ...mine.judging]) {
    if (!seen.has(tournament.id)) {
      seen.add(tournament.id);
      merged.push(tournament);
    }
  }
  return merged;
}

export function useJudgeTournaments(): JudgeTournamentsResult {
  const collection = useCollection(loadJudgeTournaments, "Failed to load tournaments");
  return {
    tournaments: collection.items,
    loading: collection.loading,
    error: collection.error,
    reload: collection.reload,
  };
}