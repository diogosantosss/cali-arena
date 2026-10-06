import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { homeRouteFor } from "@/data/auth";
import type { UserRole } from "@/data/auth";
import { useAuth } from "@/hooks/use-auth";

interface RoleGateProps {
  roles: readonly UserRole[];
  children: ReactNode;
}

/**
 * Renders its children only for the listed roles and sends everybody else to
 * the page that belongs to their role, so a judge who types a dashboard URL
 * ends up on the judge flow instead of a wall of 403s.
 */
export function RoleGate({ roles, children }: RoleGateProps) {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/" replace />;
  }

  if (!roles.includes(user.role)) {
    return <Navigate to={homeRouteFor(user.role)} replace />;
  }

  return <>{children}</>;
}
