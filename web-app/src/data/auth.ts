export type UserRole = "JUDGE" | "ADMIN";

export interface User {
  id: number;
  username: string;
  role: UserRole;
  createdAt: string;
}

export interface LoginInput {
  username: string;
  password: string;
}

export interface LoginOutput {
  token: string;
}
