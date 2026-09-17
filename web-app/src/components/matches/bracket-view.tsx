import { useState } from "react";
import type { Athlete } from "@/data/athletes";
import type { Routine } from "@/data/routines";
import type { Bracket, BracketStage } from "@/data/tournaments";
import type { Match, MatchProgress } from "@/data/matches";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge as ShadcnBadge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Plus, RefreshCw } from "lucide-react";
import { MatchCard } from "./match-card";
import { CreateMatchDialog } from "./create-match-dialog";
import { Reveal } from "@/components/shared/reveal";

interface BracketViewProps {
  brackets: Bracket[];
  matches: Match[];
  progresses: Record<number, MatchProgress>;
  athletes: Athlete[];
  routines: Routine[];
  onRefresh: () => void;
  onCreateBracket: (division: string, stage: BracketStage) => void;
  onMatchCreated: (match: Match) => void;
  onStartMatch: (match: Match) => void;
  onDeleteMatch: (match: Match) => void;
}

const stages: BracketStage[] = ["QUALIFIERS", "QUARTERFINALS", "SEMIFINALS", "FINALS"];

const stageLabels: Record<BracketStage, string> = {
  QUALIFIERS: "Qualifiers",
  QUARTERFINALS: "Quarterfinals",
  SEMIFINALS: "Semifinals",
  FINALS: "Finals",
};

export function BracketView({
  brackets,
  matches,
  progresses,
  athletes,
  routines,
  onRefresh,
  onCreateBracket,
  onMatchCreated,
  onStartMatch,
  onDeleteMatch,
}: BracketViewProps) {
  const [createMatchFor, setCreateMatchFor] = useState<Bracket | null>(null);
  const [addingDivision, setAddingDivision] = useState(false);
  const [newDivision, setNewDivision] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<Match | null>(null);
  const [deleteSaving, setDeleteSaving] = useState(false);

  const divisions = Array.from(new Set(brackets.map((b) => b.division)));
  const [pickedDivision, setPickedDivision] = useState<string | null>(null);
  const activeDivision =
    pickedDivision && divisions.includes(pickedDivision) ? pickedDivision : (divisions[0] ?? "");

  function getBracket(division: string, stage: BracketStage) {
    return brackets.find((b) => b.division === division && b.stage === stage);
  }

  function getMatchesForBracket(bracketId: number) {
    return matches.filter((m) => m.bracketId === bracketId);
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    setDeleteSaving(true);
    try {
      await onDeleteMatch(deleteTarget);
      setDeleteTarget(null);
    } finally {
      setDeleteSaving(false);
    }
  }

  function renderStages(division: string) {
    return stages.map((stage) => {
      const bracket = getBracket(division, stage);
      const bracketMatches = bracket ? getMatchesForBracket(bracket.id) : [];

      return (
        <div key={stage} className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-medium">{stageLabels[stage]}</h3>
              {bracket && (
                <ShadcnBadge variant="outline" className="text-xs">
                  {bracketMatches.length} matches
                </ShadcnBadge>
              )}
            </div>
            {bracket ? (
              <Button
                size="sm"
                variant="outline"
                onClick={() => setCreateMatchFor(bracket)}
              >
                <Plus className="w-3.5 h-3.5 mr-1" />
                Add match
              </Button>
            ) : (
              <Button
                size="sm"
                variant="outline"
                onClick={() => onCreateBracket(division, stage)}
              >
                <Plus className="w-3.5 h-3.5 mr-1" />
                Create bracket
              </Button>
            )}
          </div>

          {bracket ? (
            bracketMatches.length === 0 ? (
              <div className="flex items-center justify-center h-20 border border-dashed rounded-lg">
                <p className="text-sm text-muted-foreground">No matches yet</p>
              </div>
            ) : (
<div className="space-y-2">
                {bracketMatches.map((match, i) => (
                  <Reveal key={match.id} delay={Math.min(i, 5) * 0.03}>
                    <MatchCard
                      match={match}
                      progress={progresses[match.id]}
                      athletes={athletes}
                      routines={routines}
                      onStartMatch={onStartMatch}
                      onDeleteMatch={(match) => setDeleteTarget(match)}
                    />
                  </Reveal>
                ))}
              </div>
            )
          ) : (
            <div className="flex items-center justify-center h-20 border border-dashed rounded-lg">
              <p className="text-sm text-muted-foreground">No bracket created yet</p>
            </div>
          )}
        </div>
      );
    });
  }

  return (
    <>
      <div className="flex items-center gap-2">
        {divisions.length > 0 && (
          <Tabs value={activeDivision} onValueChange={setPickedDivision}>
            <TabsList>
              {divisions.map((division) => (
                <TabsTrigger key={division} value={division}>{division}</TabsTrigger>
              ))}
            </TabsList>
          </Tabs>
        )}
        {addingDivision ? (
          <input
            autoFocus
            value={newDivision}
            onChange={(e) => setNewDivision(e.target.value)}
            placeholder="Division name"
            className="h-8 px-2 w-40 rounded text-sm border bg-transparent"
            style={{ border: "1px solid var(--border)", color: "var(--foreground)" }}
            onKeyDown={(e) => {
              if (e.key === "Enter" && newDivision.trim()) {
                onCreateBracket(newDivision.trim(), "QUALIFIERS");
                setNewDivision("");
                setAddingDivision(false);
              }
              if (e.key === "Escape") {
                setNewDivision("");
                setAddingDivision(false);
              }
            }}
            onBlur={() => {
              setNewDivision("");
              setAddingDivision(false);
            }}
          />
        ) : (
          <button
            onClick={() => setAddingDivision(true)}
            title="Add division"
            className="p-1.5 rounded transition-colors"
            style={{ color: "var(--muted-foreground)", border: "1px solid var(--border)", background: "var(--secondary)" }}
            onMouseEnter={(e) => (e.currentTarget.style.color = "var(--accent)")}
            onMouseLeave={(e) => (e.currentTarget.style.color = "var(--muted-foreground)")}
          >
            <Plus className="w-3.5 h-3.5" />
          </button>
        )}
        <button
          onClick={onRefresh}
          title="Refresh matches"
          className="p-1.5 rounded transition-colors"
          style={{ color: "var(--muted-foreground)", border: "1px solid var(--border)", background: "var(--secondary)" }}
          onMouseEnter={(e) => (e.currentTarget.style.color = "var(--accent)")}
          onMouseLeave={(e) => (e.currentTarget.style.color = "var(--muted-foreground)")}
        >
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>

      {divisions.length === 0 ? (
        <div className="flex items-center justify-center h-20 border border-dashed rounded-lg mt-4">
          <p className="text-sm text-muted-foreground">
            No divisions yet — press + to add one and start building brackets
          </p>
        </div>
      ) : (
        <Tabs value={activeDivision} onValueChange={setPickedDivision} className="mt-4">
          {divisions.map((division) => (
            <TabsContent key={division} value={division} className="space-y-6">
              {renderStages(division)}
            </TabsContent>
          ))}
        </Tabs>
      )}

      {createMatchFor && (
        <CreateMatchDialog
          open={!!createMatchFor}
          bracket={createMatchFor}
          routines={routines}
          athletes={athletes}
          onClose={() => setCreateMatchFor(null)}
          onCreated={(match) => {
            onMatchCreated(match);
            setCreateMatchFor(null);
          }}
        />
      )}

      <Dialog
        open={deleteTarget != null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
      >
        <DialogContent style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
          <DialogHeader>
            <DialogTitle>Delete match?</DialogTitle>
            <DialogDescription>
              {deleteTarget ? (
                <>
                  {(() => {
                    const red = athletes.find((a) => a.id === deleteTarget.athleteRedId);
                    const blue = athletes.find((a) => a.id === deleteTarget.athleteBlueId);
                    const label =
                      red && blue ? `${red.name} vs ${blue.name}` : `#${deleteTarget.id}`;
                    return <>Are you sure you want to delete the match <strong>{label}</strong>? This also deletes the linked progress and cannot be undone.</>;
                  })()}
                </>
              ) : null}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              disabled={deleteSaving}
              onClick={() => setDeleteTarget(null)}
            >
              Cancel
            </Button>
            <Button
              disabled={deleteSaving}
              onClick={() => void confirmDelete()}
              style={{ background: "var(--danger)", color: "var(--danger-foreground, #fff)" }}
            >
              {deleteSaving ? "Deleting…" : "Delete"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
