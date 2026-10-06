import { useEffect, useReducer } from "react";
import { ApiError } from "@/api/client";
import { Crown, Loader2, Trophy } from "lucide-react";
import type {
  BracketLeaderboard,
  BracketMatchSummary,
  BracketStage,
  TournamentBracketsSummary,
} from "@/data/tournaments";
import { tournamentsService } from "@/services/tournaments.service";

const stageLabel: Record<BracketStage, string> = {
  QUALIFIERS: "Qualifiers",
  QUARTERFINALS: "Quarterfinals",
  SEMIFINALS: "Semifinals",
  FINALS: "Finals",
};

const stageOrder: Record<BracketStage, number> = {
  QUALIFIERS: -1,
  QUARTERFINALS: 0,
  SEMIFINALS: 1,
  FINALS: 2,
};

const medalColors = ["#f0c46a", "#c9cdd6", "#dd9b72"];
const RED = "#e98a80";
const BLUE = "#7c96c4";

const hairline = "1px solid var(--border)";

interface PreviewState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

type PreviewAction<T> =
  | { type: "start" }
  | { type: "done"; data: T }
  | { type: "fail"; message: string };

function previewReducer<T>(state: PreviewState<T>, action: PreviewAction<T>): PreviewState<T> {
  switch (action.type) {
    case "start":
      return { ...state, loading: true, error: null };
    case "done":
      return { data: action.data, loading: false, error: null };
    case "fail":
      return { data: null, loading: false, error: action.message };
  }
}

export function ScreenLeaderboardPreview({ bracketId }: { bracketId: number | null }) {
  const [state, dispatch] = useReducer(previewReducer<BracketLeaderboard>, {
    data: null,
    loading: false,
    error: null,
  });

  useEffect(() => {
    if (!bracketId) return;
    dispatch({ type: "start" });
    let cancelled = false;
    async function load() {
      try {
        const data = await tournamentsService.getBracketLeaderboard(bracketId!);
        if (!cancelled) dispatch({ type: "done", data });
      } catch (err) {
        if (!cancelled) {
          dispatch({ type: "fail", message: err instanceof ApiError ? err.message : "Failed to load leaderboard" });
        }
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [bracketId]);

  const entries = state.data?.entries ?? [];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
            Leaderboard — preview
          </p>
          <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>
            Best times that will be shown on screen
          </p>
        </div>
        {state.data && (
          <span className="px-3 py-1 rounded-full text-xs font-medium" style={{ background: "var(--accent-12)", color: "var(--accent)" }}>
            {state.data.stage} · {state.data.division}
          </span>
        )}
      </div>

      {!bracketId ? (
        <p className="py-6 text-sm" style={{ color: "var(--faint)" }}>
          Select a bracket to preview its leaderboard.
        </p>
      ) : state.loading ? (
        <p className="flex items-center gap-2 py-6 text-sm" style={{ color: "var(--muted-foreground)" }}>
          <Loader2 className="w-4 h-4 animate-spin" /> Loading leaderboard…
        </p>
      ) : state.error ? (
        <p className="px-3 py-2 rounded-lg text-sm" style={{ background: "var(--danger-10)", color: "var(--danger)", border: "1px solid var(--danger)" }}>
          {state.error}
        </p>
      ) : entries.length === 0 ? (
        <p className="py-6 text-sm" style={{ color: "var(--faint)" }}>
          No finished attempts yet — leaderboard will be empty.
        </p>
      ) : (
        <div style={{ borderTop: hairline }}>
          {entries.map((entry, index) => (
            <div
              key={entry.matchId}
              className="grid grid-cols-[44px_1fr_auto] items-baseline gap-3 py-2.5"
              style={{ borderBottom: hairline }}
            >
              <span
                className="text-sm font-bold tabular-nums leading-none"
                style={{ color: index < 3 ? medalColors[index] : "var(--faint)" }}
              >
                {String(index + 1).padStart(2, "0")}
              </span>
              <span className="text-sm font-medium truncate" style={{ color: "var(--foreground)" }}>
                {entry.athleteName}
              </span>
              <span
                className="text-sm font-semibold tabular-nums leading-none"
                style={{ color: index === 0 ? medalColors[0] : "var(--secondary-foreground)" }}
              >
                {entry.duration}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export function ScreenBracketsPreview({
  tournamentId,
  division,
}: {
  tournamentId: number;
  division: string | null;
}) {
  const [state, dispatch] = useReducer(previewReducer<TournamentBracketsSummary>, {
    data: null,
    loading: false,
    error: null,
  });

  useEffect(() => {
    if (!division) return;
    dispatch({ type: "start" });
    let cancelled = false;
    async function load() {
      try {
        const data = await tournamentsService.getBracketSummary(tournamentId, division!);
        if (!cancelled) dispatch({ type: "done", data });
      } catch (err) {
        if (!cancelled) {
          dispatch({ type: "fail", message: err instanceof ApiError ? err.message : "Failed to load brackets" });
        }
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [tournamentId, division]);

  const columns = (state.data?.brackets ?? [])
    .filter((b) => b.stage !== "QUALIFIERS")
    .sort((a, b) => stageOrder[a.stage] - stageOrder[b.stage]);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
            Brackets — preview
          </p>
          <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>
            Stages and matchups that will be shown on screen
          </p>
        </div>
        {state.data && (
          <span className="px-3 py-1 rounded-full text-xs font-medium" style={{ background: "var(--accent-12)", color: "var(--accent)" }}>
            {state.data.division}
          </span>
        )}
      </div>

      {!division ? (
        <p className="py-6 text-sm" style={{ color: "var(--faint)" }}>
          Select a division to preview its brackets.
        </p>
      ) : state.loading ? (
        <p className="flex items-center gap-2 py-6 text-sm" style={{ color: "var(--muted-foreground)" }}>
          <Loader2 className="w-4 h-4 animate-spin" /> Loading brackets…
        </p>
      ) : state.error ? (
        <p className="px-3 py-2 rounded-lg text-sm" style={{ background: "var(--danger-10)", color: "var(--danger)", border: "1px solid var(--danger)" }}>
          {state.error}
        </p>
      ) : columns.length === 0 ? (
        <p className="py-6 text-sm" style={{ color: "var(--faint)" }}>
          No brackets yet — screen will be empty.
        </p>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-x-8 gap-y-6">
            {columns.map((col) => (
              <BracketStageColumn key={col.stage} stage={col.stage} matches={col.matches} />
            ))}
          </div>
          <BracketChampion columns={columns} />
        </>
      )}
    </div>
  );
}

function BracketChampion({ columns }: { columns: TournamentBracketsSummary["brackets"] }) {
  const champion = columns.find((c) => c.stage === "FINALS")?.matches[0]?.winner;
  if (!champion || champion === "—") return null;
  return (
    <div className="flex items-center justify-center gap-2 pt-4" style={{ borderTop: hairline }}>
      <Trophy className="w-4 h-4" style={{ color: "var(--accent)" }} />
      <span className="text-sm font-semibold" style={{ color: "var(--foreground)" }}>{champion}</span>
      <span className="text-[10px] uppercase tracking-widest" style={{ color: "var(--faint)" }}>Champion</span>
    </div>
  );
}

function BracketStageColumn({
  stage,
  matches,
}: {
  stage: BracketStage;
  matches: BracketMatchSummary[];
}) {
  const decided = matches.filter((m) => m.winner !== "—").length;
  const complete = matches.length > 0 && decided === matches.length;
  const pct = matches.length > 0 ? Math.round((decided / matches.length) * 100) : 0;

  return (
    <div className="min-w-0">
      <div className="flex items-center justify-between gap-3 pt-1">
        <p
          className="text-xs font-semibold uppercase tracking-widest"
          style={{ color: complete ? "var(--accent)" : "var(--foreground)" }}
        >
          {stageLabel[stage]}
        </p>
        <span className="text-[10px] font-semibold tabular-nums" style={{ color: complete ? "var(--accent)" : "var(--faint)" }}>
          {decided}/{matches.length}
        </span>
      </div>
      <div className="h-0.5 mt-2 mb-3 overflow-hidden rounded-full" style={{ background: "var(--secondary)" }}>
        <div
          className="h-full transition-all duration-300"
          style={{ width: `${pct}%`, background: "var(--accent)", boxShadow: "0 0 8px var(--accent)" }}
        />
      </div>

      {matches.length === 0 ? (
        <p className="py-3 text-sm" style={{ color: "var(--faint)" }}>No matches yet</p>
      ) : (
        matches.map((match) => (
          <div key={match.matchId} className="pb-3">
            <div
              className="rounded-lg overflow-hidden"
              style={{
                background: "var(--card)",
                border: `1px solid ${match.winner !== "—" ? "rgba(233,138,128,0.45)" : "var(--border)"}`,
                boxShadow: match.winner !== "—" ? "0 0 18px rgba(233,138,128,0.10)" : undefined,
                transition: "border 200ms, box-shadow 200ms",
              }}
            >
              <PlayerLine name={match.athleteBlue} dot={BLUE} decided={match.winner !== "—"} won={match.winner === match.athleteBlue} />
              <div className="h-px" style={{ background: "var(--border)" }} />
              <PlayerLine name={match.athleteRed} dot={RED} decided={match.winner !== "—"} won={match.winner === match.athleteRed} />
            </div>
            <p className="mt-1 text-right text-[10px] tabular-nums" style={{ color: "var(--faint)" }}>
              #{match.matchId}
            </p>
          </div>
        ))
      )}
    </div>
  );
}

function PlayerLine({
  name,
  dot,
  decided,
  won,
}: {
  name: string;
  dot: string;
  decided: boolean;
  won: boolean;
}) {
  return (
    <div
      className="flex items-center gap-2 px-3 py-2 min-w-0"
      style={{ background: won ? "rgba(233,138,128,0.09)" : "transparent" }}
    >
      <span
        className="w-2 h-2 rounded-full shrink-0"
        style={{ background: won ? "var(--accent)" : dot, boxShadow: won ? "0 0 6px var(--accent)" : undefined }}
      />
      <span
        className="text-sm font-medium truncate"
        style={{ color: won ? "var(--accent)" : decided ? "var(--faint)" : "var(--secondary-foreground)" }}
      >
        {name}
      </span>
      {won && <Crown className="w-3.5 h-3.5 shrink-0 ml-auto" style={{ color: "var(--accent)" }} />}
    </div>
  );
}