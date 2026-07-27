import { useReducer } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import type { LoginInput } from "@/types";
import { api, ApiError } from "@/api";
import { useNavigate } from "react-router-dom";

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
  const navigate = useNavigate();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    dispatch({ type: "submit" });
    try {
      const { token } = await api.login(state.form);
      localStorage.setItem("token", token);
      dispatch({ type: "success" });
      navigate("/dashboard")
    } catch (err) {
      if (err instanceof ApiError) {
        dispatch({ type: "error", message: err.message });
      } else {
        dispatch({ type: "error", message: "An error occurred during login" });
      }
    }
  }

  return (
    <div className="relative min-h-screen bg-[#0a0a0f] flex items-center justify-center overflow-hidden">
      <div className="pointer-events-none absolute -top-24 -left-24 w-[500px] h-[500px] rounded-full bg-[radial-gradient(circle,rgba(99,57,219,0.25)_0%,transparent_70%)]" />
      <div className="pointer-events-none absolute -bottom-20 -right-20 w-[420px] h-[420px] rounded-full bg-[radial-gradient(circle,rgba(168,85,247,0.18)_0%,transparent_70%)]" />

      <Card className="relative z-10 w-[380px] border-white/10 bg-white/[0.04] backdrop-blur-xl shadow-none">
        <CardHeader className="pb-2">
          <div className="flex items-center gap-3 mb-6">
            <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-[#6339db] to-[#a855f7] flex items-center justify-center font-bold text-lg text-white">
              C
            </div>
            <div>
              <p className="text-base font-semibold text-white leading-tight">Cali Arena</p>
              <p className="text-[11px] text-white/30 uppercase tracking-widest">
                Competition Platform
              </p>
            </div>
          </div>
          <h1 className="text-2xl font-semibold text-white">Welcome back</h1>
          <p className="text-sm text-white/40 mt-1">Sign in to your account</p>
        </CardHeader>

        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4 mt-2">
            <div className="space-y-1.5">
              <Label className="text-xs text-white/50 uppercase tracking-wider">
                Username
              </Label>
              <Input
                value={state.form.username}
                onChange={(e) => dispatch({ type: "setField", field: "username", value: e.target.value })}
                placeholder="your_username"
                autoComplete="username"
                required
                className="bg-white/[0.06] border-white/10 text-white placeholder:text-white/20 focus-visible:border-purple-500/60 focus-visible:ring-0"
              />
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs text-white/50 uppercase tracking-wider">
                Password
              </Label>
              <Input
                type="password"
                value={state.form.password}
                onChange={(e) => dispatch({ type: "setField", field: "password", value: e.target.value })}
                placeholder="••••••••"
                autoComplete="current-password"
                required
                className="bg-white/[0.06] border-white/10 text-white placeholder:text-white/20 focus-visible:border-purple-500/60 focus-visible:ring-0"
              />
            </div>

            {state.error && (
              <p className="text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-md px-3 py-2">
                {state.error}
              </p>
            )}

            <Button
              type="submit"
              disabled={state.loading}
              className="w-full bg-gradient-to-r from-[#6339db] to-[#a855f7] hover:opacity-90 border-0 text-white font-medium mt-1"
            >
              {state.loading ? "Signing in…" : "Sign in"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}