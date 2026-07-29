import { useReducer, useEffect } from "react";
import type { CreateUserInput, User, UserRole } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Separator } from "@/components/ui/separator";
import { UserPlus, RefreshCw } from "lucide-react";
import { api, ApiError } from "@/api";

interface State {
  form: CreateUserInput & { role: UserRole };
  loading: boolean;
  error: string | null;
  success: boolean;
  users: User[];
  usersLoading: boolean;
  usersError: string | null;
  roleFilter: UserRole | "ALL";
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
  | { type: "reset" };

const initialState: State = {
  form: { username: "", password: "", role: "JUDGE" },
  loading: false,
  error: null,
  success: false,
  users: [],
  usersLoading: false,
  usersError: null,
  roleFilter: "ALL",
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "setField":
      return { ...state, form: { ...state.form, [action.field]: action.value }, success: false, error: null };
    case "submit":
      return { ...state, loading: true, error: null, success: false };
    case "success":
      return { ...state, loading: false, success: true, form: { ...initialState.form } };
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
    case "reset":
      return initialState;
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
      if (err instanceof ApiError) {
        dispatch({ type: "setUsersError", message: err.message });
      } else {
        dispatch({ type: "setUsersError", message: "Failed to load users" });
      }
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
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({ type: "error", message: "An error occurred while creating user" });
      }
    }
  }

  const filteredUsers = state.roleFilter === "ALL"
    ? state.users
    : state.users.filter((u) => u.role === state.roleFilter);

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Users</h1>
        <p className="text-sm text-muted-foreground mt-1">Create and manage judges and admins</p>
      </div>

      {/* Create form */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <UserPlus className="w-4 h-4" />
            New user
          </CardTitle>
          <CardDescription>The user will be able to login immediately after creation.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Username</Label>
                <Input
                  value={state.form.username}
                  onChange={(e) => dispatch({ type: "setField", field: "username", value: e.target.value })}
                  placeholder="username"
                  required
                />
              </div>
              <div className="space-y-1.5">
                <Label>Password</Label>
                <Input
                  type="password"
                  value={state.form.password}
                  onChange={(e) => dispatch({ type: "setField", field: "password", value: e.target.value })}
                  placeholder="••••••••"
                  required
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label>Role</Label>
              <Select
                value={state.form.role}
                onValueChange={(value) => dispatch({ type: "setField", field: "role", value })}
              >
                <SelectTrigger className="w-48">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="JUDGE">Judge</SelectItem>
                  <SelectItem value="ADMIN">Admin</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {state.error && (
              <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-md px-3 py-2">
                {state.error}
              </p>
            )}
            {state.success && (
              <p className="text-sm text-green-600 bg-green-500/10 border border-green-500/20 rounded-md px-3 py-2">
                User created successfully.
              </p>
            )}

            <div className="flex items-center gap-3 pt-1">
              <Button type="submit" disabled={state.loading}>
                {state.loading ? "Creating…" : "Create user"}
              </Button>
              <Badge variant="outline" className="capitalize">
                {state.form.role.toLowerCase()}
              </Badge>
            </div>
          </form>
        </CardContent>
      </Card>

      <Separator />

      {/* Users table */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-medium">All users</h2>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="icon"
              onClick={loadUsers}
              disabled={state.usersLoading}
            >
              <RefreshCw className={`w-4 h-4 ${state.usersLoading ? "animate-spin" : ""}`} />
            </Button>
            <Select
              value={state.roleFilter}
              onValueChange={(value) => dispatch({ type: "setRoleFilter", value: value as UserRole | "ALL" })}
            >
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All roles</SelectItem>
                <SelectItem value="JUDGE">Judge</SelectItem>
                <SelectItem value="ADMIN">Admin</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Username</TableHead>
                <TableHead>Role</TableHead>
                <TableHead>Created at</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {state.usersLoading ? (
                Array.from({ length: 4 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-4 w-32" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                  </TableRow>
                ))
              ) : state.usersError ? (
                <TableRow>
                  <TableCell colSpan={3} className="text-center text-destructive text-sm py-6">
                    {state.usersError}
                  </TableCell>
                </TableRow>
              ) : filteredUsers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} className="text-center text-muted-foreground text-sm py-6">
                    No users found.
                  </TableCell>
                </TableRow>
              ) : (
                filteredUsers.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium">{user.username}</TableCell>
                    <TableCell>
                      <Badge variant={user.role === "ADMIN" ? "default" : "secondary"} className="capitalize">
                        {user.role.toLowerCase()}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm">
                      {new Date(user.createdAt).toLocaleDateString()}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Card>
      </div>
    </div>
  );
}