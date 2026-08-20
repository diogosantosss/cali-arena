import { useReducer, useEffect } from "react";
import type { CreateUserInput, User, UserRole } from "@/types";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { UserPlus, RefreshCw, CalendarDays } from "lucide-react";
import { api, ApiError } from "@/api";

const roleStyles: Record<UserRole, { color: string; bg: string }> = {
  ADMIN: { color: "#e8a020", bg: "rgba(232,160,32,0.12)" },
  JUDGE: { color: "#7eb8f7", bg: "rgba(126,184,247,0.12)" },
};

interface State {
  form: CreateUserInput & { role: UserRole };
  loading: boolean;
  error: string | null;
  users: User[];
  usersLoading: boolean;
  usersError: string | null;
  search: string;
  roleFilter: UserRole | "ALL";
  formOpen: boolean;
}

type Action =
  | { type: "setField"; field: keyof State["form"]; value: string }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string }
  | { type: "setRoleFilter"; value: UserRole | "ALL" }
  | { type: "setUsers"; users: User[] }
  | { type: "setUsersLoading" }
  | { type: "setUsersError"; message: string }
  | { type: "setSearch"; value: string }
  | { type: "toggleForm" };

const initialState: State = {
  form: { username: "", password: "", role: "JUDGE" },
  loading: false,
  error: null,
  users: [],
  usersLoading: false,
  usersError: null,
  search: "",
  roleFilter: "ALL",
  formOpen: false,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setField":
      return { ...state, form: { ...state.form, [action.field]: action.value }, error: null };
    case "submit":
      return { ...state, loading: true, error: null };
    case "success":
      return { ...state, loading: false, form: { ...initialState.form }, formOpen: false };
    case "error":
      return { ...state, loading: false, error: action.message };
    case "setRoleFilter":
      return { ...state, roleFilter: action.value };
    case "setUsers":
      return { ...state, users: action.users, usersLoading: false, usersError: null };
    case "setUsersLoading":
      return { ...state, usersLoading: true, usersError: null };
    case "setUsersError":
      return { ...state, usersLoading: false, usersError: action.message };
    case "setSearch":
      return { ...state, search: action.value };
    case "toggleForm":
      return { ...state, formOpen: !state.formOpen, error: null };
    default:
      throw new Error("Unknown action");
  }
}

export function UsersPage() {
  const [state, dispatch] = useReducer(reducer, initialState);

  async function loadUsers() {
    dispatch({ type: "setUsersLoading" });
    try {
      const users = await api.getUsers();
      dispatch({ type: "setUsers", users });
    } catch (err) {
      dispatch({
        type: "setUsersError",
        message: err instanceof ApiError ? err.message : "Failed to load users",
      });
    }
  }

  useEffect(() => {
    loadUsers();
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "submit" });
    try {
      await api.createUser(state.form);
      dispatch({ type: "success" });
      loadUsers();
    } catch (err) {
      dispatch({
        type: "error",
        message: err instanceof ApiError ? err.message : "Failed to create user",
      });
    }
  }

  const query = (state.search ?? "").trim().toLowerCase();
  const filteredUsers = state.users.filter(
    (u) =>
      (state.roleFilter === "ALL" || u.role === state.roleFilter) &&
      (!query || u.username.toLowerCase().includes(query))
  );

  const selectTrigger = "h-8 text-xs border-[#252528] focus:ring-[#e8a020]/40";
  const selectContentStyle = { background: "#17171a", border: "1px solid #252528" };

  return (
    <div className="max-w-5xl mx-auto space-y-10">
      <div className="flex items-end justify-between">
        <div>
          <p className="text-xs tracking-widest uppercase mb-1.5" style={{ color: "#6b6560" }}>
            Management
          </p>
          <h1
            className="text-4xl leading-tight"
            style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "#f0ede8" }}
          >
            Users
          </h1>
        </div>

        <button
          onClick={() => dispatch({ type: "toggleForm" })}
          className="flex items-center gap-2 px-4 py-2 rounded text-sm font-medium transition-colors"
          style={{
            background: state.formOpen ? "rgba(232,160,32,0.1)" : "#e8a020",
            color: state.formOpen ? "#e8a020" : "#0f0f11",
            border: state.formOpen ? "1px solid rgba(232,160,32,0.3)" : "none",
          }}
        >
          <UserPlus className="w-3.5 h-3.5" />
          New user
        </button>
      </div>

      {state.formOpen && (
        <div className="rounded-lg p-6 space-y-5" style={{ background: "#17171a", border: "1px solid #252528" }}>
          <p className="text-xs tracking-widest uppercase" style={{ color: "#6b6560" }}>
            New user
          </p>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Username</Label>
                <Input
                  value={state.form.username}
                  onChange={(e) => dispatch({ type: "setField", field: "username", value: e.target.value })}
                  placeholder="username"
                  required
                  className="border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                  style={{ background: "#0f0f11" }}
                />
              </div>

              <div className="space-y-1.5">
                <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Password</Label>
                <Input
                  type="password"
                  value={state.form.password}
                  onChange={(e) => dispatch({ type: "setField", field: "password", value: e.target.value })}
                  placeholder="••••••••"
                  required
                  className="border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                  style={{ background: "#0f0f11" }}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs uppercase tracking-wider" style={{ color: "#6b6560" }}>Role</Label>
              <Select
                value={state.form.role}
                onValueChange={(value) => dispatch({ type: "setField", field: "role", value })}
              >
                <SelectTrigger className={`${selectTrigger} w-48`} style={{ background: "#0f0f11", color: "#a09a92" }}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent style={selectContentStyle}>
                  <SelectItem value="JUDGE" className="text-xs" style={{ color: "#a09a92" }}>Judge</SelectItem>
                  <SelectItem value="ADMIN" className="text-xs" style={{ color: "#a09a92" }}>Admin</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {state.error && (
              <p
                className="text-sm rounded px-3 py-2"
                style={{ background: "rgba(241,106,106,0.1)", color: "#f16a6a", border: "1px solid rgba(241,106,106,0.25)" }}
              >
                {state.error}
              </p>
            )}

            <div className="flex items-center gap-3 pt-1">
              <button
                type="submit"
                disabled={state.loading}
                className="px-5 py-2 rounded text-sm font-medium transition-opacity disabled:opacity-50"
                style={{ background: "#e8a020", color: "#0f0f11" }}
              >
                {state.loading ? "Creating…" : "Create"}
              </button>
              <button
                type="button"
                onClick={() => dispatch({ type: "toggleForm" })}
                className="px-4 py-2 rounded text-sm transition-colors"
                style={{ color: "#6b6560" }}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <Input
            value={state.search}
            onChange={(e) => dispatch({ type: "setSearch", value: e.target.value })}
            placeholder="Search users…"
            className="h-8 w-64 border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
            style={{ background: "#17171a" }}
          />
          <div className="flex items-center gap-2">
            <span className="text-sm tabular-nums" style={{ color: "#6b6560" }}>
              {filteredUsers.length} user{filteredUsers.length !== 1 ? "s" : ""}
            </span>
            <button
              onClick={loadUsers}
              disabled={state.usersLoading}
              className="p-1.5 rounded transition-colors"
              style={{ color: "#6b6560" }}
            >
              <RefreshCw className={`w-3.5 h-3.5 ${state.usersLoading ? "animate-spin" : ""}`} />
            </button>
            <Select
              value={state.roleFilter}
              onValueChange={(value) => dispatch({ type: "setRoleFilter", value: value as UserRole | "ALL" })}
            >
              <SelectTrigger className={`${selectTrigger} w-32`} style={{ background: "#17171a", color: "#a09a92" }}>
                <SelectValue />
              </SelectTrigger>
              <SelectContent style={selectContentStyle}>
                <SelectItem value="ALL" className="text-xs" style={{ color: "#a09a92" }}>All roles</SelectItem>
                <SelectItem value="JUDGE" className="text-xs" style={{ color: "#a09a92" }}>Judge</SelectItem>
                <SelectItem value="ADMIN" className="text-xs" style={{ color: "#a09a92" }}>Admin</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        {state.usersLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-16 rounded-lg" style={{ background: "#17171a" }}>
                <Skeleton className="h-full w-full rounded-lg opacity-40" />
              </div>
            ))}
          </div>
        ) : state.usersError ? (
          <p className="text-sm py-8 text-center" style={{ color: "#f16a6a" }}>
            {state.usersError}
          </p>
        ) : filteredUsers.length === 0 ? (
          <div className="py-20 text-center">
            <p className="text-xs tracking-widest uppercase" style={{ color: "#3a3a3d" }}>
              No users found
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            {filteredUsers.map((user, i) => {
              const r = roleStyles[user.role];
              return (
                <div
                  key={user.id}
                  className="group flex items-center gap-5 px-5 py-4 rounded-lg transition-colors animate-fade-up"
                  style={{
                    background: "#17171a",
                    border: "1px solid #252528",
                    animationDelay: `${i * 0.03}s`,
                    opacity: 0,
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.borderColor = "#363639")}
                  onMouseLeave={(e) => (e.currentTarget.style.borderColor = "#252528")}
                >
                  <div className="w-1 self-stretch rounded-full shrink-0" style={{ background: r.color, opacity: 0.5 }} />

                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate" style={{ color: "#f0ede8" }}>
                      {user.username}
                    </p>
                    <div className="flex items-center gap-3 mt-0.5">
                      <span className="flex items-center gap-1 text-xs" style={{ color: "#6b6560" }}>
                        <CalendarDays className="w-3 h-3" />
                        {new Date(user.createdAt).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}
                      </span>
                    </div>
                  </div>

                  <span className="text-xs px-2.5 py-1 rounded-full shrink-0 capitalize" style={{ background: r.bg, color: r.color }}>
                    {user.role.toLowerCase()}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}