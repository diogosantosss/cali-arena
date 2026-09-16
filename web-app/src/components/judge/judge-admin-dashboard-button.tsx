import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { LayoutDashboard } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";

export function JudgeAdminDashboardButton() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  function handleGo() {
    setOpen(false);
    navigate("/dashboard");
  }

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        aria-label="Go to admin dashboard"
        title="Go to admin dashboard"
        className="p-2 rounded-lg select-none touch-manipulation transition-colors active:opacity-80"
        style={{ background: "var(--secondary)", color: "var(--secondary-foreground)", border: "1px solid var(--border)", WebkitTapHighlightColor: "transparent" }}
      >
        <LayoutDashboard className="w-4 h-4" />
      </button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Go to admin dashboard?</DialogTitle>
            <DialogDescription>
              Are you sure you want to leave the judge screen and open the admin dashboard?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
            <Button onClick={handleGo}>Go to dashboard</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}