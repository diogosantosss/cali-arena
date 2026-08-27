import { useEffect, useState, type ReactNode } from "react";
import { getStoredToken } from "@/lib/api/client";
import { authService } from "@/features/auth/services/auth.service";
import type { User } from "@/features/auth/types";
import { AuthContext } from "@/features/auth/hooks/use-auth";

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
        // token is invalid or expired — drop it
        if (cancelled) return;
        localStorage.removeItem("token");
        setToken(null);
        setAuth({ status: "anonymous", user: null });
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
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
