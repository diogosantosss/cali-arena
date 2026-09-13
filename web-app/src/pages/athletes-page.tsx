import { useCallback, useEffect, useReducer } from "react";
import { ApiError } from "@/api/client";
import { PageHeader } from "@/components/shared/page-header";
import { ToggleButton } from "@/components/shared/toggle-button";
import { CollapsibleFormPanel } from "@/components/shared/collapsible-form-panel";
import { FormField, TextField } from "@/components/shared/form-fields";
import { FormError } from "@/components/shared/form-error";
import { DarkSelect } from "@/components/shared/dark-select";
import { ListToolbar } from "@/components/shared/list-toolbar";
import { ManagementList } from "@/components/shared/management-list";
import { ListRow } from "@/components/shared/list-row";
import { Badge } from "@/components/shared/badge";
import { formatDate } from "@/utils/format";
import { athletesService } from "@/services/athletes.service";
import type { Athlete, CreateAthleteInput } from "@/data/athletes";
import type { Gender } from "@/data/gender";
import { clubsService } from "@/services/clubs.service";
import type { Club } from "@/data/clubs";
import { UserPlus, Building2, CalendarDays, Pencil } from "lucide-react";

const genderStyles: Record<Gender, { label: string; color: string; bg: string }> = {
  MALE: { label: "Male", color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  FEMALE: { label: "Female", color: "#ec6d9f", bg: "rgba(236,109,159,0.12)" },
};

interface AthletesState {
  items: Athlete[];
  clubs: Club[];
  loading: boolean;
  error: string | null;
  search: string;
  genderFilter: Gender | "ALL";
  clubFilter: number | "ALL" | "NONE";
  formOpen: boolean;
  editingId: number | null;
  name: string;
  gender: Gender;
  clubId: number | null;
  saving: boolean;
  formError: string | null;
}

const initialAthletesState: AthletesState = {
  items: [],
  clubs: [],
  loading: false,
  error: null,
  search: "",
  genderFilter: "ALL",
  clubFilter: "ALL",
  formOpen: false,
  editingId: null,
  name: "",
  gender: "MALE",
  clubId: null,
  saving: false,
  formError: null,
};

type AthletesAction =
  | { type: "load-start" }
  | { type: "load-success"; items: Athlete[] }
  | { type: "load-error"; message: string }
  | { type: "clubs-loaded"; clubs: Club[] }
  | { type: "set-search"; value: string }
  | { type: "set-gender-filter"; value: Gender | "ALL" }
  | { type: "set-club-filter"; value: number | "ALL" | "NONE" }
  | { type: "open-create" }
  | { type: "open-edit"; athlete: Athlete }
  | { type: "close-form" }
  | { type: "set-name"; value: string }
  | { type: "set-gender"; value: Gender }
  | { type: "set-club-id"; value: number | null }
  | { type: "save-start" }
  | { type: "save-success"; updated?: Athlete }
  | { type: "save-error"; message: string };

function athletesReducer(state: AthletesState, action: AthletesAction): AthletesState {
  switch (action.type) {
    case "load-start":
      return { ...state, loading: true, error: null };
    case "load-success":
      return { ...state, items: action.items, loading: false, error: null };
    case "load-error":
      return { ...state, loading: false, error: action.message };
    case "clubs-loaded":
      return { ...state, clubs: action.clubs };
    case "set-search":
      return { ...state, search: action.value };
    case "set-gender-filter":
      return { ...state, genderFilter: action.value };
    case "set-club-filter":
      return { ...state, clubFilter: action.value };
    case "open-create":
      return {
        ...state,
        formOpen: true,
        editingId: null,
        name: "",
        gender: "MALE",
        clubId: null,
        saving: false,
        formError: null,
      };
    case "open-edit":
      return {
        ...state,
        formOpen: true,
        editingId: action.athlete.id,
        name: action.athlete.name,
        gender: action.athlete.gender,
        clubId: action.athlete.clubId,
        saving: false,
        formError: null,
      };
    case "close-form":
      return { ...state, formOpen: false, editingId: null, saving: false, formError: null };
    case "set-name":
      return { ...state, name: action.value };
    case "set-gender":
      return { ...state, gender: action.value };
    case "set-club-id":
      return { ...state, clubId: action.value };
    case "save-start":
      return { ...state, saving: true, formError: null };
    case "save-success": {
      const updated = action.updated;
      return {
        ...state,
        formOpen: false,
        editingId: null,
        saving: false,
        formError: null,
        items: updated ? state.items.map((a) => (a.id === updated.id ? updated : a)) : state.items,
      };
    }
    case "save-error":
      return { ...state, saving: false, formError: action.message };
  }
}

export function AthletesPage() {
  const [state, dispatch] = useReducer(athletesReducer, initialAthletesState);

  const loadAthletes = useCallback(async () => {
    dispatch({ type: "load-start" });
    try {
      const items = await athletesService.getAthletes();
      dispatch({ type: "load-success", items });
    } catch (err) {
      dispatch({
        type: "load-error",
        message: err instanceof ApiError ? err.message : "Failed to load athletes",
      });
    }
  }, []);

  useEffect(() => {
    void loadAthletes();
  }, [loadAthletes]);

  useEffect(() => {
    let cancelled = false;
    clubsService
      .getClubs()
      .then((clubs) => {
        if (!cancelled) dispatch({ type: "clubs-loaded", clubs });
      })
      .catch(() => {
        // silently fail — clubs just won't show in the selects
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const { editingId, name, gender, clubId } = state;
    dispatch({ type: "save-start" });
    try {
      const input: CreateAthleteInput = { name, gender, clubId };
      if (editingId != null) {
        const updated = await athletesService.updateAthlete(editingId, input);
        dispatch({ type: "save-success", updated });
      } else {
        await athletesService.createAthlete(input);
        await loadAthletes();
        dispatch({ type: "save-success" });
      }
    } catch (err) {
      dispatch({
        type: "save-error",
        message: err instanceof ApiError ? err.message : "Failed to save athlete",
      });
    }
  }

  const query = state.search.trim().toLowerCase();
  const filteredAthletes = state.items
    .filter((athlete) => state.genderFilter === "ALL" || athlete.gender === state.genderFilter)
    .filter((athlete) =>
      state.clubFilter === "ALL"
        ? true
        : state.clubFilter === "NONE"
          ? athlete.clubId == null
          : athlete.clubId === state.clubFilter,
    )
    .filter((athlete) => !query || athlete.name.toLowerCase().includes(query));

  const getClubName = (clubId: number | null) =>
    clubId == null
      ? "No club"
      : state.clubs.find((club) => club.id === clubId)?.shortName ?? `#${clubId}`;

  const editing = state.editingId != null;

  return (
    <div className="max-w-5xl mx-auto space-y-10">
      <PageHeader
        title="Athletes"
        action={
          <ToggleButton
            open={state.formOpen && state.editingId == null}
            onClick={() => dispatch({ type: state.formOpen ? "close-form" : "open-create" })}
            icon={UserPlus}
            label="New athlete"
          />
        }
      />

      <CollapsibleFormPanel
        open={state.formOpen && state.editingId == null}
        label={editing ? "Edit athlete" : "New athlete"}
        error={state.formError}
        saving={state.saving}
        submitLabel={editing ? "Save" : "Create"}
        savingLabel={editing ? "Saving…" : "Creating…"}
        onSubmit={handleSubmit}
        onCancel={() => dispatch({ type: "close-form" })}
      >
        <div className="grid grid-cols-2 gap-4">
          <div className="col-span-2">
            <TextField
              label="Name"
              value={state.name}
              onChange={(value) => dispatch({ type: "set-name", value })}
              placeholder="Athlete name"
              required
            />
          </div>

          {!editing && (
            <FormField label="Gender">
              <DarkSelect
                value={state.gender}
                onValueChange={(value) => dispatch({ type: "set-gender", value: value as Gender })}
                width="w-full"
                options={[
                  { value: "MALE", label: "Male" },
                  { value: "FEMALE", label: "Female" },
                ]}
              />
            </FormField>
          )}

          <FormField label="Club">
            <DarkSelect
              value={state.clubId == null ? "none" : String(state.clubId)}
              onValueChange={(value) =>
                dispatch({ type: "set-club-id", value: value === "none" ? null : Number(value) })
              }
              placeholder="Select club"
              width="w-full"
              options={[
                { value: "none", label: "No club" },
                ...state.clubs.map((club) => ({ value: String(club.id), label: club.name })),
              ]}
            />
          </FormField>
        </div>
      </CollapsibleFormPanel>

      <div className="space-y-4">
        <ListToolbar
          search={state.search}
          onSearchChange={(value) => dispatch({ type: "set-search", value })}
          placeholder="Search athletes…"
          count={filteredAthletes.length}
          singular="athlete"
          refreshing={state.loading}
          onRefresh={() => void loadAthletes()}
          filters={
            <>
              <DarkSelect
                variant="toolbar"
                width="w-32"
                value={state.genderFilter}
                onValueChange={(value) =>
                  dispatch({ type: "set-gender-filter", value: value as Gender | "ALL" })
                }
                options={[
                  { value: "ALL", label: "All genders" },
                  { value: "MALE", label: "Male" },
                  { value: "FEMALE", label: "Female" },
                ]}
              />
              <DarkSelect
                variant="toolbar"
                width="w-36"
                value={
                  state.clubFilter === "ALL"
                    ? "ALL"
                    : state.clubFilter === "NONE"
                      ? "NONE"
                      : String(state.clubFilter)
                }
                onValueChange={(value) => {
                  if (value === "ALL" || value === "NONE") dispatch({ type: "set-club-filter", value });
                  else dispatch({ type: "set-club-filter", value: Number(value) });
                }}
                options={[
                  { value: "ALL", label: "All clubs" },
                  { value: "NONE", label: "No club" },
                  ...state.clubs.map((club) => ({ value: String(club.id), label: club.shortName })),
                ]}
              />
            </>
          }
        />

        <ManagementList
          loading={state.loading}
          error={state.error}
          items={filteredAthletes}
          emptyLabel="No athletes found"
          getKey={(athlete) => athlete.id}
          renderRow={(athlete, index) => {
            const g = genderStyles[athlete.gender];
            return (
              <ListRow
                index={index}
                accentColor={g.color}
                title={athlete.name}
                meta={
                  <>
                    <span className="flex items-center gap-1">
                      <Building2 className="w-3 h-3" />
                      {getClubName(athlete.clubId)}
                    </span>
                    <span className="flex items-center gap-1">
                      <CalendarDays className="w-3 h-3" />
                      {formatDate(athlete.createdAt)}
                    </span>
                  </>
                }
                badge={<Badge label={g.label} color={g.color} bg={g.bg} />}
                trailing={
                  <button
                    onClick={() => dispatch({ type: "open-edit", athlete })}
                    title={state.editingId === athlete.id ? "Close edit" : "Edit athlete"}
                    className="p-1.5 rounded transition-colors"
                    style={{
                      color:
                        state.editingId === athlete.id ? "var(--accent)" : "var(--muted-foreground)",
                      background:
                        state.editingId === athlete.id ? "rgba(232,160,32,0.1)" : "transparent",
                    }}
                  >
                    <Pencil className="w-3.5 h-3.5" />
                  </button>
                }
                expanded={
                  state.editingId === athlete.id ? (
                    <form onSubmit={handleSubmit} className="space-y-4 px-5 py-4 animate-fade-up">
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <TextField
                          label="Name"
                          value={state.name}
                          onChange={(value) => dispatch({ type: "set-name", value })}
                          placeholder="Athlete name"
                          required
                        />
                        <FormField label="Club">
                          <DarkSelect
                            value={state.clubId == null ? "none" : String(state.clubId)}
                            onValueChange={(value) =>
                              dispatch({
                                type: "set-club-id",
                                value: value === "none" ? null : Number(value),
                              })
                            }
                            placeholder="Select club"
                            width="w-full"
                            options={[
                              { value: "none", label: "No club" },
                              ...state.clubs.map((club) => ({
                                value: String(club.id),
                                label: club.name,
                              })),
                            ]}
                          />
                        </FormField>
                      </div>

                      <FormError message={state.formError} />

                      <div className="flex items-center gap-2">
                        <button
                          type="submit"
                          disabled={state.saving}
                          className="px-5 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
                          style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
                        >
                          {state.saving ? "Saving…" : "Save"}
                        </button>
                        <button
                          type="button"
                          onClick={() => dispatch({ type: "close-form" })}
                          className="px-4 py-2 rounded text-sm transition-colors"
                          style={{ color: "var(--muted-foreground)" }}
                        >
                          Cancel
                        </button>
                      </div>
                    </form>
                  ) : undefined
                }
              />
            );
          }}
        />
      </div>
    </div>
  );
}