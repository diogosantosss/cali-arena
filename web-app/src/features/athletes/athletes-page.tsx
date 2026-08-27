import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/lib/api/client";
import { useCollection } from "@/hooks/use-collection";
import { PageHeader } from "@/components/shared/page-header";
import { ToggleButton } from "@/components/shared/toggle-button";
import { CollapsibleFormPanel } from "@/components/shared/collapsible-form-panel";
import { FormField, TextField } from "@/components/shared/form-fields";
import { DarkSelect } from "@/components/shared/dark-select";
import { ListToolbar } from "@/components/shared/list-toolbar";
import { ManagementList } from "@/components/shared/management-list";
import { ListRow } from "@/components/shared/list-row";
import { Badge } from "@/components/shared/badge";
import { formatDate } from "@/lib/format";
import { athletesService } from "./services/athletes.service";
import type { CreateAthleteInput } from "./types";
import type { Gender } from "@/types/gender";
import { clubsService } from "@/features/clubs/services/clubs.service";
import type { Club } from "@/features/clubs/types";
import { UserPlus, MapPin, CalendarDays } from "lucide-react";

const genderStyles: Record<Gender, { label: string; color: string; bg: string }> = {
  MALE: { label: "Male", color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
  FEMALE: { label: "Female", color: "#ec6d9f", bg: "rgba(236,109,159,0.12)" },
};

const initialForm: CreateAthleteInput = {
  name: "",
  gender: "MALE",
  clubId: 0,
};

export function AthletesPage() {
  const loadAthletes = useCallback(() => athletesService.getAthletes(), []);
  const { items, loading, error, reload } = useCollection(loadAthletes, "Failed to load athletes");

  const [clubs, setClubs] = useState<Club[]>([]);
  const [genderFilter, setGenderFilter] = useState<Gender | "ALL">("ALL");
  const [clubFilter, setClubFilter] = useState<number | "ALL">("ALL");

  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState<CreateAthleteInput>(initialForm);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [search, setSearch] = useState("");

  useEffect(() => {
    // clubs are only needed to populate the form select and the club filter
    let cancelled = false;
    clubsService
      .getClubs()
      .then((loaded) => {
        if (!cancelled) setClubs(loaded);
      })
      .catch(() => {
        // silently fail — clubs just won't show in the selects
      });
    return () => {
      cancelled = true;
    };
  }, []);

  function setField<K extends keyof CreateAthleteInput>(field: K, value: CreateAthleteInput[K]) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.clubId) return;
    setSaving(true);
    setSaveError(null);
    try {
      await athletesService.createAthlete(form);
      setForm(initialForm);
      setFormOpen(false);
      await reload();
    } catch (err) {
      setSaveError(err instanceof ApiError ? err.message : "Failed to create athlete");
    } finally {
      setSaving(false);
    }
  }

  const query = search.trim().toLowerCase();
  const filteredAthletes = items
    .filter((athlete) => genderFilter === "ALL" || athlete.gender === genderFilter)
    .filter((athlete) => clubFilter === "ALL" || athlete.clubId === clubFilter)
    .filter((athlete) => !query || athlete.name.toLowerCase().includes(query));

  const getClubName = (clubId: number) =>
    clubs.find((club) => club.id === clubId)?.shortName ?? `#${clubId}`;

  return (
    <div className="max-w-5xl mx-auto space-y-10">
      <PageHeader
        title="Athletes"
        action={
          <ToggleButton
            open={formOpen}
            onClick={() => setFormOpen(!formOpen)}
            icon={UserPlus}
            label="New athlete"
          />
        }
      />

      <CollapsibleFormPanel
        open={formOpen}
        label="New athlete"
        error={saveError}
        saving={saving}
        disabled={!form.clubId}
        onSubmit={handleSubmit}
        onCancel={() => setFormOpen(false)}
      >
        <div className="grid grid-cols-2 gap-4">
          <div className="col-span-2">
            <TextField
              label="Name"
              value={form.name}
              onChange={(value) => setField("name", value)}
              placeholder="Athlete name"
              required
            />
          </div>

          <FormField label="Gender">
            <DarkSelect
              value={form.gender}
              onValueChange={(value) => setField("gender", value as Gender)}
              width="w-full"
              options={[
                { value: "MALE", label: "Male" },
                { value: "FEMALE", label: "Female" },
              ]}
            />
          </FormField>

          <FormField label="Club">
            <DarkSelect
              value={form.clubId ? String(form.clubId) : ""}
              onValueChange={(value) => setField("clubId", Number(value))}
              placeholder="Select club"
              width="w-full"
              options={clubs.map((club) => ({ value: String(club.id), label: club.name }))}
            />
          </FormField>
        </div>
      </CollapsibleFormPanel>

      <div className="space-y-4">
        <ListToolbar
          search={search}
          onSearchChange={setSearch}
          placeholder="Search athletes…"
          count={filteredAthletes.length}
          singular="athlete"
          refreshing={loading}
          onRefresh={() => void reload()}
          filters={
            <>
              <DarkSelect
                variant="toolbar"
                width="w-32"
                value={genderFilter}
                onValueChange={(value) => setGenderFilter(value as Gender | "ALL")}
                options={[
                  { value: "ALL", label: "All genders" },
                  { value: "MALE", label: "Male" },
                  { value: "FEMALE", label: "Female" },
                ]}
              />
              <DarkSelect
                variant="toolbar"
                width="w-36"
                value={clubFilter === "ALL" ? "ALL" : String(clubFilter)}
                onValueChange={(value) => setClubFilter(value === "ALL" ? "ALL" : Number(value))}
                options={[
                  { value: "ALL", label: "All clubs" },
                  ...clubs.map((club) => ({ value: String(club.id), label: club.shortName })),
                ]}
              />
            </>
          }
        />

        <ManagementList
          loading={loading}
          error={error}
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
                      <MapPin className="w-3 h-3" />
                      {getClubName(athlete.clubId)}
                    </span>
                    <span className="flex items-center gap-1">
                      <CalendarDays className="w-3 h-3" />
                      {formatDate(athlete.createdAt)}
                    </span>
                  </>
                }
                badge={<Badge label={g.label} color={g.color} bg={g.bg} />}
              />
            );
          }}
        />
      </div>
    </div>
  );
}
