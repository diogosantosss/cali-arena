import type { CSSProperties } from "react";

export const screenBackground: CSSProperties = {
  background: "var(--spec-bg)",
  backgroundImage: `
    radial-gradient(ellipse 80% 50% at 50% 0%, var(--spec-accent-06) 0%, transparent 60%),
    radial-gradient(ellipse 60% 40% at 100% 100%, var(--spec-accent-04) 0%, transparent 50%)
  `,
};