import { useCallback, useState } from "react";
import { ApiError } from "@/lib/api/client";
import { useCollection } from "@/hooks/use-collection";
import { PageHeader } from "@/components/shared/page-header";
import { ToggleButton } from "@/components/shared/toggle-button";
import { CollapsibleFormPanel } from "@/components/shared/collapsible-form-panel";
import { TextField } from "@/components/shared/form-fields";
import { ListToolbar } from "@/components/shared/list-toolbar";
import { ManagementList } from "@/components/shared/management-list";
import { ListRow } from "@/components/shared/list-row";
import { formatDate } from "@/lib/format";
import { clubsService } from "./services/clubs.service";
import type { CreateClubInput } from "./types";
import { Building2, CalendarDays } from "lucide-react";

const initialForm: CreateClubInput = { name: "", shortName: "" };

export function ClubsPage() {
  const loadClubs = useCallback(() => clubsService.getClubs(), []);
  const { items, loading, error, reload } = useCollection(loadClubs, "Failed to load clubs");

  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState<CreateClubInput>(initialForm);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [search, setSearch] = useState("");

  function setField<K extends keyof CreateClubInput>(field: K, value: CreateClubInput[K]) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setSaveError(null);
    try {
      await clubsService.createClub(form);
      setForm(initialForm);
      setFormOpen(false);
      await reload();
    } catch (err) {
      setSaveError(err instanceof ApiError ? err.message : "Failed to create club");
    } finally {
      setSaving(false);
    }
  }

  const query = search.trim().toLowerCase();
  const filteredClubs = items.filter(
    (club) =>
      !query ||
      club.name.toLowerCase().includes(query) ||
      club.shortName.toLowerCase().includes(query)
  );

  return (
    <div className="max-w-5xl mx-auto space-y-10">
      <PageHeader
        title="Clubs"
        action={
          <ToggleButton
            open={formOpen}
            onClick={() => setFormOpen(!formOpen)}
            icon={Building2}
            label="New club"
          />
        }
      />

      <CollapsibleFormPanel
        open={formOpen}
        label="New club"
        error={saveError}
        saving={saving}
        onSubmit={handleSubmit}
        onCancel={() => setFormOpen(false)}
      >
        <div className="grid grid-cols-2 gap-4">
          <TextField
            label="Name"
            value={form.name}
            onChange={(value) => setField("name", value)}
            placeholder="Club full name"
            required
          />
          <TextField
            label={<>Short name <span style={{ color: "#3a3a3d" }}>(max 6)</span></>}
            value={form.shortName}
            onChange={(value) => setField("shortName", value)}
            placeholder="e.g. CAL"
            maxLength={6}
            required
          />
        </div>
      </CollapsibleFormPanel>

      <div className="space-y-4">
        <ListToolbar
          search={search}
          onSearchChange={setSearch}
          placeholder="Search clubs…"
          count={filteredClubs.length}
          singular="club"
          refreshing={loading}
          onRefresh={() => void reload()}
        />

        <ManagementList
          loading={loading}
          error={error}
          items={filteredClubs}
          emptyLabel="No clubs found"
          getKey={(club) => club.id}
          renderRow={(club, index) => (
            <ListRow
              index={index}
              accentColor="#e8a020"
              title={club.name}
              meta={
                <span className="flex items-center gap-1">
                  <CalendarDays className="w-3 h-3" />
                  {formatDate(club.createdAt)}
                </span>
              }
              badge={
                <span
                  className="text-xs px-2.5 py-1 rounded-md font-mono tracking-widest shrink-0 uppercase"
                  style={{ background: "#1e1e22", color: "#a09a92", border: "1px solid #252528" }}
                >
                  {club.shortName}
                </span>
              }
            />
          )}
        />
      </div>
    </div>
  );
}
