import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { RefreshCw, ChevronRight } from "lucide-react";
import { useAuth } from "@/hooks/use-auth";
import { useJudgeMatches } from "@/hooks/use-judge-matches";
import { useJudgeTournaments } from "@/hooks/use-judge-tournaments";
import { JudgeMatchCard } from "@/components/judge/judge-match-card";
import { Reveal } from "@/components/shared/reveal";
import { JudgeLogoutButton } from "@/components/judge/judge-logout-button";
import { JudgeAdminDashboardButton } from "@/components/judge/judge-admin-dashboard-button";
import { JudgeThemeToggle } from "@/components/judge/judge-theme-toggle";
import { JudgeUserBadge } from "@/components/judge/judge-user-badge";
import type { MatchStatus } from "@/data/matches";

type MatchFilter = "ALL" | "PENDING" | "RUNNING" | "FINISHED";

const filters: { value: MatchFilter; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "PENDING", label: "Pending" },
  { value: "RUNNING", label: "Running" },
  { value: "FINISHED", label: "Finished" },
];

// Remembers the tournament the judge picked, so going into /judge/:matchId and
// back (and re-logging in on the same tab) keeps the same tournament selected.
const SELECTED_TOURNAMENT_KEY = "cali-arena:judge-tournament";

export function JudgeMatchesPage() {
  const { items, loading, error, reload } = useJudgeMatches();
  const {
    tournaments,
    loading: tournamentsLoading,
    error: tournamentsError,
    reload: reloadTournaments,
  } = useJudgeTournaments();
  const [filter, setFilter] = useState<MatchFilter>("ALL");
  const [selectedId, setSelectedId] = useState<number | null>(() => {
    const saved = sessionStorage.getItem(SELECTED_TOURNAMENT_KEY);
    return saved ? Number(saved) : null;
  });
  const navigate = useNavigate();
  const { user } = useAuth();
  // Judges live here full time; admins and hosts use it to take over a match.
  const canOpenDashboard = user != null && user.role !== "JUDGE";

  const selectedTournament = tournaments.find((t) => t.id === selectedId);

  function selectTournament(tournamentId: number) {
    setSelectedId(tournamentId);
    sessionStorage.setItem(SELECTED_TOURNAMENT_KEY, String(tournamentId));
  }

  function goBackToTournaments() {
    setSelectedId(null);
    sessionStorage.removeItem(SELECTED_TOURNAMENT_KEY);
  }

  const tournamentMatches = selectedId == null ? [] : items.filter((item) => item.tournamentId === selectedId);
  const filtered =
    filter === "ALL" ? tournamentMatches : tournamentMatches.filter((item) => (item.match.status as MatchStatus) === filter);

  const showPicker = selectedTournament == null && !tournamentsLoading;

  return (
    <div className="min-h-screen" style={{ background: "var(--background)" }}>
      <header
        className="sticky top-0 z-10"
        style={{ background: "var(--background)", borderBottom: "1px solid var(--border)" }}
      >
        <div className="max-w-xl mx-auto flex items-center gap-2 px-4 py-2.5">
          <JudgeUserBadge />
          <div className="flex-1" />
          <JudgeThemeToggle />
          {canOpenDashboard && <JudgeAdminDashboardButton />}
          <JudgeLogoutButton />
        </div>
      </header>

      <div className="max-w-xl mx-auto px-4 py-6 space-y-5">
        {tournamentsLoading ? (
          <div className="space-y-3">
            {[0, 1, 2].map((i) => (
              <div key={i} className="rounded-2xl h-24 animate-pulse" style={{ background: "var(--secondary)" }} />
            ))}
          </div>
        ) : showPicker ? (
          <>
            <div className="flex items-center gap-2">
              <div className="min-w-0 flex-1">
                <h1
                  className="text-xs tracking-widest uppercase"
                  style={{ color: "var(--foreground)", fontFamily: "Geist Variable, sans-serif" }}
                >
                  Judge
                </h1>
                <p className="text-sm mt-0.5" style={{ color: "var(--muted-foreground)" }}>
                  Pick the tournament you are judging.
                </p>
              </div>
              <button
                onClick={() => void reloadTournaments()}
                aria-label="Refresh tournaments"
                title="Refresh tournaments"
                className="p-2 rounded-lg transition-colors shrink-0"
                style={{ background: "var(--secondary)", color: "var(--secondary-foreground)", border: "1px solid var(--border)" }}
              >
                <RefreshCw className={`w-4 h-4 ${tournamentsLoading ? "animate-spin" : ""}`} />
              </button>
            </div>

            {tournamentsError && (
              <div className="rounded-xl px-5 py-8 text-center space-y-3" style={{ border: "1px solid var(--border)" }}>
                <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>{tournamentsError}</p>
                <button
                  onClick={() => void reloadTournaments()}
                  className="px-4 py-2 rounded-lg text-sm font-medium"
                  style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
                >
                  Try again
                </button>
              </div>
            )}

            {!tournamentsError && tournaments.length === 0 && (
              <p className="text-center text-sm py-10" style={{ color: "var(--muted-foreground)" }}>
                You have no tournaments to judge yet.
              </p>
            )}

            {tournaments.length > 0 && (
              <div className="space-y-3">
                {tournaments.map((tournament, index) => {
                  const matchCount = items.filter((item) => item.tournamentId === tournament.id).length;
                  return (
                    <Reveal key={tournament.id} delay={index * 0.03}>
                      <button
                        onClick={() => selectTournament(tournament.id)}
                        className="w-full text-left transition-transform active:scale-[0.99]"
                      >
                        <div
                          className="rounded-2xl px-4 py-4 flex items-center gap-3"
                          style={{ background: "var(--card)", border: "1px solid var(--border)" }}
                        >
                          <div
                            className="w-11 h-11 rounded-xl flex items-center justify-center text-base font-bold"
                            style={{ background: "var(--accent-12)", color: "var(--accent)" }}
                          >
                            {tournament.name.charAt(0).toUpperCase()}
                          </div>
                          <div className="min-w-0 flex-1">
                            <p className="text-sm font-semibold truncate" style={{ color: "var(--foreground)" }}>
                              {tournament.name}
                            </p>
                            <p className="text-[11px] mt-0.5" style={{ color: "var(--muted-foreground)" }}>
                              {matchCount === 1 ? "1 match" : `${matchCount} matches`}
                            </p>
                          </div>
                          <ChevronRight className="w-4 h-4 shrink-0" style={{ color: "var(--muted-foreground)" }} />
                        </div>
                      </button>
                    </Reveal>
                  );
                })}
              </div>
            )}
          </>
        ) : (
          <>
            <div className="flex items-center justify-between gap-3">
              <div className="min-w-0">
                <h1
                  className="text-xs tracking-widest uppercase"
                  style={{ color: "var(--foreground)", fontFamily: "Geist Variable, sans-serif" }}
                >
                  Judge
                </h1>
                <p className="text-sm mt-0.5 font-semibold truncate" style={{ color: "var(--foreground)" }}>
                  {selectedTournament?.name}
                </p>
              </div>
              <button
                onClick={goBackToTournaments}
                className="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors shrink-0"
                style={{ border: "1px solid var(--border)", color: "var(--accent)", background: "transparent" }}
              >
                Switch tournament
              </button>
            </div>

            <div className="flex items-center gap-2">
              <div className="flex flex-wrap gap-2 flex-1">
                {filters.map((f) => {
                  const active = f.value === filter;
                  return (
                    <button
                      key={f.value}
                      onClick={() => setFilter(f.value)}
                      className="px-3 py-1.5 rounded-full text-xs font-medium transition-colors"
                      style={
                        active
                          ? { background: "var(--accent)", color: "var(--accent-foreground)" }
                          : { background: "var(--secondary)", color: "var(--secondary-foreground)", border: "1px solid var(--border)" }
                      }
                    >
                      {f.label}
                    </button>
                  );
                })}
              </div>
              <button
                onClick={() => void reload()}
                aria-label="Refresh matches"
                className="p-2 rounded-lg transition-colors shrink-0"
                style={{ background: "var(--secondary)", color: "var(--secondary-foreground)", border: "1px solid var(--border)" }}
              >
                <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
              </button>
            </div>

            {loading ? (
              <div className="space-y-3">
                {[0, 1, 2].map((i) => (
                  <div key={i} className="rounded-2xl h-32 animate-pulse" style={{ background: "var(--secondary)" }} />
                ))}
              </div>
            ) : error ? (
              <div className="rounded-xl px-5 py-8 text-center space-y-3" style={{ border: "1px solid var(--border)" }}>
                <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>{error}</p>
                <button
                  onClick={() => void reload()}
                  className="px-4 py-2 rounded-lg text-sm font-medium"
                  style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
                >
                  Try again
                </button>
              </div>
            ) : tournamentMatches.length === 0 ? (
              <p className="text-center text-sm py-10" style={{ color: "var(--muted-foreground)" }}>
                There are no matches in this tournament yet.
              </p>
            ) : filtered.length === 0 ? (
              <p className="text-center text-sm py-10" style={{ color: "var(--muted-foreground)" }}>
                No matches with this status.
              </p>
            ) : (
              <div className="space-y-3">
                {filtered.map((item, index) => (
                  <Reveal key={item.match.id} delay={index * 0.03}>
                    <JudgeMatchCard
                      item={item}
                      onClick={() => navigate(`/judge/${item.match.id}`)}
                    />
                  </Reveal>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}