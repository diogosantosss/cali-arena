const PANEL_TRANSITION_MS = 300;
const PANEL_EASING = "cubic-bezier(0.4, 0, 0.2, 1)";

interface AnimatedPanelProps {
  /** Must match the `activeTab` value that should reveal this panel. */
  name: string;
  activeTab: string;
  children: React.ReactNode;
}

/**
 * A single stacked panel that fades and slides in/out when it becomes the
 * active one. Panels must stay mounted for the exit transition to play, so
 * callers should render every panel unconditionally rather than gating on
 * `activeTab`. Pair it with `usePanelHost` to size the surrounding container.
 */
export function AnimatedPanel({ name, activeTab, children }: AnimatedPanelProps) {
  const active = activeTab === name;

  return (
    <div
      data-panel={name}
      className="absolute inset-x-0 top-0"
      style={{
        opacity: active ? 1 : 0,
        transform: active ? "translateY(0)" : "translateY(8px)",
        pointerEvents: active ? "auto" : "none",
        visibility: active ? "visible" : "hidden",
        zIndex: active ? 1 : 0,
        willChange: "opacity, transform",
        transitionProperty: "opacity, transform, visibility",
        transitionDuration: `${PANEL_TRANSITION_MS}ms, ${PANEL_TRANSITION_MS}ms, 0ms`,
        transitionTimingFunction: PANEL_EASING,
        // Only `visibility` is delayed, so the fade-out stays visible before the
        // panel leaves the focus/accessibility tree.
        transitionDelay: active ? "0ms, 0ms, 0ms" : `0ms, 0ms, ${PANEL_TRANSITION_MS}ms`,
      }}
    >
      {children}
    </div>
  );
}