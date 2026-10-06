import type { UserRole } from "@/data/auth";

export type { UserRole } from "@/data/auth";

export interface User {
  id: number;
  username: string;
  role: UserRole;
  createdAt: string;
}

/** Shape actually sent to POST /users (backend also exposes a separate role-update endpoint). */
export interface CreateUserInput {
  username: string;
  password: string;
  role: UserRole;
}

export interface UpdateUserRoleInput {
  userToUpdateId: number;
  role: UserRole;
}
