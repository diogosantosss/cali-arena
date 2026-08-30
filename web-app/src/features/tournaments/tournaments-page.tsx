import { useCallback, useReducer } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError } from "@/lib/api/client";
import { useCollection } from "@/hooks/use-collection";
import { PageHeader } from "@/components/shared/page-header";
import { ToggleButton } from "@/components/shared/toggle-button";
import { CollapsibleFormPanel } from "@/components/shared/collapsible-form-panel";
import { TextField, DateField } from "@/components/shared/form-fields";
import { DarkSelect } from "@/components/shared/dark-select";
import { ListToolbar } from "@/components/shared/list-toolbar";
import { ManagementList } from "@/components/shared/management-list";
import { ListRow } from "@/components/shared/list-row";
import { Badge } from "@/components/shared/badge";
import { formatDate } from "@/lib/format";
import { tournamentsService } from "./services/tournaments.service";
import type { CreateTournamentInput, TournamentStatus } from "./types";
import { MapPin, CalendarDays, ArrowRight, Plus } from "lucide-react";

const statusStyles: Record<TournamentStatus, { label: string; color: string; bg: string }> = {
  DRAFT:    { label: "Draft",    color: "var(--muted-foreground)", bg: "rgba(107,101,96,0.12)" },
  READY:    { label: "Ready",    color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  LIVE:     { label: "Live",     color: "var(--accent)", bg: "rgba(232,160,32,0.12)" },
  FINISHED: { label: "Finished", color: "#4a4a4e", bg: "rgba(74,74,78,0.12)" },
};

const initialForm: CreateTournamentInput = {
  name: "",
  location: null,
  startDate: null,
  endDate: null,
};

interface TournamentsUiState {
  formOpen: boolean;
  form: CreateTournamentInput;
  saving: boolean;
  saveError: string | null;
  statusFilter: TournamentStatus | "ALL";
}

type Action =
  | { type: "toggleForm" }
  | { type: "closeForm" }
  | { type: "setFormField"; field: keyof CreateTournamentInput; value: string | null }
  | { type: "setStatusFilter"; filter: TournamentStatus | "ALL" }
  | { type: "saveStart" }
  | { type: "saveSuccess" }
  | { type: "saveError"; message: string };

const initialUiState: TournamentsUiState = {
  formOpen: false,
  form: initialForm,
  saving: false,
  saveError: null,
  statusFilter: "ALL",
};

function reducer(state: TournamentsUiState, action: Action): TournamentsUiState {
  switch (action.type) {
    case "toggleForm":
      return { ...state, formOpen: !state.formOpen, saveError: null };
    case "closeForm":
      return { ...state, formOpen: false, saveError: null };
    case "setFormField":
      return {
        ...state,
        form: { ...state.form, [action.field]: action.value } as CreateTournamentInput,
        saveError: null,
      };
    case "setStatusFilter":
      return { ...state, statusFilter: action.filter };
    case "saveStart":
      return { ...state, saving: true, saveError: null };
    case "saveSuccess":
      return { ...state, saving: false, saveError: null, formOpen: false, form: initialForm };
    case "saveError":
      return { ...state, saving: false, saveError: action.message };
  }
}

export function TournamentsPage() {
  const loadTournaments = useCallback(() => tournamentsService.getTournaments(), []);
  const { items, loading, error, reload } = useCollection(loadTournaments, "Failed to load");

  const navigate = useNavigate();
  const [ui, dispatch] = useReducer(reducer, initialUiState);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "saveStart" });
    try {
      await tournamentsService.createTournament(ui.form);
      dispatch({ type: "saveSuccess" });
      await reload();
    } catch (err) {
      dispatch({
        type: "saveError",
        message: err instanceof ApiError ? err.message : "Failed to create",
      });
    }
  }

  const filtered = items.filter(
    (tournament) => ui.statusFilter === "ALL" || tournament.status === ui.statusFilter
  );

  return (
    <div className="max-w-5xl mx-auto space-y-10">
      <PageHeader
        title="Tournaments"
        action={
          <ToggleButton
            open={ui.formOpen}
            onClick={() => dispatch({ type: "toggleForm" })}
            icon={Plus}
            label="New tournament"
          />
        }
      />

      <CollapsibleFormPanel
        open={ui.formOpen}
        label="New tournament"
        error={ui.saveError}
        saving={ui.saving}
        onSubmit={handleSubmit}
        onCancel={() => dispatch({ type: "closeForm" })}
      >
        <div className="grid grid-cols-2 gap-4">
          <div className="col-span-2">
            <TextField
              label="Name"
              value={ui.form.name}
              onChange={(value) => dispatch({ type: "setFormField", field: "name", value })}
              placeholder="e.g. Open Lisboa 2026"
              required
            />
          </div>
          <div className="col-span-2">
            <TextField
              label={<>Location <span style={{ color: "var(--faint)" }}>(optional)</span></>}
              value={ui.form.location ?? ""}
              onChange={(value) => dispatch({ type: "setFormField", field: "location", value: value || null })}
              placeholder="e.g. Lisboa, Portugal"
            />
          </div>
          <DateField
            label={<>Start date <span style={{ color: "var(--faint)" }}>(optional)</span></>}
            value={ui.form.startDate}
            onChange={(value) => dispatch({ type: "setFormField", field: "startDate", value })}
          />
          <DateField
            label={<>End date <span style={{ color: "var(--faint)" }}>(optional)</span></>}
            value={ui.form.endDate}
            onChange={(value) => dispatch({ type: "setFormField", field: "endDate", value })}
          />
        </div>
      </CollapsibleFormPanel>

      <div className="space-y-4">
        <ListToolbar
          count={filtered.length}
          singular="tournament"
          refreshing={loading}
          onRefresh={() => void reload()}
          filters={
            <DarkSelect
              variant="toolbar"
              width="w-32"
              value={ui.statusFilter}
              onValueChange={(value) => dispatch({ type: "setStatusFilter", filter: value as TournamentStatus | "ALL" })}
              options={[
                { value: "ALL", label: "All statuses" },
                { value: "DRAFT", label: "Draft" },
                { value: "READY", label: "Ready" },
                { value: "LIVE", label: "Live" },
                { value: "FINISHED", label: "Finished" },
              ]}
            />
          }
        />

        <ManagementList
          loading={loading}
          error={error}
          items={filtered}
          emptyLabel="No tournaments found"
          getKey={(tournament) => tournament.id}
          renderRow={(tournament, index) => {
            const s = statusStyles[tournament.status];
            return (
              <ListRow
                index={index}
                accentColor={s.color}
                onClick={() => navigate(`/dashboard/tournaments/${tournament.id}`)}
                title={tournament.name}
                meta={
                  <>
                    {tournament.location && (
                      <span className="flex items-center gap-1">
                        <MapPin className="w-3 h-3" />
                        {tournament.location}
                      </span>
                    )}
                    {tournament.startDate && (
                      <span className="flex items-center gap-1">
                        <CalendarDays className="w-3 h-3" />
                        {formatDate(tournament.startDate)}
                      </span>
                    )}
                  </>
                }
                badge={<Badge label={s.label} color={s.color} bg={s.bg} />}
                trailing={
                  <ArrowRight
                    className="w-4 h-4 shrink-0 transition-transform group-hover:translate-x-0.5"
                    style={{ color: "var(--faint)" }}
                  />
                }
              />
            );
          }}
        />
      </div>
    </div>
  );
}
