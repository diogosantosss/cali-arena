import type { Athlete } from "@/data/athletes";
import { sideColor, sideLabel, type Side } from "./judge-utils";

export function JudgeAvatar({
  name,
  accentColor,
  size = 40,
  className,
}: {
  name?: string;
  accentColor: string;
  size?: number;
  className?: string;
}) {
  const initial = name ? name.charAt(0).toUpperCase() : "—";
  return (
    <span
      className={`inline-flex items-center justify-center rounded-full font-semibold shrink-0 ${className ?? ""}`}
      style={{
        width: size,
        height: size,
        background: name ? `${accentColor}2e` : "transparent",
        border: `2px solid ${name ? accentColor : "var(--border)"}`,
        color: name ? accentColor : "var(--muted-foreground)",
        fontSize: size * 0.38,
        lineHeight: 1,
      }}
    >
      {initial}
    </span>
  );
}

interface SideChoice {
  side: Side;
  athlete?: Athlete;
  clubName?: string;
}

interface JudgeSideChooserProps {
  choices: [SideChoice, SideChoice];
  onSelectSide: (side: Side) => void;
}

export function JudgeSideChooser({ choices, onSelectSide }: JudgeSideChooserProps) {
  return (
    <div className="space-y-4">
      <div>
        <h1
          className="text-lg font-semibold"
          style={{ color: "var(--foreground)" }}
        >
          Choose your side
        </h1>
        <p className="text-sm" style={{ color: "var(--muted-foreground)" }}>
          Each judge picks one athlete of the match.
        </p>
      </div>

      <div className="grid gap-3">
        {choices.map(({ side, athlete, clubName }) => {
          const accent = sideColor(side);
          return (
            <div
              key={side}
              className="rounded-2xl overflow-hidden"
              style={{
                background: "var(--card)",
                border: "1px solid var(--border)",
              }}
            >
              <div className="px-4 pt-4 pb-3 space-y-3">
                <div className="flex items-center justify-between">
                  <span
                    className="text-[11px] px-2.5 py-0.5 rounded-full font-semibold uppercase tracking-wider"
                    style={{ background: `${accent}2b`, color: accent }}
                  >
                    {sideLabel(side)}
                  </span>
                  <span
                    className="w-5 h-5 rounded-full border-2"
                    style={{ borderColor: accent }}
                  />
                </div>

                <div className="flex items-center gap-3">
                  <JudgeAvatar name={athlete?.name} accentColor={accent} size={44} />
                  <div className="min-w-0">
                    <p
                      className="text-sm font-semibold leading-tight truncate"
                      style={{ color: athlete?.name ? "var(--foreground)" : "var(--muted-foreground)" }}
                    >
                      {athlete?.name ?? "Unassigned"}
                    </p>
                    {athlete && (
                      <p className="text-xs leading-tight truncate" style={{ color: "var(--muted-foreground)" }}>
                        {clubName ?? "No club"}
                      </p>
                    )}
                  </div>
                </div>
              </div>

              <button
                disabled={!athlete}
                onClick={() => onSelectSide(side)}
                className="w-full py-3 text-sm font-semibold transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                style={{ background: accent, color: "#fff" }}
              >
                Judge {sideLabel(side)}
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}