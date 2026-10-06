import { createBrowserRouter } from "react-router-dom";
import { ADMIN_ROLES, DASHBOARD_ROLES } from "@/data/auth";
import { LoginPage } from "@/pages/login-page";
import { DashboardLayout } from "./components/layout/DashboardLayout.tsx";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { RoleGate } from "./components/RoleGate";
import { TournamentsPage } from "@/pages/tournaments-page";
import { TournamentDetailPage } from "@/pages/tournament-detail";
import { AthletesPage } from "@/pages/athletes-page";
import { ClubsPage } from "@/pages/clubs-page";
import { UsersPage } from "@/pages/users-page";
import { RoutinesPage } from "@/pages/routines-page";
import { JudgeMatchesPage } from "@/pages/judge/judge-matches-page";
import { JudgeMatchPage } from "@/pages/judge/judge-match-page";
import { JudgeRoute } from "@/components/judge/judge-route";
import { ScreenPage } from "@/pages/live/screen-page";
import { OverlayPage } from "@/pages/overlay/overlay-page.tsx";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <LoginPage />,
  },
  {
    // Admins and hosts only: a judge that reaches /dashboard is sent to /judge.
    path: "/dashboard",
    element: <ProtectedRoute roles={DASHBOARD_ROLES} />,
    children: [
      {
        element: <DashboardLayout />,
        children: [
          { index: true, element: <TournamentsPage /> },
          { path: "tournaments/:id", element: <TournamentDetailPage /> },
          // Read-only for hosts: the backend only lets admins write this data.
          { path: "athletes", element: <AthletesPage /> },
          { path: "clubs", element: <ClubsPage /> },
          { path: "routines", element: <RoutinesPage /> },
          { path: "users", element: <RoleGate roles={ADMIN_ROLES}><UsersPage /></RoleGate> },
        ],
      },
    ],
  },
  { path: "/screen/:tournamentId", element: <ScreenPage /> },
  { path: "/overlay/:tournamentId", element: <OverlayPage /> },
  {
    path: "/judge",
    element: <JudgeRoute />,
    children: [
      { index: true, element: <JudgeMatchesPage /> },
      { path: ":matchId", element: <JudgeMatchPage /> },
    ],
  },
]);
