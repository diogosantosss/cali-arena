export function ScreenHeader({
  tournamentName,
  subtitle,
  accent = "var(--spec-accent)",
}: {
  tournamentName: string;
  subtitle?: string;
  accent?: string;
}) {
  return (
    <header className="pt-14 pb-8 px-6 text-center shrink-0">
      <p
        className="text-[0.65rem] tracking-[0.45em] uppercase"
        style={{ color: "var(--spec-text-dim)" }}
      >
        Cali Arena
      </p>
      <h1 className="mt-3 font-cairo text-[2.75rem] font-bold uppercase tracking-wide leading-tight text-white">
        {tournamentName}
      </h1>
      {subtitle != null && (
        <p className="mt-2 font-cairo text-lg font-semibold uppercase tracking-widest text-[var(--spec-text-soft)]">
          {subtitle}
        </p>
      )}
      <div className="mx-auto mt-4 h-px w-20" style={{ background: accent }} />
    </header>
  );
}