import type { CSSProperties } from "react";

export const screenBackground: CSSProperties = {
  background: "#0f0f11",
  backgroundImage: `
    radial-gradient(ellipse 80% 50% at 50% 0%, rgba(232,160,32,0.06) 0%, transparent 60%),
    radial-gradient(ellipse 60% 40% at 100% 100%, rgba(232,160,32,0.04) 0%, transparent 50%)
  `,
};