import { screenBackground } from "../lib/screen-background";

export function WaitingScreen({ tournamentName }: { tournamentName: string }) {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center" style={screenBackground}>
      <div className="relative">
        <span className="absolute -inset-2 rounded-[28px] border border-[#e8a020]/40 animate-ping" />
        <div
          className="relative flex h-24 w-24 items-center justify-center rounded-[28px] bg-gradient-to-br from-[#e8a020] to-[#f0ede8]"
          style={{ boxShadow: "0 0 70px rgba(232,160,32,0.35)" }}
        >
          <span className="font-cairo text-5xl font-bold text-[#0f0f11]">C</span>
        </div>
      </div>

      <p className="mt-12 font-cairo text-5xl font-semibold uppercase tracking-wide bg-gradient-to-r from-[#e8a020] to-[#f0ede8] bg-clip-text text-transparent">
        {tournamentName}
      </p>

      <div className="mt-8 flex items-center gap-3 rounded-full border border-white/10 px-5 py-2">
        <span className="relative flex h-2.5 w-2.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full opacity-60" style={{ background: "#e8a020" }} />
          <span className="relative inline-flex h-2.5 w-2.5 rounded-full" style={{ background: "#e8a020" }} />
        </span>
        <p className="font-cairo text-sm uppercase tracking-[0.4em] text-white/40">Waiting for broadcast</p>
      </div>
    </div>
  );
}