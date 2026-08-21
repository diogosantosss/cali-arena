import { useState } from "react";
import { ApiError } from "@/lib/api/client";
import type { Athlete } from "@/features/athletes/types";
import { matchesService } from "../services/matches.service";
import type { AssignAthletesInput, Match } from "../types";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";

interface AssignAthletesDialogProps {
  open: boolean;
  match: Match | null;
  athletes: Athlete[];
  onClose: () => void;
  onAssigned: (match: Match) => void;
}

type AthleteForm = AssignAthletesInput;

const initialForm: AthleteForm = {
  athleteRedId: 0,
  athleteBlueId: 0,
};

export function AssignAthletesDialog({ open, match, athletes, onClose, onAssigned }: AssignAthletesDialogProps) {
  const [form, setForm] = useState<AthleteForm>(initialForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function setField<K extends keyof AthleteForm>(field: K, value: number) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!match || !form.athleteRedId || !form.athleteBlueId) return;
    setLoading(true);
    setError(null);
    try {
      // TODO: backend does not expose PUT /matches/{id}/athletes yet —
      // this call fails at runtime until the endpoint ships.
      const updated = await matchesService.assignAthletes(match.id, form);
      setForm(initialForm);
      onAssigned(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to assign athletes");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Assign athletes — Match #{match?.id}</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4 py-2">
          <div className="space-y-1.5">
            <Label className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-red-500" />
              Red athlete
            </Label>
            <Select
              value={form.athleteRedId ? String(form.athleteRedId) : ""}
              onValueChange={(v) => setField("athleteRedId", Number(v))}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Select athlete" />
              </SelectTrigger>
              <SelectContent>
                {athletes
                  .filter((a) => a.id !== form.athleteBlueId)
                  .map((a) => (
                    <SelectItem key={a.id} value={String(a.id)}>{a.name}</SelectItem>
                  ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-blue-500" />
              Blue athlete
            </Label>
            <Select
              value={form.athleteBlueId ? String(form.athleteBlueId) : ""}
              onValueChange={(v) => setField("athleteBlueId", Number(v))}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Select athlete" />
              </SelectTrigger>
              <SelectContent>
                {athletes
                  .filter((a) => a.id !== form.athleteRedId)
                  .map((a) => (
                    <SelectItem key={a.id} value={String(a.id)}>{a.name}</SelectItem>
                  ))}
              </SelectContent>
            </Select>
          </div>

          {error && (
            <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2">
              {error}
            </p>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button
              type="submit"
              disabled={loading || !form.athleteRedId || !form.athleteBlueId}
            >
              {loading ? "Assigning…" : "Assign athletes"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
