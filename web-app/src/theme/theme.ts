// THEME — single place to switch templates for the whole app.
//
// Set ACTIVE_THEME below and BOTH the dashboard and the live spectator
// screens are restyled together:
//   "gold" -> default-theme.css           + spectator-theme.css
//   "red"  -> default-theme-red.css       + spectator-theme-red.css
//
// To add a new template, drop a pair of CSS files in
// src/assets/styles/ and register the pair in `themes`.

export type ThemeName = "gold" | "red";

export const ACTIVE_THEME: ThemeName = "red";

const themes: Record<ThemeName, () => Promise<void>> = {
  gold: () =>
    Promise.all([
      import("@/assets/styles/default-theme.css"),
      import("@/assets/styles/spectator-theme.css"),
    ]).then(() => undefined),
  red: () =>
    Promise.all([
      import("@/assets/styles/default-theme-red.css"),
      import("@/assets/styles/spectator-theme-red.css"),
    ]).then(() => undefined),
};

export function loadTheme(): void {
  void themes[ACTIVE_THEME]();
}