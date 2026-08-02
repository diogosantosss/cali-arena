import { useReducer } from "react";
import type { ScreenState, TournamentState, Match } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Monitor } from "lucide-react";
import { api, ApiError } from "@/api";

interface Props {
  tournamentId: number;
  state: TournamentState | null;
  matches: Match[];
  onUpdated: (state: TournamentState) => void;
}

interface LocalState {
  screen: ScreenState;
  matchId: number | null;
  loading: boolean;
  error: string | null;
}

type Action =
  | { type: "setScreen"; value: ScreenState }
  | { type: "setMatchId"; value: number | null }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string };

function reducer(state: LocalState, action: Action): LocalState {
  switch (action.type) {
    case "setScreen":
      return { ...state, screen: action.value, matchId: action.value !== "BATTLE" ? null : state.matchId };
    case "setMatchId":
      return { ...state, matchId: action.value };
    case "submit":
      return { ...state, loading: true, error: null };
    case "success":
      return { ...state, loading: false };
    case "error":
      return { ...state, loading: false, error: action.message };
    default:
      throw new Error("Unknown action");
  }
}

const screenLabels: Record<ScreenState, string> = {
  WAITING: "Waiting",
  ROUTINES: "Routines",
  BATTLE: "Battle",
  WINNER: "Winner",
  LEADERBOARD: "Leaderboard",
};

export function ScreenControl({ tournamentId, state: tournamentState, matches, onUpdated }: Props) {
  const [state, dispatch] = useReducer(reducer, {
    screen: tournamentState?.currentScreen ?? "WAITING",
    matchId: tournamentState?.currentMatchId ?? null,
    loading: false,
    error: null,
  });

  async function handleUpdate() {
    dispatch({ type: "submit" });
    try {
      const updated = await api.updateScreen(tournamentId, {
        screen: state.screen,
        currentMatchId: state.screen === "BATTLE" ? state.matchId : null,
      });
      dispatch({ type: "success" });
      onUpdated(updated);
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({ type: "error", message: "Failed to update screen" });
      }
    }
  }

  const readyMatches = matches.filter((m) => m.status === "READY" || m.status === "RUNNING");

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center justify-between text-base">
          <div className="flex items-center gap-2">
            <Monitor className="w-4 h-4" />
            Screen control
          </div>
          {tournamentState && (
            <Badge variant="outline" className="capitalize">
              Current: {screenLabels[tournamentState.currentScreen]}
            </Badge>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-end gap-3">
          <div className="space-y-1.5">
            <p className="text-xs text-muted-foreground uppercase tracking-wider">Screen</p>
            <Select
              value={state.screen}
              onValueChange={(value) => dispatch({ type: "setScreen", value: value as ScreenState })}
            >
              <SelectTrigger className="w-44">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Object.entries(screenLabels).map(([value, label]) => (
                  <SelectItem key={value} value={value}>{label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {state.screen === "BATTLE" && (
            <div className="space-y-1.5">
              <p className="text-xs text-muted-foreground uppercase tracking-wider">Match</p>
              <Select
                value={state.matchId ? String(state.matchId) : ""}
                onValueChange={(value) => dispatch({ type: "setMatchId", value: Number(value) })}
              >
                <SelectTrigger className="w-48">
                  <SelectValue placeholder="Select match" />
                </SelectTrigger>
                <SelectContent>
                  {readyMatches.map((m) => (
                    <SelectItem key={m.id} value={String(m.id)}>
                      Match #{m.id}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <Button
            onClick={handleUpdate}
            disabled={state.loading || (state.screen === "BATTLE" && !state.matchId)}
          >
            {state.loading ? "Updating…" : "Update screen"}
          </Button>
        </div>

        {state.error && (
          <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2 mt-3">
            {state.error}
          </p>
        )}
      </CardContent>
    </Card>
  );
}