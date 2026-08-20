import { useState } from "react";
import type { Athlete, Bracket, BracketStage, Gender, Match, Routine, User } from "@/types";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Plus } from "lucide-react";
import { MatchCard } from "./MatchCard";
import { CreateMatchDialog } from "./CreateMatchDialog";
import { AssignAthletesDialog } from "./AssignAthletesDialog";

interface Props {
  brackets: Bracket[];
  matches: Match[];
  athletes: Athlete[];
  routines: Routine[];
  judges: User[];
  onCreateBracket: (gender: Gender, stage: BracketStage) => void;
  onMatchCreated: (match: Match) => void;
  onMatchUpdated: (match: Match) => void;
  onStartMatch: (match: Match) => void;
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
  athletes,
  routines,
  judges,
  onCreateBracket,
  onMatchCreated,
  onMatchUpdated,
  onStartMatch,
}: Props) {
  const [createMatchFor, setCreateMatchFor] = useState<Bracket | null>(null);
  const [assignAthletesFor, setAssignAthletesFor] = useState<Match | null>(null);

  function getBracket(gender: Gender, stage: BracketStage) {
    return brackets.find((b) => b.gender === gender && b.stage === stage);
  }

  function getMatchesForBracket(bracketId: number) {
    return matches.filter((m) => m.bracketId === bracketId);
  }

  function renderStages(gender: Gender) {
    return stages.map((stage) => {
      const bracket = getBracket(gender, stage);
      const bracketMatches = bracket ? getMatchesForBracket(bracket.id) : [];

      return (
        <div key={stage} className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-medium">{stageLabels[stage]}</h3>
              {bracket && (
                <Badge variant="outline" className="text-xs">
                  {bracketMatches.length} matches
                </Badge>
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
                onClick={() => onCreateBracket(gender, stage)}
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
                {bracketMatches.map((match) => (
                  <MatchCard
                    key={match.id}
                    match={match}
                    athletes={athletes}
                    routines={routines}
                    judges={judges}
                    onAssignAthletes={(m) => setAssignAthletesFor(m)}
                    onStartMatch={onStartMatch}
                  />
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
      <Tabs defaultValue="MALE">
        <TabsList>
          <TabsTrigger value="MALE">Male</TabsTrigger>
          <TabsTrigger value="FEMALE">Female</TabsTrigger>
        </TabsList>

        <TabsContent value="MALE" className="space-y-6 mt-4">
          {renderStages("MALE")}
        </TabsContent>

        <TabsContent value="FEMALE" className="space-y-6 mt-4">
          {renderStages("FEMALE")}
        </TabsContent>
      </Tabs>

      {createMatchFor && (
        <CreateMatchDialog
          open={!!createMatchFor}
          bracket={createMatchFor}
          routines={routines}
          judges={judges}
          athletes={athletes}
          onClose={() => setCreateMatchFor(null)}
          onCreated={(match) => {
            onMatchCreated(match);
            setCreateMatchFor(null);
          }}
        />
      )}

      {assignAthletesFor && (
        <AssignAthletesDialog
          open={!!assignAthletesFor}
          match={assignAthletesFor}
          athletes={athletes}
          onClose={() => setAssignAthletesFor(null)}
          onAssigned={(match) => {
            onMatchUpdated(match);
            setAssignAthletesFor(null);
          }}
        />
      )}
    </>
  );
}