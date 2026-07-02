# Cali Arena — API Documentation

> WebSockets are out of scope for now and will be documented separately.

---

## General

### Authentication

Most endpoints require a Bearer token in the `Authorization` header:

```
Authorization: Bearer <token>
```

Endpoints that do **not** require authentication are marked with 🔓.

### Roles

| Role    | Description                        |
|---------|------------------------------------|
| `ADMIN` | Full access to all endpoints       |
| `JUDGE` | Access to match judging operations |

### Common Error Responses

These errors can occur on any endpoint and are not repeated per endpoint:

| Status | Error                   | Cause                               |
|--------|-------------------------|-------------------------------------|
| `401`  | `unauthorized`          | Missing or invalid token            |
| `403`  | `forbidden`             | Authenticated but insufficient role |
| `500`  | `internal_server_error` | Unexpected server error             |

### Response Format

All error responses follow this shape:

```json
{
  "error": "error_code"
}
```

All success responses return the resource directly or a list.

---

## Auth

### 🔓 POST `/api/auth/register`

Register a new user.

**Body:**
```json
{
  "username": "string",
  "password": "string",
  "role": "ADMIN | JUDGE"
}
```

**Success `201`:**
```json
{
  "id": 1,
  "username": "alice",
  "role": "ADMIN"
}
```

**Errors:**

| Status | Error                     | Cause                       |
|--------|---------------------------|-----------------------------|
| `400`  | `invalid_body`            | Missing or malformed fields |
| `409`  | `username_already_exists` | Username is taken           |

---

### 🔓 POST `/api/auth/login`

Login and receive a token.

**Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Success `200`:**
```json
{
  "token": "string"
}
```

**Errors:**

| Status | Error                 | Cause                            |
|--------|-----------------------|----------------------------------|
| `400`  | `invalid_body`        | Missing or malformed fields      |
| `401`  | `invalid_credentials` | Wrong username or password       |

---

### POST `/api/auth/logout`

Invalidate the current token.

**Success `200`:**
```json
{
  "message": "logged_out"
}
```

**Errors:**

| Status | Error          | Cause                 |
|--------|----------------|-----------------------|
| `401`  | `unauthorized` | Token missing/invalid |

---

### GET `/api/auth/me`

Get the currently authenticated user.

**Success `200`:**
```json
{
  "id": 1,
  "username": "alice",
  "role": "ADMIN"
}
```

---

## Users

> Requires role: `ADMIN`

### GET `/api/users`

Get all users.

**Success `200`:**
```json
[
  { "id": 1, "username": "alice", "role": "ADMIN" }
]
```

---

### GET `/api/users/{id}`

Get a user by ID.

**Success `200`:**
```json
{
  "id": 1,
  "username": "alice",
  "role": "ADMIN"
}
```

**Errors:**

| Status | Error            | Cause             |
|--------|------------------|-------------------|
| `404`  | `user_not_found` | ID does not exist |

---

## Clubs

> Requires role: `ADMIN`

### POST `/api/clubs`

Create a new club.

**Body:**
```json
{
  "name": "string",
  "shortName": "string | null"
}
```

**Success `201`:**
```json
{
  "id": 1,
  "name": "CrossFit Porto",
  "shortName": "CFP"
}
```

**Errors:**

| Status | Error                 | Cause                     |
|--------|-----------------------|---------------------------|
| `400`  | `invalid_body`        | Missing or invalid fields |
| `409`  | `club_already_exists` | Club name already taken   |

---

### GET `/api/clubs`

Get all clubs.

**Success `200`:**
```json
[
  { "id": 1, "name": "CrossFit Porto", "shortName": "CFP" }
]
```

---

### GET `/api/clubs/{id}`

Get a club by ID.

**Success `200`:**
```json
{
  "id": 1,
  "name": "CrossFit Porto",
  "shortName": "CFP"
}
```

**Errors:**

| Status | Error            | Cause              |
|--------|------------------|--------------------|
| `404`  | `club_not_found` | ID does not exist  |

---

## Athletes

> Requires role: `ADMIN`

### POST `/api/athletes`

Create a new athlete.

**Body:**
```json
{
  "name": "string",
  "gender": "MALE | FEMALE",
  "clubId": 1
}
```

**Success `201`:**
```json
{
  "id": 1,
  "name": "João Silva",
  "gender": "MALE",
  "clubId": 1
}
```

**Errors:**

| Status | Error            | Cause                     |
|--------|------------------|---------------------------|
| `400`  | `invalid_body`   | Missing or invalid fields |
| `404`  | `club_not_found` | clubId does not exist     |

---

### GET `/api/athletes`

Get all athletes. Optionally filter by club or gender.

**Query params:** `clubId`, `gender`

**Success `200`:**
```json
[
  { "id": 1, "name": "João Silva", "gender": "MALE", "clubId": 1 }
]
```

---

### GET `/api/athletes/{id}`

Get an athlete by ID.

**Success `200`:**
```json
{
  "id": 1,
  "name": "João Silva",
  "gender": "MALE",
  "clubId": 1
}
```

**Errors:**

| Status | Error               | Cause             |
|--------|---------------------|-------------------|
| `404`  | `athlete_not_found` | ID does not exist |

---

### PUT `/api/athletes/{id}`

Update an athlete.

**Body:**
```json
{
  "name": "string",
  "gender": "MALE | FEMALE",
  "clubId": 1
}
```

**Success `200`:**
```json
{
  "id": 1,
  "name": "João Silva Updated",
  "gender": "MALE",
  "clubId": 1
}
```

**Errors:**

| Status | Error               | Cause                     |
|--------|---------------------|---------------------------|
| `400`  | `invalid_body`      | Missing or invalid fields |
| `404`  | `athlete_not_found` | Athlete ID does not exist |
| `404`  | `club_not_found`    | clubId does not exist     |

---

## Endurance Routines

> Requires role: `ADMIN`

### POST `/api/routines`

Create a new endurance routine.

**Body:**
```json
{
  "name": "string",
  "timeCapSeconds": 600
}
```

**Success `201`:**
```json
{
  "id": 1,
  "name": "Hero WOD",
  "timeCapSeconds": 600
}
```

**Errors:**

| Status | Error                    | Cause                     |
|--------|--------------------------|---------------------------|
| `400`  | `invalid_body`           | Missing or invalid fields |
| `409`  | `routine_already_exists` | Routine name taken        |

---

### GET `/api/routines`

Get all routines.

**Success `200`:**
```json
[
  { "id": 1, "name": "Hero WOD", "timeCapSeconds": 600 }
]
```

---

### GET `/api/routines/{id}`

Get a routine by ID, including its exercises.

**Success `200`:**
```json
{
  "id": 1,
  "name": "Hero WOD",
  "timeCapSeconds": 600,
  "exercises": [
    {
      "id": 1,
      "name": "Burpee",
      "targetReps": 20,
      "addedWeight": null,
      "exerciseOrder": 1,
      "supersetOrder": null,
      "type": "NORMAL"
    }
  ]
}
```

**Errors:**

| Status | Error               | Cause             |
|--------|---------------------|-------------------|
| `404`  | `routine_not_found` | ID does not exist |

---

### POST `/api/routines/{id}/exercises`

Add an exercise to a routine.

**Body:**
```json
{
  "name": "string",
  "targetReps": 20,
  "addedWeight": 10.5,
  "exerciseOrder": 1,
  "supersetOrder": null,
  "type": "NORMAL | UNBROKEN | SUPERSET"
}
```

**Success `201`:**
```json
{
  "id": 1,
  "routineId": 1,
  "name": "Burpee",
  "targetReps": 20,
  "addedWeight": null,
  "exerciseOrder": 1,
  "supersetOrder": null,
  "type": "NORMAL"
}
```

**Errors:**

| Status | Error               | Cause                     |
|--------|---------------------|---------------------------|
| `400`  | `invalid_body`      | Missing or invalid fields |
| `404`  | `routine_not_found` | Routine ID does not exist |

---

## Tournaments

> Requires role: `ADMIN` unless stated otherwise.

### POST `/api/tournaments`

Create a new tournament.

**Body:**
```json
{
  "name": "string",
  "location": "string | null",
  "startDate": "ISO8601 | null",
  "endDate": "ISO8601 | null"
}
```

**Success `201`:**
```json
{
  "id": 1,
  "name": "Open 2025",
  "location": "Porto",
  "startDate": "2025-06-01T00:00:00Z",
  "endDate": null,
  "status": "DRAFT"
}
```

**Errors:**

| Status | Error                       | Cause                     |
|--------|-----------------------------|---------------------------|
| `400`  | `invalid_body`              | Missing or invalid fields |
| `409`  | `tournament_already_exists` | Tournament name taken     |

---

### GET `/api/tournaments`

Get all tournaments. Optionally filter by status.

**Query params:** `status` (`DRAFT | READY | LIVE | FINISHED`)

**Success `200`:**
```json
[
  { "id": 1, "name": "Open 2025", "status": "DRAFT" }
]
```

---

### GET `/api/tournaments/{id}`

Get a tournament by ID.

**Success `200`:**
```json
{
  "id": 1,
  "name": "Open 2025",
  "location": "Porto",
  "startDate": "2025-06-01T00:00:00Z",
  "endDate": null,
  "status": "DRAFT"
}
```

**Errors:**

| Status | Error                  | Cause             |
|--------|------------------------|-------------------|
| `404`  | `tournament_not_found` | ID does not exist |

---

### PUT `/api/tournaments/{id}/status`

Update the status of a tournament.

**Body:**
```json
{
  "status": "DRAFT | READY | LIVE | FINISHED"
}
```

**Success `200`:**
```json
{
  "id": 1,
  "status": "LIVE"
}
```

**Errors:**

| Status | Error                       | Cause                         |
|--------|-----------------------------|-------------------------------|
| `400`  | `invalid_body`              | Invalid or missing status     |
| `400`  | `invalid_status_transition` | Status transition not allowed |
| `404`  | `tournament_not_found`      | ID does not exist             |

---

### POST `/api/tournaments/{id}/brackets`

Create a bracket for a tournament.

**Body:**
```json
{
  "gender": "MALE | FEMALE",
  "stage": "QUALIFIERS | QUARTERFINALS | SEMIFINALS | FINALS"
}
```

**Success `201`:**
```json
{
  "id": 1,
  "tournamentId": 1,
  "gender": "MALE",
  "stage": "QUALIFIERS"
}
```

**Errors:**

| Status | Error                    | Cause                                |
|--------|--------------------------|--------------------------------------|
| `400`  | `invalid_body`           | Missing or invalid fields            |
| `404`  | `tournament_not_found`   | Tournament ID does not exist         |
| `409`  | `bracket_already_exists` | Bracket for that gender+stage exists |

---

### GET `/api/tournaments/{id}/brackets`

Get all brackets for a tournament. Optionally filter by gender.

**Query params:** `gender`

**Success `200`:**
```json
[
  { "id": 1, "tournamentId": 1, "gender": "MALE", "stage": "QUALIFIERS" }
]
```

**Errors:**

| Status | Error                  | Cause             |
|--------|------------------------|-------------------|
| `404`  | `tournament_not_found` | ID does not exist |

---

### GET `/api/tournaments/{id}/state`

Get the current display state of a tournament.

> Accessible by `ADMIN` and `JUDGE`

**Success `200`:**
```json
{
  "tournamentId": 1,
  "currentScreen": "WAITING | BRACKET | BATTLE | WINNER | LEADERBOARD",
  "currentMatchId": 3
}
```

**Errors:**

| Status | Error                        | Cause                        |
|--------|------------------------------|------------------------------|
| `404`  | `tournament_not_found`       | Tournament ID does not exist |
| `404`  | `tournament_state_not_found` | State not yet created        |

---

### PUT `/api/tournaments/{id}/state/screen`

Update the current screen of a tournament.

**Body:**
```json
{
  "screen": "WAITING | BRACKET | BATTLE | WINNER | LEADERBOARD",
  "currentMatchId": 3
}
```

**Success `200`:**
```json
{
  "tournamentId": 1,
  "currentScreen": "BATTLE",
  "currentMatchId": 3
}
```

**Errors:**

| Status | Error                        | Cause                        |
|--------|------------------------------|------------------------------|
| `400`  | `invalid_body`               | Missing or invalid fields    |
| `404`  | `tournament_not_found`       | Tournament ID does not exist |
| `404`  | `tournament_state_not_found` | State not yet initialized    |

---

## Matches

### POST `/api/brackets/{bracketId}/matches`

Create a match inside a bracket.

> Requires role: `ADMIN`

**Body:**
```json
{
  "routineId": 1,
  "redFromMatchId": null,
  "blueFromMatchId": null
}
```

**Success `201`:**
```json
{
  "id": 1,
  "bracketId": 1,
  "routineId": 1,
  "athleteRedId": null,
  "athleteBlueId": null,
  "redFromMatchId": null,
  "blueFromMatchId": null,
  "winnerAthleteId": null,
  "status": "PENDING"
}
```

**Errors:**

| Status | Error               | Cause                     |
|--------|---------------------|---------------------------|
| `400`  | `invalid_body`      | Missing or invalid fields |
| `404`  | `bracket_not_found` | Bracket ID does not exist |
| `404`  | `routine_not_found` | Routine ID does not exist |

---

### GET `/api/brackets/{bracketId}/matches`

Get all matches for a bracket.

> Requires role: `ADMIN` or `JUDGE`

**Success `200`:**
```json
[
  {
    "id": 1,
    "bracketId": 1,
    "routineId": 1,
    "status": "PENDING"
  }
]
```

**Errors:**

| Status | Error               | Cause                     |
|--------|---------------------|---------------------------|
| `404`  | `bracket_not_found` | Bracket ID does not exist |

---

### GET `/api/matches/{id}`

Get a match by ID.

> Requires role: `ADMIN` or `JUDGE`

**Success `200`:**
```json
{
  "id": 1,
  "bracketId": 1,
  "routineId": 1,
  "athleteRedId": 2,
  "athleteBlueId": 3,
  "redFromMatchId": null,
  "blueFromMatchId": null,
  "winnerAthleteId": null,
  "status": "PENDING"
}
```

**Errors:**

| Status | Error             | Cause             |
|--------|-------------------|-------------------|
| `404`  | `match_not_found` | ID does not exist |

---

### PUT `/api/matches/{id}/status`

Update the status of a match.

> Requires role: `ADMIN`

**Body:**
```json
{
  "status": "PENDING | READY | RUNNING | PAUSED | FINISHED"
}
```

**Success `200`:**
```json
{
  "id": 1,
  "status": "RUNNING"
}
```

**Errors:**

| Status | Error                       | Cause                         |
|--------|-----------------------------|-------------------------------|
| `400`  | `invalid_body`              | Invalid or missing status     |
| `400`  | `invalid_status_transition` | Status transition not allowed |
| `404`  | `match_not_found`           | ID does not exist             |

---

### PUT `/api/matches/{id}/winner`

Set the winner of a match.

> Requires role: `ADMIN`

**Body:**
```json
{
  "winnerAthleteId": 2
}
```

**Success `200`:**
```json
{
  "id": 1,
  "winnerAthleteId": 2,
  "status": "FINISHED"
}
```

**Errors:**

| Status | Error                  | Cause                               |
|--------|------------------------|-------------------------------------|
| `400`  | `invalid_body`         | Missing or invalid fields           |
| `400`  | `athlete_not_in_match` | Athlete is not red or blue in match |
| `404`  | `match_not_found`      | Match ID does not exist             |

---

### GET `/api/matches/{id}/progress`

Get the live progress of a match.

> Requires role: `ADMIN` or `JUDGE`

**Success `200`:**
```json
{
  "matchId": 1,
  "redCurrentExerciseId": 2,
  "blueCurrentExerciseId": 2,
  "redCurrentReps": 10,
  "blueCurrentReps": 8,
  "redFinishedAt": null,
  "blueFinishedAt": null,
  "timerStartedAt": "2025-06-01T10:00:00Z",
  "timerRemainingSeconds": 300
}
```

**Errors:**

| Status | Error                | Cause                        |
|--------|----------------------|------------------------------|
| `404`  | `match_not_found`    | Match ID does not exist      |
| `404`  | `progress_not_found` | Progress not yet initialized |

---

### POST `/api/matches/{id}/events`

Register a match event (judging action).

> Requires role: `JUDGE`

**Body:**
```json
{
  "eventType": "MATCH_STARTED | REP_ADDED | REP_REMOVED | EXERCISE_CHANGED | MATCH_PAUSED | MATCH_FINISHED",
  "payload": "string | null"
}
```

**Payload examples by event type:**

| eventType          | payload example                                           |
|--------------------|-----------------------------------------------------------|
| `MATCH_STARTED`    | `null`                                                    |
| `REP_ADDED`        | `{"athlete":"red","exerciseId":2,"reps":1}`               |
| `REP_REMOVED`      | `{"athlete":"blue","exerciseId":2,"reps":1}`              |
| `EXERCISE_CHANGED` | `{"athlete":"red","fromExerciseId":2,"toExerciseId":3}`   |
| `MATCH_PAUSED`     | `{"remainingSeconds":245}`                                |
| `MATCH_FINISHED`   | `null`                                                    |

**Success `201`:**
```json
{
  "id": 1,
  "matchId": 1,
  "judgeId": 2,
  "eventType": "REP_ADDED",
  "payload": "{\"athlete\":\"red\",\"exerciseId\":2,\"reps\":1}",
  "createdAt": "2025-06-01T10:01:00Z"
}
```

**Errors:**

| Status | Error               | Cause                         |
|--------|---------------------|-------------------------------|
| `400`  | `invalid_body`      | Missing or invalid fields     |
| `400`  | `match_not_running` | Match is not in RUNNING state |
| `404`  | `match_not_found`   | Match ID does not exist       |

---

### GET `/api/matches/{id}/events`

Get all events for a match.

> Requires role: `ADMIN` or `JUDGE`

**Success `200`:**
```json
[
  {
    "id": 1,
    "matchId": 1,
    "judgeId": 2,
    "eventType": "REP_ADDED",
    "payload": "{\"athlete\":\"red\",\"exerciseId\":2,\"reps\":1}",
    "createdAt": "2025-06-01T10:01:00Z"
  }
]
```

**Errors:**

| Status | Error             | Cause                   |
|--------|-------------------|-------------------------|
| `404`  | `match_not_found` | Match ID does not exist |

---

## Status Transition Rules

### Tournament

```
DRAFT → READY → LIVE → FINISHED
```

### Match

```
PENDING → READY → RUNNING → PAUSED → RUNNING → FINISHED
```

---

## Notes

- All timestamps are returned as ISO 8601 UTC strings.
- WebSocket endpoints for live match updates will be documented separately.