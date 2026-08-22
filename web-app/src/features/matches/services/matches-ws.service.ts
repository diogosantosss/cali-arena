import { Client, type IMessage } from "@stomp/stompjs";
import { getStoredToken } from "@/lib/api/client";

export type JudgeEvent =
  | { type: "REPS"; side: "RED" | "BLUE"; reps: number; exerciseId?: number | null }
  | { type: "FINISHED"; side: "RED" | "BLUE"; finishedAt: string }
  | { type: "ERROR"; message: string };

/**
 * Opens the judge STOMP channel for a match and subscribes to its
 * broadcast topic. The caller owns the returned client lifecycle
 * (call `deactivate()` when leaving the match).
 */
export function createJudgeClient(
  matchId: number,
  onEvent: (event: JudgeEvent) => void,
): Client {
  const token = getStoredToken();
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  const client = new Client({
    brokerURL: `${protocol}://${window.location.host}/ws${token ? `?token=${token}` : ""}`,
    reconnectDelay: 3000,
    onConnect: () => {
      client.subscribe(`/topic/matches/${matchId}`, (message: IMessage) => {
        console.info(`WEBSOCKET: Message incoming - ${message.body}`)
        try {
          onEvent(JSON.parse(message.body) as JudgeEvent);
        } catch {
          // malformed frame — ignore
        }
      });
    },
  });
  client.activate();
  return client;
}

export function publishAdjust(
  client: Client | null,
  matchId: number,
  side: "RED" | "BLUE",
  reps: number,
): boolean {
  if (!client?.connected) return false;
  client.publish({
    destination: `/app/matches/${matchId}/actions`,
    body: JSON.stringify({ side, reps }),
  });
  return true;
}
