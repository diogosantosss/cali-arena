import { useCallback, useLayoutEffect, useRef, useState } from "react";
import { ChevronDown } from "lucide-react";

interface Tab {
  id: string;
  label: string;
  icon?: React.ReactNode;
  badge?: number;
}

interface TabsProps {
  tabs: Tab[];
  activeTab: string;
  onChange: (tabId: string) => void;
  variant?: "default" | "pills" | "underline";
  className?: string;
  dropdown?: boolean;
}

export function Tabs({
  tabs,
  activeTab,
  onChange,
  variant = "default",
  className = "",
  dropdown = false,
}: TabsProps) {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const tabRefs = useRef<Record<string, HTMLButtonElement | null>>({});
  const [indicator, setIndicator] = useState<{ left: number; width: number } | null>(null);

  // `tabs` is usually an inline literal, so depend on the ids rather than the
  // array identity to avoid re-measuring (and re-setting state) every render.
  const tabKey = tabs.map((t) => t.id).join("|");

  const measureIndicator = useCallback(() => {
    const tab = tabRefs.current[activeTab];
    setIndicator(tab ? { left: tab.offsetLeft, width: tab.offsetWidth } : null);
  }, [activeTab]);

  useLayoutEffect(() => {
    if (variant !== "underline") return;

    measureIndicator();

    const observer = new ResizeObserver(measureIndicator);
    if (listRef.current) observer.observe(listRef.current);
    Object.values(tabRefs.current).forEach((tab) => tab && observer.observe(tab));
    window.addEventListener("resize", measureIndicator);

    return () => {
      observer.disconnect();
      window.removeEventListener("resize", measureIndicator);
    };
  }, [measureIndicator, tabKey, variant]);

  if (dropdown && tabs.length > 4) {
    const active = tabs.find((t) => t.id === activeTab);
    const others = tabs.filter((t) => t.id !== activeTab);

    return (
      <div className={`relative inline-block ${className}`}>
        <button
          onClick={() => setDropdownOpen(!dropdownOpen)}
          className="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-lg transition-colors"
          style={{
            background: "var(--accent)",
            color: "var(--accent-foreground)",
          }}
          aria-expanded={dropdownOpen}
          aria-haspopup="listbox"
        >
          {active?.icon && <span>{active.icon}</span>}
          <span>{active?.label}</span>
          <ChevronDown className={`w-4 h-4 transition-transform ${dropdownOpen ? "rotate-180" : ""}`} />
        </button>

        {dropdownOpen && (
          <div className="absolute right-0 top-full mt-2 z-20 min-w-[160px] rounded-lg shadow-lg border" style={{ background: "var(--card)", borderColor: "var(--border)" }}>
            <ul role="listbox">
              <li role="option">
                <button
                  onClick={() => { onChange(active!.id); setDropdownOpen(false); }}
                  className={`w-full px-4 py-2 text-left transition-colors flex items-center gap-2 ${activeTab === active!.id ? "bg-accent/10" : ""}`}
                  style={{ color: activeTab === active!.id ? "var(--accent)" : "var(--foreground)" }}
                  aria-selected={activeTab === active!.id}
                >
                  {active?.icon && <span>{active.icon}</span>}
                  <span>{active!.label}</span>
                </button>
              </li>
              {others.map((tab) => (
                <li key={tab.id} role="option">
                  <button
                    onClick={() => { onChange(tab.id); setDropdownOpen(false); }}
                    className={`w-full px-4 py-2 text-left transition-colors flex items-center gap-2 ${activeTab === tab.id ? "bg-accent/10" : ""}`}
                    style={{ color: activeTab === tab.id ? "var(--accent)" : "var(--foreground)" }}
                    aria-selected={activeTab === tab.id}
                  >
                    {tab.icon && <span>{tab.icon}</span>}
                    <span>{tab.label}</span>
                    {tab.badge && <span className="ml-auto px-2 py-0.5 text-xs rounded-full" style={{ background: "var(--accent-12)", color: "var(--accent)" }}>{tab.badge}</span>}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    );
  }

  const baseStyles = "flex items-center gap-1.5 px-3 py-2 text-sm font-medium rounded-lg transition-colors";
  const variants = {
    default: {
      base: baseStyles,
      active: "bg-accent text-accent-foreground",
      inactive: "text-muted-foreground hover:text-foreground hover:bg-muted",
    },
    pills: {
      base: baseStyles,
      active: "bg-accent text-accent-foreground",
      inactive: "text-muted-foreground hover:text-foreground hover:bg-muted",
    },
    underline: {
      base: "relative flex items-center gap-1.5 px-3 py-3 text-sm font-medium transition-colors",
      active: "text-accent",
      inactive: "text-muted-foreground hover:text-foreground",
    },
  };

  const { base, active, inactive } = variants[variant];

  return (
    <div
      ref={listRef}
      className={`flex flex-wrap gap-1 ${variant === "underline" ? "relative border-b" : ""} ${className}`}
      style={{ borderColor: "var(--border)" }}
      role="tablist"
    >
      {tabs.map((tab) => {
        const selected = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            ref={(el) => {
              tabRefs.current[tab.id] = el;
            }}
            role="tab"
            aria-selected={selected}
            onClick={() => onChange(tab.id)}
            className={`${base} ${selected ? active : inactive}`}
          >
            {tab.icon && <span className="w-4 h-4">{tab.icon}</span>}
            <span>{tab.label}</span>
            {tab.badge && <span className="ml-1 px-1.5 py-0.5 text-xs rounded-full" style={{ background: "var(--accent-12)", color: "var(--accent)" }}>{tab.badge}</span>}
          </button>
        );
      })}

      {variant === "underline" && indicator && (
        <span
          aria-hidden="true"
          className="absolute bottom-[-1px] h-0.5 rounded-full pointer-events-none"
          style={{
            background: "var(--accent)",
            transform: `translateX(${indicator.left}px)`,
            width: indicator.width,
            transition: "transform 300ms cubic-bezier(0.4, 0, 0.2, 1), width 300ms cubic-bezier(0.4, 0, 0.2, 1)",
          }}
        />
      )}
    </div>
  );
}