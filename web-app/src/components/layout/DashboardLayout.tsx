import { Outlet, NavLink, useNavigate, useLocation } from "react-router-dom";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import {
  Trophy,
  Users,
  PersonStanding,
  Building2,
  LogOut,
  Dumbbell,
  Monitor,
} from "lucide-react";

const navItems = [
  { to: "/dashboard", label: "Tournaments", icon: Trophy, end: true },
  { to: "/dashboard/athletes", label: "Athletes", icon: PersonStanding },
  { to: "/dashboard/clubs", label: "Clubs", icon: Building2 },
  { to: "/dashboard/users", label: "Users", icon: Users },
  { to: "/dashboard/routines", label: "Routines", icon: Dumbbell },
  { to: "/dashboard/screen", label: "Screen", icon: Monitor },
];

const pageTitles: Record<string, string> = {
  "/dashboard": "Tournaments",
  "/dashboard/athletes": "Athletes",
  "/dashboard/clubs": "Clubs",
  "/dashboard/users": "Users",
  "/dashboard/routines": "Routines",
  "/dashboard/screen": "Screen Manager",
};

export function DashboardLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const title = pageTitles[location.pathname] ?? "Dashboard";

  function handleLogout() {
    localStorage.removeItem("token");
    navigate("/");
  }

  return (
    <TooltipProvider>
      <div className="flex min-h-screen w-full" style={{ background: "#0f0f11" }}>
        <aside
          className="flex flex-col items-center w-14 shrink-0 py-5 gap-1"
          style={{ background: "#0f0f11", borderRight: "1px solid #252528" }}
        >
          <div className="mb-5">
            <div
              className="w-7 h-7 rounded flex items-center justify-center text-[11px] font-bold"
              style={{ background: "#e8a020", color: "#0f0f11" }}
            >
              C
            </div>
          </div>

          <div className="flex-1 flex flex-col items-center gap-0.5 w-full px-2">
            {navItems.map((item) => (
              <Tooltip key={item.to}>
                <TooltipTrigger asChild>
                  <NavLink
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      `w-full flex items-center justify-center h-9 rounded transition-colors ${
                        isActive
                          ? "text-[#e8a020]"
                          : "text-[#3a3a3d] hover:text-[#6b6560]"
                      }`
                    }
                    style={({ isActive }) =>
                      isActive ? { background: "rgba(232,160,32,0.08)" } : {}
                    }
                  >
                    <item.icon className="w-[15px] h-[15px]" />
                  </NavLink>
                </TooltipTrigger>
                <TooltipContent
                  side="right"
                  className="text-xs"
                  style={{ background: "#17171a", color: "#f0ede8", border: "1px solid #252528" }}
                >
                  {item.label}
                </TooltipContent>
              </Tooltip>
            ))}
          </div>

          <Tooltip>
            <TooltipTrigger asChild>
              <button
                onClick={handleLogout}
                className="w-10 h-9 flex items-center justify-center rounded transition-colors text-[#3a3a3d] hover:text-[#e8a020]"
              >
                <LogOut className="w-[15px] h-[15px]" />
              </button>
            </TooltipTrigger>
            <TooltipContent
              side="right"
              className="text-xs"
              style={{ background: "#17171a", color: "#f0ede8", border: "1px solid #252528" }}
            >
              Logout
            </TooltipContent>
          </Tooltip>
        </aside>

        <div className="flex flex-col flex-1 min-w-0">
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

          <main className="flex-1 overflow-auto px-7 py-7">
            <Outlet />
          </main>
        </div>
      </div>
    </TooltipProvider>
  );
}
