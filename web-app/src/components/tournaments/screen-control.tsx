import { useReducer, useState, useRef, useEffect } from "react";
import { ApiError } from "@/api/client";
import type { Athlete } from "@/data/athletes";
import type { Routine, RoutineOverview } from "@/data/routines";
import { tournamentsService } from "@/services/tournaments.service";
import type { Bracket, BracketStage, ScreenState, TournamentState } from "@/data/tournaments";
import { BattlePanel } from "@/components/matches/battle-panel";
import type { Match } from "@/data/matches";
import { ScreenRoutinesPanel } from "./screen-routines-panel";
import { ScreenBracketsPreview, ScreenLeaderboardPreview } from "./screen-previews";
import { AnimatedPanel } from "@/components/shared/animated-panel";
import { usePanelHost } from "@/hooks/use-panel-host";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Check,
  ExternalLink,
  Loader2,
  Network,
  Swords,
  Trophy,
  Monitor,
  Activity,
  List,
} from "lucide-react";

const screenOptions: { value: ScreenState; label: string; icon: React.ReactNode }[] = [
  { value: "WAITING", label: "Waiting", icon: <Monitor className="w-4 h-4" /> },
  { value: "ROUTINES", label: "Routines", icon: <Activity className="w-4 h-4" /> },
  { value: "BATTLE", label: "Battle", icon: <Swords className="w-4 h-4" /> },
  { value: "LEADERBOARD", label: "Leaderboard", icon: <Trophy className="w-4 h-4" /> },
  { value: "BRACKETS", label: "Brackets", icon: <List className="w-4 h-4" /> },
];

const stageLabel: Record<BracketStage, string> = {
  QUALIFIERS: "Qualifiers",
  QUARTERFINALS: "Quarterfinals",
  SEMIFINALS: "Semifinals",
  FINALS: "Finals",
};

interface ScreenControlProps {
  tournamentId: number;
  state: TournamentState | null;
  matches: Match[];
  athletes: Athlete[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
  brackets: Bracket[];
  onUpdated: (state: TournamentState) => void;
}

interface ScreenControlState {
  screen: ScreenState;
  matchId: number | null;
  bracketId: number | null;
  division: string | null;
  loading: boolean;
  error: string | null;
}

type Action =
  | { type: "setScreen"; screen: ScreenState }
  | { type: "setMatchId"; id: number }
  | { type: "setBracketId"; id: number }
  | { type: "setDivision"; division: string }
  | { type: "updateStart" }
  | { type: "updateDone" }
  | { type: "updateError"; message: string }
  | { type: "battleError"; message: string | null };

function createInitialState(state: TournamentState | null): ScreenControlState {
  return {
    screen: state?.currentScreen ?? "WAITING",
    matchId: state?.currentMatchId ?? null,
    bracketId: state?.currentBracketId ?? null,
    division: state?.currentDivision ?? null,
    loading: false,
    error: null,
  };
}

function reducer(state: ScreenControlState, action: Action): ScreenControlState {
  switch (action.type) {
    case "setScreen":
      // Selections are intentionally kept so each screen's panel stays mounted
      // and can animate in/out instead of being torn down on every switch.
      return {
        ...state,
        screen: action.screen,
        error: null,
      };
    case "setMatchId":
      return { ...state, matchId: action.id, error: null };
    case "setBracketId":
      return { ...state, bracketId: action.id, error: null };
    case "setDivision":
      return { ...state, division: action.division, error: null };
    case "updateStart":
      return { ...state, loading: true, error: null };
    case "updateDone":
      return { ...state, loading: false };
    case "updateError":
      return { ...state, loading: false, error: action.message };
    case "battleError":
      return { ...state, error: action.message };
  }
}

export function ScreenControl({
  tournamentId,
  state: tournamentState,
  matches,
  athletes,
  routines,
  overviews,
  brackets,
  onUpdated,
}: ScreenControlProps) {
  const [ui, dispatch] = useReducer(reducer, tournamentState, createInitialState);
  const [justApplied, setJustApplied] = useState(false);
  const appliedTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const { hostRef: panelHostRef, hostStyle: panelHostStyle } = usePanelHost(ui.screen);

  useEffect(() => {
    return () => window.clearTimeout(appliedTimer.current);
  }, []);

  const readyMatches = matches.filter((m) => m.status !== "FINISHED");
  const divisions = Array.from(new Set(brackets.map((b) => b.division)));

  const isDirty =
    ui.screen !== (tournamentState?.currentScreen ?? "WAITING") ||
    (ui.screen === "BATTLE" && ui.matchId !== tournamentState?.currentMatchId) ||
    (ui.screen === "LEADERBOARD" && ui.bracketId !== tournamentState?.currentBracketId) ||
    (ui.screen === "BRACKETS" && ui.division !== tournamentState?.currentDivision);

  const pendingDetail = (() => {
    switch (ui.screen) {
      case "BATTLE": {
        const match = matches.find((m) => m.id === ui.matchId);
        if (!match) return null;
        const red = athletes.find((a) => a.id === match.athleteRedId)?.name ?? "Not assigned";
        const blue = athletes.find((a) => a.id === match.athleteBlueId)?.name ?? "Not assigned";
        return `${red} vs ${blue}`;
      }
      case "LEADERBOARD": {
        const bracket = brackets.find((b) => b.id === ui.bracketId);
        return bracket ? `${bracket.stage} · ${bracket.division}` : null;
      }
      case "BRACKETS":
        return ui.division ?? null;
      default:
        return null;
    }
  })();

  async function handleUpdateScreen() {
    dispatch({ type: "updateStart" });
    try {
      const updated = await tournamentsService.updateScreen(tournamentId, {
        screen: ui.screen,
        currentMatchId: ui.screen === "BATTLE" ? ui.matchId : null,
        currentBracketId: ui.screen === "LEADERBOARD" ? ui.bracketId : null,
        currentDivision: ui.screen === "BRACKETS" ? ui.division : null,
      });
      onUpdated(updated);
      dispatch({ type: "updateDone" });
      setJustApplied(true);
      appliedTimer.current = window.setTimeout(() => setJustApplied(false), 1600);
    } catch (err) {
      dispatch({
        type: "updateError",
        message: err instanceof ApiError ? err.message : "Failed to update screen",
      });
    }
  }

  const updateDisabled =
    ui.loading ||
    (ui.screen === "BATTLE" && !ui.matchId) ||
    (ui.screen === "LEADERBOARD" && !ui.bracketId) ||
    (ui.screen === "BRACKETS" && !ui.division);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
            Spectator Screen
          </p>
          <div className="flex items-center gap-2 mt-1">
            <span className="text-lg font-medium" style={{ color: "var(--foreground)" }}>Screen Control</span>
            <span
              className={`flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] uppercase tracking-widest font-medium ${
                isDirty ? "bg-muted text-muted-foreground" : "bg-accent/10 text-accent"
              }`}
            >
              <span className={`w-1.5 h-1.5 rounded-full ${isDirty ? "" : "animate-pulse"}`} />
              {isDirty ? "Pending" : "Live"}
            </span>
          </div>
        </div>
        <a
          href={`/screen/${tournamentId}`}
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-1.5 text-sm font-medium transition-colors"
          style={{ color: "var(--muted-foreground)" }}
          onMouseEnter={(e) => (e.currentTarget.style.color = "var(--accent)")}
          onMouseLeave={(e) => (e.currentTarget.style.color = "var(--muted-foreground)")}
        >
          <ExternalLink className="w-3.5 h-3.5" />
          Open Screen
        </a>
      </div>

      {/* Screen Selection */}
      <div className="space-y-3">
        <div className="flex flex-wrap gap-2">
          {screenOptions.map(({ value, label, icon }) => {
            const active = ui.screen === value;
            return (
              <button
                key={value}
                onClick={() => dispatch({ type: "setScreen", screen: value })}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all border ${
                  active
                    ? "border-accent bg-accent/5 text-accent"
                    : "border-transparent text-muted-foreground hover:text-foreground hover:bg-muted"
                }`}
              >
                <span className={active ? "text-accent" : "text-muted-foreground"}>{icon}</span>
                <span>{label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Context Selectors */}
      <div className="flex flex-wrap items-end gap-4" style={{ opacity: isDirty ? 1 : 0.5 }}>
        {ui.screen === "BATTLE" && (
          <div className="space-y-1.5 flex-1 min-w-[280px]">
            <label className="text-[10px] uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
              Match
            </label>
            <Select
              value={ui.matchId ? String(ui.matchId) : ""}
              onValueChange={(value) => dispatch({ type: "setMatchId", id: Number(value) })}
            >
              <SelectTrigger className="h-9 text-sm w-full border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--foreground)" }}>
                <Swords className="w-4 h-4 shrink-0" style={{ color: "var(--muted-foreground)" }} />
                <SelectValue placeholder="Select a match" />
              </SelectTrigger>
              <SelectContent position="popper" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
                {readyMatches.map((m) => {
                  const red = athletes.find((a) => a.id === m.athleteRedId);
                  const blue = athletes.find((a) => a.id === m.athleteBlueId);
                  const bracket = brackets.find((b) => b.id === m.bracketId);
                  const label = `${red?.name ?? "Not assigned"} vs ${blue?.name ?? "Not assigned"}`;
                  return (
                    <SelectItem key={m.id} value={String(m.id)} className="text-sm" style={{ color: "var(--foreground)" }}>
                      {bracket ? `${stageLabel[bracket.stage]} — ${label}` : label}
                    </SelectItem>
                  );
                })}
              </SelectContent>
            </Select>
          </div>
        )}

        {ui.screen === "LEADERBOARD" && (
          <div className="space-y-1.5 flex-1 min-w-[280px]">
            <label className="text-[10px] uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
              Bracket
            </label>
            <Select
              value={ui.bracketId ? String(ui.bracketId) : ""}
              onValueChange={(value) => dispatch({ type: "setBracketId", id: Number(value) })}
            >
              <SelectTrigger className="h-9 text-sm w-full border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--foreground)" }}>
                <Trophy className="w-4 h-4 shrink-0" style={{ color: "var(--muted-foreground)" }} />
                <SelectValue placeholder="Select a bracket" />
              </SelectTrigger>
              <SelectContent position="popper" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
                {brackets.map((b) => (
                  <SelectItem key={b.id} value={String(b.id)} className="text-sm" style={{ color: "var(--foreground)" }}>
                    {b.stage} · {b.division}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}

        {ui.screen === "BRACKETS" && (
          <div className="space-y-1.5 flex-1 min-w-[280px]">
            <label className="text-[10px] uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
              Division
            </label>
            <Select
              value={ui.division ?? ""}
              onValueChange={(value) => dispatch({ type: "setDivision", division: value })}
            >
              <SelectTrigger className="h-9 text-sm w-full border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--foreground)" }}>
                <Network className="w-4 h-4 shrink-0" style={{ color: "var(--muted-foreground)" }} />
                <SelectValue placeholder="Select division" />
              </SelectTrigger>
              <SelectContent position="popper" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
                {divisions.map((d) => (
                  <SelectItem key={d} value={d} className="text-sm" style={{ color: "var(--foreground)" }}>{d}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}
      </div>

      {/* Action Bar */}
      <div className="flex items-center justify-between gap-4 pt-2 border-t" style={{ borderColor: "var(--border)" }}>
        <div className="flex items-center gap-2 text-sm" style={{ color: "var(--muted-foreground)" }}>
          {isDirty ? (
            <>
              <span className="w-2 h-2 rounded-full" style={{ background: "var(--muted-foreground)" }} />
              <span>Will show <strong style={{ color: "var(--foreground)" }}>{screenOptions.find((o) => o.value === ui.screen)?.label ?? ui.screen}</strong>{pendingDetail && <><span className="mx-2">·</span> {pendingDetail}</>} — press update to go live</span>
            </>
          ) : (
            <>
              <span className="w-2 h-2 rounded-full animate-pulse" style={{ background: "var(--accent)" }} />
              <span>Live now — <strong style={{ color: "var(--foreground)" }}>{screenOptions.find((o) => o.value === ui.screen)?.label ?? ui.screen}</strong>{pendingDetail && <><span className="mx-2">·</span> {pendingDetail}</>}</span>
            </>
          )}
        </div>

        <button
          onClick={() => void handleUpdateScreen()}
          disabled={updateDisabled || !isDirty}
          className="flex items-center gap-2 px-5 h-9 rounded-lg text-sm font-medium transition-all disabled:opacity-40 disabled:cursor-not-allowed"
          style={{
            background: isDirty ? "var(--accent)" : "var(--muted)",
            color: isDirty ? "var(--accent-foreground)" : "var(--muted-foreground)",
          }}
        >
          {ui.loading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              Applying…
            </>
          ) : justApplied ? (
            <>
              <Check className="w-4 h-4" />
              Applied
            </>
          ) : isDirty ? (
            "Update Screen"
          ) : (
            "No Changes"
          )}
        </button>
      </div>

      {ui.error && (
        <div className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm" style={{ background: "var(--danger-10)", color: "var(--danger)", border: "1px solid var(--danger)" }}>
          <Loader2 className="w-4 h-4" style={{ color: "var(--danger)" }} />
          {ui.error}
        </div>
      )}

      {/* Screen-specific Panels */}
      <div ref={panelHostRef} className="relative" style={panelHostStyle}>
        <AnimatedPanel name="BATTLE" activeTab={ui.screen}>
          {ui.matchId && (
            <BattlePanel
              key={ui.matchId}
              matchId={ui.matchId}
              athletes={athletes}
              routines={routines}
              overviews={overviews}
              onError={(message) => dispatch({ type: "battleError", message })}
            />
          )}
        </AnimatedPanel>

        <AnimatedPanel name="ROUTINES" activeTab={ui.screen}>
          <ScreenRoutinesPanel tournamentId={tournamentId} routines={routines} />
        </AnimatedPanel>

        <AnimatedPanel name="LEADERBOARD" activeTab={ui.screen}>
          <ScreenLeaderboardPreview bracketId={ui.bracketId} />
        </AnimatedPanel>

        <AnimatedPanel name="BRACKETS" activeTab={ui.screen}>
          <ScreenBracketsPreview tournamentId={tournamentId} division={ui.division} />
        </AnimatedPanel>
      </div>
    </div>
  );
}