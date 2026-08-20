import { useReducer } from "react";
import { api, ApiError } from "@/api";
import type { Athlete, AssignAthletesInput, Match } from "@/types";
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

interface Props {
  open: boolean;
  match: Match | null;
  athletes: Athlete[];
  onClose: () => void;
  onAssigned: (match: Match) => void;
}

interface State {
  form: AssignAthletesInput;
  loading: boolean;
  error: string | null;
}

type Action =
  | { type: "setField"; field: keyof AssignAthletesInput; value: number }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string };

const initialForm: AssignAthletesInput = {
  athleteRedId: 0,
  athleteBlueId: 0,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setField":
      return { ...state, form: { ...state.form, [action.field]: action.value } };
    case "submit":
      return { ...state, loading: true, error: null };
    case "success":
      return { loading: false, error: null, form: initialForm };
    case "error":
      return { ...state, loading: false, error: action.message };
    default:
      throw new Error("Unknown action");
  }
}

export function AssignAthletesDialog({ open, match, athletes, onClose, onAssigned }: Props) {
  const [state, dispatch] = useReducer(reducer, { form: initialForm, loading: false, error: null });

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!match || !state.form.athleteRedId || !state.form.athleteBlueId) return;
    dispatch({ type: "submit" });
    try {
      const updated = await api.assignAthletesToMatch(match.id, state.form);
      onAssigned(updated);
      dispatch({ type: "success" });
      onAssigned(updated);
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({ type: "error", message: "Failed to assign athletes" });
      }
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
              value={state.form.athleteRedId ? String(state.form.athleteRedId) : ""}
              onValueChange={(v) => dispatch({ type: "setField", field: "athleteRedId", value: Number(v) })}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Select athlete" />
              </SelectTrigger>
              <SelectContent>
                {athletes
                  .filter((a) => a.id !== state.form.athleteBlueId)
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
              value={state.form.athleteBlueId ? String(state.form.athleteBlueId) : ""}
              onValueChange={(v) => dispatch({ type: "setField", field: "athleteBlueId", value: Number(v) })}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Select athlete" />
              </SelectTrigger>
              <SelectContent>
                {athletes
                  .filter((a) => a.id !== state.form.athleteRedId)
                  .map((a) => (
                    <SelectItem key={a.id} value={String(a.id)}>{a.name}</SelectItem>
                  ))}
              </SelectContent>
            </Select>
          </div>

          {state.error && (
            <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2">
              {state.error}
            </p>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button
              type="submit"
              disabled={state.loading || !state.form.athleteRedId || !state.form.athleteBlueId}
            >
              {state.loading ? "Assigning…" : "Assign athletes"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}