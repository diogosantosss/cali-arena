import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { RefreshCw } from "lucide-react";
import { useAuth } from "@/hooks/use-auth";
import { useJudgeMatches } from "@/hooks/use-judge-matches";
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

export function JudgeMatchesPage() {
  const { items, loading, error, reload } = useJudgeMatches();
  const [filter, setFilter] = useState<MatchFilter>("ALL");
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const filtered =
    filter === "ALL" ? items : items.filter((item) => (item.match.status as MatchStatus) === filter);

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
          {isAdmin && <JudgeAdminDashboardButton />}
          <JudgeLogoutButton />
        </div>
      </header>

      <div className="max-w-xl mx-auto px-4 py-6 space-y-5">
        <div>
          <h1
            className="text-xs tracking-widest uppercase"
            style={{ color: "var(--foreground)", fontFamily: "Geist Variable, sans-serif" }}
          >
            Judge
          </h1>
          <p className="text-sm mt-0.5" style={{ color: "var(--muted-foreground)" }}>
            Pick the match you are judging.
          </p>
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
        ) : filtered.length === 0 ? (
          <p className="text-center text-sm py-10" style={{ color: "var(--muted-foreground)" }}>
            {filter === "ALL" ? "There are no matches to judge yet." : "No matches with this status."}
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
      </div>
    </div>
  );
}