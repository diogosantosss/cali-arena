import { Moon, Sun } from "lucide-react";
import { useTheme } from "@/hooks/use-theme";

export function JudgeThemeToggle() {
  const { theme, toggleTheme } = useTheme();

  return (
    <button
      onClick={toggleTheme}
      aria-label="Toggle theme"
      title={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
      className="p-2 rounded-lg select-none touch-manipulation transition-colors active:opacity-80"
      style={{ background: "var(--secondary)", color: "var(--secondary-foreground)", border: "1px solid var(--border)", WebkitTapHighlightColor: "transparent" }}
    >
      {theme === "dark" ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
    </button>
  );
}