# Getting Started

## Prerequisites

Ensure the following are installed before proceeding:

| Tool   | Version  |
|--------|----------|
| JDK    | 21+      |
| Docker | Latest   |
| Gradle | Wrapper  |

---

## Environment Configuration

Create a `.env` file at the **root of the project** with the following variables:

```env
POSTGRES_USER=dbuser
POSTGRES_PASSWORD=changeit
POSTGRES_DB=db
DB_URL=jdbc:postgresql://agenda-postgres:5432/db?user=dbuser&password=changeit
```

> **Important:** This file contains sensitive credentials and must never be committed to version control. It is already listed in `.gitignore`.

---

## Running the Application

### Locally (without Docker)

```bash
./gradlew bootRun
```

The Gradle `dotenv` plugin reads the `.env` file automatically and injects all variables into the process environment. Spring Boot then resolves them from `application-prod.properties` (e.g. `${DB_URL}`).

> Requires a locally running PostgreSQL instance on port `5432`.

---

### With Docker

```bash
./gradlew allUp
```

This command orchestrates the full startup sequence:

1. **`assemble`** — compiles and packages the application JAR
2. **`extractUberJar`** — extracts the JAR into `build/dependency` for layered Docker builds
3. **`buildImageJvm`** — builds the application image (`caliarena-jvm`)
4. **`buildImagePostgres`** — builds the database image (`caliarena-postgres`)
5. **`buildImageUbuntu`** — builds the utility image (`caliarena-ubuntu`)
6. **`docker compose up`** — starts all containers, passing the `.env` file from the project root via `--env-file`

Once running, the application is available at:

```
http://localhost:8080
```

---

### Stopping the Application

```bash
./gradlew allDown
```

---

## Configuration Flow

Both `bootRun` and `allUp` read from the same `.env` file, ensuring a consistent configuration across local and Docker environments.

```
.env
 ├── bootRun  →  dotenv plugin injects variables  →  Spring Boot resolves ${VAR}
 └── allUp    →  docker compose --env-file        →  container environment variables  →  Spring Boot resolves ${VAR}
```