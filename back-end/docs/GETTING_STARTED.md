# Getting Started

## Prerequisites

| Tool   | Version |
|--------|---------|
| JDK    | 21+     |
| Docker | Latest  |

## Setup

Create a `.env` file at the **project root** with the database credentials:

```env
POSTGRES_USER=dbuser
POSTGRES_PASSWORD=changeit
POSTGRES_DB=db
DB_URL=jdbc:postgresql://caliarena-postgres:5432/db?user=dbuser&password=changeit
```

> **Important:** this file contains credentials and must never be committed. It is gitignored.

## Build & run with Docker

From `back-end/`:

```bash
./gradlew allUp      # builds everything (JVM + Postgres + nginx) and starts the stack
./gradlew allDown    # stops the stack
```

Once running:

- App (JVM): `http://localhost:8080`
- Web (nginx): `http://localhost:4000`

The JVM container starts with the **`prod`** Spring profile
(`SPRING_PROFILES_ACTIVE=prod` set in `app/docker-compose.yaml`), reading config from
`application-prod.properties` via the `.env` variables.

## Run locally (without Docker)

```bash
./gradlew bootRun
```

> Requires PostgreSQL running on `localhost:5432` and the local profile (`dev`) set in
> `app/src/main/resources/application.properties`. This file is dev-only and not committed.

---

## Backend structure

```
back-end/
├── app/                  # entry point, Spring Boot config, Docker + Compose
│   ├── docker/           # Dockerfiles (jvm, postgres)
│   ├── docker-compose.yaml
│   └── src/main/resources/  # application.properties + per-profile config
├── http/                 # REST controllers, websocket controllers, DTOs
├── service/              # business logic / use cases
├── domain/               # pure domain models & rules
├── repo-jpa/             # JPA entities and repositories (persistence)
├── nginx/                # nginx reverse-proxy (web front on port 4000)
├── docs/                 # documentation
└── build.gradle.kts      # build + Docker tasks (allUp / allDown)
```
