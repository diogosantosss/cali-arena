import { createBrowserRouter, redirect } from "react-router-dom";
import { TournamentsPage } from "./pages/TournamentsPage.tsx";
import { LoginPage } from "./pages/LoginPage";
import { DashboardLayout } from "./components/layout/DashboardLayout.tsx";
import { TournamentDetailPage } from "./pages/TournamentDetailPage.tsx";
import { AthletesPage } from "./pages/AthletesPage.tsx";
import { ClubsPage } from "./pages/ClubsPage.tsx";
import { UsersPage } from "./pages/UsersPage.tsx";
import { RoutinesPage } from "./pages/RoutinesPage.tsx";
import { ScreenPage } from "./pages/ScreenPage.tsx";

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