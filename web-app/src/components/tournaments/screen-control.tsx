import { useReducer, useState, useRef, useEffect } from "react";
import { ApiError } from "@/api/client";
import type { Athlete } from "@/data/athletes";
import type { Routine, RoutineOverview } from "@/data/routines";
import { tournamentsService } from "@/services/tournaments.service";
import type { Bracket, ScreenState, TournamentState } from "@/data/tournaments";
import { BattlePanel } from "@/components/matches/battle-panel";
import type { Match } from "@/data/matches";
import { ScreenRoutinesPanel } from "./screen-routines-panel";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Check,
  ClipboardList,
  ExternalLink,
  Hourglass,
  Loader2,
  MonitorPlay,
  Network,
  Swords,
  Trophy,
  type LucideIcon,
} from "lucide-react";

const screenOptions: { value: ScreenState; label: string; icon: LucideIcon }[] = [
  { value: "WAITING", label: "Waiting", icon: Hourglass },
  { value: "ROUTINES", label: "Routines", icon: ClipboardList },
  { value: "BATTLE", label: "Battle", icon: Swords },
  { value: "LEADERBOARD", label: "Leaderboard", icon: Trophy },
  { value: "BRACKETS", label: "Brackets", icon: Network },
];

const screenLabel = (screen: ScreenState) =>
  screenOptions.find((option) => option.value === screen)?.label ?? screen;

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
      return {
        ...state,
        screen: action.screen,
        matchId: action.screen === "BATTLE" ? state.matchId : null,
        bracketId: action.screen === "LEADERBOARD" ? state.bracketId : null,
        division: action.screen === "BRACKETS" ? state.division : null,
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
        const red = athletes.find((a) => a.id === match.athleteRedId)?.name ?? "Red";
        const blue = athletes.find((a) => a.id === match.athleteBlueId)?.name ?? "Blue";
        return `#${match.id} · ${red} vs ${blue}`;
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
    <div className="rounded-lg overflow-hidden" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
      <div className="flex items-center justify-between px-5 py-4">
        <div>
          <p className="text-xs tracking-widest uppercase" style={{ color: "var(--muted-foreground)" }}>
            Screen control
          </p>
          <h3
            className="text-xl leading-tight mt-1"
            style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "var(--foreground)" }}
          >
            Spectator screen
          </h3>
        </div>
        <a
          href={`/screen/${tournamentId}`}
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-1.5 px-2.5 py-1 rounded text-[11px] font-medium transition-colors"
          style={{ background: "var(--secondary)", color: "var(--secondary-foreground)", border: "1px solid var(--border)" }}
          onMouseEnter={(e) => (e.currentTarget.style.color = "var(--accent)")}
          onMouseLeave={(e) => (e.currentTarget.style.color = "var(--secondary-foreground)")}
        >
          <ExternalLink className="w-3 h-3" />
          Open screen
        </a>
      </div>

      <div style={{ borderTop: "1px solid var(--border)" }} className="px-5 py-5 space-y-5">
        <div>
          <p className="text-[10px] uppercase tracking-widest mb-2" style={{ color: "var(--muted-foreground)" }}>
            Screen
          </p>
          <div className="flex flex-wrap items-center gap-1 p-1 rounded-lg" style={{ background: "var(--secondary)", border: "1px solid var(--border)" }}>
            {screenOptions.map(({ value, label, icon: Icon }) => {
              const active = ui.screen === value;
              return (
                <button
                  key={value}
                  onClick={() => dispatch({ type: "setScreen", screen: value })}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-colors"
                  style={{
                    background: active ? "var(--accent)" : "transparent",
                    color: active ? "var(--accent-foreground)" : "var(--secondary-foreground)",
                  }}
                  onMouseEnter={(e) => {
                    if (!active) {
                      e.currentTarget.style.color = "var(--foreground)";
                      e.currentTarget.style.background = "var(--border)";
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!active) {
                      e.currentTarget.style.color = "var(--secondary-foreground)";
                      e.currentTarget.style.background = "transparent";
                    }
                  }}
                >
                  <Icon className="w-3.5 h-3.5" />
                  {label}
                </button>
              );
            })}
          </div>
        </div>

        <div className="flex flex-wrap items-end gap-3">
          {ui.screen === "BATTLE" && (
            <div className="space-y-1.5">
              <p className="text-[10px] uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>Match</p>
              <Select
                value={ui.matchId ? String(ui.matchId) : ""}
                onValueChange={(value) => dispatch({ type: "setMatchId", id: Number(value) })}
              >
                <SelectTrigger className="h-8 text-xs w-72 border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--secondary-foreground)" }}>
                  <Swords className="w-3.5 h-3.5 shrink-0" style={{ color: "var(--muted-foreground)" }} />
                  <SelectValue placeholder="Select match" />
                </SelectTrigger>
                <SelectContent position="popper" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
                  {readyMatches.map((m) => {
                    const red = athletes.find((a) => a.id === m.athleteRedId);
                    const blue = athletes.find((a) => a.id === m.athleteBlueId);
                    return (
                      <SelectItem key={m.id} value={String(m.id)} className="text-xs" style={{ color: "var(--secondary-foreground)" }}>
                        #{m.id} — {red?.name ?? "Red"} vs {blue?.name ?? "Blue"}
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>
          )}

          {ui.screen === "LEADERBOARD" && (
            <div className="space-y-1.5">
              <p className="text-[10px] uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>Bracket</p>
              <Select
                value={ui.bracketId ? String(ui.bracketId) : ""}
                onValueChange={(value) => dispatch({ type: "setBracketId", id: Number(value) })}
              >
                <SelectTrigger className="h-8 text-xs w-72 border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--secondary-foreground)" }}>
                  <Trophy className="w-3.5 h-3.5 shrink-0" style={{ color: "var(--muted-foreground)" }} />
                  <SelectValue placeholder="Select bracket" />
                </SelectTrigger>
                <SelectContent position="popper" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
                  {brackets.map((b) => (
                    <SelectItem key={b.id} value={String(b.id)} className="text-xs" style={{ color: "var(--secondary-foreground)" }}>
                      {b.stage} · {b.division}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {ui.screen === "BRACKETS" && (
            <div className="space-y-1.5">
              <p className="text-[10px] uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>Division</p>
              <Select
                value={ui.division ?? ""}
                onValueChange={(value) => dispatch({ type: "setDivision", division: value })}
              >
                <SelectTrigger className="h-8 text-xs w-56 border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--secondary-foreground)" }}>
                  <Network className="w-3.5 h-3.5 shrink-0" style={{ color: "var(--muted-foreground)" }} />
                  <SelectValue placeholder="Select division" />
                </SelectTrigger>
                <SelectContent position="popper" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
                  {divisions.map((d) => (
                    <SelectItem key={d} value={d} className="text-xs" style={{ color: "var(--secondary-foreground)" }}>{d}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <button
            onClick={() => void handleUpdateScreen()}
            disabled={updateDisabled}
            className="flex items-center gap-1.5 px-4 h-8 rounded text-xs font-medium transition-opacity disabled:opacity-50 ml-auto"
            style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
          >
            {ui.loading ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                Updating…
              </>
            ) : justApplied ? (
              <>
                <Check className="w-3.5 h-3.5" />
                Applied
              </>
            ) : (
              <>
                <MonitorPlay className="w-3.5 h-3.5" />
                Update screen
              </>
            )}
          </button>
        </div>

        <div
          className="flex items-center gap-2.5 text-xs rounded-lg px-3 py-2"
          style={{ background: "var(--background)", border: "1px solid var(--border)" }}
        >
          <span className="w-2 h-2 rounded-full animate-pulse shrink-0" style={{ background: "var(--accent)" }} />
          <span className="uppercase tracking-widest text-[10px]" style={{ color: "var(--muted-foreground)" }}>
            {isDirty ? "Will show" : "Currently live"}
          </span>
          <span className="font-medium truncate" style={{ color: "var(--foreground)" }}>
            {screenLabel(ui.screen)}
            {pendingDetail && <span> · {pendingDetail}</span>}
          </span>
          {isDirty && (
            <span
              className="ml-auto shrink-0 text-[10px] px-2 py-0.5 rounded-full uppercase tracking-wider"
              style={{ background: "var(--accent-12)", color: "var(--accent)" }}
            >
              Not applied
            </span>
          )}
        </div>

        {ui.error && (
          <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded px-3 py-2">
            {ui.error}
          </p>
        )}

        {ui.screen === "BATTLE" && ui.matchId && (
          <BattlePanel
            key={ui.matchId}
            matchId={ui.matchId}
            athletes={athletes}
            routines={routines}
            overviews={overviews}
            onError={(message) => dispatch({ type: "battleError", message })}
          />
        )}

        {ui.screen === "ROUTINES" && (
          <ScreenRoutinesPanel tournamentId={tournamentId} routines={routines} />
        )}
      </div>
    </div>
  );
}