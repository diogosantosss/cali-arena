import { useState } from "react";
import { ApiError } from "@/lib/api/client";
import type { Athlete } from "@/features/athletes/types";
import type { Routine } from "@/features/routines/types";
import type { User } from "@/features/users/types";
import { matchesService } from "../services/matches.service";
import type { Bracket } from "@/features/tournaments/types";
import type { CreateMatchInput, Match } from "../types";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";

interface CreateMatchDialogProps {
  open: boolean;
  bracket: Bracket;
  routines: Routine[];
  judges: User[];
  athletes: Athlete[];
  onClose: () => void;
  onCreated: (match: Match) => void;
}

type MatchForm = Omit<CreateMatchInput, "bracketId">;

const initialForm: MatchForm = {
  routineId: 0,
  judgeId: 0,
  athleteRedId: 0,
  athleteBlueId: 0,
};

export function CreateMatchDialog({ open, bracket, routines, judges, athletes, onClose, onCreated }: CreateMatchDialogProps) {
  const [form, setForm] = useState<MatchForm>(initialForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function setField<K extends keyof MatchForm>(field: K, value: number) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  const isValid = form.routineId && form.judgeId && form.athleteRedId && form.athleteBlueId;

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
            <Label>Judge</Label>
            <Select
              value={form.judgeId ? String(form.judgeId) : ""}
              onValueChange={(v) => setField("judgeId", Number(v))}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Select judge" />
              </SelectTrigger>
              <SelectContent>
                {judges.map((j) => (
                  <SelectItem key={j.id} value={String(j.id)}>{j.username}</SelectItem>
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
            <Button type="submit" disabled={loading || !isValid}>
              {loading ? "Creating…" : "Create match"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
