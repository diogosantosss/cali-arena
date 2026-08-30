import { useReducer } from "react";
import { ApiError } from "@/lib/api/client";
import type { Athlete } from "@/features/athletes/types";
import type { Routine, RoutineOverview } from "@/features/routines/types";
import { tournamentsService } from "../services/tournaments.service";
import type { Bracket, ScreenState, TournamentState } from "../types";
import { BattlePanel } from "@/features/matches/components/battle-panel";
import type { Match } from "@/features/matches/types";
import { ScreenRoutinesPanel } from "./screen-routines-panel";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ExternalLink } from "lucide-react";

const screenLabels: Record<ScreenState, string> = {
  WAITING: "Waiting",
  ROUTINES: "Routines",
  BATTLE: "Battle",
  LEADERBOARD: "Leaderboard",
  BRACKETS: "Brackets",
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
    } catch (err) {
      dispatch({
        type: "updateError",
        message: err instanceof ApiError ? err.message : "Failed to update screen",
      });
    }
  }

  const readyMatches = matches.filter((m) => m.status !== "FINISHED");

  const divisions = Array.from(new Set(brackets.map((b) => b.division)));

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
        <div className="flex items-center gap-3">
          {tournamentState && (
            <span
              className="text-[11px] px-2.5 py-1 rounded-full"
              style={{ background: "rgba(232,160,32,0.12)", color: "var(--accent)" }}
            >
              Current: {screenLabels[tournamentState.currentScreen]}
            </span>
          )}
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
      </div>

      <div style={{ borderTop: "1px solid var(--border)" }} className="px-5 py-4 space-y-5">
        <div className="flex items-end gap-3">
          <div className="space-y-1.5">
            <p className="text-[10px] uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>Screen</p>
            <Select value={ui.screen} onValueChange={(value) => dispatch({ type: "setScreen", screen: value as ScreenState })}>
              <SelectTrigger className="h-8 text-xs w-44 border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--secondary-foreground)" }}>
                <SelectValue />
              </SelectTrigger>
              <SelectContent style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
                {Object.entries(screenLabels).map(([value, label]) => (
                  <SelectItem key={value} value={value} className="text-xs" style={{ color: "var(--secondary-foreground)" }}>{label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {ui.screen === "BATTLE" && (
            <div className="space-y-1.5">
              <p className="text-[10px] uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>Match</p>
              <Select
                value={ui.matchId ? String(ui.matchId) : ""}
                onValueChange={(value) => dispatch({ type: "setMatchId", id: Number(value) })}
              >
                <SelectTrigger className="h-8 text-xs w-64 border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--secondary-foreground)" }}>
                  <SelectValue placeholder="Select match" />
                </SelectTrigger>
                <SelectContent style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
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
                <SelectTrigger className="h-8 text-xs w-64 border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--secondary-foreground)" }}>
                  <SelectValue placeholder="Select bracket" />
                </SelectTrigger>
                <SelectContent style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
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
                <SelectTrigger className="h-8 text-xs w-44 border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--secondary-foreground)" }}>
                  <SelectValue placeholder="Select division" />
                </SelectTrigger>
                <SelectContent style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
                  {divisions.map((d) => (
                    <SelectItem key={d} value={d} className="text-xs" style={{ color: "var(--secondary-foreground)" }}>{d}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <button
            onClick={() => void handleUpdateScreen()}
            disabled={ui.loading || (ui.screen === "BATTLE" && !ui.matchId) || (ui.screen === "LEADERBOARD" && !ui.bracketId) || (ui.screen === "BRACKETS" && !ui.division)}
            className="px-4 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
            style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
          >
            {ui.loading ? "Updating…" : "Update screen"}
          </button>
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
