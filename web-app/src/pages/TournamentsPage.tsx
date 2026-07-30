import { useReducer, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import type { CreateTournamentInput, Tournament, TournamentStatus } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Separator } from "@/components/ui/separator";
import { Trophy, RefreshCw, ArrowRight } from "lucide-react";
import { api, ApiError } from "@/api";

const statusVariant: Record<TournamentStatus, "default" | "secondary" | "outline" | "destructive"> = {
  DRAFT: "outline",
  READY: "secondary",
  LIVE: "default",
  FINISHED: "destructive",
};

interface State {
  form: CreateTournamentInput;
  loading: boolean;
  error: string | null;
  success: boolean;
  tournaments: Tournament[];
  tournamentsLoading: boolean;
  tournamentsError: string | null;
  statusFilter: TournamentStatus | "ALL";
}

type Action =
  | { type: "setField"; field: keyof CreateTournamentInput; value: string | null }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string }
  | { type: "setTournaments"; tournaments: Tournament[] }
  | { type: "setTournamentsLoading" }
  | { type: "setTournamentsError"; message: string }
  | { type: "setStatusFilter"; value: TournamentStatus | "ALL" };

const initialForm: CreateTournamentInput = {
  name: "",
  location: null,
  startDate: null,
  endDate: null,
};

const initialState: State = {
  form: initialForm,
  loading: false,
  error: null,
  success: false,
  tournaments: [],
  tournamentsLoading: false,
  tournamentsError: null,
  statusFilter: "ALL",
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setField":
      return { ...state, form: { ...state.form, [action.field]: action.value }, success: false, error: null };
    case "submit":
      return { ...state, loading: true, error: null, success: false };
    case "success":
      return { ...state, loading: false, success: true, form: initialForm };
    case "error":
      return { ...state, loading: false, error: action.message };
    case "setTournaments":
      return { ...state, tournaments: action.tournaments, tournamentsLoading: false, tournamentsError: null };
    case "setTournamentsLoading":
      return { ...state, tournamentsLoading: true, tournamentsError: null };
    case "setTournamentsError":
      return { ...state, tournamentsLoading: false, tournamentsError: action.message };
    case "setStatusFilter":
      return { ...state, statusFilter: action.value };
    default:
      throw new Error("Unknown action");
  }
}

export function TournamentsPage() {
  const [state, dispatch] = useReducer(reducer, initialState);
  const navigate = useNavigate();

  async function loadTournaments() {
    dispatch({ type: "setTournamentsLoading" });
    try {
      const tournaments = await api.getTournaments();
      dispatch({ type: "setTournaments", tournaments });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "setTournamentsError", message: err.message });
      } else {
        dispatch({ type: "setTournamentsError", message: "Failed to load tournaments" });
      }
    }
  }

  useEffect(() => {
    loadTournaments();
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "submit" });
    try {
      await api.createTournament(state.form);
      dispatch({ type: "success" });
      loadTournaments();
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({ type: "error", message: "An error occurred while creating tournament" });
      }
    }
  }

  const filteredTournaments = state.tournaments.filter(
    (t) => state.statusFilter === "ALL" || t.status === state.statusFilter
  );

  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Tournaments</h1>
        <p className="text-sm text-muted-foreground mt-1">Create and manage tournaments</p>
      </div>

      {/* Create form */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Trophy className="w-4 h-4" />
            New tournament
          </CardTitle>
          <CardDescription>
            Location and dates are optional and can be added later.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5 col-span-2">
                <Label>Name</Label>
                <Input
                  value={state.form.name}
                  onChange={(e) => dispatch({ type: "setField", field: "name", value: e.target.value })}
                  placeholder="Tournament name"
                  required
                />
              </div>
              <div className="space-y-1.5 col-span-2">
                <Label>Location <span className="text-muted-foreground text-xs">(optional)</span></Label>
                <Input
                  value={state.form.location ?? ""}
                  onChange={(e) => dispatch({ type: "setField", field: "location", value: e.target.value || null })}
                  placeholder="e.g. Lisboa, Portugal"
                />
              </div>
              <div className="space-y-1.5">
                <Label>Start date <span className="text-muted-foreground text-xs">(optional)</span></Label>
                <Input
                  type="date"
                  value={state.form.startDate ?? ""}
                  onChange={(e) => dispatch({ type: "setField", field: "startDate", value: e.target.value || null })}
                />
              </div>
              <div className="space-y-1.5">
                <Label>End date <span className="text-muted-foreground text-xs">(optional)</span></Label>
                <Input
                  type="date"
                  value={state.form.endDate ?? ""}
                  onChange={(e) => dispatch({ type: "setField", field: "endDate", value: e.target.value || null })}
                />
              </div>
            </div>

            {state.error && (
              <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2">
                {state.error}
              </p>
            )}
            {state.success && (
              <p className="text-sm text-green-600 bg-green-500/10 border border-green-500/20 rounded-md px-3 py-2">
                Tournament created successfully.
              </p>
            )}

            <Button type="submit" disabled={state.loading}>
              {state.loading ? "Creating…" : "Create tournament"}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Separator />

      {/* Tournaments table */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-medium">
            All tournaments
            {!state.tournamentsLoading && (
              <span className="text-muted-foreground font-normal ml-2 text-sm">
                ({filteredTournaments.length})
              </span>
            )}
          </h2>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="icon"
              onClick={loadTournaments}
              disabled={state.tournamentsLoading}
            >
              <RefreshCw className={`w-4 h-4 ${state.tournamentsLoading ? "animate-spin" : ""}`} />
            </Button>
            <Select
              value={state.statusFilter}
              onValueChange={(value) => dispatch({ type: "setStatusFilter", value: value as TournamentStatus | "ALL" })}
            >
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All statuses</SelectItem>
                <SelectItem value="DRAFT">Draft</SelectItem>
                <SelectItem value="READY">Ready</SelectItem>
                <SelectItem value="LIVE">Live</SelectItem>
                <SelectItem value="FINISHED">Finished</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Location</TableHead>
                <TableHead>Start date</TableHead>
                <TableHead>Status</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {state.tournamentsLoading ? (
                Array.from({ length: 4 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-4 w-40" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-28" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                    <TableCell />
                  </TableRow>
                ))
              ) : state.tournamentsError ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-destructive text-sm py-6">
                    {state.tournamentsError}
                  </TableCell>
                </TableRow>
              ) : filteredTournaments.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground text-sm py-6">
                    No tournaments found.
                  </TableCell>
                </TableRow>
              ) : (
                filteredTournaments.map((tournament) => (
                  <TableRow key={tournament.id} className="cursor-pointer hover:bg-muted/50">
                    <TableCell className="font-medium">{tournament.name}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {tournament.location ?? "—"}
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm">
                      {tournament.startDate
                        ? new Date(tournament.startDate).toLocaleDateString()
                        : "—"}
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusVariant[tournament.status]} className="capitalize">
                        {tournament.status.toLowerCase()}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => navigate(`/dashboard/tournaments/${tournament.id}`)}
                      >
                        <ArrowRight className="w-4 h-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Card>
      </div>
    </div>
  );
}