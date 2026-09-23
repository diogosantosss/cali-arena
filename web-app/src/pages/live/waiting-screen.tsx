import { MonitorPlay } from "lucide-react";
import { screenBackground } from "@/utils/screen-background";
import { ScreenHeader } from "./screen-header";

export function WaitingScreen({ tournamentName }: { tournamentName: string }) {
  return (
    <div className="min-h-screen flex flex-col" style={screenBackground}>
      <ScreenHeader tournamentName={tournamentName} />

      <div className="relative flex-1 flex items-center justify-center pb-28">
        <div
          className="pointer-events-none absolute rounded-full"
          style={{
            width: 620,
            height: 620,
            background:
              "radial-gradient(ellipse 50% 50% at 50% 50%, var(--spec-accent-10) 0%, transparent 70%)",
          }}
        />

        <div className="relative flex flex-col items-center gap-8">
          <div className="relative flex h-28 w-28 items-center justify-center">
            <span
              className="absolute inset-0 rounded-full border animate-pulse"
              style={{ borderColor: "var(--spec-accent-22)" }}
            />
            <span className="absolute inset-4 rounded-full" style={{ background: "var(--spec-accent-12)" }} />
            <MonitorPlay
              className="relative h-10 w-10 animate-pulse"
              style={{ color: "var(--spec-accent)" }}
              strokeWidth={1.5}
            />
          </div>

          <p className="font-cairo text-[2.25rem] font-bold uppercase tracking-[0.3em] text-white">
            Waiting for broadcast
          </p>

          <div className="-mt-2 flex items-center gap-2">
            {[0, 1, 2].map((i) => (
              <span
                key={i}
                className="h-2 w-2 rounded-full animate-pulse"
                style={{ background: "var(--spec-accent-45)", animationDelay: `${i * 220}ms` }}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}