import { Trophy } from "lucide-react";
import type { BracketMatchSummary, BracketStage, TournamentBracketsSummary } from "@/features/tournaments/types";
import { screenBackground } from "../lib/screen-background";

const bracketStageOrder: Record<BracketStage, number> = {
  QUALIFIERS: -1,
  QUARTERFINALS: 0,
  SEMIFINALS: 1,
  FINALS: 2,
};

const bracketStageLabel: Record<BracketStage, string> = {
  QUALIFIERS: "Qualifiers",
  QUARTERFINALS: "Quarterfinals",
  SEMIFINALS: "Semifinals",
  FINALS: "Finals",
};

export function BracketsScreen({ tournamentName, summary }: {
  tournamentName: string;
  summary: TournamentBracketsSummary;
}) {
  const columns = summary.brackets
    .filter((b) => b.stage !== "QUALIFIERS")
    .sort((a, b) => bracketStageOrder[a.stage] - bracketStageOrder[b.stage]);

  const finalMatch = columns.find((c) => c.stage === "FINALS")?.matches[0];
  const champion =
    finalMatch && finalMatch.winner !== "—" ? finalMatch.winner : null;

  return (
    <div className="h-screen flex flex-col overflow-hidden" style={{ ...screenBackground, color: "white" }}>
      <div className="text-center pt-10 px-16 shrink-0">
        <p className="font-cairo text-6xl font-semibold leading-tight uppercase bg-gradient-to-r from-[#e8a020] to-[#f0ede8] bg-clip-text text-transparent">
          {tournamentName}
        </p>
        <p className="mt-4 font-cairo text-[2rem] font-semibold uppercase tracking-widest text-white/60">
          Brackets · {summary.division}
        </p>
      </div>

      {columns.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <p className="font-cairo text-white/25 uppercase tracking-widest text-lg">No brackets yet</p>
        </div>
      ) : (
        <div className="flex-1 min-h-0 flex items-center justify-center px-16 py-8">
          <div className="flex w-full max-w-[80rem] min-h-0 items-stretch gap-10">
            {columns.map((col) => {
              const variant: "qf" | "sf" | "final" =
                col.stage === "FINALS" ? "final" : col.stage === "SEMIFINALS" ? "sf" : "qf";
              const isFinal = variant === "final";

              return (
                <div
                  key={col.stage}
                  className="relative flex-1 min-h-0 flex flex-col rounded-2xl px-5 py-6"
                  style={{
                    background: "rgba(255,255,255,0.02)",
                    border: isFinal ? "1px solid rgba(232,160,32,0.22)" : "1px solid rgba(232,160,32,0.08)",
                  }}
                >
                  {isFinal && (
                    <div
                      className="pointer-events-none absolute inset-0 rounded-2xl"
                      style={{ background: "radial-gradient(circle at 50% 18%, rgba(232,160,32,0.08), transparent 70%)" }}
                    />
                  )}

                  <div className="mb-5 flex items-center gap-3">
                    <span className="h-px flex-1" style={{ background: "rgba(232,160,32,0.12)" }} />
                    <p className="text-center font-cairo text-lg uppercase tracking-[0.2em]" style={{ color: "rgba(232,160,32,0.8)" }}>
                      {bracketStageLabel[col.stage]}
                    </p>
                    <span className="h-px flex-1" style={{ background: "rgba(232,160,32,0.12)" }} />
                  </div>

                  <div className="flex-1 min-h-0 w-full flex flex-col justify-around gap-4">
                    {col.matches.length === 0 ? (
                      <p className="font-cairo text-white/20 uppercase tracking-widest text-center text-sm">
                        No matches yet
                      </p>
                    ) : (
                      col.matches.map((m) => (
                        <BracketMatchCard key={m.matchId} match={m} variant={variant} />
                      ))
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {champion && (
        <div className="pb-8 pt-1 shrink-0 text-center">
          <Trophy className="mx-auto mb-3 h-8 w-8" style={{ color: "#e8a020" }} strokeWidth={1.5} />
          <p className="font-cairo text-xs uppercase tracking-[0.4em] text-white/40">Champion</p>
          <p className="mt-1 font-cairo text-[2rem] font-bold leading-none bg-gradient-to-r from-[#e8a020] to-[#f0ede8] bg-clip-text text-transparent">
            {champion}
          </p>
        </div>
      )}
    </div>
  );
}

function BracketMatchCard({ match, variant }: { match: BracketMatchSummary; variant: "qf" | "sf" | "final" }) {
  const decided = match.winner !== "—";
  const redWon = decided && match.winner === match.athleteRed;
  const blueWon = decided && match.winner === match.athleteBlue;

  const isFinal = variant === "final";
  const fontSize =
    isFinal ? "text-[2rem]" : variant === "sf" ? "text-[1.6rem]" : "text-[1.35rem]";
  const maxWidth = isFinal ? "26rem" : variant === "sf" ? "22rem" : "18rem";

  return (
    <div
      className={`mx-auto w-full rounded-2xl px-5 py-4 space-y-2.5 font-cairo ${fontSize}`}
      style={{
        maxWidth,
        background: isFinal ? "rgba(232,160,32,0.08)" : "rgba(255,255,255,0.04)",
        border: isFinal ? "1px solid rgba(232,160,32,0.45)" : "1px solid rgba(255,255,255,0.08)",
        boxShadow: isFinal ? "0 0 44px rgba(232,160,32,0.12)" : undefined,
      }}
    >
      <BracketAthleteRow name={match.athleteRed} state={redWon ? "won" : blueWon ? "lost" : "pending"} />
      <BracketAthleteRow name={match.athleteBlue} state={blueWon ? "won" : redWon ? "lost" : "pending"} />
      <div className="flex items-center justify-center h-5">
        {!decided && (
          <p className="font-cairo text-sm uppercase tracking-widest text-white/30 text-center">TBD</p>
        )}
      </div>
    </div>
  );
}

function BracketAthleteRow({ name, state }: { name: string; state: "won" | "lost" | "pending" }) {
  const won = state === "won";

  return (
    <p
      className="flex items-center justify-between gap-3 font-semibold leading-none"
      style={{
        color: won ? "#e8a020" : "rgba(255,255,255,0.85)",
        opacity: state === "lost" ? 0.4 : 1,
        transition: "opacity 300ms",
      }}
    >
      <span>{name}</span>
      {won && (
        <span className="font-cairo text-[0.75rem] font-bold tracking-widest" style={{ color: "#e8a020" }}>
          WINNER
        </span>
      )}
    </p>
  );
}