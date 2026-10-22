import type { OverlayProps, OverlaySide } from "@/data/overlay";
import type { OverlayExerciseItem } from "@/utils/exercise-labels";
import { Trophy } from "lucide-react";
import { useElapsedMs } from "@/hooks/use-elapsed-ms";
import { formatTime } from "@/utils/format-time";

type Variant = "band" | "plate" | "bare" | "bar" | "solid" | "card";

function ExerciseItems({ items, dim }: { items: OverlayExerciseItem[]; dim: string }) {
  if (items.length === 0) return <span>—</span>;
  return (
    <>
      {items.map((item, i) => (
        <span key={i}>
          {i > 0 && <span className={dim}> - </span>}
          <span className={item.active ? "text-white" : dim}>{item.label}</span>
        </span>
      ))}
    </>
  );
}

const PLATE: React.CSSProperties = {
  background: "rgba(0,0,0,0.85)",
};

const BAND_H = "13rem";
const CUT = "5.5rem";
const ON_COLOR: React.CSSProperties = { textShadow: "0 2px 18px rgba(0,0,0,0.55)" };

export function Overlay({ data, mode = "live", connected = true, variant = "band" }: OverlayProps) {
  const elapsed = useElapsedMs(data.finished ? null : data.timerStartedAt);
  const time = data.finished || !data.timerStartedAt ? data.time : formatTime(elapsed);
  const waiting = data.waiting === true;

  if (variant === "band") {
    return <BandOverlay data={data} time={time} />;
  }

  const anim = (name: string) =>
    mode === "static" ? undefined : { animation: `${name} 0.4s cubic-bezier(0.22,1,0.36,1) both` };

  const onCard = variant === "card" || variant === "solid";
  const solid = variant === "solid";
  const plate = variant === "plate";

  return (
    <div className="relative h-screen w-screen overflow-hidden">
      {waiting ? (
        <WaitingState name={data.tournamentName} />
      ) : (
        <div className="absolute inset-x-0 top-0 flex justify-center py-5" style={anim("overlay-drop")}>
          {onCard ? (
            <div
              className={
                solid
                  ? "rounded-2xl bg-black/55 px-10 py-3 backdrop-blur-sm"
                  : "rounded-2xl bg-white/92 px-9 py-4"
              }
            >
              <p
                className={
                  solid
                    ? "font-cairo text-[4.5rem] font-extrabold leading-none tabular-nums text-white"
                    : "text-center font-cairo text-[4rem] font-extrabold leading-none tabular-nums text-black"
                }
              >
                {time}
              </p>
            </div>
          ) : (
            <div
              className="text-center"
              style={plate ? { ...PLATE, padding: "1.5rem 3rem", borderRadius: "1rem" } : undefined}
            >
              <p
                className="font-cairo text-[4.5rem] font-extrabold leading-none tabular-nums text-white"
                style={{ textShadow: "0 4px 30px rgba(0,0,0,0.7)" }}
              >
                {time}
              </p>
            </div>
          )}
        </div>
      )}

      {!waiting && (
        <>
          <div className="absolute bottom-10 left-10" style={anim("overlay-slide-left")}>
            <CornerCard side={data.red} variant={variant} />
          </div>

          <div className="absolute bottom-10 right-10" style={anim("overlay-slide-right")}>
            <CornerCard side={data.blue} variant={variant} />
          </div>
        </>
      )}

      <div
        className="absolute right-10 top-10 flex items-center gap-2"
        style={{ opacity: connected ? 0 : 1, transition: "opacity 200ms" }}
      >
        <span className="h-2 w-2 rounded-full bg-red-500" />
        <span className="font-cairo text-xs font-bold uppercase tracking-[0.2em] text-white">Sem ligação</span>
      </div>
    </div>
  );
}

function BandSide({ side, align }: { side: OverlaySide | null; align: "left" | "right" }) {
  const isRight = align === "right";
  const endPad = isRight ? "3.5rem" : CUT;
  const startPad = isRight ? CUT : "3.5rem";

  return (
    <div
      className="flex h-full flex-col justify-center"
      style={{ textAlign: align, paddingLeft: startPad, paddingRight: endPad }}
    >
      {side ? (
        <>
          <p
            className="font-cairo text-[2.75rem] font-extrabold uppercase leading-none tracking-tight text-white"
            style={ON_COLOR}
          >
            {side.name}
          </p>

          {side.finished ? (
            <p
              className="mt-1.5 flex items-center font-cairo text-[1.5rem] font-bold uppercase leading-none tracking-wide"
              style={{
                color: side.won ? "var(--spec-gold)" : "rgba(255,255,255,0.7)",
                justifyContent: align === "right" ? "flex-end" : "flex-start",
                ...ON_COLOR,
              }}
            >
              {side.won && <Trophy className="mr-2 h-5 w-5" strokeWidth={2.5} />}
              <span className="mr-3">Finished</span>
              <span className="tabular-nums text-white">{side.time}</span>
            </p>
          ) : (
            <>
              <p
                className="mt-1.5 font-cairo text-[1.5rem] font-bold uppercase leading-none tracking-wide text-white/75"
                style={ON_COLOR}
              >
                <ExerciseItems items={side.items} dim="text-white/35" />
              </p>
              {side.next && (
                <p
                  className="mt-1 font-cairo text-base font-bold uppercase leading-none tracking-wide text-white/45"
                  style={ON_COLOR}
                >
                  Next · <span className="text-white/65">{side.next}</span>
                </p>
              )}
              <p
                className="mt-1 font-cairo text-[3.5rem] font-extrabold leading-none tabular-nums text-white"
                style={ON_COLOR}
              >
                <span key={side.currentReps} className="inline-block animate-overlay-rep">
                  {side.currentReps}
                </span>
                <span className="text-white/50">/{side.targetReps}</span>
              </p>
            </>
          )}
        </>
      ) : (
        <p className="font-cairo text-[2.5rem] font-extrabold uppercase leading-none text-white/30" style={ON_COLOR}>
          —
        </p>
      )}
    </div>
  );
}

function BandOverlay({ data, time }: { data: OverlayProps["data"]; time: string }) {
  const waiting = data.waiting === true;
  return (
    <div className="relative h-screen w-screen overflow-hidden">
      {waiting ? (
        <WaitingState name={data.tournamentName} />
      ) : (
        <div
          className="absolute inset-x-0 top-0 flex justify-center py-5"
          style={{ animation: "overlay-drop 0.4s cubic-bezier(0.22,1,0.36,1) both" }}
        >
          <div className="flex flex-col items-center gap-1.5 rounded-2xl bg-black/80 px-10 py-3">
            <p className="font-cairo text-[4.5rem] font-extrabold leading-none tabular-nums text-white">
              {time}
            </p>
            {(data.stage || data.timeCap) && (
              <p className="font-cairo text-sm font-bold uppercase leading-none tracking-[0.25em] text-white/45">
                {data.stage && <span>{data.stage}</span>}
                {data.stage && data.timeCap && <span className="px-2.5 text-white/25">·</span>}
                {data.timeCap && <span>Time cap {data.timeCap}</span>}
              </p>
            )}
          </div>
        </div>
      )}

      {!waiting && (
        <>
          <div
            className="absolute bottom-0 left-0 w-[38%]"
            style={{
              height: BAND_H,
              background:
                "linear-gradient(95deg, color-mix(in srgb, var(--spec-red) 85%, black) 0%, color-mix(in srgb, var(--spec-red) 60%, transparent) 42%, transparent 74%)",
              clipPath: `polygon(0 0, 100% 0, calc(100% - ${CUT}) 100%, 0 100%)`,
            }}
          >
            <BandSide side={data.red} align="left" />
          </div>

          <div
            className="absolute bottom-0 right-0 w-[38%]"
            style={{
              height: BAND_H,
              background:
                "linear-gradient(265deg, color-mix(in srgb, var(--spec-blue) 85%, black) 0%, color-mix(in srgb, var(--spec-blue) 60%, transparent) 42%, transparent 74%)",
              clipPath: `polygon(${CUT} 0, 100% 0, 100% 100%, 0 100%)`,
            }}
          >
            <BandSide side={data.blue} align="right" />
          </div>
        </>
      )}
    </div>
  );
}

function WaitingState({ name }: { name: string | null | undefined }) {
  return (
    <div
      className="absolute inset-x-0 top-0 flex flex-col items-center py-6"
      style={{ animation: "overlay-drop 0.4s cubic-bezier(0.22,1,0.36,1) both" }}
    >
      <p
        className="font-cairo text-[2.5rem] font-extrabold uppercase leading-none tracking-[0.14em] text-white"
        style={{ textShadow: "0 4px 30px rgba(0,0,0,0.7)" }}
      >
        Waiting for next match
      </p>
      {name && (
        <p className="mt-3 font-cairo text-base font-bold uppercase leading-none tracking-[0.3em] text-white/45">
          {name}
        </p>
      )}
    </div>
  );
}

function CornerCard({ side, variant }: { side: OverlaySide | null; variant: Variant }) {
  const plate = variant === "plate";
  const solid = variant === "solid";
  const onCard = variant === "card";

  if (!side) {
    return (
      <div
        className="flex h-28 w-72 items-center justify-center"
        style={plate ? { ...PLATE, padding: "1.25rem 2.5rem", borderRadius: "1rem" } : undefined}
      >
        <span
          className={
            onCard
              ? "rounded-2xl bg-white/92 px-6 py-3 font-cairo text-xs font-bold uppercase tracking-[0.2em] text-black/25"
              : "font-cairo text-xs font-bold uppercase tracking-[0.2em] text-white/30"
          }
        >
          Sem atleta
        </span>
      </div>
    );
  }

  if (solid) {
    return (
      <div className="w-72 overflow-hidden rounded-2xl bg-black/55 text-center backdrop-blur-sm">
        <div className="px-6 pt-5 pb-6">
          <p
            className="font-cairo text-[1.625rem] font-extrabold uppercase leading-tight tracking-wide text-white"
            style={{ textShadow: `0 2px 18px ${side.color}` }}
          >
            {side.won && <Trophy className="mr-2 inline h-5 w-5 align-[-0.15em]" strokeWidth={2.5} style={{ color: "var(--spec-gold)" }} />}
            {side.name}
          </p>
          {side.finished ? (
            <p className="mt-3 font-cairo text-3xl font-extrabold leading-none tabular-nums text-white/45">
              {side.won && <Trophy className="mr-2 inline h-5 w-5 align-[-0.1em]" strokeWidth={2.5} style={{ color: "var(--spec-gold)" }} />}
              {side.time}
            </p>
          ) : (
            <>
              <p className="mt-2.5 font-cairo text-lg font-bold leading-tight text-white/60">
                <ExerciseItems items={side.items} dim="text-white/30" />
              </p>
              {side.next && (
                <p className="mt-1.5 font-cairo text-sm font-bold leading-none text-white/40">
                  Next · <span className="text-white/65">{side.next}</span>
                </p>
              )}
              <p className="mt-2 font-cairo text-[3.25rem] font-extrabold leading-none tabular-nums text-white">
                <span key={side.currentReps} className="inline-block animate-overlay-rep">
                  {side.currentReps}
                </span>
                <span className="text-white/25">/{side.targetReps}</span>
              </p>
            </>
          )}
        </div>
        <div className="h-1.5 w-full" style={{ background: side.color }} />
      </div>
    );
  }

  return (
    <div
      className={plate ? "w-96 rounded-2xl" : "w-96"}
      style={plate ? { ...PLATE, padding: "1.5rem 2rem" } : undefined}
    >
      <p
        className="font-cairo text-[1.75rem] font-extrabold uppercase leading-none tracking-wide"
        style={{ color: side.color, textShadow: "0 2px 20px rgba(0,0,0,0.9)" }}
      >
        {side.won && <Trophy className="mr-2 inline h-5 w-5 align-[-0.15em]" strokeWidth={2.5} style={{ color: "var(--spec-gold)" }} />}
        {side.name}
      </p>
      {side.finished ? (
        <p
          className="mt-1 font-cairo text-2xl font-bold leading-none text-white/50"
          style={{ textShadow: "0 2px 14px rgba(0,0,0,0.9)" }}
        >
          {side.won && <Trophy className="mr-2 inline h-5 w-5 align-[-0.1em]" strokeWidth={2.5} style={{ color: "var(--spec-gold)" }} />}
          <span className="uppercase tracking-[0.2em]">Finished</span>{" "}
          <span
            className="ml-1 font-extrabold tabular-nums"
            style={{ color: side.won ? side.color : "rgba(255,255,255,0.75)" }}
          >
            {side.time}
          </span>
        </p>
      ) : (
        <p
          className="mt-1 font-cairo text-2xl font-bold leading-none text-white/80"
          style={{ textShadow: "0 2px 14px rgba(0,0,0,0.9)" }}
        >
          <ExerciseItems items={side.items} dim="text-white/40" />
          <span className="whitespace-nowrap text-white/50">
            {" "}
            <span key={side.currentReps} className="inline-block animate-overlay-rep text-white">
              {side.currentReps}
            </span>
            /{side.targetReps}
          </span>
        </p>
      )}
      {!side.finished && side.next && (
        <p
          className="mt-1 font-cairo text-base font-bold leading-none text-white/35"
          style={{ textShadow: "0 2px 14px rgba(0,0,0,0.9)" }}
        >
          Next · <span className="text-white/60">{side.next}</span>
        </p>
      )}
    </div>
  );
}
