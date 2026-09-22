import { Fragment } from "react";
import { ChevronRight, Crown, GitMerge, Network, Shuffle, Trophy } from "lucide-react";
import type { BracketMatchSummary, BracketStage, TournamentBracketsSummary } from "@/data/tournaments";
import { screenBackground } from "@/utils/screen-background";
import { ScreenHeader } from "./screen-header";

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

const bracketStageIcon: Record<BracketStage, typeof Network> = {
  QUALIFIERS: Shuffle,
  QUARTERFINALS: Network,
  SEMIFINALS: GitMerge,
  FINALS: Trophy,
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
      <ScreenHeader tournamentName={tournamentName} subtitle={`Brackets · ${summary.division}`} />

      {columns.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <p className="font-cairo text-[var(--spec-text-ghost)] uppercase tracking-widest text-lg">No brackets yet</p>
        </div>
      ) : (
        <div className="flex-1 min-h-0 flex items-center justify-center px-16 py-8">
          <div className="flex w-full max-w-[82rem] min-h-0 items-stretch gap-5">
            {columns.map((col, index) => {
              const variant: "qf" | "sf" | "final" =
                col.stage === "FINALS" ? "final" : col.stage === "SEMIFINALS" ? "sf" : "qf";
              const isFinal = variant === "final";
              const decided = col.matches.filter((m) => m.winner !== "—").length;
              const StageIcon = bracketStageIcon[col.stage];

              return (
                <Fragment key={col.stage}>
                  {index > 0 && <ColumnConnector />}
                  <div
                    className="relative flex-1 min-h-0 flex flex-col rounded-2xl px-5 py-6 overflow-hidden"
                    style={{
                      background: "var(--spec-surface-glass)",
                      border: isFinal ? "1px solid var(--spec-accent-22)" : "1px solid var(--spec-accent-08)",
                    }}
                  >
                    {isFinal && (
                      <div
                        className="pointer-events-none absolute inset-0 rounded-2xl"
                        style={{ background: "radial-gradient(circle at 50% 12%, var(--spec-accent-10), transparent 70%)" }}
                      />
                    )}

                    <div className="relative mb-5 flex items-center gap-3">
                      <span className="h-px flex-1" style={{ background: "var(--spec-accent-12)" }} />
                      <StageIcon className="h-5 w-5 shrink-0" style={{ color: "var(--spec-accent-80)" }} strokeWidth={1.75} />
                      <p className="text-center font-cairo text-lg uppercase tracking-[0.2em]" style={{ color: "var(--spec-accent-80)" }}>
                        {bracketStageLabel[col.stage]}
                      </p>
                      {col.matches.length > 0 && (
                        <span
                          className="font-cairo text-[0.7rem] font-bold uppercase tracking-[0.2em] px-2 py-0.5 rounded-full"
                          style={{
                            color: decided === col.matches.length ? "var(--spec-accent)" : "var(--spec-text-dim)",
                            background: decided === col.matches.length ? "var(--spec-accent-12)" : "var(--spec-surface-glass-2)",
                            border: "1px solid var(--spec-accent-22)",
                          }}
                        >
                          {decided}/{col.matches.length}
                        </span>
                      )}
                      <span className="h-px flex-1" style={{ background: "var(--spec-accent-12)" }} />
                    </div>

                    <div className="relative flex-1 min-h-0 w-full flex flex-col justify-around gap-4">
                      {col.matches.length === 0 ? (
                        <p className="font-cairo text-[var(--spec-text-faint)] uppercase tracking-widest text-center text-sm">
                          No matches yet
                        </p>
                      ) : (
                        col.matches.map((m) => (
                          <BracketMatchCard key={m.matchId} match={m} variant={variant} />
                        ))
                      )}
                    </div>
                  </div>
                </Fragment>
              );
            })}
          </div>
        </div>
      )}

      {champion && (
        <div
          className="shrink-0 text-center pt-4 pb-8"
          style={{ background: "radial-gradient(ellipse 38% 80% at 50% 0%, var(--spec-accent-14), transparent 72%)" }}
        >
          <Trophy className="mx-auto mb-2 h-9 w-9" style={{ color: "var(--spec-accent-75)" }} strokeWidth={1.5} />
          <p className="font-cairo text-xs uppercase tracking-[0.4em] text-[var(--spec-text-muted)]">Champion</p>
          <p className="mt-1 font-cairo text-[2.25rem] font-bold leading-none bg-gradient-to-r from-[var(--spec-accent)] to-[var(--spec-title-end)] bg-clip-text text-transparent">
            {champion}
          </p>
        </div>
      )}
    </div>
  );
}

function ColumnConnector() {
  return (
    <div className="relative flex w-8 shrink-0 self-stretch items-center justify-center">
      <div
        className="absolute inset-y-[8%] left-1/2 w-px -translate-x-1/2"
        style={{
          background:
            "linear-gradient(180deg, transparent 0%, var(--spec-accent-18) 30%, var(--spec-accent-40) 50%, var(--spec-accent-18) 70%, transparent 100%)",
        }}
      />
      <ChevronRight className="relative h-5 w-5" style={{ color: "var(--spec-accent-45)" }} strokeWidth={1.75} />
    </div>
  );
}

function BracketMatchCard({ match, variant }: { match: BracketMatchSummary; variant: "qf" | "sf" | "final" }) {
  const decided = match.winner !== "—";
  const redWon = decided && match.winner === match.athleteRed;
  const blueWon = decided && match.winner === match.athleteBlue;
  const live = !!match.startedAt && !decided;

  const isFinal = variant === "final";
  const fontSize =
    isFinal ? "text-[2rem]" : variant === "sf" ? "text-[1.5rem]" : "text-[1.3rem]";
  const maxWidth = isFinal ? "27rem" : variant === "sf" ? "23rem" : "19rem";

  return (
    <div
      className={`mx-auto w-full rounded-xl px-5 py-4 font-cairo ${fontSize} animate-fade-up`}
      style={{
        maxWidth,
        background: decided
          ? "linear-gradient(180deg, var(--spec-accent-10), var(--spec-surface-glass-2))"
          : "var(--spec-surface-glass-2)",
        border: decided
          ? "1px solid var(--spec-accent-45)"
          : isFinal
            ? "1px solid var(--spec-accent-22)"
            : "1px solid var(--spec-border-glass)",
        boxShadow: decided ? "0 0 40px var(--spec-accent-12)" : undefined,
        transition: "background 300ms, border 300ms, box-shadow 300ms",
      }}
    >
      <div className="space-y-2.5">
        <AthleteRow name={match.athleteRed} dot="var(--spec-red)" won={redWon} faded={blueWon} />
        <AthleteRow name={match.athleteBlue} dot="var(--spec-blue)" won={blueWon} faded={redWon} />
      </div>

      {!decided && (
        <div className="mt-3.5 flex h-6 items-center justify-center">
          {live ? (
            <span className="flex items-center gap-2 font-bold uppercase tracking-[0.3em]" style={{ color: "var(--spec-green)" }}>
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full opacity-70" style={{ background: "var(--spec-green)" }} />
                <span className="relative inline-flex h-2 w-2 rounded-full" style={{ background: "var(--spec-green)" }} />
              </span>
              Live
            </span>
          ) : (
            <p className="font-cairo text-sm uppercase tracking-widest text-[var(--spec-text-dim)] text-center">TBD</p>
          )}
        </div>
      )}
    </div>
  );
}

function AthleteRow({ name, dot, won, faded }: { name: string; dot: string; won: boolean; faded: boolean }) {
  return (
    <p
      className="flex items-center gap-3 font-semibold leading-none"
      style={{
        color: won ? "var(--spec-accent)" : "var(--spec-text-bright)",
        opacity: faded ? 0.35 : 1,
        transition: "opacity 300ms, color 300ms",
      }}
    >
      <span className="h-2 w-2 shrink-0 rounded-full" style={{ background: dot, opacity: faded ? 0.45 : 1 }} />
      <span className="truncate">{name}</span>
      {won && <Crown className="ml-auto h-[0.8em] w-[0.8em] shrink-0" style={{ color: "var(--spec-accent)" }} strokeWidth={2} />}
    </p>
  );
}