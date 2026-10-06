import { useLayoutEffect, useRef, useState } from "react";

const PANEL_TRANSITION_MS = 300;
const PANEL_EASING = "cubic-bezier(0.4, 0, 0.2, 1)";

/**
 * Wires a stack of absolutely positioned panels (see `AnimatedPanel`) so the
 * host grows/shrinks to fit whichever panel is active, instead of collapsing to
 * zero or overflowing its content.
 */
export function usePanelHost(activeTab: string) {
  const hostRef = useRef<HTMLDivElement>(null);
  const [height, setHeight] = useState(0);

  useLayoutEffect(() => {
    const host = hostRef.current;
    if (!host) return;

    const measure = () => {
      const panel = host.querySelector<HTMLElement>(`[data-panel="${activeTab}"]`);
      setHeight(panel?.offsetHeight ?? 0);
    };

    measure();

    // Panels resize on their own when async data lands or the window reflows.
    const observer = new ResizeObserver(measure);
    host.querySelectorAll<HTMLElement>("[data-panel]").forEach((panel) => observer.observe(panel));
    return () => observer.disconnect();
  }, [activeTab]);

  return {
    hostRef,
    hostStyle: {
      height,
      transition: `height ${PANEL_TRANSITION_MS}ms ${PANEL_EASING}`,
    },
  };
}