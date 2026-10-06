import { Navigate, Outlet, useLocation } from "react-router-dom";
import { JUDGE_ROLES, homeRouteFor } from "@/data/auth";
import { useAuth } from "@/hooks/use-auth";

/**
 * Guards the judge flow. Admins, hosts and judges pass; the judge pages are
 * standalone (no dashboard sidebar), like /screen. Anybody else is sent to the
 * page that belongs to their role.
 */
export function JudgeRoute() {
  const { isAuthenticated, isLoading, user } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <div className="min-h-screen" style={{ background: "var(--background)" }} />;
  }

  if (!isAuthenticated) {
    // Send the user to the login page, remembering where they wanted to go so
    // they land back on /judge after signing in instead of the admin dashboard.
    return (
      <Navigate
        to="/"
        replace
        state={{ from: `${location.pathname}${location.search}` }}
      />
    );
  }

  if (user && !JUDGE_ROLES.includes(user.role)) {
    return <Navigate to={homeRouteFor(user.role)} replace />;
  }

  return <Outlet />;
}
