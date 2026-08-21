import { createBrowserRouter, redirect } from "react-router-dom";
import { LoginPage } from "./features/auth/components/login-page";
import { DashboardLayout } from "./components/layout/DashboardLayout.tsx";
import { TournamentsPage } from "./features/tournaments/tournaments-page";
import { TournamentDetailPage } from "./features/tournaments/tournament-detail";
import { AthletesPage } from "./features/athletes/athletes-page";
import { ClubsPage } from "./features/clubs/clubs-page";
import { UsersPage } from "./features/users/users-page";
import { RoutinesPage } from "./features/routines/routines-page";
import { ScreenPage } from "./features/live/screen-page";

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
  { path: "/screen/:tournamentId", element: <ScreenPage /> },
]);