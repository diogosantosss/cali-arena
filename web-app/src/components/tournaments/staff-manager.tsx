import { useState, useEffect } from "react";
import { UserMinus, Gavel, Loader2, AlertCircle, Crown } from "lucide-react";
import { usePermissions } from "@/hooks/use-permissions";
import { tournamentStaffService } from "@/services/tournament-staff.service";
import type { TournamentStaff, StaffMember } from "@/services/tournament-staff.service";

interface StaffManagerProps {
  tournamentId: number;
  staff: TournamentStaff | null;
  loading: boolean;
  error: string | null;
  saving: boolean;
  onAddJudge: (userId: number) => Promise<void>;
  onRemoveJudge: (userId: number) => Promise<void>;
}

export function StaffManager({
  tournamentId,
  staff,
  loading,
  error,
  saving,
  onAddJudge,
  onRemoveJudge,
}: StaffManagerProps) {
  const { isAdmin, isHost } = usePermissions();
  const canManage = isAdmin || isHost;
  const [searchUserId, setSearchUserId] = useState("");
  const [availableJudges, setAvailableJudges] = useState<StaffMember[]>([]);
  const [judgesLoading, setJudgesLoading] = useState(true);
  const [judgesError, setJudgesError] = useState<string | null>(null);

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

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="w-5 h-5 animate-spin" style={{ color: "var(--accent)" }} />
      </div>
    );
  }

  if (error && !staff) {
    return (
      <div className="p-4 rounded-lg" style={{ background: "var(--danger-10)", border: "1px solid var(--danger)" }}>
        <div className="flex items-center gap-2">
          <AlertCircle className="w-4 h-4" style={{ color: "var(--danger)" }} />
          <span style={{ color: "var(--danger)" }}>{error}</span>
        </div>
      </div>
    );
  }

  async function handleAddJudge() {
    if (!searchUserId) return;
    setSearchUserId("");
    await onAddJudge(Number(searchUserId));
  }

  async function handleRemoveJudge(userId: number) {
    if (!confirm("Remove this judge from the tournament?")) return;
    await onRemoveJudge(userId);
  }

  if (!staff) return null;

  return (
    <div className="space-y-6">
      {/* Host */}
      <div className="rounded-lg p-4" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
        <div className="flex items-center gap-2 mb-3">
          <Crown className="w-4 h-4" style={{ color: "var(--accent)" }} />
          <h3 className="text-sm font-medium" style={{ color: "var(--foreground)" }}>Host</h3>
        </div>
        {staff.host ? (
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold"
              style={{ background: "rgba(126,184,247,0.12)", color: "#7eb8f7" }}>
              {staff.host.username.charAt(0).toUpperCase()}
            </div>
            <div>
              <p className="font-medium text-sm" style={{ color: "var(--foreground)" }}>{staff.host.username}</p>
              <p className="text-xs" style={{ color: "var(--muted-foreground)" }}>Host</p>
            </div>
          </div>
        ) : (
          <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>No host assigned</p>
        )}
      </div>

      {/* Judges */}
      <div className="rounded-lg p-4" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Gavel className="w-4 h-4" style={{ color: "var(--accent)" }} />
            <h3 className="text-sm font-medium" style={{ color: "var(--foreground)" }}>Judges ({staff.judges.length})</h3>
          </div>
          {canManage && (
            <div className="flex items-center gap-2">
              {judgesLoading ? (
                <Loader2 className="w-4 h-4 animate-spin" style={{ color: "var(--accent)" }} />
              ) : judgesError ? (
                <span className="text-xs" style={{ color: "var(--danger)" }}>{judgesError}</span>
              ) : availableJudges.length === 0 ? (
                <span className="text-xs" style={{ color: "var(--muted-foreground)" }}>No available judges</span>
              ) : (
                <select
                  value={searchUserId}
                  onChange={(e) => setSearchUserId(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleAddJudge()}
                  className="px-2 py-1.5 text-sm rounded border min-w-[180px]"
                  style={{ background: "var(--background)", borderColor: "var(--border)", color: "var(--foreground)" }}
                >
                  <option value="" disabled>
                    Select a judge…
                  </option>
                  {availableJudges.map((judge) => (
                    <option key={judge.id} value={String(judge.id)}>
                      {judge.username}
                    </option>
                  ))}
                </select>
              )}
              <button
                onClick={handleAddJudge}
                disabled={saving || !searchUserId || judgesLoading || availableJudges.length === 0}
                className="px-3 py-1.5 text-xs rounded transition-colors"
                style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
              >
                {saving ? "Adding…" : "Add Judge"}
              </button>
            </div>
          )}
        </div>

        {staff.judges.length === 0 ? (
          <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>No judges assigned</p>
        ) : (
          <ul className="space-y-2">
            {staff.judges.map((judge) => (
              <li key={judge.id} className="group flex items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold"
                    style={{ background: "rgba(126,184,247,0.12)", color: "#7eb8f7" }}>
                    {judge.username.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <p className="font-medium text-sm" style={{ color: "var(--foreground)" }}>{judge.username}</p>
                    <p className="text-xs" style={{ color: "var(--muted-foreground)" }}>Judge</p>
                  </div>
                </div>
                {canManage && (
                  <button
                    onClick={() => handleRemoveJudge(judge.id)}
                    disabled={saving}
                    className="p-1.5 rounded transition-colors opacity-0 group-hover:opacity-100"
                    style={{ color: "var(--muted-foreground)" }}
                    onMouseEnter={(e) => (e.currentTarget.style.color = "var(--danger)")}
                    onMouseLeave={(e) => (e.currentTarget.style.color = "var(--muted-foreground)")}
                  >
                    <UserMinus className="w-3.5 h-3.5" />
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
