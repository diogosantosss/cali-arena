import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/hooks/use-auth";

/**
 * Guards the judge flow. Only authenticated JUDGE/ADMIN users pass; the
 * judge pages are standalone (no dashboard sidebar), like /screen.
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

  if (user?.role !== "JUDGE" && user?.role !== "ADMIN") {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}