import { DASHBOARD_ROLES, JUDGE_ROLES } from "@/data/auth";
import type { UserRole } from "@/data/auth";
import { useAuth } from "@/hooks/use-auth";

export interface Permissions {
  role: UserRole | undefined;
  isAdmin: boolean;
  isHost: boolean;
  isJudge: boolean;
  hasDashboard: boolean;
  /** Admins and hosts may drive matches from the judge flow. */
  hasJudgeFlow: boolean;
  /** Creating tournaments is reserved to admins. */
  canCreateTournament: boolean;
  /** Global data (users, athletes, clubs, routines) is managed by admins only. */
  canManageGlobalData: boolean;
  /** Deleting a match is reserved to admins. */
  canDeleteMatch: boolean;
}

/**
 * Mirrors the role gates of the backend (@RequiresRole) so the UI hides what
 * the API would refuse anyway. Tournament scoped actions (brackets, matches,
 * screen, staff) stay available to hosts, whose real authority comes from
 * `tournaments.host_id` and is checked server side.
 */
export function usePermissions(): Permissions {
  const { user } = useAuth();
  const role = user?.role;

  const isAdmin = role === "ADMIN";

  return {
    role,
    isAdmin,
    isHost: role === "HOST",
    isJudge: role === "JUDGE",
    hasDashboard: role != null && DASHBOARD_ROLES.includes(role),
    hasJudgeFlow: role != null && JUDGE_ROLES.includes(role),
    canCreateTournament: isAdmin,
    canManageGlobalData: isAdmin,
    canDeleteMatch: isAdmin,
  };
}
