import { useEffect, useRef, useState } from "react";
import { Crown, Trophy } from "lucide-react";
import type { BracketMatchSummary, BracketStage, TournamentBracketsSummary } from "@/data/tournaments";
import { ScreenHeader } from "./screen-header";

const C = {
  bg: "#0D0D0F",
  card: "#111113",
  text: "rgba(255,255,255,0.94)",
  textSoft: "rgba(255,255,255,0.72)",
  textMuted: "rgba(255,255,255,0.5)",
  textFaint: "rgba(255,255,255,0.28)",
  line: "rgba(255,255,255,0.16)",
  accent: "#E98A80",
  accentSoft: "rgba(233,138,128,0.1)",
  blue: "#7C96C4",
  blueDim: "rgba(124,150,196,0.9)",
};

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

interface Point {
  left: number;
  right: number;
  y: number;
}

type Key = string;

export function BracketsScreen({ tournamentName, summary }: {
  tournamentName: string;
  summary: TournamentBracketsSummary;
}) {
  const columns = summary.brackets
    .filter((b) => b.stage !== "QUALIFIERS")
    .sort((a, b) => bracketStageOrder[a.stage] - bracketStageOrder[b.stage]);

  const finalMatch = columns.find((c) => c.stage === "FINALS")?.matches[0];
  const champion = finalMatch && finalMatch.winner !== "—" ? finalMatch.winner : null;

  const rootRef = useRef<HTMLDivElement>(null);
  const [points, setPoints] = useState<Record<Key, Point>>({});

  const stageKey = (stage: BracketStage, index: number) => `${stage}:${index}`;

  useEffect(() => {
    function compute() {
      const root = rootRef.current;
      if (!root) return;
      const rootRect = root.getBoundingClientRect();
      const next: Record<Key, Point> = {};
      root.querySelectorAll<HTMLElement>("[data-match]").forEach((el) => {
        const key = el.dataset.match;
        if (!key) return;
        const rect = el.getBoundingClientRect();
        next[key] = {
          left: rect.left - rootRect.left,
          right: rect.right - rootRect.left,
          y: rect.top + rect.height / 2 - rootRect.top,
        };
      });
      setPoints((prev) => (JSON.stringify(prev) === JSON.stringify(next) ? prev : next));
    }

    compute();
    const observer = new ResizeObserver(compute);
    if (rootRef.current) observer.observe(rootRef.current);
    window.addEventListener("resize", compute);
    return () => {
      observer.disconnect();
      window.removeEventListener("resize", compute);
    };
  }, [columns]);

  function groupIndices(upCount: number, downCount: number): number[][] {
    if (upCount === 0 || downCount === 0) return [];
    const size = Math.ceil(upCount / downCount);
    const groups: number[][] = [];
    for (let i = 0; i < downCount; i++) {
      const start = i * size;
      groups.push(Array.from({ length: Math.min(size, upCount - start) }, (_, j) => start + j));
    }
    return groups;
  }

  function connectorPath(ups: Point[], down: Point): string {
    if (ups.length === 0) return "";
    // eixo vertical comum entre os cards de cima e de baixo + entrada lateral esquerda no card de destino
    const midX = ups[0].right + (down.left - ups[0].right) / 2;
    const ys = [...ups.map((u) => u.y), down.y];
    const topY = Math.min(...ys);
    const bottomY = Math.max(...ys);
    let d = `M ${midX} ${topY} V ${bottomY}`;
    for (const u of ups) {
      d += ` M ${u.right} ${u.y} H ${midX}`;
    }
    d += ` M ${midX} ${down.y} H ${down.left}`;
    return d;
  }

  const qfKeys = columns.find((c) => c.stage === "QUARTERFINALS")?.matches.map((_, i) => stageKey("QUARTERFINALS", i)) ?? [];
  const sfKeys = columns.find((c) => c.stage === "SEMIFINALS")?.matches.map((_, i) => stageKey("SEMIFINALS", i)) ?? [];
  const finalKeys = columns.find((c) => c.stage === "FINALS")?.matches.map((_, i) => stageKey("FINALS", i)) ?? [];

  function resolve(groups: number[][], upKeys: string[], downKeys: string[]) {
    return groups
      .map((group, downIdx) => ({
        ups: group.map((i) => points[upKeys[i]]).filter((p): p is Point => !!p),
        down: points[downKeys[downIdx]],
      }))
      .filter((p) => p.ups.length > 0 && !!p.down);
  }

  const connectors = [
    ...resolve(groupIndices(qfKeys.length, sfKeys.length), qfKeys, sfKeys).map((p) => connectorPath(p.ups, p.down)),
    ...resolve(groupIndices(sfKeys.length, finalKeys.length), sfKeys, finalKeys).map((p) => connectorPath(p.ups, p.down)),
  ];

  return (
    <div className="h-screen flex flex-col overflow-hidden" style={{ background: C.bg, color: C.text }}>
      <ScreenHeader tournamentName={tournamentName} subtitle={`Brackets · ${summary.division}`} accent={C.accent} />

      {columns.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <p className="font-cairo text-[var(--spec-text-ghost)] uppercase tracking-widest text-lg">No brackets yet</p>
        </div>
      ) : (
        <div className="flex-1 min-h-0 flex items-stretch justify-center px-20 py-6">
          <div ref={rootRef} className="relative flex w-full max-w-[86rem] min-h-0 gap-[6rem]">
            {columns.map((col) => (
              <div key={col.stage} className="flex-1 min-h-0 flex flex-col">
                <div className="mb-6">
                  <StageHeader label={bracketStageLabel[col.stage]} decided={col.matches.filter((m) => m.winner !== "—").length} total={col.matches.length} />
                </div>
                <div className="flex-1 min-h-0 flex flex-col justify-around gap-5">
                  {col.matches.length === 0 ? (
                    <p className="font-cairo text-[var(--spec-text-faint)] uppercase tracking-widest text-center text-sm">
                      No matches yet
                    </p>
                  ) : (
                    col.matches.map((match, i) => (
                      <div key={match.matchId} data-match={stageKey(col.stage, i)} className="flex items-center">
                        <BracketMatchRow match={match} />
                      </div>
                    ))
                  )}
                </div>
              </div>
            ))}

            <svg
              className="pointer-events-none absolute inset-0 h-full w-full"
              aria-hidden
            >
              {connectors.map((d, i) => (
                <path key={i} d={d} fill="none" stroke={C.line} strokeWidth={1.5} />
              ))}
            </svg>
          </div>
        </div>
      )}

      {champion && (
        <div className="shrink-0 text-center pt-4 pb-10">
          <Trophy className="mx-auto mb-2 h-6 w-6" style={{ color: C.accent }} strokeWidth={1.5} />
          <p className="font-cairo text-[0.6rem] uppercase tracking-[0.4em] text-[var(--spec-text-muted)]">Champion</p>
          <p className="mt-1 font-cairo text-[2rem] font-bold leading-none text-white">{champion}</p>
        </div>
      )}
    </div>
  );
}

function StageHeader({ label, decided, total }: { label: string; decided: number; total: number }) {
  const complete = total > 0 && decided === total;
  return (
    <div>
      <div className="flex items-center justify-center gap-3">
        <p className="font-cairo text-[1.05rem] uppercase tracking-[0.28em]" style={{ color: complete ? C.accent : C.textSoft }}>
          {label}
        </p>
        <span
          className="rounded-full px-2 py-0.5 font-cairo text-[0.62rem] font-semibold tracking-widest"
          style={{
            color: complete ? C.accent : C.textMuted,
            background: complete ? C.accentSoft : "rgba(255,255,255,0.05)",
            border: `1px solid ${complete ? C.accent : "rgba(255,255,255,0.09)"}`,
          }}
        >
          {decided}/{total}
        </span>
      </div>
      <div className="mt-3 h-px w-full" style={{ background: C.line }} />
    </div>
  );
}

function BracketMatchRow({ match }: { match: BracketMatchSummary }) {
  const decided = match.winner !== "—";
  const redWon = decided && match.winner === match.athleteRed;
  const blueWon = decided && match.winner === match.athleteBlue;

  return (
    <div
      className="w-full rounded-xl px-4 py-2.5 font-cairo text-[1.05rem]"
      style={{
        background: C.card,
        border: `1px solid ${decided ? C.accent : "rgba(255,255,255,0.07)"}`,
        transition: "border 300ms",
      }}
    >
      <div className="flex flex-col gap-1.5">
        <AthleteRow name={match.athleteRed} isSecond={false} decided={decided} won={redWon} />
        <AthleteRow name={match.athleteBlue} isSecond decided={decided} won={blueWon} />
      </div>
    </div>
  );
}

function AthleteRow({
  name,
  isSecond,
  decided,
  won,
}: {
  name: string;
  isSecond: boolean;
  decided: boolean;
  won: boolean;
}) {
  const dotColor = won ? C.accent : isSecond ? C.blueDim : C.textFaint;
  return (
    <div
      className="flex items-center gap-3 rounded-lg px-2 py-1 font-semibold leading-none"
      style={{
        color: won ? "#ffffff" : decided ? C.textMuted : C.textSoft,
        background: won ? C.accentSoft : undefined,
        transition: "color 300ms, background 300ms",
      }}
    >
      <span className="h-1.5 w-1.5 shrink-0 rounded-full" style={{ background: dotColor }} />
      <span className="truncate">{name}</span>
      {won && <Crown className="ml-auto h-[0.85em] w-[0.85em] shrink-0" style={{ color: C.accent }} strokeWidth={2} />}
    </div>
  );
}