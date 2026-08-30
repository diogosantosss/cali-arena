import { Medal, Trophy } from "lucide-react";
import type { BracketLeaderboard } from "@/features/tournaments/types";
import { screenBackground } from "../lib/screen-background";

const podiumMeta = [
  {
    color: "var(--spec-gold)",
    soft: "var(--spec-gold-soft)",
    border: "var(--spec-gold-border)",
    glow: "var(--spec-gold-glow)",
  },
  {
    color: "var(--spec-silver)",
    soft: "var(--spec-silver-soft)",
    border: "var(--spec-silver-border)",
    glow: "var(--spec-silver-border)",
  },
  {
    color: "var(--spec-bronze)",
    soft: "var(--spec-bronze-soft)",
    border: "var(--spec-bronze-border)",
    glow: "var(--spec-bronze-border)",
  },
];

function displayOrder(count: number): number[] {
  if (count >= 3) return [1, 0, 2];
  if (count === 2) return [1, 0];
  return [0];
}

export function LeaderboardScreen({
  tournamentName,
  leaderboard,
}: {
  tournamentName: string;
  leaderboard: BracketLeaderboard;
}) {
  const entries = leaderboard.entries;
  const podiumEntries = entries.slice(0, 3);
  const restEntries = entries.slice(3);

  return (
    <div className="min-h-screen flex flex-col" style={{ ...screenBackground, color: "white" }}>
      <div className="text-center pt-16 px-16 shrink-0">
        <p className="font-cairo text-6xl font-semibold leading-tight uppercase bg-gradient-to-r from-[var(--spec-accent)] to-[var(--spec-title-end)] bg-clip-text text-transparent">
          {tournamentName}
        </p>
        <p className="mt-4 font-cairo text-[2rem] font-semibold uppercase tracking-widest text-[var(--spec-text-soft)]">
          Best times — {leaderboard.stage} · {leaderboard.division}
        </p>
      </div>

      {entries.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <p className="font-cairo text-[var(--spec-text-ghost)] uppercase tracking-widest text-lg">
            No finished attempts yet
          </p>
        </div>
      ) : (
        <div className="flex-1 flex flex-col items-center justify-center px-16 pb-16">
          {podiumEntries.length > 0 && (
            <div className="flex items-end justify-center gap-6">
              {displayOrder(podiumEntries.length).map((index) => {
                const entry = podiumEntries[index];
                const position = index + 1;
                const meta = podiumMeta[index];
                const isFirst = position === 1;

                return (
                  <div
                    key={entry.matchId}
                    className="flex flex-col items-center rounded-3xl text-center"
                    style={{
                      background: meta.soft,
                      border: `1px solid ${meta.border}`,
                      boxShadow: isFirst ? `0 0 80px ${meta.glow}` : undefined,
                      ...(isFirst ? { padding: "2.5rem 3.5rem" } : { padding: "1.75rem 2.5rem 2rem" }),
                    }}
                  >
                    <div className={isFirst ? "mb-4" : "mb-3"} style={{ color: meta.color }}>
                      {isFirst ? <Trophy className="w-14 h-14" strokeWidth={1.5} /> : <Medal className="w-10 h-10" strokeWidth={1.5} />}
                    </div>

                    <p
                      className={
                        "font-cairo font-bold leading-none " + (isFirst ? "text-[3.5rem]" : "text-[2.25rem]")
                      }
                    >
                      {position}º
                    </p>

                    <p
                      className={
                        "font-cairo font-semibold leading-tight text-white mt-3 " +
                        (isFirst ? "text-[1.75rem]" : "text-[1.25rem]")
                      }
                    >
                      {entry.athleteName}
                    </p>

                    <p
                      className="font-cairo font-bold leading-none tabular-nums mt-5"
                      style={{ color: meta.color, fontSize: isFirst ? "1.75rem" : "1.25rem" }}
                    >
                      {entry.duration}
                    </p>
                  </div>
                );
              })}
            </div>
          )}

          {restEntries.length > 0 && (
            <div className="w-full mt-10">
              <div
                className="grid gap-2"
                style={{ gridTemplateColumns: "repeat(auto-fill, minmax(340px, 1fr))" }}
              >
                {restEntries.map((entry, index) => (
                  <div
                    key={entry.matchId}
                    className="grid grid-cols-[56px_1fr_auto] items-center gap-4 px-5 py-2.5 rounded-lg"
                    style={{ background: "var(--spec-surface-glass-2)" }}
                  >
                    <span className="font-cairo text-[1.3rem] font-bold leading-none text-[var(--spec-text-muted)]">
                      {index + 4}º
                    </span>
                    <span className="font-cairo text-[1.3rem] font-semibold leading-none text-white truncate">
                      {entry.athleteName}
                    </span>
                    <span className="font-cairo text-[1.3rem] font-bold leading-none tabular-nums text-[var(--spec-text-grey)]">
                      {entry.duration}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}