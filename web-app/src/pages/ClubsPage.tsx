import { useReducer, useEffect } from "react";
import type { Club, CreateClubInput } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Separator } from "@/components/ui/separator";
import { Building2, RefreshCw } from "lucide-react";
import { api, ApiError } from "@/api";

interface State {
  form: CreateClubInput;
  loading: boolean;
  error: string | null;
  success: boolean;
  clubs: Club[];
  clubsLoading: boolean;
  clubsError: string | null;
}

type Action =
  | { type: "setField"; field: keyof CreateClubInput; value: string }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string }
  | { type: "setClubs"; clubs: Club[] }
  | { type: "setClubsLoading" }
  | { type: "setClubsError"; message: string };

const initialForm: CreateClubInput = { name: "", shortName: "" };

const initialState: State = {
  form: initialForm,
  loading: false,
  error: null,
  success: false,
  clubs: [],
  clubsLoading: false,
  clubsError: null,
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
    case "setClubs":
      return { ...state, clubs: action.clubs, clubsLoading: false, clubsError: null };
    case "setClubsLoading":
      return { ...state, clubsLoading: true, clubsError: null };
    case "setClubsError":
      return { ...state, clubsLoading: false, clubsError: action.message };
    default:
      throw new Error("Unknown action");
  }
}

export function ClubsPage() {
  const [state, dispatch] = useReducer(reducer, initialState);

  async function loadClubs() {
    dispatch({ type: "setClubsLoading" });
    try {
      const clubs = await api.getClubs();
      dispatch({ type: "setClubs", clubs });
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "setClubsError", message: err.message });
      } else {
        dispatch({ type: "setClubsError", message: "Failed to load clubs" });
      }
    }
  }

  useEffect(() => {
    loadClubs();
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "submit" });
    try {
      await api.createClub(state.form);
      dispatch({ type: "success" });
      loadClubs();
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({ type: "error", message: "An error occurred while creating club" });
      }
    }
  }

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Clubs</h1>
        <p className="text-sm text-muted-foreground mt-1">Create and manage clubs</p>
      </div>

      {/* Create form */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Building2 className="w-4 h-4" />
            New club
          </CardTitle>
          <CardDescription>
            Short name is used as an abbreviation displayed in tables and brackets.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1.5 col-span-2">
                <Label>Name</Label>
                <Input
                  value={state.form.name}
                  onChange={(e) => dispatch({ type: "setField", field: "name", value: e.target.value })}
                  placeholder="Club full name"
                  required
                />
              </div>
              <div className="space-y-1.5">
                <Label>Short name</Label>
                <Input
                  value={state.form.shortName}
                  onChange={(e) => dispatch({ type: "setField", field: "shortName", value: e.target.value })}
                  placeholder="e.g. CAL"
                  maxLength={6}
                  required
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
                Club created successfully.
              </p>
            )}

            <Button type="submit" disabled={state.loading}>
              {state.loading ? "Creating…" : "Create club"}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Separator />

      {/* Clubs table */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-medium">
            All clubs
            {!state.clubsLoading && (
              <span className="text-muted-foreground font-normal ml-2 text-sm">
                ({state.clubs.length})
              </span>
            )}
          </h2>
          <Button
            variant="outline"
            size="icon"
            onClick={loadClubs}
            disabled={state.clubsLoading}
          >
            <RefreshCw className={`w-4 h-4 ${state.clubsLoading ? "animate-spin" : ""}`} />
          </Button>
        </div>

        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Short name</TableHead>
                <TableHead>Created at</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {state.clubsLoading ? (
                Array.from({ length: 4 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-4 w-40" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  </TableRow>
                ))
              ) : state.clubsError ? (
                <TableRow>
                  <TableCell colSpan={3} className="text-center text-destructive text-sm py-6">
                    {state.clubsError}
                  </TableCell>
                </TableRow>
              ) : state.clubs.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} className="text-center text-muted-foreground text-sm py-6">
                    No clubs found.
                  </TableCell>
                </TableRow>
              ) : (
                state.clubs.map((club) => (
                  <TableRow key={club.id}>
                    <TableCell className="font-medium">{club.name}</TableCell>
                    <TableCell>
                      <span className="text-xs font-mono bg-muted px-2 py-0.5 rounded">
                        {club.shortName}
                      </span>
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm">
                      {new Date(club.createdAt).toLocaleDateString()}
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