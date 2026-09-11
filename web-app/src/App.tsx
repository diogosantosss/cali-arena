import { RouterProvider } from "react-router-dom";
import { router } from "./router";
import { AuthProvider } from "./context/auth-provider";
import { ThemeProvider } from "./context/theme-provider";

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </ThemeProvider>
  );
}