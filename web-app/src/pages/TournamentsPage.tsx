import { useReducer, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import type { CreateTournamentInput, Tournament, TournamentStatus } from "@/types";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { MapPin, CalendarDays, ArrowRight, Plus, RefreshCw } from "lucide-react";
import { api, ApiError } from "@/api";

const statusStyles: Record<TournamentStatus, { label: string; color: string; bg: string }> = {
  DRAFT:    { label: "Draft",    color: "#6b6560", bg: "rgba(107,101,96,0.12)" },
  READY:    { label: "Ready",    color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  LIVE:     { label: "Live",     color: "#e8a020", bg: "rgba(232,160,32,0.12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
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
  formOpen: boolean;
}

type Action =
  | { type: "setField"; field: keyof CreateTournamentInput; value: string | null }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string }
  | { type: "setTournaments"; tournaments: Tournament[] }
  | { type: "setTournamentsLoading" }
  | { type: "setTournamentsError"; message: string }
  | { type: "setStatusFilter"; value: TournamentStatus | "ALL" }
  | { type: "toggleForm" };

const initialForm: CreateTournamentInput = { name: "", location: null, startDate: null, endDate: null };

const initialState: State = {
  form: initialForm,
  loading: false,
  error: null,
  success: false,
  tournaments: [],
  tournamentsLoading: false,
  tournamentsError: null,
  statusFilter: "ALL",
  formOpen: false,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setField":
      return { ...state, form: { ...state.form, [action.field]: action.value }, success: false, error: null };
    case "submit":
      return { ...state, loading: true, error: null, success: false };
    case "success":
      return { ...state, loading: false, success: true, form: initialForm, formOpen: false };
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
    case "toggleForm":
      return { ...state, formOpen: !state.formOpen, error: null };
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
      dispatch({ type: "setTournamentsError", message: err instanceof ApiError ? err.message : "Failed to load" });
    }
  }

  useEffect(() => { loadTournaments(); }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "submit" });
    try {
      await api.createTournament(state.form);
      dispatch({ type: "success" });
      loadTournaments();
    } catch (err) {
      dispatch({ type: "error", message: err instanceof ApiError ? err.message : "Failed to create" });
    }
  }

  const filtered = state.tournaments.filter(
    (t) => state.statusFilter === "ALL" || t.status === state.statusFilter
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
            Tournaments
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
          <Plus className="w-3.5 h-3.5" />
          New tournament
        </button>
      </div>

      {state.formOpen && (
        <div
          className="rounded-lg p-6 space-y-5"
          style={{ background: "#17171a", border: "1px solid #252528" }}
        >
          <p
            className="text-xs tracking-widest uppercase"
            style={{ color: "#6b6560" }}
          >
            New tournament
          </p>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2 space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Name</Label>
                <Input
                  value={state.form.name}
                  onChange={(e) => dispatch({ type: "setField", field: "name", value: e.target.value })}
                  placeholder="e.g. Open Lisboa 2026"
                  required
                  className="border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                  style={{ background: "#0f0f11" }}
                />
              </div>
              <div className="col-span-2 space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>
                  Location <span style={{ color: "#3a3a3d" }}>(optional)</span>
                </Label>
                <Input
                  value={state.form.location ?? ""}
                  onChange={(e) => dispatch({ type: "setField", field: "location", value: e.target.value || null })}
                  placeholder="e.g. Lisboa, Portugal"
                  className="border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                  style={{ background: "#0f0f11" }}
                />
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>
                  Start date <span style={{ color: "#3a3a3d" }}>(optional)</span>
                </Label>
                <Input
                  type="date"
                  value={state.form.startDate ?? ""}
                  onChange={(e) => dispatch({ type: "setField", field: "startDate", value: e.target.value || null })}
                  className="border-[#252528] text-[#f0ede8] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                  style={{ background: "#0f0f11", colorScheme: "dark" }}
                />
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>
                  End date <span style={{ color: "#3a3a3d" }}>(optional)</span>
                </Label>
                <Input
                  type="date"
                  value={state.form.endDate ?? ""}
                  onChange={(e) => dispatch({ type: "setField", field: "endDate", value: e.target.value || null })}
                  className="border-[#252528] text-[#f0ede8] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                  style={{ background: "#0f0f11", colorScheme: "dark" }}
                />
              </div>
            </div>

            {state.error && (
              <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded px-3 py-2">
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
          <div className="flex items-center gap-2">
            <span className="text-sm" style={{ color: "#6b6560" }}>
              {filtered.length} tournament{filtered.length !== 1 ? "s" : ""}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={loadTournaments}
              disabled={state.tournamentsLoading}
              className="p-1.5 rounded transition-colors"
              style={{ color: "#6b6560" }}
            >
              <RefreshCw className={`w-3.5 h-3.5 ${state.tournamentsLoading ? "animate-spin" : ""}`} />
            </button>
            <Select
              value={state.statusFilter}
              onValueChange={(v) => dispatch({ type: "setStatusFilter", value: v as TournamentStatus | "ALL" })}
            >
              <SelectTrigger
                className="h-8 text-xs w-32 border-[#252528] focus:ring-[#e8a020]/40"
                style={{ background: "#17171a", color: "#a09a92" }}
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent style={{ background: "#17171a", border: "1px solid #252528" }}>
                {["ALL", "DRAFT", "READY", "LIVE", "FINISHED"].map((s) => (
                  <SelectItem key={s} value={s} className="text-xs" style={{ color: "#a09a92" }}>
                    {s === "ALL" ? "All statuses" : s.charAt(0) + s.slice(1).toLowerCase()}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {state.tournamentsLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-16 rounded-lg" style={{ background: "#17171a" }}>
                <Skeleton className="h-full w-full rounded-lg opacity-40" />
              </div>
            ))}
          </div>
        ) : state.tournamentsError ? (
          <p className="text-sm text-destructive py-8 text-center">{state.tournamentsError}</p>
        ) : filtered.length === 0 ? (
          <div className="py-20 text-center">
            <p className="text-xs tracking-widest uppercase" style={{ color: "#3a3a3d" }}>
              No tournaments found
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {filtered.map((t, i) => {
              const s = statusStyles[t.status];
              return (
                <div
                  key={t.id}
                  onClick={() => navigate(`/dashboard/tournaments/${t.id}`)}
                  className="group flex items-center gap-5 px-5 py-4 rounded-lg cursor-pointer transition-colors animate-fade-up"
                  style={{
                    background: "#17171a",
                    border: "1px solid #252528",
                    animationDelay: `${i * 0.04}s`,
                    opacity: 0,
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.borderColor = "#363639")}
                  onMouseLeave={(e) => (e.currentTarget.style.borderColor = "#252528")}
                >
                  <div
                    className="w-1 self-stretch rounded-full shrink-0"
                    style={{ background: s.color, opacity: 0.5 }}
                  />

                  <div className="flex-1 min-w-0">
                    <p
                      className="text-sm font-medium truncate"
                      style={{ color: "#f0ede8" }}
                    >
                      {t.name}
                    </p>
                    <div className="flex items-center gap-3 mt-0.5">
                      {t.location && (
                        <span className="flex items-center gap-1 text-xs" style={{ color: "#6b6560" }}>
                          <MapPin className="w-3 h-3" />
                          {t.location}
                        </span>
                      )}
                      {t.startDate && (
                        <span className="flex items-center gap-1 text-xs" style={{ color: "#6b6560" }}>
                          <CalendarDays className="w-3 h-3" />
                          {new Date(t.startDate).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}
                        </span>
                      )}
                    </div>
                  </div>

                  <span
                    className="text-xs px-2.5 py-1 rounded-full shrink-0"
                    style={{ background: s.bg, color: s.color }}
                  >
                    {s.label}
                  </span>

                  <ArrowRight
                    className="w-4 h-4 shrink-0 transition-transform group-hover:translate-x-0.5"
                    style={{ color: "#3a3a3d" }}
                  />
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
