import { useReducer } from "react";
import { ApiError } from "@/lib/api/client";
import type { Athlete } from "@/features/athletes/types";
import type { Routine, RoutineOverview } from "@/features/routines/types";
import { tournamentsService } from "../services/tournaments.service";
import type { ScreenState, TournamentState } from "../types";
import { BattlePanel } from "@/features/matches/components/battle-panel";
import type { Match } from "@/features/matches/types";
import { ScreenRoutinesPanel } from "./screen-routines-panel";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ExternalLink } from "lucide-react";

const screenLabels: Record<ScreenState, string> = {
  WAITING: "Waiting",
  ROUTINES: "Routines",
  BATTLE: "Battle",
  WINNER: "Winner",
  LEADERBOARD: "Leaderboard",
};

interface ScreenControlProps {
  tournamentId: number;
  state: TournamentState | null;
  matches: Match[];
  athletes: Athlete[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
  onUpdated: (state: TournamentState) => void;
}

interface ScreenControlState {
  screen: ScreenState;
  matchId: number | null;
  loading: boolean;
  error: string | null;
}

type Action =
  | { type: "setScreen"; screen: ScreenState }
  | { type: "setMatchId"; id: number }
  | { type: "updateStart" }
  | { type: "updateDone" }
  | { type: "updateError"; message: string }
  | { type: "battleError"; message: string | null };

function createInitialState(state: TournamentState | null): ScreenControlState {
  return {
    screen: state?.currentScreen ?? "WAITING",
    matchId: state?.currentMatchId ?? null,
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
        error: null,
      };
    case "setMatchId":
      return { ...state, matchId: action.id, error: null };
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
  onUpdated,
}: ScreenControlProps) {
  const [ui, dispatch] = useReducer(reducer, tournamentState, createInitialState);

  async function handleUpdateScreen() {
    dispatch({ type: "updateStart" });
    try {
      const updated = await tournamentsService.updateScreen(tournamentId, {
        screen: ui.screen,
        currentMatchId: ui.screen === "BATTLE" ? ui.matchId : null,
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

  return (
    <div className="rounded-lg overflow-hidden" style={{ background: "#17171a", border: "1px solid #252528" }}>
      <div className="flex items-center justify-between px-5 py-4">
        <div>
          <p className="text-xs tracking-widest uppercase" style={{ color: "#6b6560" }}>
            Screen control
          </p>
          <h3
            className="text-xl leading-tight mt-1"
            style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "#f0ede8" }}
          >
            Spectator screen
          </h3>
        </div>
        <div className="flex items-center gap-3">
          {tournamentState && (
            <span
              className="text-[11px] px-2.5 py-1 rounded-full"
              style={{ background: "rgba(232,160,32,0.12)", color: "#e8a020" }}
            >
              Current: {screenLabels[tournamentState.currentScreen]}
            </span>
          )}
          <a
            href={`/screen/${tournamentId}`}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 px-2.5 py-1 rounded text-[11px] font-medium transition-colors"
            style={{ background: "#1e1e22", color: "#a09a92", border: "1px solid #252528" }}
            onMouseEnter={(e) => (e.currentTarget.style.color = "#e8a020")}
            onMouseLeave={(e) => (e.currentTarget.style.color = "#a09a92")}
          >
            <ExternalLink className="w-3 h-3" />
            Open screen
          </a>
        </div>
      </div>

      <div style={{ borderTop: "1px solid #252528" }} className="px-5 py-4 space-y-5">
        <div className="flex items-end gap-3">
          <div className="space-y-1.5">
            <p className="text-[10px] uppercase tracking-widest" style={{ color: "#6b6560" }}>Screen</p>
            <Select value={ui.screen} onValueChange={(value) => dispatch({ type: "setScreen", screen: value as ScreenState })}>
              <SelectTrigger className="h-8 text-xs w-44 border-[#252528] focus:ring-[#e8a020]/40" style={{ background: "#0f0f11", color: "#a09a92" }}>
                <SelectValue />
              </SelectTrigger>
              <SelectContent style={{ background: "#17171a", border: "1px solid #252528" }}>
                {Object.entries(screenLabels).map(([value, label]) => (
                  <SelectItem key={value} value={value} className="text-xs" style={{ color: "#a09a92" }}>{label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {ui.screen === "BATTLE" && (
            <div className="space-y-1.5">
              <p className="text-[10px] uppercase tracking-widest" style={{ color: "#6b6560" }}>Match</p>
              <Select
                value={ui.matchId ? String(ui.matchId) : ""}
                onValueChange={(value) => dispatch({ type: "setMatchId", id: Number(value) })}
              >
                <SelectTrigger className="h-8 text-xs w-64 border-[#252528] focus:ring-[#e8a020]/40" style={{ background: "#0f0f11", color: "#a09a92" }}>
                  <SelectValue placeholder="Select match" />
                </SelectTrigger>
                <SelectContent style={{ background: "#17171a", border: "1px solid #252528" }}>
                  {readyMatches.map((m) => {
                    const red = athletes.find((a) => a.id === m.athleteRedId);
                    const blue = athletes.find((a) => a.id === m.athleteBlueId);
                    return (
                      <SelectItem key={m.id} value={String(m.id)} className="text-xs" style={{ color: "#a09a92" }}>
                        #{m.id} — {red?.name ?? "Red"} vs {blue?.name ?? "Blue"}
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>
          )}

          <button
            onClick={() => void handleUpdateScreen()}
            disabled={ui.loading || (ui.screen === "BATTLE" && !ui.matchId)}
            className="px-4 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
            style={{ background: "#e8a020", color: "#0f0f11" }}
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
