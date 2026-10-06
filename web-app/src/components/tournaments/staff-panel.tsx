import { useState, useEffect } from "react";
import { UserMinus, Loader2, AlertCircle, Plus, RefreshCw, UserPlus, UserX, Pencil, UserCog } from "lucide-react";
import { usePermissions } from "@/hooks/use-permissions";
import { tournamentStaffService } from "@/services/tournament-staff.service";
import type { TournamentStaff, StaffMember } from "@/services/tournament-staff.service";
import { ConfirmDialog } from "./confirm-dialog";

interface StaffPanelProps {
  tournamentId: number;
  staff: TournamentStaff | null;
  loading: boolean;
  error: string | null;
  onAddJudge: (userId: number) => Promise<void>;
  onRemoveJudge: (userId: number) => Promise<void>;
  onAssignHost: (userId: number) => Promise<void>;
}

function Avatar({ username, role }: { username: string; role: "HOST" | "JUDGE" }) {
  const isHost = role === "HOST";
  return (
    <div className="flex items-center gap-2">
      <div className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold"
        style={{
          background: isHost ? "rgba(251,191,36,0.15)" : "rgba(126,184,247,0.12)",
          color: isHost ? "#fbbf24" : "#7eb8f7"
        }}>
        {username.charAt(0).toUpperCase()}
      </div>
      <div>
        <p className="font-medium text-sm" style={{ color: "var(--foreground)" }}>{username}</p>
        <p className="text-xs" style={{ color: "var(--muted-foreground)" }}>
          {isHost ? "Host" : "Judge"}
        </p>
      </div>
    </div>
  );
}

export function StaffPanel({
  tournamentId,
  staff,
  loading,
  error,
  onAddJudge,
  onRemoveJudge,
  onAssignHost,
}: StaffPanelProps) {
  const { isAdmin, isHost } = usePermissions();
  const canManage = isAdmin || isHost;
  const [availableJudges, setAvailableJudges] = useState<StaffMember[]>([]);
  const [judgesLoading, setJudgesLoading] = useState(true);
  const [judgesError, setJudgesError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [addingJudgeId, setAddingJudgeId] = useState<number | null>(null);
  const [removingJudgeId, setRemovingJudgeId] = useState<number | null>(null);
  const [confirmAddJudge, setConfirmAddJudge] = useState<StaffMember | null>(null);
  const [confirmRemoveJudge, setConfirmRemoveJudge] = useState<StaffMember | null>(null);
  const [hostDropdownOpen, setHostDropdownOpen] = useState(false);
  const [availableHosts, setAvailableHosts] = useState<StaffMember[]>([]);
  const [hostsLoading, setHostsLoading] = useState(false);
  const [hostsError, setHostsError] = useState<string | null>(null);
  const [confirmHost, setConfirmHost] = useState<StaffMember | null>(null);
  const [assigningHostId, setAssigningHostId] = useState<number | null>(null);

  useEffect(() => {
    if (!staff) return;
    const currentStaff = staff;
    async function fetchJudges() {
      try {
        setJudgesLoading(true);
        setJudgesError(null);
        const allJudges = await tournamentStaffService.getAvailableJudges();
        const assignedIds = new Set(currentStaff.judges?.map((j) => j.id) ?? []);
        const unassigned = allJudges.filter((j) => !assignedIds.has(j.id));
        setAvailableJudges(unassigned);
      } catch (err) {
        setJudgesError(err instanceof Error ? err.message : "Failed to load judges");
      } finally {
        setJudgesLoading(false);
      }
    }
    fetchJudges();
  }, [staff, tournamentId]);

  async function handleAddJudge(judge: StaffMember) {
    setConfirmAddJudge(judge);
    setOpen(false);
  }

  async function handleConfirmAddJudge() {
    if (!confirmAddJudge) return;
    const id = confirmAddJudge.id;
    setAddingJudgeId(id);
    setConfirmAddJudge(null);
    try {
      await onAddJudge(id);
    } finally {
      setAddingJudgeId(null);
    }
  }

  async function handleRemoveJudge(userId: number) {
    if (!staff) return;
    const judge = staff.judges.find((j) => j.id === userId);
    if (!judge) return;
    setConfirmRemoveJudge(judge);
  }

  async function handleConfirmRemoveJudge() {
    if (!confirmRemoveJudge) return;
    const id = confirmRemoveJudge.id;
    setRemovingJudgeId(id);
    setConfirmRemoveJudge(null);
    try {
      await onRemoveJudge(id);
    } finally {
      setRemovingJudgeId(null);
    }
  }

  async function handleRefreshJudges() {
    if (!staff) return;
    setJudgesLoading(true);
    setJudgesError(null);
    try {
      const allJudges = await tournamentStaffService.getAvailableJudges();
      const assignedIds = new Set(staff?.judges?.map((j) => j.id) ?? []);
      const unassigned = allJudges.filter((j) => !assignedIds.has(j.id));
      setAvailableJudges(unassigned);
    } catch (err) {
      setJudgesError(err instanceof Error ? err.message : "Failed to load judges");
    } finally {
      setJudgesLoading(false);
    }
  }

  async function fetchHosts() {
    setHostsLoading(true);
    setHostsError(null);
    try {
      const allHosts = await tournamentStaffService.getAvailableHosts();
      const current = staff?.host?.id;
      setAvailableHosts(allHosts.filter((host) => host.id !== current));
    } catch (err) {
      setHostsError(err instanceof Error ? err.message : "Failed to load hosts");
    } finally {
      setHostsLoading(false);
    }
  }

  async function toggleHostDropdown() {
    const willOpen = !hostDropdownOpen;
    setHostDropdownOpen(willOpen);
    if (willOpen) {
      await fetchHosts();
    }
  }

  function handleSelectHost(host: StaffMember) {
    setConfirmHost(host);
    setHostDropdownOpen(false);
  }

  async function handleConfirmAssignHost() {
    if (!confirmHost) return;
    const userId = confirmHost.id;
    setAssigningHostId(userId);
    setConfirmHost(null);
    try {
      await onAssignHost(userId);
      await fetchHosts();
    } finally {
      setAssigningHostId(null);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center gap-3 py-8">
        <Loader2 className="w-5 h-5 animate-spin" style={{ color: "var(--accent)" }} />
        <span className="text-sm" style={{ color: "var(--muted-foreground)" }}>Loading staff…</span>
      </div>
    );
  }

  if (error && !staff) {
    return (
      <div className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm" style={{ background: "var(--danger-10)", color: "var(--danger)", border: "1px solid var(--danger)" }}>
        <AlertCircle className="w-4 h-4" />
        {error}
      </div>
    );
  }

  if (!staff) return null;

  return (
    <div>
      <div>
        {error ? (
          <div className="mb-4 flex items-center gap-2 px-3 py-2 rounded-lg text-sm" style={{ background: "var(--danger-10)", color: "var(--danger)", border: "1px solid var(--danger)" }}>
            <AlertCircle className="w-4 h-4" />
            {error}
          </div>
        ) : null}
        {judgesError ? (
          <div className="mb-4 flex items-center gap-2 px-3 py-2 rounded-lg text-sm" style={{ background: "var(--danger-10)", color: "var(--danger)", border: "1px solid var(--danger)" }}>
            <AlertCircle className="w-4 h-4" />
            {judgesError}
          </div>
        ) : null}
      </div>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-6 flex-wrap">
          {/* Host */}
          <div className="flex items-center gap-3">
            {staff.host ? (
              <Avatar username={staff.host.username} role="HOST" />
            ) : (
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold"
                  style={{ background: "rgba(251,191,36,0.1)", color: "#fbbf24", border: "1px dashed var(--border)" }}>
                  +
                </div>
                <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>No host assigned</p>
              </div>
            )}

            {isAdmin && (
              <div className="relative">
                <button
                  onClick={toggleHostDropdown}
                  className="p-1.5 rounded transition-colors hover:bg-accent/10"
                  style={{ color: "var(--muted-foreground)" }}
                  title={staff.host ? "Change host" : "Assign host"}
                  aria-label={staff.host ? "Change host" : "Assign host"}
                  aria-expanded={hostDropdownOpen}
                  aria-haspopup="listbox"
                >
                  <Pencil className="w-4 h-4" />
                </button>

                {hostDropdownOpen && (
                  <div className="absolute left-0 top-full mt-2 z-20 min-w-[240px] rounded-lg shadow-lg border"
                    style={{ background: "var(--card)", borderColor: "var(--border)" }}>
                    <div className="flex items-center justify-between px-4 py-2 border-b" style={{ borderColor: "var(--border)" }}>
                      <p className="text-sm font-medium" style={{ color: "var(--foreground)" }}>Available Hosts</p>
                      <button
                        onClick={fetchHosts}
                        disabled={hostsLoading}
                        className="p-1.5 rounded transition-colors disabled:opacity-50"
                        style={{ color: "var(--muted-foreground)" }}
                        title="Refresh hosts list"
                        aria-label="Refresh hosts list"
                      >
                        <RefreshCw className={`w-4 h-4 ${hostsLoading ? "animate-spin" : ""}`} />
                      </button>
                    </div>
                    {hostsLoading ? (
                      <div className="p-4 flex items-center justify-center gap-2">
                        <Loader2 className="w-4 h-4 animate-spin" style={{ color: "var(--accent)" }} />
                        <span className="text-sm" style={{ color: "var(--muted-foreground)" }}>Loading…</span>
                      </div>
                    ) : hostsError ? (
                      <div className="p-4 text-center">
                        <AlertCircle className="w-5 h-5 mx-auto mb-2" style={{ color: "var(--danger)" }} />
                        <p className="text-sm" style={{ color: "var(--danger)" }}>{hostsError}</p>
                      </div>
                    ) : availableHosts.length === 0 ? (
                      <div className="p-4 text-center">
                        <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>No available hosts</p>
                      </div>
                    ) : (
                      <ul className="max-h-60 overflow-y-auto" role="listbox">
                        {availableHosts.map((host) => (
                          <li key={host.id} role="option" className="flex items-center justify-between px-4 py-2.5 hover:bg-accent/10 transition-colors">
                            <div className="flex items-center gap-3" style={{ color: "var(--foreground)" }}>
                              <div className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold"
                                style={{ background: "rgba(251,191,36,0.12)", color: "#fbbf24" }}>
                                {host.username.charAt(0).toUpperCase()}
                              </div>
                              <span className="font-medium">{host.username}</span>
                            </div>
                            <button
                              onClick={() => handleSelectHost(host)}
                              className="px-3 py-1.5 text-xs font-medium rounded transition-colors shrink-0"
                              style={{ border: "1px solid var(--border)", color: "var(--foreground)" }}
                            >
                              Make host
                            </button>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Judges */}
          <div className="flex items-center gap-3">
            {staff.judges.length > 0 ? (
              <div className="flex -space-x-2" role="list" aria-label="Assigned judges">
                {staff.judges.slice(0, 5).map((judge) => (
                  <div key={judge.id} className="w-8 h-8 rounded-full border-2 flex items-center justify-center text-xs font-bold"
                    style={{ background: "rgba(126,184,247,0.12)", color: "#7eb8f7", borderColor: "var(--background)" }}
                    role="listitem"
                    title={judge.username}>
                    {judge.username.charAt(0).toUpperCase()}
                  </div>
                ))}
                {staff.judges.length > 5 && (
                  <div className="w-8 h-8 rounded-full border-2 flex items-center justify-center text-xs font-medium"
                    style={{ background: "var(--muted)", color: "var(--muted-foreground)", borderColor: "var(--background)" }}
                    role="listitem">
                    +{staff.judges.length - 5}
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold"
                  style={{ background: "rgba(126,184,247,0.1)", color: "#7eb8f7", border: "1px dashed var(--border)" }}>
                  +
                </div>
                <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>No judges assigned</p>
              </div>
            )}
          </div>
        </div>

        {/* Add Judge Dropdown - only for host/admin */}
        {canManage && (
          <div className="relative">
<button
          onClick={() => setOpen(!open)}
          className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors hover:bg-accent/10"
          style={{ border: "1px solid var(--border)", color: "var(--accent)", background: "transparent" }}
          aria-expanded={open}
          aria-haspopup="listbox"
        >
          <Plus className="w-4 h-4" />
          Add Judge
        </button>

            {open && (
              <div className="absolute right-0 top-full mt-2 z-20 min-w-[240px] rounded-lg shadow-lg border"
                style={{ background: "var(--card)", borderColor: "var(--border)" }}>
                <div className="flex items-center justify-between px-4 py-2 border-b" style={{ borderColor: "var(--border)" }}>
                  <p className="text-sm font-medium" style={{ color: "var(--foreground)" }}>Available Judges</p>
                  <button
                    onClick={handleRefreshJudges}
                    disabled={judgesLoading}
                    className="p-1.5 rounded transition-colors disabled:opacity-50"
                    style={{ color: "var(--muted-foreground)" }}
                    title="Refresh judges list"
                    aria-label="Refresh judges list"
                  >
                    <RefreshCw className={`w-4 h-4 ${judgesLoading ? "animate-spin" : ""}`} />
                  </button>
                </div>
                {judgesLoading ? (
                  <div className="p-4 flex items-center justify-center gap-2">
                    <Loader2 className="w-4 h-4 animate-spin" style={{ color: "var(--accent)" }} />
                    <span className="text-sm" style={{ color: "var(--muted-foreground)" }}>Loading…</span>
                  </div>
                ) : judgesError ? (
                  <div className="p-4 text-center">
                    <AlertCircle className="w-5 h-5 mx-auto mb-2" style={{ color: "var(--danger)" }} />
                    <p className="text-sm" style={{ color: "var(--danger)" }}>{judgesError}</p>
                  </div>
                ) : availableJudges.length === 0 ? (
                  <div className="p-4 text-center">
                    <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>No available judges</p>
                  </div>
                ) : (
                  <ul className="max-h-60 overflow-y-auto" role="listbox">
                    {availableJudges.map((judge) => (
                      <li key={judge.id} role="option" className="flex items-center justify-between px-4 py-2.5 hover:bg-accent/10 transition-colors">
                        <div className="flex items-center gap-3" style={{ color: "var(--foreground)" }}>
                          <div className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold"
                            style={{ background: "rgba(126,184,247,0.12)", color: "#7eb8f7" }}>
                            {judge.username.charAt(0).toUpperCase()}
                          </div>
                          <span className="font-medium">{judge.username}</span>
                        </div>
                        <button
                          onClick={() => { handleAddJudge(judge); }}
                          disabled={addingJudgeId === judge.id}
                          className="px-3 py-1.5 text-xs font-medium rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
                          style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
                        >
                          {addingJudgeId === judge.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : "Add"}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </div>
        )}
      </div>
      {/* Expanded judges list when there are judges assigned */}
      {canManage && staff.judges.length > 0 && (
        <div className="mt-4 pt-4 border-t" style={{ borderColor: "var(--border)" }}>
          <div className="flex items-center justify-between mb-3">
            <p className="text-xs uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>Assigned Judges</p>
            <button
              onClick={handleRefreshJudges}
              disabled={judgesLoading}
              className="p-1.5 rounded transition-colors disabled:opacity-50"
              style={{ color: "var(--muted-foreground)" }}
              title="Refresh judges list"
              aria-label="Refresh judges list"
            >
              <RefreshCw className={`w-4 h-4 ${judgesLoading ? "animate-spin" : ""}`} />
            </button>
          </div>
          <div className="flex flex-wrap gap-2">
            {staff.judges.map((judge) => {
              const isRemoving = removingJudgeId === judge.id;
              return (
                <div
                  key={judge.id}
                  className={`flex items-center gap-2 px-3 py-1.5 rounded-lg ${isRemoving ? "animate-exit" : ""}`}
                  style={{
                    background: "rgba(126,184,247,0.08)",
                    border: "1px solid var(--border)",
                    opacity: isRemoving ? 0 : 1,
                    transform: isRemoving ? "translateX(10px)" : "none",
                    transition: "opacity 0.2s ease, transform 0.2s ease",
                  }}
                >
                  <Avatar username={judge.username} role="JUDGE" />
                  <button
                    onClick={() => handleRemoveJudge(judge.id)}
                    disabled={removingJudgeId !== null}
                    className="p-1 rounded hover:bg-danger/10 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    style={{ color: "var(--muted-foreground)" }}
                    aria-label={`Remove ${judge.username}`}
                    title="Remove judge"
                  >
                    {removingJudgeId === judge.id ? <Loader2 className="w-4 h-4 animate-spin" style={{ color: "var(--danger)" }} /> : <UserMinus className="w-4 h-4" />}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

      <ConfirmDialog
        open={confirmAddJudge !== null}
        onClose={() => { setConfirmAddJudge(null); setOpen(false); }}
        title="Add Judge"
        message={`Add ${confirmAddJudge?.username} as a judge to this tournament?`}
        confirmLabel={addingJudgeId !== null ? <Loader2 className="w-4 h-4 animate-spin" /> : "Add Judge"}
        onConfirm={handleConfirmAddJudge}
        icon={<UserPlus className="w-10 h-10" style={{ color: "var(--accent)" }} />}
      />

      <ConfirmDialog
        open={confirmRemoveJudge !== null}
        onClose={() => { setConfirmRemoveJudge(null); setOpen(false); }}
        title="Remove Judge"
        message={`Remove ${confirmRemoveJudge?.username} from this tournament?`}
        confirmLabel={removingJudgeId !== null ? <Loader2 className="w-4 h-4 animate-spin" /> : "Remove"}
        onConfirm={handleConfirmRemoveJudge}
        icon={<UserX className="w-10 h-10" style={{ color: "var(--danger)" }} />}
      />

      <ConfirmDialog
        open={confirmHost !== null}
        onClose={() => setConfirmHost(null)}
        title="Assign Host"
        message={`Make ${confirmHost?.username} the host of this tournament? The current host will lose access.`}
        confirmLabel={assigningHostId !== null ? <Loader2 className="w-4 h-4 animate-spin" /> : "Assign Host"}
        onConfirm={handleConfirmAssignHost}
        icon={<UserCog className="w-10 h-10" style={{ color: "var(--accent)" }} />}
      />
    </div>
  );
}