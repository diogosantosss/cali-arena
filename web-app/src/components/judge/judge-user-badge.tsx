import { Crown, Gavel, Shield } from "lucide-react";
import { useAuth } from "@/hooks/use-auth";
import type { UserRole } from "@/data/auth";

const roleIcons: Record<UserRole, typeof Shield> = {
  ADMIN: Shield,
  HOST: Crown,
  JUDGE: Gavel,
};

export function JudgeUserBadge() {
  const { user } = useAuth();
  if (!user) return null;

  const RoleIcon = roleIcons[user.role];

  return (
    <span
      className="flex items-center gap-1.5 text-[11px] px-2 py-1 rounded-lg shrink-0 max-w-[45vw] sm:max-w-none"
      style={{ background: "var(--secondary)", color: "var(--secondary-foreground)", border: "1px solid var(--border)" }}
    >
      <RoleIcon className="w-3 h-3 shrink-0" />
      <span className="truncate font-medium">{user.username}</span>
      <span className="opacity-60">·</span>
      <span className="tracking-wider uppercase opacity-70">{user.role}</span>
    </span>
  );
}
