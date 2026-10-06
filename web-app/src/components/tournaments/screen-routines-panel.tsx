import { useEffect, useState } from "react";
import { ApiError } from "@/api/client";
import type { Routine } from "@/data/routines";
import { tournamentsService } from "@/services/tournaments.service";
import type { ScreenRoutine } from "@/data/tournaments";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Eye, EyeOff, Trash2, Plus, Loader2, GripVertical } from "lucide-react";

interface ScreenRoutinesPanelProps {
  tournamentId: number;
  routines: Routine[];
}

export function ScreenRoutinesPanel({ tournamentId, routines }: ScreenRoutinesPanelProps) {
  const [screenRoutines, setScreenRoutines] = useState<ScreenRoutine[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [pendingKey, setPendingKey] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function loadScreenRoutines() {
      try {
        const loaded = await tournamentsService.getScreenRoutines(tournamentId);
        if (!cancelled) setScreenRoutines(loaded);
      } catch {
        // silently fail
      }
    }
    loadScreenRoutines();
    return () => {
      cancelled = true;
    };
  }, [tournamentId]);

  async function handleAdd(routineId: number, label: string) {
    const currentMax = screenRoutines.reduce((max, r) => Math.max(max, r.displayOrder), -1);
    setPendingKey("add");
    try {
      const created = await tournamentsService.createScreenRoutine(tournamentId, {
        routineId,
        displayOrder: currentMax + 1,
        label: label || undefined,
      });
      setScreenRoutines((current) => [...current, created]);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to add routine");
    } finally {
      setPendingKey(null);
    }
  }

  async function handleToggleVisibility(sr: ScreenRoutine) {
    setPendingKey(`visibility:${sr.id}`);
    try {
      const updated = await tournamentsService.updateScreenRoutineVisibility(
        sr.tournamentId,
        sr.id,
        !sr.isVisible
      );
      setScreenRoutines((current) => current.map((r) => (r.id === updated.id ? updated : r)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update visibility");
    } finally {
      setPendingKey(null);
    }
  }

  async function handleDelete(sr: ScreenRoutine) {
    if (!confirm("Remove this routine from the screen?")) return;
    setPendingKey(`delete:${sr.id}`);
    try {
      await tournamentsService.deleteScreenRoutine(sr.tournamentId, sr.id);
      setScreenRoutines((current) => current.filter((r) => r.id !== sr.id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete routine");
    } finally {
      setPendingKey(null);
    }
  }

  const sortedScreenRoutines = [...screenRoutines].sort((a, b) => a.displayOrder - b.displayOrder);
  const visibleCount = sortedScreenRoutines.filter((r) => r.isVisible).length;

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
            Screen Routines
          </p>
          <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>
            Shown when screen is set to <span className="font-medium" style={{ color: "var(--foreground)" }}>Routines</span>
          </p>
        </div>
        <span className="px-3 py-1 rounded-full text-xs font-medium" style={{ background: "var(--accent-12)", color: "var(--accent)" }}>
          {visibleCount} visible · {sortedScreenRoutines.length} total
        </span>
      </div>

      {error && (
        <div className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm" style={{ background: "var(--danger-10)", color: "var(--danger)", border: "1px solid var(--danger)" }}>
          {error}
        </div>
      )}

      {/* Routines List */}
      {sortedScreenRoutines.length === 0 ? (
        <div className="py-8 text-center">
          <GripVertical className="w-10 h-10 mx-auto mb-3" style={{ color: "var(--muted)" }} />
          <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>No routines added yet</p>
          <p className="text-xs mt-1" style={{ color: "var(--muted-foreground)" }}>Click "Add Routine" below to get started</p>
        </div>
      ) : (
        <div className="space-y-2" style={{ borderColor: "var(--border)" }}>
          {sortedScreenRoutines.map((sr, idx) => {
            const routineName = routines.find((r) => r.id === sr.routineId)?.name ?? `Routine #${sr.routineId}`;
            const busy = Boolean(pendingKey) && pendingKey!.includes(String(sr.id));
            return (
              <div
                key={sr.id}
                className={`flex items-center gap-3 px-4 py-3 rounded-xl ${sr.isVisible ? "" : "opacity-50"} ${busy ? "animate-pulse" : ""}`}
                style={{
                  background: sr.isVisible ? "var(--background)" : "var(--muted)",
                  border: "1px solid var(--border)",
                  transition: "all 0.15s ease",
                }}
                onMouseEnter={(e) => { e.currentTarget.style.background = sr.isVisible ? "var(--muted)" : "var(--muted)"; }}
                onMouseLeave={(e) => { e.currentTarget.style.background = sr.isVisible ? "var(--background)" : "var(--muted)"; }}
              >
                {/* Drag handle */}
                <div className="p-1.5 shrink-0" style={{ color: "var(--muted-foreground)" }}>
                  <GripVertical className="w-4 h-4" />
                </div>

                {/* Position number */}
                <div className="w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold shrink-0" style={{ background: "var(--secondary)", color: "var(--muted-foreground)" }}>
                  {busy ? <Loader2 className="w-3 h-3 animate-spin" /> : idx + 1}
                </div>

                {/* Routine info */}
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate" style={{ color: "var(--foreground)" }}>{routineName}</p>
                  {sr.label && <p className="text-xs truncate" style={{ color: "var(--muted-foreground)" }}>{sr.label}</p>}
                </div>

                {/* Visibility toggle */}
                <button
                  onClick={() => void handleToggleVisibility(sr)}
                  disabled={busy}
                  className="p-2 rounded-lg shrink-0 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                  style={{ color: sr.isVisible ? "var(--accent)" : "var(--muted-foreground)", background: sr.isVisible ? "var(--accent-10)" : "transparent" }}
                  title={sr.isVisible ? "Hide from screen" : "Show on screen"}
                  onMouseEnter={(e) => { e.currentTarget.style.background = sr.isVisible ? "var(--accent-20)" : "var(--muted)"; }}
                  onMouseLeave={(e) => { e.currentTarget.style.background = sr.isVisible ? "var(--accent-10)" : "transparent"; }}
                >
                  {busy ? <Loader2 className="w-4 h-4 animate-spin" /> : sr.isVisible ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                </button>

                {/* Delete */}
                <button
                  onClick={() => void handleDelete(sr)}
                  disabled={busy}
                  className="p-2 rounded-lg shrink-0 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                  style={{ color: "var(--muted-foreground)" }}
                  title="Remove routine"
                  onMouseEnter={(e) => { e.currentTarget.style.color = "var(--danger)"; e.currentTarget.style.background = "rgba(241,106,106,0.1)"; }}
                  onMouseLeave={(e) => { e.currentTarget.style.color = "var(--muted-foreground)"; e.currentTarget.style.background = "transparent"; }}
                >
                  {busy ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                </button>
              </div>
            );
          })}
        </div>
      )}

      {/* Add Routine */}
      <div className="pt-2 border-t" style={{ borderColor: "var(--border)" }}>
        <AddScreenRoutinePanel routines={routines} pending={pendingKey !== null} onAdd={(id, label) => void handleAdd(id, label)} />
      </div>
    </div>
  );
}

function AddScreenRoutinePanel({
  routines,
  pending,
  onAdd,
}: {
  routines: Routine[];
  pending: boolean;
  onAdd: (routineId: number, label: string) => void;
}) {
  const [routineId, setRoutineId] = useState<number | null>(null);
  const [label, setLabel] = useState("");

  return (
    <div className="flex flex-wrap items-center gap-3">
      <Select value={routineId ? String(routineId) : ""} onValueChange={(v) => setRoutineId(Number(v))}>
        <SelectTrigger className="h-9 text-sm flex-1 min-w-[200px] max-w-[300px] border-border focus:ring-accent/40" style={{ background: "var(--background)", color: "var(--foreground)" }}>
          <SelectValue placeholder="Select a routine…" />
        </SelectTrigger>
        <SelectContent position="popper" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
          {routines.map((r) => (
            <SelectItem key={r.id} value={String(r.id)} className="text-sm" style={{ color: "var(--foreground)" }}>{r.name}</SelectItem>
          ))}
        </SelectContent>
      </Select>

      <input
        className="h-9 rounded-lg border border-border bg-transparent px-3 text-sm w-40 max-w-[200px] placeholder:text-faint focus:outline-none focus:ring-2 focus:ring-accent/40 shrink-0"
        style={{ color: "var(--foreground)" }}
        placeholder="Label (optional)"
        value={label}
        onChange={(e) => setLabel(e.target.value)}
      />

      <button
        disabled={!routineId || pending}
        className="flex items-center gap-2 px-4 h-9 rounded-lg text-sm font-medium transition-all disabled:opacity-40 disabled:cursor-not-allowed shrink-0"
        style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
        onClick={() => routineId && onAdd(routineId, label)}
      >
        <Plus className="w-4 h-4" />
        Add
      </button>
    </div>
  );
}