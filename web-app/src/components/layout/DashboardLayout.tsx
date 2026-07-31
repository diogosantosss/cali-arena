import { Outlet, NavLink, useNavigate } from "react-router-dom";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import { Separator } from "@/components/ui/separator";
import {
  Trophy,
  Users,
  PersonStanding,
  Building2,
  LogOut,
  Dumbbell,
} from "lucide-react";

const navItems = [
  { to: "/dashboard", label: "Tournaments", icon: Trophy, end: true },
  { to: "/dashboard/athletes", label: "Athletes", icon: PersonStanding },
  { to: "/dashboard/clubs", label: "Clubs", icon: Building2 },
  { to: "/dashboard/users", label: "Users", icon: Users },
  { to: "/dashboard/routines", label: "Routines", icon: Dumbbell },
];

export function DashboardLayout() {
  const navigate = useNavigate();

  function handleLogout() {
    localStorage.removeItem("token");
    navigate("/");
  }

  return (
    <SidebarProvider>
      <div className="flex min-h-screen w-full">
        <Sidebar>
          <SidebarHeader className="p-4">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[#6339db] to-[#a855f7] flex items-center justify-center font-bold text-sm text-white">
                C
              </div>
              <div>
                <p className="text-sm font-semibold leading-tight">Cali Arena</p>
                <p className="text-[10px] text-muted-foreground uppercase tracking-widest">
                  Admin Dashboard
                </p>
              </div>
            </div>
          </SidebarHeader>

          <Separator />

          <SidebarContent>
            <SidebarGroup>
              <SidebarGroupLabel>Management</SidebarGroupLabel>
              <SidebarMenu>
                {navItems.map((item) => (
                  <SidebarMenuItem key={item.to}>
                    <SidebarMenuButton asChild>
                      <NavLink
                        to={item.to}
                        end={item.end}
                        className={({ isActive }) =>
                          isActive ? "text-primary font-medium" : ""
                        }
                      >
                        <item.icon className="w-4 h-4" />
                        <span>{item.label}</span>
                      </NavLink>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroup>
          </SidebarContent>

          <SidebarFooter>
            <Separator />
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton onClick={handleLogout}>
                  <LogOut className="w-4 h-4" />
                  <span>Logout</span>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarFooter>
        </Sidebar>

        <div className="flex flex-col flex-1 min-w-0">
          <header className="h-12 border-b flex items-center px-4 gap-2">
            <SidebarTrigger />
            <Separator orientation="vertical" className="h-4" />
          </header>
          <main className="flex-1 p-6 overflow-auto">
            <Outlet />
          </main>
        </div>
      </div>
    </SidebarProvider>
  );
}