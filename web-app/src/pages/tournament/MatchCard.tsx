import { useState } from "react";
import type { Athlete, Match, Routine, User } from "@/types";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { UserRound, Swords, ChevronDown, ChevronUp } from "lucide-react";

interface Props {
  match: Match;
  athletes: Athlete[];
  routines: Routine[];
  judges: User[];
  onAssignAthletes: (match: Match) => void;
  onStartMatch: (match: Match) => void;
}

const statusVariant: Record<Match["status"], "default" | "secondary" | "outline" | "destructive"> = {
  PENDING: "outline",
  READY: "secondary",
  RUNNING: "default",
  PAUSED: "outline",
  FINISHED: "destructive",
};

export function MatchCard({ match, athletes, routines, judges, onAssignAthletes, onStartMatch }: Props) {
  const [expanded, setExpanded] = useState(false);

  const redAthlete = athletes.find((a) => a.id === match.athleteRedId);
  const blueAthlete = athletes.find((a) => a.id === match.athleteBlueId);
  const routine = routines.find((r) => r.id === match.routineId);
  const judge = judges.find((j) => j.id === match.judgeId);

  return (
    <Card className="overflow-hidden">
      <CardContent className="p-0">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <Badge variant={statusVariant[match.status]} className="capitalize text-xs">
              {match.status.toLowerCase()}
            </Badge>
            <span className="text-sm font-medium">Match #{match.id}</span>
            {routine && (
              <span className="text-xs text-muted-foreground">{routine.name}</span>
            )}
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setExpanded((v) => !v)}
          >
            {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </Button>
        </div>

        <div className="grid grid-cols-3 items-center px-4 pb-3 gap-2">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-red-500 shrink-0" />
            {redAthlete ? (
              <span className="text-sm font-medium">{redAthlete.name}</span>
            ) : (
              <span className="text-sm text-muted-foreground italic">
                {match.redFromMatchId ? `Winner of #${match.redFromMatchId}` : "Not assigned"}
              </span>
            )}
          </div>

          <div className="flex justify-center">
            <Swords className="w-4 h-4 text-muted-foreground" />
          </div>

          <div className="flex items-center gap-2 justify-end">
            {blueAthlete ? (
              <span className="text-sm font-medium">{blueAthlete.name}</span>
            ) : (
              <span className="text-sm text-muted-foreground italic">
                {match.blueFromMatchId ? `Winner of #${match.blueFromMatchId}` : "Not assigned"}
              </span>
            )}
            <div className="w-2 h-2 rounded-full bg-blue-500 shrink-0" />
          </div>
        </div>

        {expanded && (
          <div className="border-t px-4 py-3 space-y-3">
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1">Judge</p>
                <div className="flex items-center gap-1.5">
                  <UserRound className="w-3.5 h-3.5 text-muted-foreground" />
                  <span>{judge?.username ?? `#${match.judgeId}`}</span>
                </div>
              </div>
              {match.winnerAthleteId && (
                <div>
                  <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1">Winner</p>
                  <span className="font-medium text-green-600">
                    {athletes.find((a) => a.id === match.winnerAthleteId)?.name ?? `#${match.winnerAthleteId}`}
                  </span>
                </div>
              )}
              {match.startedAt && (
                <div>
                  <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1">Started at</p>
                  <span>{new Date(match.startedAt).toLocaleTimeString()}</span>
                </div>
              )}
              {match.finishedAt && (
                <div>
                  <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1">Finished at</p>
                  <span>{new Date(match.finishedAt).toLocaleTimeString()}</span>
                </div>
              )}
            </div>

            <div className="flex gap-2">
              {match.status === "PENDING" && !match.athleteRedId && !match.athleteBlueId && !match.redFromMatchId && !match.blueFromMatchId && (
                <Button size="sm" variant="outline" onClick={() => onAssignAthletes(match)}>
                  Assign athletes
                </Button>
              )}
              {match.status === "READY" && (
                <Button size="sm" onClick={() => onStartMatch(match)}>
                  Start match
                </Button>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}