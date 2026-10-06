import { Navigate, Outlet } from "react-router-dom";
import { homeRouteFor } from "@/data/auth";
import type { UserRole } from "@/data/auth";
import { useAuth } from "@/hooks/use-auth";

interface ProtectedRouteProps {
  /** When given, only these roles may enter; the others go to their own home. */
  roles?: readonly UserRole[];
}

/**
 * Wraps authenticated routes. While the session check (getMe) is running it
 * shows a blank placeholder so the page does not flash; once it resolves, an
 * authenticated user is rendered and an anonymous one is sent to the login
 * page. This guarantees a user is never left stuck on a dashboard page when
 * the backend goes down or the token becomes invalid.
 */
export function ProtectedRoute({ roles }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, user } = useAuth();

  if (isLoading) {
    return <div className="min-h-screen" style={{ background: "var(--background)" }} />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  if (roles && user && !roles.includes(user.role)) {
    return <Navigate to={homeRouteFor(user.role)} replace />;
  }

  return <Outlet />;
}
