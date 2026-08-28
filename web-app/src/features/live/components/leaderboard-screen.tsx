import type { BracketLeaderboard } from "@/features/tournaments/types";
import { screenBackground } from "../lib/screen-background";

export function LeaderboardScreen({
  tournamentName,
  leaderboard,
}: {
  tournamentName: string;
  leaderboard: BracketLeaderboard;
}) {
  const entries = leaderboard.entries;

  return (
    <div className="min-h-screen flex flex-col" style={{ ...screenBackground, color: "white" }}>
      <div className="text-center pt-16 px-16">
        <p className="font-cairo text-6xl font-semibold leading-tight uppercase bg-gradient-to-r from-[#e8a020] to-[#f0ede8] bg-clip-text text-transparent">
          {tournamentName}
        </p>
        <p className="mt-4 font-cairo text-[2rem] font-semibold uppercase tracking-widest text-white/60">
          Best times — {leaderboard.stage} · {leaderboard.gender}
        </p>
      </div>

      {entries.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <p className="font-cairo text-white/25 uppercase tracking-widest text-lg">
            No finished attempts yet
          </p>
        </div>
      ) : (
        <div className="flex-1 flex items-start justify-center pt-14 px-16">
          <div className="w-full max-w-3xl">
            <div className="grid grid-cols-[64px_1fr_170px] gap-4 px-6 pb-3 font-cairo text-[1.1rem] uppercase tracking-widest text-white/40">
              <span>#</span>
              <span>Athlete</span>
              <span className="text-right">Time</span>
            </div>

            <div className="space-y-1.5">
              {entries.map((entry, index) => {
                const position = index + 1;
                const isPodium = position <= 3;
                const medalColor = ["#f5c453", "#c9c9cf", "#cd8b4c"][position - 1];

                return (
                  <div
                    key={entry.matchId}
                    className="grid grid-cols-[64px_1fr_170px] items-center gap-4 px-6 py-4 rounded-lg"
                    style={{
                      background: isPodium ? `${medalColor}1a` : "rgba(255,255,255,0.04)",
                      border: isPodium ? `1px solid ${medalColor}55` : "1px solid transparent",
                    }}
                  >
                    <span
                      className="font-cairo text-[1.75rem] font-bold leading-none"
                      style={{ color: isPodium ? medalColor : "rgba(255,255,255,0.4)" }}
                    >
                      {position}
                    </span>
                    <span className="font-cairo text-[1.75rem] font-semibold leading-none text-white">
                      {entry.athleteName}
                    </span>
                    <span
                      className="font-cairo text-[1.75rem] font-bold leading-none tabular-nums text-right"
                      style={{ color: isPodium ? medalColor : "#a09a92" }}
                    >
                      {entry.duration}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}