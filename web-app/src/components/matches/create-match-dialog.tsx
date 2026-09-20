import { useState } from "react";
import { ApiError } from "@/api/client";
import type { Athlete } from "@/data/athletes";
import type { Routine } from "@/data/routines";
import { matchesService } from "@/services/matches.service";
import type { Bracket } from "@/data/tournaments";
import type { CreateMatchInput, Match } from "@/data/matches";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";

interface CreateMatchDialogProps {
  open: boolean;
  bracket: Bracket;
  routines: Routine[];
  athletes: Athlete[];
  onClose: () => void;
  onCreated: (match: Match) => void;
}

type MatchForm = Omit<CreateMatchInput, "bracketId">;

const NO_SHOW = "0";

const initialForm: MatchForm = {
  routineId: 0,
  athleteRedId: null,
  athleteBlueId: null,
};

export function CreateMatchDialog({ open, bracket, routines, athletes, onClose, onCreated }: CreateMatchDialogProps) {
  const [form, setForm] = useState<MatchForm>(initialForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function setField<K extends keyof MatchForm>(field: K, value: number | null) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  const isValid = form.routineId !== 0 && (form.athleteRedId != null || form.athleteBlueId != null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isValid) return;
    setLoading(true);
    setError(null);
    try {
      const match = await matchesService.createMatch({ ...form, bracketId: bracket.id });
      setForm(initialForm);
      onCreated(match);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create match");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>New match</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4 py-2">
          <div className="space-y-1.5">
            <Label>Routine</Label>
            <Select
              value={form.routineId ? String(form.routineId) : ""}
              onValueChange={(v) => setField("routineId", Number(v))}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Select routine" />
              </SelectTrigger>
              <SelectContent>
                {routines.map((r) => (
                  <SelectItem key={r.id} value={String(r.id)}>{r.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-red-500" />
              Red athlete
            </Label>
            <Select
              value={form.athleteRedId == null ? NO_SHOW : String(form.athleteRedId)}
              onValueChange={(v) => setField("athleteRedId", v === NO_SHOW ? null : Number(v))}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Select athlete" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NO_SHOW}>No show</SelectItem>
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
              value={form.athleteBlueId == null ? NO_SHOW : String(form.athleteBlueId)}
              onValueChange={(v) => setField("athleteBlueId", v === NO_SHOW ? null : Number(v))}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Select athlete" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NO_SHOW}>No show</SelectItem>
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
            <Button type="submit" disabled={loading || !isValid}>
              {loading ? "Creating…" : "Create match"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}