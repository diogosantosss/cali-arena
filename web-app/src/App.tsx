import { RouterProvider } from "react-router-dom";
import { router } from "./router";
import { AuthProvider } from "./context/auth-provider";
import { ThemeProvider } from "./context/theme-provider";

export default function App() {
  
  if (typeof window !== 'undefined' && 'serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  }

  return (
    <ThemeProvider>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </ThemeProvider>
  );
}