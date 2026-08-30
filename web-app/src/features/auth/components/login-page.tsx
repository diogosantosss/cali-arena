import { useReducer } from "react";
import { useNavigate } from "react-router-dom";
import { Sun, Moon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api/client";
import { authService } from "../services/auth.service";
import type { LoginInput } from "../types";
import { useAuth } from "../hooks/use-auth";
import { useTheme } from "@/app/hooks/use-theme";

interface LoginState {
  form: LoginInput;
  loading: boolean;
  error: string | null;
}

type LoginAction =
  | { type: "setField"; field: keyof LoginInput; value: string }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string };

const initialLoginState: LoginState = {
  form: { username: "", password: "" },
  loading: false,
  error: null,
};

function loginReducer(state: LoginState, action: LoginAction): LoginState {
  switch (action.type) {
    case "setField":
      return { ...state, form: { ...state.form, [action.field]: action.value } };
    case "submit":
      return { ...state, loading: true, error: null };
    case "success":
      return { ...state, loading: false };
    case "error":
      return { ...state, loading: false, error: action.message };
    default:
      throw new Error("Unknown action");
  }
}

export function LoginPage() {
  const [state, dispatch] = useReducer(loginReducer, initialLoginState);
  const { login } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "submit" });
    try {
      const response = await authService.createToken(state.form);
      login(response.token);
      dispatch({ type: "success" });
      navigate("/dashboard");
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({ type: "error", message: "An error occurred during login" });
      }
    }
  }

  return (
    <div
      className="relative min-h-screen flex"
      style={{
        background: "var(--background)",
        backgroundImage: `
          radial-gradient(ellipse 80% 50% at 20% 0%, rgba(232,160,32,0.07) 0%, transparent 60%),
          radial-gradient(ellipse 60% 40% at 80% 100%, rgba(232,160,32,0.04) 0%, transparent 50%)
        `,
      }}
    >
      <button
        onClick={toggleTheme}
        aria-label="Toggle theme"
        title={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
        className="absolute top-6 right-7 w-8 h-8 flex items-center justify-center rounded transition-colors text-foreground opacity-80 hover:text-accent hover:opacity-100"
      >
        {theme === "dark" ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
      </button>
      <div className="hidden lg:flex flex-col justify-between w-[45%] px-16 py-14 border-r border-border">
        <div className="flex items-center gap-2">
          <div
            className="w-6 h-6 rounded flex items-center justify-center text-[10px] font-bold"
            style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
          >
            C
          </div>
          <span className="text-sm tracking-widest uppercase text-muted-foreground" style={{ fontFamily: "Geist Variable, sans-serif" }}>
            Cali Arena
          </span>
        </div>

        <div
          className="space-y-6 animate-fade-up"
          style={{ animationDelay: "0.1s", opacity: 0 }}
        >
          <div
            className="text-5xl leading-[1.1] tracking-tight"
            style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "var(--foreground)" }}
          >
            Where athletes<br />
            <span style={{ color: "var(--accent)" }}>compete.</span>
          </div>
          <p className="text-sm leading-relaxed max-w-xs" style={{ color: "var(--muted-foreground)" }}>
            Full competition management — brackets, live scoring, and spectator screens in one place.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="h-px flex-1" style={{ background: "var(--border)" }} />
          <span className="text-xs" style={{ color: "var(--faint)" }}>CALI ARENA © 2026</span>
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center px-6">
        <div
          className="w-full max-w-[340px] space-y-8 animate-fade-up"
          style={{ animationDelay: "0.2s", opacity: 0 }}
        >
          <div className="lg:hidden flex items-center gap-2 mb-4">
            <div
              className="w-6 h-6 rounded flex items-center justify-center text-[10px] font-bold"
              style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
            >
              C
            </div>
            <span className="text-sm tracking-widest uppercase" style={{ color: "var(--muted-foreground)" }}>
              Cali Arena
            </span>
          </div>

          <div>
            <p className="text-xs tracking-widest uppercase mb-2" style={{ color: "var(--muted-foreground)" }}>
              Admin access
            </p>
            <h1
              className="text-3xl leading-tight"
              style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "var(--foreground)" }}
            >
              Sign in
            </h1>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-1.5">
              <Label
                htmlFor="username"
                className="text-xs tracking-wider uppercase"
                style={{ color: "var(--muted-foreground)" }}
              >
                Username
              </Label>
              <Input
                id="username"
                value={state.form.username}
                onChange={(e) => dispatch({ type: "setField", field: "username", value: e.target.value })}
                placeholder="your_username"
                autoComplete="username"
                required
                className="border-border text-foreground placeholder:text-faint focus-visible:ring-accent/40 focus-visible:border-accent/60"
                style={{ background: "var(--card)" }}
              />
            </div>

            <div className="space-y-1.5">
              <Label
                htmlFor="password"
                className="text-xs tracking-wider uppercase"
                style={{ color: "var(--muted-foreground)" }}
              >
                Password
              </Label>
              <Input
                id="password"
                type="password"
                value={state.form.password}
                onChange={(e) => dispatch({ type: "setField", field: "password", value: e.target.value })}
                placeholder="••••••••"
                autoComplete="current-password"
                required
                className="border-border text-foreground placeholder:text-faint focus-visible:ring-accent/40 focus-visible:border-accent/60"
                style={{ background: "var(--card)" }}
              />
            </div>

            {state.error && (
              <p className="text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded px-3 py-2">
                {state.error}
              </p>
            )}

            <Button
              type="submit"
              disabled={state.loading}
              className="w-full font-medium tracking-wide text-sm"
              style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
            >
              {state.loading ? "Signing in…" : "Sign in"}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
