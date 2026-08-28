import { screenBackground } from "../lib/screen-background";

export function ScreenLoading({ label = "Loading..." }: { label?: string }) {
  return (
    <div className="min-h-screen flex items-center justify-center" style={screenBackground}>
      <p className="text-white/30 uppercase tracking-widest">{label}</p>
    </div>
  );
}