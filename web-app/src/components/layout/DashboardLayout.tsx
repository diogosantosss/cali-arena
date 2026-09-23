import { Outlet, NavLink, useNavigate, useLocation } from "react-router-dom";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import {
  Trophy,
  Users,
  PersonStanding,
  Building2,
  LogOut,
  Dumbbell,
  Sun,
  Moon,
  PanelLeftClose,
} from "lucide-react";
import { useAuth } from "@/hooks/use-auth";
import { useTheme } from "@/hooks/use-theme";
import { useState } from "react";

const navItems = [
  { to: "/dashboard", label: "Tournaments", icon: Trophy, end: true },
  { to: "/dashboard/athletes", label: "Athletes", icon: PersonStanding },
  { to: "/dashboard/clubs", label: "Clubs", icon: Building2 },
  { to: "/dashboard/users", label: "Users", icon: Users },
  { to: "/dashboard/routines", label: "Routines", icon: Dumbbell },
];

const pageTitles: Record<string, string> = {
  "/dashboard": "Tournaments",
  "/dashboard/athletes": "Athletes",
  "/dashboard/clubs": "Clubs",
  "/dashboard/users": "Users",
  "/dashboard/routines": "Routines",
};

export function DashboardLayout() {
  const { logout, user } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  const title = pageTitles[location.pathname] ?? "Dashboard";

  function handleLogout() {
    logout();
    navigate("/");
  }

  return (
    <TooltipProvider>
      <div className="flex h-screen w-full overflow-hidden" style={{ background: "var(--background)" }}>
        <aside
          className={`flex flex-col items-center ${collapsed ? "w-14" : "w-14 lg:w-48"} shrink-0 h-full py-5 gap-1 transition-[width] duration-300 ease-in-out`}
          style={{ background: "var(--background)", borderRight: "1px solid var(--border)" }}
        >
          <div className={`mb-5 flex flex-col items-center gap-1 ${collapsed ? "" : "animate-fade-scale"}`}>
            {!collapsed && (
              <>
                <span
                  className="hidden lg:inline text-sm font-semibold tracking-widest uppercase leading-none"
                  style={{ color: "var(--foreground)" }}
                >
                  Cali Arena
                </span>
                <span
                  className="hidden lg:inline text-[10px] uppercase tracking-[0.3em] leading-none"
                  style={{ color: "var(--accent)" }}
                >
                  Admin Panel
                </span>
              </>
            )}
            <button
              onClick={() => setCollapsed((v) => !v)}
              aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
              title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
              className="flex items-center justify-center mt-1 transition-colors"
              style={{ color: "var(--muted-foreground)" }}
              onMouseEnter={(e) => (e.currentTarget.style.color = "var(--accent)")}
              onMouseLeave={(e) => (e.currentTarget.style.color = "var(--muted-foreground)")}
            >
              <PanelLeftClose
                className={`w-4 h-4 transition-transform duration-300 ease-in-out ${collapsed ? "rotate-180" : ""}`}
              />
            </button>
          </div>

          <div className={`flex-1 flex flex-col gap-0.5 w-full ${collapsed ? "items-center" : "items-center lg:items-stretch lg:px-10"}`}>
            {navItems.map((item) => (
              <Tooltip key={item.to}>
                <TooltipTrigger asChild>
                  <NavLink
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      `w-full flex items-center ${collapsed ? "justify-center" : "justify-center lg:justify-start"
                      } gap-3 h-9 rounded transition-colors ${isActive
                        ? "text-accent"
                        : "text-faint hover:text-muted-foreground"
                      }`
                    }
                    style={({ isActive }) =>
                      isActive ? { background: "var(--accent-08)" } : {}
                    }
                  >
                    <item.icon className="w-[15px] h-[15px] shrink-0" />
                    <span
                      className={`${collapsed ? "hidden" : "hidden lg:inline"} text-xs font-medium`}
                      style={{ color: "inherit" }}
                    >
                      {item.label}
                    </span>
                  </NavLink>
                </TooltipTrigger>
                <TooltipContent
                  side="right"
                  className={`text-xs ${collapsed ? "" : "lg:hidden"}`}
                  style={{ background: "var(--card)", color: "var(--foreground)", border: "1px solid var(--border)" }}
                >
                  {item.label}
                </TooltipContent>
              </Tooltip>
            ))}
          </div>

          {user && (
            <div className={`w-full flex flex-col items-center gap-1 mb-2 ${collapsed ? "" : "lg:px-10"}`}>
              <span className={`${collapsed ? "hidden" : "hidden lg:inline"} text-[11px]`} style={{ color: "var(--muted-foreground)" }}>
                Logged as <span style={{ color: "var(--foreground)" }}>{user.username}</span>
              </span>
              <span
                className={`${collapsed ? "hidden" : ""} text-[9px] uppercase tracking-widest px-1.5 py-0.5 rounded-full`}
                style={{ background: "var(--accent-12)", color: "var(--accent)" }}
              >
                {user.role}
              </span>
            </div>
          )}

          <Tooltip>
            <TooltipTrigger asChild>
              <button
                onClick={handleLogout}
                className="w-full flex items-center justify-center gap-3 h-9 rounded transition-colors text-faint hover:text-accent"
              >
                <LogOut className="w-[15px] h-[15px] shrink-0" />
                <span className={`${collapsed ? "hidden" : "hidden lg:inline"} text-xs font-medium`}>Logout</span>
              </button>
            </TooltipTrigger>
            <TooltipContent
              side="right"
              className={`text-xs ${collapsed ? "" : "lg:hidden"}`}
              style={{ background: "var(--card)", color: "var(--foreground)", border: "1px solid var(--border)" }}
            >
              Logout
            </TooltipContent>
          </Tooltip>
        </aside>

        <div className="flex flex-col flex-1 min-w-0 h-full min-h-0">
          <header
            className="h-11 flex items-center justify-between px-7 shrink-0"
            style={{ borderBottom: "1px solid var(--border)" }}
          >
            <span
              className="text-xs tracking-widest uppercase"
              style={{ color: "var(--muted-foreground)", fontFamily: "Geist Variable, sans-serif" }}
            >
              {title}
            </span>
            <button
              onClick={toggleTheme}
              aria-label="Toggle theme"
              title={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
              className="w-8 h-8 flex items-center justify-center rounded transition-colors text-foreground opacity-80 hover:text-accent hover:opacity-100"
            >
              {theme === "dark" ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
            </button>
          </header>

          <main className="flex-1 overflow-y-auto px-7 py-7">
            <div className="animate-fade-up h-full" style={{ opacity: 0 }}>
              <Outlet />
            </div>
          </main>
        </div>
      </div>
    </TooltipProvider>
  );
}