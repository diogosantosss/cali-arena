import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api/client";
import type { Routine } from "@/features/routines/types";
import { tournamentsService } from "../services/tournaments.service";
import type { ScreenRoutine } from "../types";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Eye, EyeOff, Trash2, Plus, ArrowUp, ArrowDown } from "lucide-react";

interface ScreenRoutinesPanelProps {
  tournamentId: number;
  routines: Routine[];
}

export function ScreenRoutinesPanel({ tournamentId, routines }: ScreenRoutinesPanelProps) {
  const [screenRoutines, setScreenRoutines] = useState<ScreenRoutine[]>([]);
  const [error, setError] = useState<string | null>(null);

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
    try {
      const created = await tournamentsService.createScreenRoutine(tournamentId, {
        routineId,
        displayOrder: currentMax + 1,
        label: label || undefined,
      });
      setScreenRoutines((current) => [...current, created]);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to add routine");
    }
  }

  async function handleToggleVisibility(sr: ScreenRoutine) {
    try {
      const updated = await tournamentsService.updateScreenRoutineVisibility(
        sr.tournamentId,
        sr.id,
        !sr.isVisible
      );
      setScreenRoutines((current) => current.map((r) => (r.id === updated.id ? updated : r)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to update visibility");
    }
  }

  async function handleMove(sr: ScreenRoutine, direction: "up" | "down") {
    const sorted = [...screenRoutines].sort((a, b) => a.displayOrder - b.displayOrder);
    const idx = sorted.findIndex((r) => r.id === sr.id);
    const swapIdx = direction === "up" ? idx - 1 : idx + 1;
    if (swapIdx < 0 || swapIdx >= sorted.length) return;
    const swap = sorted[swapIdx];
    try {
      const [updatedA, updatedB] = await Promise.all([
        tournamentsService.updateScreenRoutineDisplayOrder(sr.tournamentId, sr.id, swap.displayOrder),
        tournamentsService.updateScreenRoutineDisplayOrder(swap.tournamentId, swap.id, sr.displayOrder),
      ]);
      setScreenRoutines((current) =>
        current.map((r) => (r.id === updatedA.id ? updatedA : r.id === updatedB.id ? updatedB : r))
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to reorder");
    }
  }

  async function handleDelete(sr: ScreenRoutine) {
    try {
      await tournamentsService.deleteScreenRoutine(sr.tournamentId, sr.id);
      setScreenRoutines((current) => current.filter((r) => r.id !== sr.id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete routine");
    }
  }

  const sortedScreenRoutines = [...screenRoutines].sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <div className="rounded-lg" style={{ background: "#0f0f11", border: "1px solid #252528" }}>
      <div className="flex items-center justify-between px-5 py-3">
        <div className="space-y-0.5">
          <p className="text-[10px] uppercase tracking-widest" style={{ color: "#6b6560" }}>Screen routines</p>
          <p className="text-sm font-medium" style={{ color: "#f0ede8" }}>
            Shown when screen is set to Routines
          </p>
        </div>
        <span
          className="text-[11px] px-2.5 py-1 rounded-full tabular-nums"
          style={{ background: "rgba(232,160,32,0.12)", color: "#e8a020" }}
        >
          {sortedScreenRoutines.filter((r) => r.isVisible).length}/{sortedScreenRoutines.length} visible
        </span>
      </div>

      {error && (
        <p className="mx-5 mb-3 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded px-3 py-2">
          {error}
        </p>
      )}

      <div style={{ borderTop: "1px solid #252528" }}>
        {sortedScreenRoutines.length === 0 ? (
          <p className="px-5 py-8 text-center text-sm" style={{ color: "#6b6560" }}>
            No routines added yet.
          </p>
        ) : (
          sortedScreenRoutines.map((sr, idx) => {
            const routineName = routines.find((r) => r.id === sr.routineId)?.name ?? `Routine #${sr.routineId}`;
            return (
              <div
                key={sr.id}
                className={`flex items-center gap-3 px-5 py-2.5 ${sr.isVisible ? "" : "opacity-50"}`}
                style={{ borderTop: "1px solid #19191c" }}
              >
                <div className="flex flex-col gap-0.5 shrink-0">
                  <button
                    onClick={() => void handleMove(sr, "up")}
                    disabled={idx === 0}
                    className="p-0.5 rounded transition-colors disabled:opacity-20 disabled:cursor-not-allowed"
                    style={{ color: "#6b6560" }}
                    onMouseEnter={(e) => (e.currentTarget.style.color = "#f0ede8")}
                    onMouseLeave={(e) => (e.currentTarget.style.color = "#6b6560")}
                  >
                    <ArrowUp className="w-3 h-3" />
                  </button>
                  <button
                    onClick={() => void handleMove(sr, "down")}
                    disabled={idx === sortedScreenRoutines.length - 1}
                    className="p-0.5 rounded transition-colors disabled:opacity-20 disabled:cursor-not-allowed"
                    style={{ color: "#6b6560" }}
                    onMouseEnter={(e) => (e.currentTarget.style.color = "#f0ede8")}
                    onMouseLeave={(e) => (e.currentTarget.style.color = "#6b6560")}
                  >
                    <ArrowDown className="w-3 h-3" />
                  </button>
                </div>

                <div
                  className="w-6 h-6 rounded-md flex items-center justify-center text-xs font-bold shrink-0"
                  style={{ background: "#1e1e22", color: "#6b6560" }}
                >
                  {idx + 1}
                </div>

                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate" style={{ color: "#f0ede8" }}>{routineName}</p>
                  {sr.label && (
                    <p className="text-xs truncate" style={{ color: "#6b6560" }}>{sr.label}</p>
                  )}
                </div>

                <button
                  onClick={() => void handleToggleVisibility(sr)}
                  className="p-1.5 rounded-md shrink-0 transition-colors"
                  style={{ color: "#6b6560" }}
                  title={sr.isVisible ? "Hide" : "Show"}
                  onMouseEnter={(e) => { e.currentTarget.style.color = "#e8a020"; e.currentTarget.style.background = "#1e1e22"; }}
                  onMouseLeave={(e) => { e.currentTarget.style.color = "#6b6560"; e.currentTarget.style.background = "transparent"; }}
                >
                  {sr.isVisible ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
                </button>

                <button
                  onClick={() => void handleDelete(sr)}
                  className="p-1.5 rounded-md shrink-0 transition-colors"
                  style={{ color: "#6b6560" }}
                  title="Delete"
                  onMouseEnter={(e) => { e.currentTarget.style.color = "#f16a6a"; e.currentTarget.style.background = "rgba(241,106,106,0.1)"; }}
                  onMouseLeave={(e) => { e.currentTarget.style.color = "#6b6560"; e.currentTarget.style.background = "transparent"; }}
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            );
          })
        )}

        <div className="px-5 py-3" style={{ borderTop: "1px solid #252528" }}>
          <AddScreenRoutinePanel routines={routines} onAdd={(id, label) => void handleAdd(id, label)} />
        </div>
      </div>
    </div>
  );
}

function AddScreenRoutinePanel({
  routines,
  onAdd,
}: {
  routines: Routine[];
  onAdd: (routineId: number, label: string) => void;
}) {
  const [routineId, setRoutineId] = useState<number | null>(null);
  const [label, setLabel] = useState("");

  return (
    <div className="flex items-center gap-2">
      <Select value={routineId ? String(routineId) : ""} onValueChange={(v) => setRoutineId(Number(v))}>
        <SelectTrigger className="h-8 text-xs flex-1 min-w-0 border-[#252528] focus:ring-[#e8a020]/40" style={{ background: "#17171a", color: "#a09a92" }}>
          <SelectValue placeholder="Add a routine…" />
        </SelectTrigger>
        <SelectContent style={{ background: "#17171a", border: "1px solid #252528" }}>
          {routines.map((r) => (
            <SelectItem key={r.id} value={String(r.id)} className="text-xs" style={{ color: "#a09a92" }}>{r.name}</SelectItem>
          ))}
        </SelectContent>
      </Select>

      <input
        className="h-8 rounded-md border border-[#252528] bg-transparent px-2.5 text-xs w-28 placeholder:text-[#3a3a3d] focus:outline-none focus:ring-1 focus:ring-[#e8a020]/40 shrink-0"
        style={{ color: "#f0ede8" }}
        placeholder="Label"
        value={label}
        onChange={(e) => setLabel(e.target.value)}
      />

      <button
        disabled={!routineId}
        className="flex items-center gap-1 px-3 h-8 rounded text-xs font-medium transition-opacity disabled:opacity-40 shrink-0"
        style={{ background: "#e8a020", color: "#0f0f11" }}
        onClick={() => routineId && onAdd(routineId, label)}
      >
        <Plus className="w-3.5 h-3.5" />
        Add
      </button>
    </div>
  );
}
