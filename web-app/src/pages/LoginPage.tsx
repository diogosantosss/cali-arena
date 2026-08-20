import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { LoginInput } from "@/types";
import { api, ApiError } from "@/api";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/AuthContext";
import { useReducer } from "react";

interface State {
  form: LoginInput;
  loading: boolean;
  error: string | null;
}

type LoginAction =
  | { type: "setField"; field: keyof LoginInput; value: string }
  | { type: "submit" }
  | { type: "success" }
  | { type: "error"; message: string };

const initialState: State = {
  form: { username: "", password: "" },
  loading: false,
  error: null,
};

function loginReducer(state: State, action: LoginAction): State {
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
  const [state, dispatch] = useReducer(loginReducer, initialState);
  const { login } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "submit" });
    try {
      const response = await api.createToken(state.form);
      await login(response.token);
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
      className="min-h-screen flex"
      style={{
        background: "#0f0f11",
        backgroundImage: `
          radial-gradient(ellipse 80% 50% at 20% 0%, rgba(232,160,32,0.07) 0%, transparent 60%),
          radial-gradient(ellipse 60% 40% at 80% 100%, rgba(232,160,32,0.04) 0%, transparent 50%)
        `,
      }}
    >
      <div className="hidden lg:flex flex-col justify-between w-[45%] px-16 py-14 border-r border-[#252528]">
        <div className="flex items-center gap-2">
          <div
            className="w-6 h-6 rounded flex items-center justify-center text-[10px] font-bold"
            style={{ background: "#e8a020", color: "#0f0f11" }}
          >
            C
          </div>
          <span className="text-sm tracking-widest uppercase text-[#6b6560]" style={{ fontFamily: "Geist Variable, sans-serif" }}>
            Cali Arena
          </span>
        </div>

        <div
          className="space-y-6 animate-fade-up"
          style={{ animationDelay: "0.1s", opacity: 0 }}
        >
          <div
            className="text-5xl leading-[1.1] tracking-tight"
            style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "#f0ede8" }}
          >
            Where athletes<br />
            <span style={{ color: "#e8a020" }}>compete.</span>
          </div>
          <p className="text-sm leading-relaxed max-w-xs" style={{ color: "#6b6560" }}>
            Full competition management — brackets, live scoring, and spectator screens in one place.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="h-px flex-1" style={{ background: "#252528" }} />
          <span className="text-xs" style={{ color: "#3a3a3d" }}>CALI ARENA © 2026</span>
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
              style={{ background: "#e8a020", color: "#0f0f11" }}
            >
              C
            </div>
            <span className="text-sm tracking-widest uppercase" style={{ color: "#6b6560" }}>
              Cali Arena
            </span>
          </div>

          <div>
            <p className="text-xs tracking-widest uppercase mb-2" style={{ color: "#6b6560" }}>
              Admin access
            </p>
            <h1
              className="text-3xl leading-tight"
              style={{ fontFamily: "DM Serif Display, Georgia, serif", color: "#f0ede8" }}
            >
              Sign in
            </h1>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-1.5">
              <Label
                htmlFor="username"
                className="text-xs tracking-wider uppercase"
                style={{ color: "#6b6560" }}
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
                className="border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                style={{ background: "#17171a" }}
              />
            </div>

            <div className="space-y-1.5">
              <Label
                htmlFor="password"
                className="text-xs tracking-wider uppercase"
                style={{ color: "#6b6560" }}
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
                className="border-[#252528] text-[#f0ede8] placeholder:text-[#3a3a3d] focus-visible:ring-[#e8a020]/40 focus-visible:border-[#e8a020]/60"
                style={{ background: "#17171a" }}
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
              style={{ background: "#e8a020", color: "#0f0f11" }}
            >
              {state.loading ? "Signing in…" : "Sign in"}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
