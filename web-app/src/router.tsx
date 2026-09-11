import { createBrowserRouter, redirect } from "react-router-dom";
import { LoginPage } from "@/pages/login-page";
import { DashboardLayout } from "./components/layout/DashboardLayout.tsx";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { TournamentsPage } from "@/pages/tournaments-page";
import { TournamentDetailPage } from "@/pages/tournament-detail";
import { AthletesPage } from "@/pages/athletes-page";
import { ClubsPage } from "@/pages/clubs-page";
import { UsersPage } from "@/pages/users-page";
import { RoutinesPage } from "@/pages/routines-page";
import { ScreenPage } from "@/pages/live/screen-page";

export const router = createBrowserRouter([
  { 
    path: "/", 
    element: <LoginPage />,
    loader: () => {
      if (localStorage.getItem("token")) 
        return redirect("/dashboard");
      return null;
    }, 
  },
  {
    path: "/dashboard",
    element: <ProtectedRoute />,
    children: [
      {
        element: <DashboardLayout />,
        children: [
          { index: true, element: <TournamentsPage /> },
          { path: "tournaments/:id", element: <TournamentDetailPage /> },
          { path: "athletes", element: <AthletesPage /> },
          { path: "clubs", element: <ClubsPage /> },
          { path: "users", element: <UsersPage /> },
          { path: "routines", element: <RoutinesPage /> },
        ],
      },
    ],
  },
  { path: "/screen/:tournamentId", element: <ScreenPage /> },
]);