import { Link, useLocation } from "react-router-dom";

export function NotFoundPage() {
  const { pathname } = useLocation();

  return (
    <div
      className="min-h-screen flex items-center justify-center px-6"
      style={{ background: "var(--background)", color: "var(--foreground)" }}
    >
      <div className="text-center">
        <p className="text-xs uppercase tracking-widest" style={{ color: "var(--muted-foreground)" }}>
          Error 404
        </p>
        <h1 className="mt-4 text-6xl font-extrabold tracking-tight leading-none" style={{ color: "var(--accent)" }}>
          404
        </h1>
        <p className="mt-3 text-lg font-medium" style={{ color: "var(--secondary-foreground)" }}>
          Page not found
        </p>
        <p className="mt-1.5 text-sm" style={{ color: "var(--muted-foreground)" }}>
          <span className="text-xs font-mono tabular-nums">{pathname}</span> doesn't exist or was moved.
        </p>
        <Link
          to="/dashboard"
          className="inline-flex items-center gap-2 mt-7 px-5 h-9 rounded-lg text-sm font-medium transition-all"
          style={{ background: "var(--accent)", color: "var(--accent-foreground)" }}
        >
          Go to dashboard
        </Link>
      </div>
    </div>
  );
}