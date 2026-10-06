import { useCallback, useEffect, useReducer, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ApiError } from "@/api/client";
import { tournamentsService } from "@/services/tournaments.service";
import type { Bracket, BracketStage, Tournament, TournamentState } from "@/data/tournaments";
import { athletesService } from "@/services/athletes.service";
import type { Athlete } from "@/data/athletes";
import { routinesService } from "@/services/routines.service";
import type { Routine, RoutineOverview } from "@/data/routines";
import { matchesService } from "@/services/matches.service";
import type { Match, MatchProgress } from "@/data/matches";
import { ScreenControl } from "@/components/tournaments/screen-control";
import { BracketView } from "@/components/matches/bracket-view";
import { StaffPanel } from "@/components/tournaments/staff-panel";
import { Tabs } from "@/components/shared/tabs";
import { AnimatedPanel as TabPanel } from "@/components/shared/animated-panel";
import { usePanelHost } from "@/hooks/use-panel-host";
import { usePermissions } from "@/hooks/use-permissions";
import type { TournamentStaff } from "@/services/tournament-staff.service";
import { tournamentStaffService } from "@/services/tournament-staff.service";
import { ConfirmDialog } from "@/components/tournaments/confirm-dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { ArrowLeft, MapPin, CalendarDays, LayoutDashboard, Users, Monitor, Grid, Settings, TriangleAlert, Loader2 } from "lucide-react";

const statusStyles: Record<Tournament["status"], { label: string; color: string; bg: string }> = {
  DRAFT: { label: "Draft", color: "var(--muted-foreground)", bg: "rgba(107,101,96,0.12)" },
  READY: { label: "Ready", color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  LIVE: { label: "Live", color: "var(--accent)", bg: "var(--accent-12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
};

interface DetailState {
  loading: boolean;
  error: string | null;
  tournament: Tournament | null;
  state: TournamentState | null;
  brackets: Bracket[];
  matches: Match[];
  progresses: Record<number, MatchProgress>;
  athletes: Athlete[];
  routines: Routine[];
  overviews: Record<string, RoutineOverview>;
  staff: TournamentStaff | null;
  staffLoading: boolean;
  staffError: string | null;
}

type Action =
  | { type: "loadStart" }
  | {
      type: "loadSuccess";
      tournament: Tournament;
      state: TournamentState | null;
      brackets: Bracket[];
      matches: Match[];
      progresses: Record<number, MatchProgress>;
      athletes: Athlete[];
      routines: Routine[];
      overviews: Record<string, RoutineOverview>;
      staff: TournamentStaff | null;
      staffLoading: boolean;
      staffError: string | null;
    }
  | { type: "loadError"; message: string }
  | { type: "stateUpdated"; state: TournamentState }
  | { type: "bracketCreated"; bracket: Bracket }
  | { type: "matchCreated"; match: Match }
  | { type: "matchUpdated"; match: Match }
  | { type: "matchDeleted"; matchId: number }
  | { type: "staffLoaded"; staff: TournamentStaff }
  | { type: "staffLoadingStart" }
  | { type: "staffLoadingEnd" }
  | { type: "staffError"; message: string }
  | { type: "refreshed"; brackets: Bracket[]; matches: Match[]; progresses: Record<number, MatchProgress> };

const initialDetailState: DetailState = {
  loading: false,
  error: null,
  tournament: null,
  state: null,
  brackets: [],
  matches: [],
  progresses: {},
  athletes: [],
  routines: [],
  overviews: {},
  staff: null,
  staffLoading: false,
  staffError: null,
};

function reducer(state: DetailState, action: Action): DetailState {
  switch (action.type) {
    case "loadStart":
      return { ...initialDetailState, loading: true };
    case "loadSuccess":
      return {
        loading: false,
        error: null,
        tournament: action.tournament,
        state: action.state,
        brackets: action.brackets,
        matches: action.matches,
        progresses: action.progresses,
        athletes: action.athletes,
        routines: action.routines,
        overviews: action.overviews,
        staff: action.staff,
        staffLoading: action.staffLoading,
        staffError: action.staffError,
      };
    case "loadError":
      return { ...state, loading: false, error: action.message };
    case "stateUpdated":
      return { ...state, state: action.state };
    case "bracketCreated":
      return { ...state, brackets: [...state.brackets, action.bracket] };
    case "matchCreated":
      return { ...state, matches: [...state.matches, action.match] };
    case "matchUpdated":
      return {
        ...state,
        matches: state.matches.map((m) => (m.id === action.match.id ? action.match : m)),
      };
    case "matchDeleted":
      return {
        ...state,
        matches: state.matches.filter((m) => m.id !== action.matchId),
        progresses: Object.fromEntries(
          Object.entries(state.progresses).filter(([id]) => Number(id) !== action.matchId),
        ),
      };
    case "refreshed":
      return { ...state, brackets: action.brackets, matches: action.matches, progresses: action.progresses };
    case "staffLoaded":
      return { ...state, staff: action.staff, staffLoading: false, staffError: null };
    case "staffLoadingStart":
      return { ...state, staffLoading: true, staffError: null };
    case "staffLoadingEnd":
      return { ...state, staffLoading: false };
    case "staffError":
      return { ...state, staffLoading: false, staffError: action.message };
  }
}

export function TournamentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const tournamentId = Number(id);
  const { canDeleteMatch, isAdmin, isHost } = usePermissions();
  const canManage = isAdmin || isHost;

const [activeTab, setActiveTab] = useState("overview");
  const { hostRef: panelHostRef, hostStyle: panelHostStyle } = usePanelHost(activeTab);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const [deletingTournament, setDeletingTournament] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [data, dispatch] = useReducer(reducer, initialDetailState);
  const {
    loading: tournamentLoading,
    error: tournamentError,
    tournament,
    state: tournamentState,
    brackets,
    matches,
    progresses,
    athletes,
    routines,
    overviews,
    staff,
    staffLoading,
    staffError,
  } = data;

  const tabs = [
    { id: "overview", label: "Overview", icon: <LayoutDashboard className="w-4 h-4" /> },
    { id: "staff", label: "Staff", icon: <Users className="w-4 h-4" />, badge: staff?.judges.length ?? 0 },
    { id: "screen", label: "Screen", icon: <Monitor className="w-4 h-4" /> },
    { id: "brackets", label: "Brackets", icon: <Grid className="w-4 h-4" />, badge: brackets.length },
    { id: "settings", label: "Settings", icon: <Settings className="w-4 h-4" /> },
  ];

const loadProgresses = useCallback(async (allMatches: Match[]) => {
    const finished = allMatches.filter((m) => m.status === "FINISHED");
    const entries = await Promise.all(
      finished.map(async (m) => {
        const progress = await matchesService.getProgressByMatchId(m.id).catch(() => null);
        return [m.id, progress] as const;
      }),
    );
    return Object.fromEntries(entries.filter(([, p]) => p !== null)) as Record<number, MatchProgress>;
  }, []);

  const loadTournament = useCallback(async () => {
    dispatch({ type: "loadStart" });
    try {
      const [loadedTournament, loadedState, loadedBrackets, loadedAthletes, loadedRoutines] =
        await Promise.all([
          tournamentsService.getTournamentById(tournamentId),
          tournamentsService.getTournamentState(tournamentId),
          tournamentsService.getBracketsByTournamentId(tournamentId),
          athletesService.getAthletes(),
          routinesService.getRoutines(),
        ]);

      const allMatches = await Promise.all(
        loadedBrackets.map((b) => matchesService.getMatchesByBracketId(b.id))
      );
      const flatMatches = allMatches.flat();
      const loadedProgresses = await loadProgresses(flatMatches);

      const loadedOverviews = await Promise.all(
        loadedRoutines.map(async (r) => {
          const overview = await routinesService.getRoutineOverview(r.name);
          return [r.name, overview] as const;
        })
      );

      dispatch({
        type: "loadSuccess",
        tournament: loadedTournament,
        state: loadedState,
        brackets: loadedBrackets,
        matches: flatMatches,
        progresses: loadedProgresses,
        athletes: loadedAthletes,
        routines: loadedRoutines,
        overviews: Object.fromEntries(loadedOverviews),
        staff: null,
        staffLoading: false,
        staffError: null,
      });
    } catch (err) {
      dispatch({
        type: "loadError",
        message: err instanceof ApiError ? err.message : "Failed to load tournament",
      });
    }
  }, [tournamentId, loadProgresses]);

  const refreshMatches = useCallback(async () => {
    try {
      const loadedBrackets = await tournamentsService.getBracketsByTournamentId(tournamentId);
      const allMatches = await Promise.all(
        loadedBrackets.map((b) => matchesService.getMatchesByBracketId(b.id))
      );
      const flatMatches = allMatches.flat();
      const loadedProgresses = await loadProgresses(flatMatches);
      dispatch({ type: "refreshed", brackets: loadedBrackets, matches: flatMatches, progresses: loadedProgresses });
    } catch (err) {
      console.error(err);
    }
  }, [tournamentId, loadProgresses]);

  useEffect(() => {
    const handle = setTimeout(() => {
      void loadTournament();
    }, 0);
    return () => clearTimeout(handle);
  }, [loadTournament]);

  useEffect(() => {
    if (!tournament) return;
    let cancelled = false;
    dispatch({ type: "staffLoadingStart" });
    tournamentStaffService
      .getStaff(tournamentId)
      .then((staff) => {
        if (!cancelled) dispatch({ type: "staffLoaded", staff });
      })
      .catch((err) => {
        if (!cancelled) dispatch({ type: "staffError", message: err instanceof Error ? err.message : "Failed to load staff" });
      });
    return () => {
      cancelled = true;
    };
  }, [tournament, tournamentId]);

  async function handleCreateBracket(division: string, stage: BracketStage) {
    try {
      const bracket = await tournamentsService.createBracket({ tournamentId, division, stage });
      dispatch({ type: "bracketCreated", bracket });
    } catch (err) {
      console.error(err);
    }
  }

  async function handleStartMatch(match: Match) {
    try {
      await matchesService.startMatch(match.id);
      const updated = await matchesService.getMatchById(match.id);
      dispatch({ type: "matchUpdated", match: updated });
    } catch (err) {
      console.error(err);
    }
  }

  async function handleDeleteMatch(match: Match) {
    try {
      await matchesService.deleteMatch(match.id);
      dispatch({ type: "matchDeleted", matchId: match.id });
    } catch (err) {
      console.error(err);
    }
  }

  async function handleAddJudge(userId: number) {
    try {
      dispatch({ type: "staffLoadingStart" });
      const updated = await tournamentStaffService.addJudge(tournamentId, userId);
      dispatch({ type: "staffLoaded", staff: updated });
    } catch (err) {
      dispatch({ type: "staffError", message: err instanceof Error ? err.message : "Failed to add judge" });
      console.error(err);
    }
  }

async function handleRemoveJudge(userId: number) {
    try {
      dispatch({ type: "staffLoadingStart" });
      await tournamentStaffService.removeJudge(tournamentId, userId);
      const updated = await tournamentStaffService.getStaff(tournamentId);
      dispatch({ type: "staffLoaded", staff: updated });
    } catch (err) {
      dispatch({ type: "staffError", message: err instanceof Error ? err.message : "Failed to remove judge" });
      console.error(err);
    }
  }

  async function handleAssignHost(userId: number) {
    try {
      dispatch({ type: "staffLoadingStart" });
      const updated = await tournamentStaffService.assignHost(tournamentId, userId);
      dispatch({ type: "staffLoaded", staff: updated });
    } catch (err) {
      dispatch({ type: "staffError", message: err instanceof Error ? err.message : "Failed to assign host" });
      console.error(err);
    }
  }

async function handleStatusChange(newStatus: Tournament["status"]) {
    try {
      await tournamentsService.updateTournamentStatus(tournamentId, newStatus);
      const updated = await tournamentsService.getTournamentById(tournamentId);
      dispatch({ type: "loadSuccess", ...data, tournament: updated } as any);
    } catch (err) {
      console.error(err);
    }
  }

  async function handleDeleteTournament() {
    setDeletingTournament(true);
    setDeleteError(null);
    try {
      await tournamentsService.deleteTournament(tournamentId);
      setConfirmDeleteOpen(false);
      navigate("/dashboard");
    } catch (err) {
      setDeleteError(err instanceof Error ? err.message : "Failed to delete tournament");
    } finally {
      setDeletingTournament(false);
    }
  }

  if (tournamentLoading && !tournament) {
    return (
      <div className="max-w-5xl mx-auto space-y-6">
        <Skeleton className="h-6 w-48" style={{ background: "var(--border)" }} />
        <Skeleton className="h-24 w-full" style={{ background: "var(--border)" }} />
        <Skeleton className="h-64 w-full" style={{ background: "var(--border)" }} />
      </div>
    );
  }

  if (tournamentError) {
    return (
      <div className="max-w-5xl mx-auto">
        <p className="text-sm text-destructive">{tournamentError}</p>
      </div>
    );
  }

  if (!tournament) return null;

  const s = statusStyles[tournament.status];
  const finishedMatches = matches.filter((m) => m.status === "FINISHED").length;
  const totalMatches = matches.length;
  const nextMatch = matches.find((m) => m.status === "PENDING" || m.status === "READY");
  const nextMatchBracket = nextMatch ? brackets.find((b) => b.id === nextMatch.bracketId) : null;
  const nextMatchRed = nextMatch ? athletes.find((a) => a.id === nextMatch.athleteRedId) : null;
  const nextMatchBlue = nextMatch ? athletes.find((a) => a.id === nextMatch.athleteBlueId) : null;
  const nextMatchRoutine = nextMatch ? routines.find((r) => r.id === nextMatch.routineId) : null;

  return (
    <div className="max-w-5xl mx-auto animate-fade-up" style={{ opacity: 0 }}>
      {/* Header */}
      <div className="space-y-4 mb-6">
        <button
          onClick={() => navigate("/dashboard")}
          className="flex items-center gap-1.5 text-xs transition-colors"
          style={{ color: "var(--muted-foreground)" }}
          onMouseEnter={(e) => (e.currentTarget.style.color = "var(--secondary-foreground)")}
          onMouseLeave={(e) => (e.currentTarget.style.color = "var(--muted-foreground)")}
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          All tournaments
        </button>

        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div className="space-y-2">
            <h1 className="text-3xl sm:text-4xl font-medium leading-tight" style={{ color: "var(--foreground)" }}>
              {tournament.name}
            </h1>
            <div className="flex flex-wrap items-center gap-4">
              {tournament.location && (
                <span className="flex items-center gap-1.5 text-sm" style={{ color: "var(--muted-foreground)" }}>
                  <MapPin className="w-4 h-4" />
                  {tournament.location}
                </span>
              )}
              {tournament.startDate && (
                <span className="flex items-center gap-1.5 text-sm" style={{ color: "var(--muted-foreground)" }}>
                  <CalendarDays className="w-4 h-4" />
                  {new Date(tournament.startDate).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}
                  {tournament.endDate && (
                    <> — {new Date(tournament.endDate).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}</>
                  )}
                </span>
              )}
            </div>
          </div>

          <span className="text-xs px-3 py-1 rounded-full" style={{ background: s.bg, color: s.color }}>
            {s.label}
          </span>
        </div>
      </div>

      {/* Tabs */}
      <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} variant="underline" />

      {/* Tab Panels */}
<div ref={panelHostRef} className="mt-6 relative" style={panelHostStyle}>
        <TabPanel name="overview" activeTab={activeTab}>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {/* Quick Stats */}
            <div className="rounded-lg p-4 sm:col-span-2 lg:col-span-1" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
              <p className="text-xs uppercase tracking-wide mb-3" style={{ color: "var(--muted-foreground)" }}>Quick Stats</p>
              <dl className="space-y-3">
                <div className="flex justify-between">
                  <dt style={{ color: "var(--muted-foreground)" }}>Total Matches</dt>
                  <dd className="font-medium">{totalMatches}</dd>
                </div>
                <div className="flex justify-between">
                  <dt style={{ color: "var(--muted-foreground)" }}>Completed</dt>
                  <dd className="font-medium" style={{ color: "var(--accent)" }}>{finishedMatches}</dd>
                </div>
                <div className="flex justify-between">
                  <dt style={{ color: "var(--muted-foreground)" }}>Remaining</dt>
                  <dd className="font-medium">{totalMatches - finishedMatches}</dd>
                </div>
                <div className="flex justify-between">
                  <dt style={{ color: "var(--muted-foreground)" }}>Brackets</dt>
                  <dd className="font-medium">{brackets.length}</dd>
                </div>
                <div className="flex justify-between">
                  <dt style={{ color: "var(--muted-foreground)" }}>Athletes</dt>
                  <dd className="font-medium">{athletes.length}</dd>
                </div>
              </dl>
            </div>

            {/* Next Match */}
            <div className="rounded-lg p-4 sm:col-span-2 lg:col-span-1" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
              <p className="text-xs uppercase tracking-wide mb-3" style={{ color: "var(--muted-foreground)" }}>Next Match</p>
              {nextMatch ? (
                <div className="space-y-2">
                  <p className="font-medium">{nextMatchBracket?.division ?? "Unknown"} • {nextMatchBracket?.stage ?? "Unknown"}</p>
                  <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>
                    {nextMatchRed?.name ?? "TBD"} vs {nextMatchBlue?.name ?? "TBD"}
                  </p>
                  <p className="text-xs" style={{ color: "var(--muted-foreground)" }}>
                    {nextMatchRoutine?.name ?? "No routine"}
                  </p>
                </div>
              ) : (
                <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>No upcoming matches</p>
              )}
            </div>

            {/* Host & Judges Summary */}
            <div className="rounded-lg p-4 sm:col-span-2 lg:col-span-1" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
              <p className="text-xs uppercase tracking-wide mb-3" style={{ color: "var(--muted-foreground)" }}>Staff</p>
              <dl className="space-y-2">
                <div className="flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold" style={{ background: "rgba(251,191,36,0.15)", color: "#fbbf24" }}>
                    {staff?.host?.username?.charAt(0).toUpperCase() ?? "?"}
                  </span>
                  <div className="flex-1 min-w-0">
                    <dt className="text-xs" style={{ color: "var(--muted-foreground)" }}>Host</dt>
                    <dd className="font-medium truncate">{staff?.host?.username ?? "Not assigned"}</dd>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold" style={{ background: "rgba(126,184,247,0.12)", color: "#7eb8f7" }}>
                    <Users className="w-3.5 h-3.5" />
                  </span>
                  <div>
                    <dt className="text-xs" style={{ color: "var(--muted-foreground)" }}>Judges</dt>
                    <dd className="font-medium">{staff?.judges.length ?? 0} assigned</dd>
                  </div>
                </div>
              </dl>
              {canManage && (
                <button
                  onClick={() => setActiveTab("staff")}
                  className="mt-3 w-full px-3 py-1.5 text-xs font-medium rounded transition-colors"
                  style={{ background: "var(--accent-12)", color: "var(--accent)" }}
                >
                  Manage Staff →
                </button>
              )}
            </div>
          </div>
        </TabPanel>

        <TabPanel name="staff" activeTab={activeTab}>
          <StaffPanel
            tournamentId={tournamentId}
            staff={staff}
            loading={staffLoading}
            error={staffError}
onAddJudge={handleAddJudge}
            onRemoveJudge={handleRemoveJudge}
            onAssignHost={handleAssignHost}
          />
        </TabPanel>

        <TabPanel name="screen" activeTab={activeTab}>
          <ScreenControl
            tournamentId={tournamentId}
            state={tournamentState}
            matches={matches}
            athletes={athletes}
            routines={routines}
            overviews={overviews}
            brackets={brackets}
            onUpdated={(updated) => dispatch({ type: "stateUpdated", state: updated })}
          />
        </TabPanel>

        <TabPanel name="brackets" activeTab={activeTab}>
          <BracketView
            brackets={brackets}
            matches={matches}
            progresses={progresses}
            athletes={athletes}
            routines={routines}
            onRefresh={() => void refreshMatches()}
            onCreateBracket={handleCreateBracket}
            onMatchCreated={(match) => dispatch({ type: "matchCreated", match })}
            onStartMatch={handleStartMatch}
            onDeleteMatch={canDeleteMatch ? handleDeleteMatch : undefined}
          />
        </TabPanel>

        <TabPanel name="settings" activeTab={activeTab}>
          {canManage ? (
            <>
              <h2 className="text-lg font-medium mb-6" style={{ color: "var(--foreground)" }}>Tournament Settings</h2>
              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-medium mb-2" style={{ color: "var(--foreground)" }}>Status</label>
                  <div className="flex flex-wrap gap-2">
                    {(["DRAFT", "READY", "LIVE", "FINISHED"] as const).map((status) => (
                      <button
                        key={status}
                        onClick={() => handleStatusChange(status)}
                        disabled={tournament.status === status}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${tournament.status === status ? "bg-accent text-accent-foreground" : "bg-muted text-muted-foreground hover:bg-muted/80"}`}
                      >
                        {statusStyles[status].label}
                      </button>
                    ))}
                  </div>
                </div>

                {isAdmin && (
                  <div className="rounded-xl p-5" style={{ background: "var(--danger-08)", border: "1px solid var(--danger-25)" }}>
                    <div className="flex items-center gap-2 mb-4">
                      <TriangleAlert className="w-4 h-4" style={{ color: "var(--danger)" }} />
                      <span className="text-xs font-semibold uppercase tracking-widest" style={{ color: "var(--danger)" }}>
                        Danger Zone
                      </span>
                    </div>

                    <div className="rounded-lg p-4 flex flex-wrap items-center justify-between gap-4" style={{ background: "var(--danger-04)", border: "1px solid var(--danger-15)" }}>
                      <div className="space-y-1 min-w-0">
                        <p className="text-sm font-medium" style={{ color: "var(--foreground)" }}>Delete tournament</p>
                        <p className="text-xs" style={{ color: "var(--muted-foreground)" }}>
                          Permanently removes this tournament with all of its brackets, matches and results. This cannot be undone.
                        </p>
                      </div>
                      <button
                        onClick={() => setConfirmDeleteOpen(true)}
                        className="px-4 h-9 rounded-lg text-sm font-medium transition-colors shrink-0 hover:opacity-90"
                        style={{ background: "var(--danger)", color: "var(--danger-foreground)" }}
                      >
                        Delete
                      </button>
                    </div>

                    {deleteError && (
                      <p className="mt-3 text-xs" style={{ color: "var(--danger)" }}>
                        {deleteError}
                      </p>
                    )}
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="rounded-lg p-6 text-center" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
              <Settings className="w-12 h-12 mx-auto mb-4" style={{ color: "var(--muted-foreground)" }} />
              <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>You don't have permission to modify tournament settings.</p>
            </div>
          )}
</TabPanel>
      </div>

      <ConfirmDialog
        open={confirmDeleteOpen}
        onClose={() => {
          if (!deletingTournament) {
            setConfirmDeleteOpen(false);
            setDeleteError(null);
          }
        }}
        title="Delete tournament"
        message={`Permanently delete "${tournament.name}" with all of its brackets, matches and results? This cannot be undone.`}
        confirmLabel={deletingTournament ? <Loader2 className="w-4 h-4 animate-spin" /> : "Delete"}
        onConfirm={handleDeleteTournament}
        icon={<TriangleAlert className="w-10 h-10" style={{ color: "var(--danger)" }} />}
        destructive
      />
    </div>
  );
}
