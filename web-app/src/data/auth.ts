export type UserRole = "ADMIN" | "HOST" | "JUDGE";

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

/** Only admins reach the management dashboard. */
export const ADMIN_ROLES: readonly UserRole[] = ["ADMIN"];

/** Admins and tournament hosts reach the management dashboard. */
export const DASHBOARD_ROLES: readonly UserRole[] = ["ADMIN", "HOST"];

/** Admins, hosts and judges may drive a match from the judge flow. */
export const JUDGE_ROLES: readonly UserRole[] = ["ADMIN", "HOST", "JUDGE"];

/**
 * Where a user belongs after signing in. Judges never work on the dashboard:
 * they always land on the judge flow (the mobile/PWA part of the app).
 */
export function homeRouteFor(role: UserRole | undefined | null): string {
  return role === "JUDGE" ? "/judge" : "/dashboard";
}
