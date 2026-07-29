import { useReducer, useEffect } from "react";
import type { Athlete, Club, CreateAthleteInput, Gender } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Separator } from "@/components/ui/separator";
import { UserPlus, RefreshCw } from "lucide-react";
import { api, ApiError } from "@/api";

interface State {
  form: CreateAthleteInput;
  loading: boolean;
  error: string | null;
  success: boolean;
  athletes: Athlete[];
  athletesLoading: boolean;
  athletesError: string | null;
  clubs: Club[];
  clubsLoading: boolean;
  genderFilter: Gender | "ALL";
  clubFilter: number | "ALL";
}

type Action =
  | { type: "setField"; field: keyof CreateAthleteInput; value: string | number }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string }
  | { type: "setAthletes"; athletes: Athlete[] }
  | { type: "setAthletesLoading" }
  | { type: "setAthletesError"; message: string }
  | { type: "setClubs"; clubs: Club[] }
  | { type: "setClubsLoading" }
  | { type: "setGenderFilter"; value: Gender | "ALL" }
  | { type: "setClubFilter"; value: number | "ALL" };

const initialForm: CreateAthleteInput = {
  name: "",
  gender: "MALE",
  clubId: 0,
};

const initialState: State = {
  form: initialForm,
  loading: false,
  error: null,
  success: false,
  athletes: [],
  athletesLoading: false,
  athletesError: null,
  clubs: [],
  clubsLoading: false,
  genderFilter: "ALL",
  clubFilter: "ALL",
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
    case "setAthletes":
      return { ...state, athletes: action.athletes, athletesLoading: false, athletesError: null };
    case "setAthletesLoading":
      return { ...state, athletesLoading: true, athletesError: null };
    case "setAthletesError":
      return { ...state, athletesLoading: false, athletesError: action.message };
    case "setClubs":
      return { ...state, clubs: action.clubs, clubsLoading: false };
    case "setClubsLoading":
      return { ...state, clubsLoading: true };
    case "setGenderFilter":
      return { ...state, genderFilter: action.value };
    case "setClubFilter":
      return { ...state, clubFilter: action.value };
    default:
      throw new Error("Unknown action");
  }
}

export function AthletesPage() {
  const [state, dispatch] = useReducer(reducer, initialState);

  async function loadAthletes() {
    dispatch({ type: "setAthletesLoading" });
    try {
      const athletes = await api.getAthletes();
      dispatch({ type: "setAthletes", athletes });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "setAthletesError", message: err.message });
      } else {
        dispatch({ type: "setAthletesError", message: "Failed to load athletes" });
      }
    }
  }

  async function loadClubs() {
    dispatch({ type: "setClubsLoading" });
    try {
      const clubs = await api.getClubs();
      dispatch({ type: "setClubs", clubs });
    } catch (err) {
      // silently fail — clubs just won't show in the select
    }
  }

  useEffect(() => {
    loadAthletes();
    loadClubs();
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!state.form.clubId) return;
    dispatch({ type: "submit" });
    try {
      await api.createAthlete(state.form);
      dispatch({ type: "success" });
      loadAthletes();
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({ type: "error", message: "An error occurred while creating athlete" });
      }
    }
  }

  const filteredAthletes = state.athletes
    .filter((a) => state.genderFilter === "ALL" || a.gender === state.genderFilter)
    .filter((a) => state.clubFilter === "ALL" || a.clubId === state.clubFilter);

  const getClubName = (clubId: number) =>
    state.clubs.find((c) => c.id === clubId)?.shortName ?? `#${clubId}`;

  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Athletes</h1>
        <p className="text-sm text-muted-foreground mt-1">Create and manage athletes</p>
      </div>

      {/* Create form */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <UserPlus className="w-4 h-4" />
            New athlete
          </CardTitle>
          <CardDescription>Athlete will be assigned to a club and gender category.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1.5 col-span-1">
                <Label>Name</Label>
                <Input
                  value={state.form.name}
                  onChange={(e) => dispatch({ type: "setField", field: "name", value: e.target.value })}
                  placeholder="Athlete name"
                  required
                />
              </div>

              <div className="space-y-1.5">
                <Label>Gender</Label>
                <Select
                  value={state.form.gender}
                  onValueChange={(value) => dispatch({ type: "setField", field: "gender", value })}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="MALE">Male</SelectItem>
                    <SelectItem value="FEMALE">Female</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label>Club</Label>
                <Select
                  value={state.form.clubId ? String(state.form.clubId) : ""}
                  onValueChange={(value) => dispatch({ type: "setField", field: "clubId", value: Number(value) })}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Select club" />
                  </SelectTrigger>
                  <SelectContent>
                    {state.clubs.map((club) => (
                      <SelectItem key={club.id} value={String(club.id)}>
                        {club.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {state.error && (
              <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2">
                {state.error}
              </p>
            )}
            {state.success && (
              <p className="text-sm text-green-600 bg-green-500/10 border border-green-500/20 rounded-md px-3 py-2">
                Athlete created successfully.
              </p>
            )}

            <Button type="submit" disabled={state.loading || !state.form.clubId}>
              {state.loading ? "Creating…" : "Create athlete"}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Separator />

      {/* Athletes table */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-medium">
            All athletes
            {!state.athletesLoading && (
              <span className="text-muted-foreground font-normal ml-2 text-sm">
                ({filteredAthletes.length})
              </span>
            )}
          </h2>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="icon"
              onClick={loadAthletes}
              disabled={state.athletesLoading}
            >
              <RefreshCw className={`w-4 h-4 ${state.athletesLoading ? "animate-spin" : ""}`} />
            </Button>
            <Select
              value={state.genderFilter}
              onValueChange={(value) => dispatch({ type: "setGenderFilter", value: value as Gender | "ALL" })}
            >
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All genders</SelectItem>
                <SelectItem value="MALE">Male</SelectItem>
                <SelectItem value="FEMALE">Female</SelectItem>
              </SelectContent>
            </Select>
            <Select
              value={state.clubFilter === "ALL" ? "ALL" : String(state.clubFilter)}
              onValueChange={(value) =>
                dispatch({ type: "setClubFilter", value: value === "ALL" ? "ALL" : Number(value) })
              }
            >
              <SelectTrigger className="w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All clubs</SelectItem>
                {state.clubs.map((club) => (
                  <SelectItem key={club.id} value={String(club.id)}>
                    {club.shortName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Gender</TableHead>
                <TableHead>Club</TableHead>
                <TableHead>Created at</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {state.athletesLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-4 w-36" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  </TableRow>
                ))
              ) : state.athletesError ? (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-destructive text-sm py-6">
                    {state.athletesError}
                  </TableCell>
                </TableRow>
              ) : filteredAthletes.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-muted-foreground text-sm py-6">
                    No athletes found.
                  </TableCell>
                </TableRow>
              ) : (
                filteredAthletes.map((athlete) => (
                  <TableRow key={athlete.id}>
                    <TableCell className="font-medium">{athlete.name}</TableCell>
                    <TableCell>
                      <Badge variant={athlete.gender === "MALE" ? "default" : "secondary"} className="capitalize">
                        {athlete.gender.toLowerCase()}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {getClubName(athlete.clubId)}
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm">
                      {new Date(athlete.createdAt).toLocaleDateString()}
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