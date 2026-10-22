# Cali Arena

Cali Arena is a real-time competition management platform for calisthenics battles and endurance events.
It provides live match tracking, athlete progress visualization, judge controls, and centralized tournament management.

The platform ships as one backend plus one web application that carries four different surfaces:

| Surface | Route | Audience |
| --- | --- | --- |
| Admin dashboard | `/dashboard` | Admins & tournament hosts |
| Live spectator screens | `/screen/:tournamentId` | The public / the big screen |
| OBS overlay | `/overlay/:tournamentId` | Broadcast (OBS browser source) |
| Judge app (PWA) | `/judge` | Judges on the floor, on their phones |

> **Project status:** the platform is in a good state and is **no longer actively being developed**.
> The test suite is green, the whole stack boots with a single command, and the features below are the ones that are actually live.

---

## What it is made of

A real-time platform for managing and displaying calisthenics competitions.

- **Backend** (Kotlin · Spring Boot · PostgreSQL · Docker) — REST API with a real-time engine over SSE/WebSocket: competition state is pushed live to all connected clients.
- **Admin web dashboard** (React · TypeScript) — manage tournaments, athletes, routines, brackets (organized by division), and matches; drive the broadcast with a screen control panel.
- **Live web interface for spectators** (React · TypeScript) — real-time screens for battles (reps and scores), routine schedules, bracket trees organized by division, and per-bracket best-time leaderboards.
- **Mobile app for judges** — a **PWA** (React · TypeScript): installable on any phone, it lets a judge pick a match, control the athletes' reps and timers, and follow the result live. The app shell is precached for offline use, and the judge flow is what judges actually run on the floor.

---

## Architecture

```
                    ┌──────────────────────────────┐
  spectators  ──────▶│  GET /screen-routines/listen │  SSE  (public, keep-alive)
                    └──────────────┬───────────────┘
                                   │ competition state
  judge PWA  ───────▶│  /ws (STOMP) /topic/matches/… │  WebSocket (judge control)
                    └──────────────┬───────────────┘
                                   │
                     ┌─────────────▼─────────────┐
                     │   Spring Boot back-end    │
                     │  :app → :http → :service   │
                     │           :domain          │
                     │        :repo-jpa (JPA)     │
                     └───────┬───────────┬────────┘
                             │           │
                        PostgreSQL     Redis
```

**Two real-time channels, on purpose:**

- **SSE** (`GET /api/tournaments/{id}/screen-routines/listen`) fans out tournament state to every spectator screen. It is intentionally unauthenticated — the public screens must work without a login — and it sends a keep-alive every 10 s.
- **WebSocket / STOMP** (`/ws`, topic `/topic/matches/{matchId}`) carries the judge's live actions (`START`, `ADJUST`, `FINISH`) and their results. The handshake is authorized (`ADMIN`, `HOST`, `JUDGE`) and every inbound message is re-checked against the tournament-scoped permissions.

**Layering** (`back-end/`): `:domain` holds the pure model and rules (no Spring), `:repo-jpa` the entities and a small unit-of-work abstraction, `:service` the use cases (all of them returning `Either<ApiError, T>` instead of throwing), `:http` the controllers and DTOs, and `:app` the boot, the WebSocket configuration, the interceptors and the Docker setup.

---

## Features

**Competition management**

- Tournaments with a lifecycle (`DRAFT → READY → LIVE → FINISHED`), one host and N assigned judges per tournament.
- Athletes and clubs, with gender and division filters.
- Endurance routines built from exercises, each routine carrying a time cap.
- Exercise types: `NORMAL`, `UNBROKEN`, `SUPERSET` — superset progression advances a side inside the group instead of jumping to the next exercise.
- Brackets per division with the stages `QUALIFIERS`, `QUARTERFINALS`, `SEMIFINALS`, `FINALS`, plus a per-bracket best-time leaderboard and a per-division summary.
- Matches with live rep counting, per-side finish times, and single-athlete matches (one side may be empty — only the time cap decides).

**Broadcast**

- A screen state machine (`WAITING`, `ROUTINES`, `BATTLE`, `LEADERBOARD`, `BRACKETS`) driven from the dashboard; switching the state is what tells the live screens what to show.
- Screen routines: the ordered, labelled list of routines displayed on the live screens, editable from the dashboard.
- An OBS browser-source overlay with a transparent background, so it composites over a camera feed.

**Judge PWA**

- Installable (`vite-plugin-pwa` + Workbox): app shell precached, standalone display, portrait, with shortcuts to the judge and dashboard flows.
- Role-aware login — a judge lands straight in the judge flow and never sees the dashboard.
- Match list → match control: start the match, adjust reps, force a finish, with live timers and the match result pushed over the WebSocket.

**Accounts and access control**

- Roles `ADMIN`, `HOST`, `JUDGE`, enforced at two levels: a coarse global gate at the HTTP/WebSocket edge, and tournament-scoped permissions inside the transaction (admin, the tournament host, or a judge assigned to that tournament).
- Opaque bearer tokens (SHA-256 hashed at rest, rolling 1 h TTL, absolute 24 h TTL, max 3 active tokens per user). Passwords are BCrypt-hashed.
- Errors are returned as RFC 7807 `application/problem+json`.

---

## Tech stack

| Layer | Stack |
| --- | --- |
| Backend | Kotlin 2.4 · Spring Boot 3.5 · Spring MVC · Spring Data JPA · spring-messaging (STOMP) · PostgreSQL · Redis · JDK 21 (virtual threads) · OpenAPI 3.0.3 / Swagger UI |
| Web | React 19 · TypeScript · Vite 8 · React Router 7 · Tailwind CSS 4 · shadcn/ui · `@stomp/stompjs` · `vite-plugin-pwa` (Workbox) |
| Infrastructure | Docker · Docker Compose · nginx (SPA + `/api` + `/ws` reverse proxy) · Gradle 9.6 (Kotlin DSL) · ktlint |
| Native client (legacy) | Kotlin Multiplatform · Jetpack Compose (Android) · Ktor · Krossbow (STOMP) · Koin · Coroutines |

---

## Repository layout

```
cali-arena/
├── back-end/          # Kotlin · Spring Boot
│   ├── app/           # entry point, Spring config, WebSocket, interceptors, Docker + Compose
│   ├── http/          # REST controllers, STOMP controller, DTOs, problem+json mapping
│   ├── service/       # use cases, SSE publisher, screen-effect handlers, mappers, rate limiter
│   ├── domain/        # pure domain model and rules (no Spring, no JPA)
│   ├── repo-jpa/      # JPA entities, repositories, unit-of-work, SQL scripts
│   ├── nginx/         # reverse proxy + SPA image (builds web-app/)
│   └── docs/          # GETTING_STARTED.md, openapi.yaml, architecture & ER diagrams
├── web-app/           # React · TypeScript — dashboard, live screens, OBS overlay, judge PWA
├── mobile-app/        # Kotlin Multiplatform native judge client (earlier native attempt, iOS still pending)
└── docs/              # assets and screenshots (to be filled in)
```

---

## Getting started

### Backend

```bash
cd back-end
./gradlew allUp      # builds the JVM, Postgres, Redis and nginx images and starts the stack
./gradlew allDown    # stops the stack
```

- App (JVM): `http://localhost:8080`
- Web (nginx): `http://localhost:4000`
- Swagger UI: `http://localhost:8080/swagger-ui.html` (dev profile only)

Credentials come from `back-end/.env` (gitignored). See
[`back-end/docs/GETTING_STARTED.md`](./back-end/docs/GETTING_STARTED.md) for the template and the
run-without-Docker flow.

### Web app

```bash
cd web-app
npm install
npm run dev          # proxies /api and /ws to http://localhost:8080
```

`npm run build` produces the static bundle, `npm run lint` runs ESLint.

---

## Tests and CI

| Module | Style | Tests |
| --- | --- | --- |
| `:http` | JUnit 5 + Mockito (controllers instantiated directly) | 170 |
| `:service` | JUnit 5 + Mockito (repositories and unit-of-work mocked) | 181 |
| `:repo-jpa` | JUnit 5 + Testcontainers (real PostgreSQL 16) | 86 |

**437 tests, all passing.** `./gradlew finalTestSummary` prints the aggregated result.

CI (`.github/workflows/ci.yml`) runs on every push and pull request to `dev`: JDK 21, `ktlintCheck`,
then `:http:test :service:test :repo-jpa:test`.

---

## API reference

The full OpenAPI 3.0.3 specification lives in [`back-end/docs/openapi.yaml`](./back-end/docs/openapi.yaml)
and is served as Swagger UI in the dev profile. Errors follow RFC 7807 (`application/problem+json`).

Main endpoint groups: `/api/users`, `/api/tournaments` (plus `/state/screen` and `/staff`),
`/api/brackets`, `/api/matches`, `/api/routines`, `/api/athletes`, `/api/clubs`, and
`/api/tournaments/{id}/screen-routines`.

---

## Known gaps

Small things that are still open, listed so the state of the project stays explicit:

- The Redis rate limiter (`@RateLimited`) is fully implemented but is not attached to any endpoint yet.
- `back-end/docs/openapi.yaml` drifted from the code: it still documents a `/api/matches/judge/{judgeId}` endpoint that no longer exists, and misses `/api/users/judges`.
- The native `mobile-app/` iOS target still calls a `Greeting()` symbol that no longer exists in `sharedLogic`, so that target does not compile.
- `docs/` has no assets yet.

---

Diogo | 22 October 2026
