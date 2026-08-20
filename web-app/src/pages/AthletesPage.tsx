import { useReducer, useEffect } from "react";
import type { Athlete, Club, CreateAthleteInput, Gender } from "@/types";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { UserPlus, RefreshCw, MapPin, CalendarDays } from "lucide-react";
import { api, ApiError } from "@/api";

const genderStyles: Record<Gender, { label: string; color: string; bg: string }> = {
  MALE: { label: "Male", color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  FEMALE: { label: "Female", color: "#ec6d9f", bg: "rgba(236,109,159,0.12)" },
};

interface State {
  form: CreateAthleteInput;
  loading: boolean;
  error: string | null;
  athletes: Athlete[];
  athletesLoading: boolean;
  athletesError: string | null;
  clubs: Club[];
  search: string;
  genderFilter: Gender | "ALL";
  clubFilter: number | "ALL";
  formOpen: boolean;
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
  | { type: "setSearch"; value: string }
  | { type: "setGenderFilter"; value: Gender | "ALL" }
  | { type: "setClubFilter"; value: number | "ALL" }
  | { type: "toggleForm" };

const initialForm: CreateAthleteInput = {
  name: "",
  gender: "MALE",
  clubId: 0,
};

const initialState: State = {
  form: initialForm,
  loading: false,
  error: null,
  athletes: [],
  athletesLoading: false,
  athletesError: null,
  clubs: [],
  search: "",
  genderFilter: "ALL",
  clubFilter: "ALL",
  formOpen: false,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setField":
      return { ...state, form: { ...state.form, [action.field]: action.value }, error: null };
    case "submit":
      return { ...state, loading: true, error: null };
    case "success":
      return { ...state, loading: false, form: initialForm, formOpen: false };
    case "error":
      return { ...state, loading: false, error: action.message };
    case "setAthletes":
      return { ...state, athletes: action.athletes, athletesLoading: false, athletesError: null };
    case "setAthletesLoading":
      return { ...state, athletesLoading: true, athletesError: null };
    case "setAthletesError":
      return { ...state, athletesLoading: false, athletesError: action.message };
    case "setClubs":
      return { ...state, clubs: action.clubs };
    case "setSearch":
      return { ...state, search: action.value };
    case "setGenderFilter":
      return { ...state, genderFilter: action.value };
    case "setClubFilter":
      return { ...state, clubFilter: action.value };
    case "toggleForm":
      return { ...state, formOpen: !state.formOpen, error: null };
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
      dispatch({
        type: "setAthletesError",
        message: err instanceof ApiError ? err.message : "Failed to load athletes",
      });
    }
  }

  async function loadClubs() {
    try {
      const clubs = await api.getClubs();
      dispatch({ type: "setClubs", clubs });
    } catch {
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
      dispatch({
        type: "error",
        message: err instanceof ApiError ? err.message : "Failed to create athlete",
      });
    }
  }

  const query = (state.search ?? "").trim().toLowerCase();
  const filteredAthletes = state.athletes
    .filter((a) => state.genderFilter === "ALL" || a.gender === state.genderFilter)
    .filter((a) => state.clubFilter === "ALL" || a.clubId === state.clubFilter)
    .filter((a) => !query || a.name.toLowerCase().includes(query));

  const getClubName = (clubId: number) =>
    state.clubs.find((c) => c.id === clubId)?.shortName ?? `#${clubId}`;

  const selectTrigger = "h-8 text-xs border-[#252528] focus:ring-[#e8a020]/40";
  const selectContentStyle = { background: "#17171a", border: "1px solid #252528" };

  return (
    <div className="max-w-5xl mx-auto space-y-10">
      <div className="flex items-end justify-between">
        <div>
          <p className="text-xs tracking-widest uppercase mb-1.5" style={{ color: "#6b6560" }}>
            Management
          </p>
          <h1
            className="text-4xl leading-tight"
            style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "#f0ede8" }}
          >
            Athletes
          </h1>
        </div>

        <button
          onClick={() => dispatch({ type: "toggleForm" })}
          className="flex items-center gap-2 px-4 py-2 rounded text-sm font-medium transition-colors"
          style={{
            background: state.formOpen ? "rgba(232,160,32,0.1)" : "#e8a020",
            color: state.formOpen ? "#e8a020" : "#0f0f11",
            border: state.formOpen ? "1px solid rgba(232,160,32,0.3)" : "none",
          }}
        >
          <UserPlus className="w-3.5 h-3.5" />
          New athlete
        </button>
      </div>

      {state.formOpen && (
        <div className="rounded-lg p-6 space-y-5" style={{ background: "#17171a", border: "1px solid #252528" }}>
          <p className="text-xs tracking-widest uppercase" style={{ color: "#6b6560" }}>
            New athlete
          </p>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2 space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Name</Label>
                <Input
                  value={state.form.name}
                  onChange={(e) => dispatch({ type: "setField", field: "name", value: e.target.value })}
                  placeholder="Athlete name"
                  required
                  className="border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                  style={{ background: "#0f0f11" }}
                />
              </div>

              <div className="space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Gender</Label>
                <Select
                  value={state.form.gender}
                  onValueChange={(value) => dispatch({ type: "setField", field: "gender", value })}
                >
                  <SelectTrigger className={`${selectTrigger} w-full`} style={{ background: "#0f0f11", color: "#a09a92" }}>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent style={selectContentStyle}>
                    <SelectItem value="MALE" className="text-xs" style={{ color: "#a09a92" }}>Male</SelectItem>
                    <SelectItem value="FEMALE" className="text-xs" style={{ color: "#a09a92" }}>Female</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Club</Label>
                <Select
                  value={state.form.clubId ? String(state.form.clubId) : ""}
                  onValueChange={(value) => dispatch({ type: "setField", field: "clubId", value: Number(value) })}
                >
                  <SelectTrigger className={`${selectTrigger} w-full`} style={{ background: "#0f0f11", color: "#a09a92" }}>
                    <SelectValue placeholder="Select club" />
                  </SelectTrigger>
                  <SelectContent style={selectContentStyle}>
                    {state.clubs.map((club) => (
                      <SelectItem key={club.id} value={String(club.id)} className="text-xs" style={{ color: "#a09a92" }}>
                        {club.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {state.error && (
              <p
                className="text-sm rounded px-3 py-2"
                style={{ background: "rgba(241,106,106,0.1)", color: "#f16a6a", border: "1px solid rgba(241,106,106,0.25)" }}
              >
                {state.error}
              </p>
            )}

            <div className="flex items-center gap-3 pt-1">
              <button
                type="submit"
                disabled={state.loading || !state.form.clubId}
                className="px-5 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
                style={{ background: "#e8a020", color: "#0f0f11" }}
              >
                {state.loading ? "Creating…" : "Create"}
              </button>
              <button
                type="button"
                onClick={() => dispatch({ type: "toggleForm" })}
                className="px-4 py-2 rounded text-sm transition-colors"
                style={{ color: "#6b6560" }}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <Input
            value={state.search}
            onChange={(e) => dispatch({ type: "setSearch", value: e.target.value })}
            placeholder="Search athletes…"
            className="h-8 w-64 border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
            style={{ background: "#17171a" }}
          />
          <div className="flex items-center gap-2">
            <span className="text-sm tabular-nums" style={{ color: "#6b6560" }}>
              {filteredAthletes.length} athlete{filteredAthletes.length !== 1 ? "s" : ""}
            </span>
            <button
              onClick={loadAthletes}
              disabled={state.athletesLoading}
              className="p-1.5 rounded transition-colors"
              style={{ color: "#6b6560" }}
            >
              <RefreshCw className={`w-3.5 h-3.5 ${state.athletesLoading ? "animate-spin" : ""}`} />
            </button>
            <Select
              value={state.genderFilter}
              onValueChange={(value) => dispatch({ type: "setGenderFilter", value: value as Gender | "ALL" })}
            >
              <SelectTrigger className={`${selectTrigger} w-32`} style={{ background: "#17171a", color: "#a09a92" }}>
                <SelectValue />
              </SelectTrigger>
              <SelectContent style={selectContentStyle}>
                <SelectItem value="ALL" className="text-xs" style={{ color: "#a09a92" }}>All genders</SelectItem>
                <SelectItem value="MALE" className="text-xs" style={{ color: "#a09a92" }}>Male</SelectItem>
                <SelectItem value="FEMALE" className="text-xs" style={{ color: "#a09a92" }}>Female</SelectItem>
              </SelectContent>
            </Select>
            <Select
              value={state.clubFilter === "ALL" ? "ALL" : String(state.clubFilter)}
              onValueChange={(value) =>
                dispatch({ type: "setClubFilter", value: value === "ALL" ? "ALL" : Number(value) })
              }
            >
              <SelectTrigger className={`${selectTrigger} w-36`} style={{ background: "#17171a", color: "#a09a92" }}>
                <SelectValue />
              </SelectTrigger>
              <SelectContent style={selectContentStyle}>
                <SelectItem value="ALL" className="text-xs" style={{ color: "#a09a92" }}>All clubs</SelectItem>
                {state.clubs.map((club) => (
                  <SelectItem key={club.id} value={String(club.id)} className="text-xs" style={{ color: "#a09a92" }}>
                    {club.shortName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {state.athletesLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-16 rounded-lg" style={{ background: "#17171a" }}>
                <Skeleton className="h-full w-full rounded-lg opacity-40" />
              </div>
            ))}
          </div>
        ) : state.athletesError ? (
          <p className="text-sm py-8 text-center" style={{ color: "#f16a6a" }}>
            {state.athletesError}
          </p>
        ) : filteredAthletes.length === 0 ? (
          <div className="py-20 text-center">
            <p className="text-xs tracking-widest uppercase" style={{ color: "#3a3a3d" }}>
              No athletes found
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {filteredAthletes.map((a, i) => {
              const g = genderStyles[a.gender];
              return (
                <div
                  key={a.id}
                  className="group flex items-center gap-5 px-5 py-4 rounded-lg transition-colors animate-fade-up"
                  style={{
                    background: "#17171a",
                    border: "1px solid #252528",
                    animationDelay: `${i * 0.03}s`,
                    opacity: 0,
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.borderColor = "#363639")}
                  onMouseLeave={(e) => (e.currentTarget.style.borderColor = "#252528")}
                >
                  <div className="w-1 self-stretch rounded-full shrink-0" style={{ background: g.color, opacity: 0.5 }} />

                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate" style={{ color: "#f0ede8" }}>
                      {a.name}
                    </p>
                    <div className="flex items-center gap-3 mt-0.5">
                      <span className="flex items-center gap-1 text-xs" style={{ color: "#6b6560" }}>
                        <MapPin className="w-3 h-3" />
                        {getClubName(a.clubId)}
                      </span>
                      <span className="flex items-center gap-1 text-xs" style={{ color: "#6b6560" }}>
                        <CalendarDays className="w-3 h-3" />
                        {new Date(a.createdAt).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}
                      </span>
                    </div>
                  </div>

                  <span className="text-xs px-2.5 py-1 rounded-full shrink-0 capitalize" style={{ background: g.bg, color: g.color }}>
                    {g.label}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}