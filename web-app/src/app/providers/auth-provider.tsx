import { useEffect, useState, type ReactNode } from "react";
import { getStoredToken } from "@/lib/api/client";
import { authService } from "@/features/auth/services/auth.service";
import type { User } from "@/features/auth/types";
import { AuthContext } from "@/features/auth/hooks/use-auth";

/**
 * Returns true while on a protected route. Used to decide whether a failed
 * session check should hard-reload back to the login page.
 */
function onProtectedRoute(): boolean {
  return window.location.pathname !== "/";
}

interface AuthStatus {
  status: "loading" | "authenticated" | "anonymous";
  user: User | null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getStoredToken());
  const [auth, setAuth] = useState<AuthStatus>(() =>
    getStoredToken() ? { status: "loading", user: null } : { status: "anonymous", user: null }
  );

  useEffect(() => {
    if (!token) return;

    let cancelled = false;
    authService
      .getMe()
      .then((user) => {
        if (!cancelled) setAuth({ status: "authenticated", user });
      })
      .catch(() => {
        // Session check failed: the token is invalid/expired or the backend is
        // unreachable. Drop the token and go back to the login page so the user
        // is never left stuck on a dashboard page.
        if (cancelled) return;
        localStorage.removeItem("token");
        setToken(null);
        setAuth({ status: "anonymous", user: null });
        if (onProtectedRoute()) {
          window.location.replace("/");
        }
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  function login(newToken: string) {
    localStorage.setItem("token", newToken);
    setToken(newToken);
    setAuth({ status: "loading", user: null });
  }

  function logout() {
    void authService.logout();
    localStorage.removeItem("token");
    setToken(null);
    setAuth({ status: "anonymous", user: null });
  }

  const value = {
    user: auth.user,
    token,
    login,
    logout,
    isLoading: auth.status === "loading",
    isAuthenticated: auth.status === "authenticated",
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
