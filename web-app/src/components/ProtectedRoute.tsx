import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/hooks/use-auth";

/**
 * Wraps authenticated routes. While the session check (getMe) is running it
 * shows a blank placeholder so the page does not flash; once it resolves, an
 * authenticated user is rendered and an anonymous one is sent to the login
 * page. This guarantees a user is never left stuck on a dashboard page when
 * the backend goes down or the token becomes invalid.
 */
export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <div className="min-h-screen" style={{ background: "#0f0f11" }} />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}