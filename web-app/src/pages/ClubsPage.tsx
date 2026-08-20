import { useReducer, useEffect } from "react";
import type { Club, CreateClubInput } from "@/types";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Building2, RefreshCw, CalendarDays } from "lucide-react";
import { api, ApiError } from "@/api";

interface State {
  form: CreateClubInput;
  loading: boolean;
  error: string | null;
  clubs: Club[];
  clubsLoading: boolean;
  clubsError: string | null;
  search: string;
  formOpen: boolean;
}

type Action =
  | { type: "setField"; field: keyof CreateClubInput; value: string }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string }
  | { type: "setClubs"; clubs: Club[] }
  | { type: "setClubsLoading" }
  | { type: "setClubsError"; message: string }
  | { type: "setSearch"; value: string }
  | { type: "toggleForm" };

const initialForm: CreateClubInput = { name: "", shortName: "" };

const initialState: State = {
  form: initialForm,
  loading: false,
  error: null,
  clubs: [],
  clubsLoading: false,
  clubsError: null,
  search: "",
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
    case "setClubs":
      return { ...state, clubs: action.clubs, clubsLoading: false, clubsError: null };
    case "setClubsLoading":
      return { ...state, clubsLoading: true, clubsError: null };
    case "setClubsError":
      return { ...state, clubsLoading: false, clubsError: action.message };
    case "setSearch":
      return { ...state, search: action.value };
    case "toggleForm":
      return { ...state, formOpen: !state.formOpen, error: null };
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
      dispatch({
        type: "setClubsError",
        message: err instanceof ApiError ? err.message : "Failed to load clubs",
      });
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
      dispatch({
        type: "error",
        message: err instanceof ApiError ? err.message : "Failed to create club",
      });
    }
  }

  const query = (state.search ?? "").trim().toLowerCase();
  const filteredClubs = state.clubs.filter(
    (c) =>
      !query ||
      c.name.toLowerCase().includes(query) ||
      c.shortName.toLowerCase().includes(query)
  );

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
            Clubs
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
          <Building2 className="w-3.5 h-3.5" />
          New club
        </button>
      </div>

      {state.formOpen && (
        <div className="rounded-lg p-6 space-y-5" style={{ background: "#17171a", border: "1px solid #252528" }}>
          <p className="text-xs tracking-widest uppercase" style={{ color: "#6b6560" }}>
            New club
          </p>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Name</Label>
                <Input
                  value={state.form.name}
                  onChange={(e) => dispatch({ type: "setField", field: "name", value: e.target.value })}
                  placeholder="Club full name"
                  required
                  className="border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                  style={{ background: "#0f0f11" }}
                />
              </div>

              <div className="space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>
                  Short name <span style={{ color: "#3a3a3d" }}>(max 6)</span>
                </Label>
                <Input
                  value={state.form.shortName}
                  onChange={(e) => dispatch({ type: "setField", field: "shortName", value: e.target.value })}
                  placeholder="e.g. CAL"
                  maxLength={6}
                  required
                  className="border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                  style={{ background: "#0f0f11" }}
                />
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
                disabled={state.loading}
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
            placeholder="Search clubs…"
            className="h-8 w-64 border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
            style={{ background: "#17171a" }}
          />
          <div className="flex items-center gap-2">
            <span className="text-sm tabular-nums" style={{ color: "#6b6560" }}>
              {filteredClubs.length} club{filteredClubs.length !== 1 ? "s" : ""}
            </span>
            <button
              onClick={loadClubs}
              disabled={state.clubsLoading}
              className="p-1.5 rounded transition-colors"
              style={{ color: "#6b6560" }}
            >
              <RefreshCw className={`w-3.5 h-3.5 ${state.clubsLoading ? "animate-spin" : ""}`} />
            </button>
          </div>
        </div>

        {state.clubsLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-16 rounded-lg" style={{ background: "#17171a" }}>
                <Skeleton className="h-full w-full rounded-lg opacity-40" />
              </div>
            ))}
          </div>
        ) : state.clubsError ? (
          <p className="text-sm py-8 text-center" style={{ color: "#f16a6a" }}>
            {state.clubsError}
          </p>
        ) : filteredClubs.length === 0 ? (
          <div className="py-20 text-center">
            <p className="text-xs tracking-widest uppercase" style={{ color: "#3a3a3d" }}>
              No clubs found
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {filteredClubs.map((club, i) => (
              <div
                key={club.id}
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
                <div
                  className="w-1 self-stretch rounded-full shrink-0"
                  style={{ background: "#e8a020", opacity: 0.5 }}
                />

                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate" style={{ color: "#f0ede8" }}>
                    {club.name}
                  </p>
                  <div className="flex items-center gap-3 mt-0.5">
                    <span className="flex items-center gap-1 text-xs" style={{ color: "#6b6560" }}>
                      <CalendarDays className="w-3 h-3" />
                      {new Date(club.createdAt).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}
                    </span>
                  </div>
                </div>

                <span
                  className="text-xs px-2.5 py-1 rounded-md font-mono tracking-widest shrink-0 uppercase"
                  style={{ background: "#1e1e22", color: "#a09a92", border: "1px solid #252528" }}
                >
                  {club.shortName}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}