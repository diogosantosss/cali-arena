import { Outlet, NavLink, useNavigate, useLocation } from "react-router-dom";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import {
  Trophy,
  Users,
  PersonStanding,
  Building2,
  LogOut,
  Dumbbell,
} from "lucide-react";
import { useAuth } from "@/features/auth/hooks/use-auth";

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
  const navigate = useNavigate();
  const location = useLocation();

  const title = pageTitles[location.pathname] ?? "Dashboard";

  function handleLogout() {
    logout();
    navigate("/");
  }

  return (
    <TooltipProvider>
      <div className="flex h-screen w-full overflow-hidden" style={{ background: "#0f0f11" }}>
        <aside
          className="flex flex-col items-center w-14 lg:w-48 shrink-0 h-full py-5 gap-1"
          style={{ background: "#0f0f11", borderRight: "1px solid #252528" }}
        >
          <div className="mb-5 flex items-center justify-center gap-2">
            <div
              className="w-7 h-7 shrink-0 rounded flex items-center justify-center text-[11px] font-bold"
              style={{ background: "#e8a020", color: "#0f0f11" }}
            >
              C
            </div>
            <span className="hidden lg:inline text-xs tracking-widest uppercase" style={{ color: "#f0ede8" }}>
              Cali Arena
            </span>
          </div>

          <div className="flex-1 flex flex-col items-center lg:items-stretch gap-0.5 w-full lg:px-10">
            {navItems.map((item) => (
              <Tooltip key={item.to}>
                <TooltipTrigger asChild>
                  <NavLink
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      `w-full flex items-center justify-center lg:justify-start gap-3 h-9 rounded transition-colors ${
                        isActive
                          ? "text-[#e8a020]"
                          : "text-[#3a3a3d] hover:text-[#6b6560]"
                      }`
                    }
                    style={({ isActive }) =>
                      isActive ? { background: "rgba(232,160,32,0.08)" } : {}
                    }
                  >
                    <item.icon className="w-[15px] h-[15px] shrink-0" />
                    <span
                      className="hidden lg:inline text-xs font-medium"
                      style={{ color: "inherit" }}
                    >
                      {item.label}
                    </span>
                  </NavLink>
                </TooltipTrigger>
                <TooltipContent
                  side="right"
                  className="text-xs lg:hidden"
                  style={{ background: "#17171a", color: "#f0ede8", border: "1px solid #252528" }}
                >
                  {item.label}
                </TooltipContent>
              </Tooltip>
            ))}
          </div>

          {user && (
            <div className="w-full flex flex-col items-center gap-1 mb-2 lg:px-10">
              <span className="hidden lg:inline text-[11px]" style={{ color: "#6b6560" }}>
                Logged as <span style={{ color: "#f0ede8" }}>{user.username}</span>
              </span>
              <span
                className="text-[9px] uppercase tracking-widest px-1.5 py-0.5 rounded-full"
                style={{ background: "rgba(232,160,32,0.12)", color: "#e8a020" }}
              >
                {user.role}
              </span>
            </div>
          )}

          <Tooltip>
            <TooltipTrigger asChild>
              <button
                onClick={handleLogout}
                className="w-full flex items-center justify-center gap-3 h-9 rounded transition-colors text-[#3a3a3d] hover:text-[#e8a020]"
              >
                <LogOut className="w-[15px] h-[15px] shrink-0" />
                <span className="hidden lg:inline text-xs font-medium">Logout</span>
              </button>
            </TooltipTrigger>
            <TooltipContent
              side="right"
              className="text-xs lg:hidden"
              style={{ background: "#17171a", color: "#f0ede8", border: "1px solid #252528" }}
            >
              Logout
            </TooltipContent>
          </Tooltip>
        </aside>

        <div className="flex flex-col flex-1 min-w-0 h-full min-h-0">
          <header
            className="h-11 flex items-center px-7 shrink-0"
            style={{ borderBottom: "1px solid #252528" }}
          >
            <span
              className="text-xs tracking-widest uppercase"
              style={{ color: "#6b6560", fontFamily: "Geist Variable, sans-serif" }}
            >
              {title}
            </span>
          </header>

          <main className="flex-1 overflow-y-auto px-7 py-7">
            <Outlet />
          </main>
        </div>
      </div>
    </TooltipProvider>
  );
}