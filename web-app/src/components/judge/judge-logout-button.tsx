import { useState } from "react";
import { LogOut } from "lucide-react";
import { useAuth } from "@/hooks/use-auth";
import { Button } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";

export function JudgeLogoutButton() {
  const { logout } = useAuth();
  const [open, setOpen] = useState(false);

  function handleLogout() {
    setOpen(false);
    logout();
  }

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        aria-label="Log out"
        className="p-2 rounded-lg select-none touch-manipulation transition-colors active:opacity-80"
        style={{ background: "var(--secondary)", color: "var(--secondary-foreground)", border: "1px solid var(--border)", WebkitTapHighlightColor: "transparent" }}
      >
        <LogOut className="w-4 h-4" />
      </button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Log out?</DialogTitle>
            <DialogDescription>
              Are you sure you want to log out?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
            <Button variant="destructive" onClick={handleLogout}>
              Log out
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}