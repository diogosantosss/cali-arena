import { useCallback, useReducer } from "react";
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
import { usersService } from "./services/users.service";
import type { CreateUserInput, UserRole } from "./types";
import { UserPlus, CalendarDays } from "lucide-react";

const roleStyles: Record<UserRole, { color: string; bg: string }> = {
  ADMIN: { color: "#e8a020", bg: "rgba(232,160,32,0.12)" },
  JUDGE: { color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
};

const initialForm: CreateUserInput = {
  username: "",
  password: "",
  role: "JUDGE",
};

interface UsersUiState {
  formOpen: boolean;
  form: CreateUserInput;
  saving: boolean;
  saveError: string | null;
  search: string;
  roleFilter: UserRole | "ALL";
}

type Action =
  | { type: "toggleForm" }
  | { type: "closeForm" }
  | { type: "setFormField"; field: keyof CreateUserInput; value: string }
  | { type: "setSearch"; value: string }
  | { type: "setRoleFilter"; filter: UserRole | "ALL" }
  | { type: "saveStart" }
  | { type: "saveSuccess" }
  | { type: "saveError"; message: string };

const initialUiState: UsersUiState = {
  formOpen: false,
  form: initialForm,
  saving: false,
  saveError: null,
  search: "",
  roleFilter: "ALL",
};

function reducer(state: UsersUiState, action: Action): UsersUiState {
  switch (action.type) {
    case "toggleForm":
      return { ...state, formOpen: !state.formOpen, saveError: null };
    case "closeForm":
      return { ...state, formOpen: false, saveError: null };
    case "setFormField":
      return {
        ...state,
        form: { ...state.form, [action.field]: action.value } as CreateUserInput,
        saveError: null,
      };
    case "setSearch":
      return { ...state, search: action.value };
    case "setRoleFilter":
      return { ...state, roleFilter: action.filter };
    case "saveStart":
      return { ...state, saving: true, saveError: null };
    case "saveSuccess":
      return { ...state, saving: false, saveError: null, formOpen: false, form: initialForm };
    case "saveError":
      return { ...state, saving: false, saveError: action.message };
  }
}

export function UsersPage() {
  const loadUsers = useCallback(() => usersService.getUsers(), []);
  const { items, loading, error, reload } = useCollection(loadUsers, "Failed to load users");

  const [ui, dispatch] = useReducer(reducer, initialUiState);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "saveStart" });
    try {
      await usersService.createUser(ui.form);
      dispatch({ type: "saveSuccess" });
      await reload();
    } catch (err) {
      dispatch({
        type: "saveError",
        message: err instanceof ApiError ? err.message : "Failed to create user",
      });
    }
  }

  const query = ui.search.trim().toLowerCase();
  const filteredUsers = items
    .filter((user) => ui.roleFilter === "ALL" || user.role === ui.roleFilter)
    .filter((user) => !query || user.username.toLowerCase().includes(query));

  return (
    <div className="max-w-5xl mx-auto space-y-10">
      <PageHeader
        title="Users"
        action={
          <ToggleButton
            open={ui.formOpen}
            onClick={() => dispatch({ type: "toggleForm" })}
            icon={UserPlus}
            label="New user"
          />
        }
      />

      <CollapsibleFormPanel
        open={ui.formOpen}
        label="New user"
        error={ui.saveError}
        saving={ui.saving}
        onSubmit={handleSubmit}
        onCancel={() => dispatch({ type: "closeForm" })}
      >
        <div className="grid grid-cols-2 gap-4">
          <TextField
            label="Username"
            value={ui.form.username}
            onChange={(value) => dispatch({ type: "setFormField", field: "username", value })}
            placeholder="username"
            required
          />
          <TextField
            label="Password"
            type="password"
            value={ui.form.password}
            onChange={(value) => dispatch({ type: "setFormField", field: "password", value })}
            placeholder="••••••••"
            required
          />
        </div>

        <FormField label="Role">
          <DarkSelect
            width="w-48"
            value={ui.form.role}
            onValueChange={(value) => dispatch({ type: "setFormField", field: "role", value: value as UserRole })}
            options={[
              { value: "JUDGE", label: "Judge" },
              { value: "ADMIN", label: "Admin" },
            ]}
          />
        </FormField>
      </CollapsibleFormPanel>

      <div className="space-y-4">
        <ListToolbar
          search={ui.search}
          onSearchChange={(value) => dispatch({ type: "setSearch", value })}
          placeholder="Search users…"
          count={filteredUsers.length}
          singular="user"
          refreshing={loading}
          onRefresh={() => void reload()}
          filters={
            <DarkSelect
              variant="toolbar"
              width="w-32"
              value={ui.roleFilter}
              onValueChange={(value) => dispatch({ type: "setRoleFilter", filter: value as UserRole | "ALL" })}
              options={[
                { value: "ALL", label: "All roles" },
                { value: "JUDGE", label: "Judge" },
                { value: "ADMIN", label: "Admin" },
              ]}
            />
          }
        />

        <ManagementList
          loading={loading}
          error={error}
          items={filteredUsers}
          emptyLabel="No users found"
          getKey={(user) => user.id}
          renderRow={(user, index) => (
            <ListRow
              index={index}
              accentColor={roleStyles[user.role].color}
              title={user.username}
              meta={
                <span className="flex items-center gap-1">
                  <CalendarDays className="w-3 h-3" />
                  {formatDate(user.createdAt)}
                </span>
              }
              badge={
                <Badge
                  label={user.role.toLowerCase()}
                  color={roleStyles[user.role].color}
                  bg={roleStyles[user.role].bg}
                  capitalize
                />
              }
            />
          )}
        />
      </div>
    </div>
  );
}
