# Gym CRM — microservices

Spring Boot 3.2 microservices (Java 17 bytecode, built and run on JDK 21) for a gym CRM:

```mermaid
flowchart LR
    client([Client]) -->|REST + user JWT| gym[gym-crm :8080]
    client -->|REST + service JWT| tws[trainer-workload-service :8082]
    gym -->|JPA| mysql[(MySQL)]
    gym -->|JMS workload events| mq[[ActiveMQ :61616]]
    mq -->|JMS listener| tws
    tws -->|Spring Data| mongo[(MongoDB)]
    gym -.->|registers| eureka[discovery-server :8761]
    tws -.->|registers| eureka
```

| Component | Source / image | Port | Role |
|---|---|---|---|
| `discovery-server` | `discovery-server/` | 8761 | Eureka registry |
| `gym-crm` | `gym-crm/` | 8080 | REST API + JWT security; trainees, trainers, trainings in MySQL; publishes training events |
| `trainer-workload-service` | `trainer-workload-service/` | 8082 | Consumes the events, keeps monthly workload summaries per trainer in MongoDB, serves them over REST |
| `activemq` | `apache/activemq-classic:6.2.0` | 61616 · 8161 | Message broker · web console |
| `mongo-db` | `mongo:7.0` | 27017 | `trainer_workload` database (`trainer_workloads` collection) |
| `mysql-db` | `mysql:8.4` | 3306 | `gymcrm_dev` database |

The three Maven projects are independent (no parent pom); each builds and tests on its own.

## Quick start (Docker)

Requires Docker with Compose v2 (2.20+) and roughly 4 GB of free RAM.

```bash
cp .env.example .env                    # optional: every value has a default in docker-compose.yml
docker compose up -d --build --wait     # build images, start the stack, wait until everything is healthy
bash scripts/smoke-test.sh              # end to end: register → create training → read summary from MongoDB
```

The first start takes a few minutes (Maven build). Then:

| What | URL |
|---|---|
| Eureka dashboard | http://localhost:8761 |
| gym-crm Swagger UI | http://localhost:8080/swagger-ui/index.html |
| trainer-workload-service Swagger UI | http://localhost:8082/swagger-ui/index.html |
| ActiveMQ console | http://localhost:8161/admin (`admin` / `admin`) |
| Health | `/actuator/health` on ports 8761, 8080, 8082 |

```bash
docker compose logs -f trainer-workload-service     # follow one service (every line carries [txn:<id>])
docker compose down                                 # stop, keep data
docker compose down -v                              # stop and wipe MySQL / MongoDB / ActiveMQ data
docker compose exec mongo-db mongosh -u root -p mongo-root-pass --authenticationDatabase admin trainer_workload
```

**Try the APIs.** gym-crm exposes registration and login publicly and requires the user JWT elsewhere.
`trainer-workload-service` only accepts an inter-service JWT (HS256, claims `scope=INTERNAL_SERVICE` and
`targetService=trainer-workload-service`, signed with `INTERNAL_JWT_SECRET`). Ready-made requests for
VS Code's REST Client: `messaging-demo-script.http` (the full flow) and `nosql-demo-script.http`
(the MongoDB module). Both work against the Compose defaults.

## Configuration

Compose reads `.env` (template: [`.env.example`](.env.example)); nothing there is required.
The variables it feeds are the ones the services already read from their property files:

| Variable | Used by | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` (Compose: `APP_PROFILE`, default `dev`) | gym-crm, trainer-workload | Spring profile, see below |
| `EUREKA_URL` | gym-crm, trainer-workload | Eureka endpoint |
| `INTERNAL_JWT_SECRET` | gym-crm, trainer-workload | Shared inter-service signing secret (≥ 32 chars, must match) |
| `JWT_SECRET` | gym-crm | User-facing JWT signing key (≥ 32 chars) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | gym-crm | MySQL connection (`dev`/`stg`/`prod`) |
| `ACTIVEMQ_BROKER_URL`, `ACTIVEMQ_USER`, `ACTIVEMQ_PASSWORD` | gym-crm, trainer-workload | Broker connection |
| `MONGODB_URI` | trainer-workload | MongoDB connection string |
| `SERVER_PORT` | all | HTTP port (set by the image) |

**Profiles.** `local` (the default outside Docker): H2 in memory, localhost broker/Mongo, for IDE work.
`dev` (the Compose default): MySQL with `hbm2ddl=update` (schema is created on first start), debug logging,
and the demo publisher endpoint in trainer-workload. `stg`/`prod`: connection settings must be supplied,
gym-crm uses `hbm2ddl=validate` (**the schema must already exist** — the project has no migration tool),
and Swagger is off in `prod`. Set `APP_PROFILE` to try them.

## Develop without containerizing the apps

Run only the infrastructure in Docker and the services from your IDE:

```bash
docker compose up -d --wait discovery-server mysql-db mongo-db activemq
```

With the `dev` profile the services' own defaults already match this setup (`localhost:3306` with
`gymcrm_dev`/`gymcrm_dev`, broker `admin`/`admin`). MongoDB in Compose has authentication on, so give
trainer-workload `MONGODB_URI=mongodb://root:mongo-root-pass@localhost:27017/trainer_workload?authSource=admin`.
The `local` profile needs no MySQL (gym-crm uses H2).

## Build and test

```bash
cd trainer-workload-service && mvn verify       # same for discovery-server and gym-crm; JDK 21
```

- **JDK 21** is required to run the tests: the surefire `argLine` uses `-XX:+EnableDynamicAgentLoading`
  (Mockito's agent), which older JVMs reject.
- **Coverage.** trainer-workload-service configures JaCoCo in its pom (report: `target/site/jacoco/index.html`).
  gym-crm's surefire `argLine` references `@{argLine}` but its pom has no JaCoCo plugin to define it, so a bare
  `mvn verify` fails with *Could not find or load main class @{argLine}*. Attach the agent from the command line
  (this is what CI does), or add the same JaCoCo block as trainer-workload-service to the pom:
  `mvn org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.12:report`

## CI/CD — `.github/workflows/ci.yml` (GitHub Actions)

| Job | Runs | Does |
|---|---|---|
| `test` | every push / PR, one matrix entry per module | JDK 21 `mvn verify`, line-coverage summary (target 80%, warning only), uploads surefire + JaCoCo reports |
| `smoke` | every push / PR | `docker compose up --build --wait`, then `scripts/smoke-test.sh`; dumps container logs on failure |
| `publish` | default branch or `v*` tag, after `test` + `smoke` pass | Builds and pushes `ghcr.io/<owner>/<repo>/<service>` tagged with branch, `latest`, short SHA and semver |

To enforce the coverage target, set `COVERAGE_ENFORCE: "true"` in the workflow `env`.
To run the published images instead of building:

```bash
IMAGE_PREFIX=ghcr.io/<owner>/<repo> IMAGE_TAG=latest docker compose pull
IMAGE_PREFIX=ghcr.io/<owner>/<repo> IMAGE_TAG=latest docker compose up -d --no-build --wait
```

(`docker login ghcr.io` first if the packages are private.) CI publishes `linux/amd64` only; on Apple Silicon
build locally, or add `platforms:` to the build step.

## Image design

One parameterized [`Dockerfile`](Dockerfile) serves all three services (`--build-arg MODULE=… PORT=…`):

- multi-stage — dependency layer (cached until the pom changes) → package → Spring Boot layered jar → JRE-only runtime
- unprivileged user (uid 10001); `./logs` is writable because logback also writes rolling files there
- `HEALTHCHECK` on `/actuator/health`; Compose uses it to start services in dependency order
- exec-form entrypoint (java is PID 1) so SIGTERM reaches Spring's graceful shutdown (`stop_grace_period: 30s`)
- container-aware heap (`-XX:MaxRAMPercentage=75`) and `-XX:+ExitOnOutOfMemoryError`
- tests are not run during the image build; CI runs them

## Troubleshooting

- **`port is already allocated`** — something local already uses 3306/27017/8080/…; set the matching `*_PORT` in `.env`.
- **Stack slow to become healthy** — gym-crm waits for MySQL, ActiveMQ and Eureka. `docker compose ps` shows the state,
  `docker compose logs <service>` the reason. Services listen on `127.0.0.1` only; set `BIND_ADDRESS=0.0.0.0` to expose them.
- **MongoDB authentication failed** — the password is embedded in `MONGODB_URI`; avoid `@ : / ? # %` or URL-encode them.
- **Changed a password after the first start** — MySQL/MongoDB only apply credentials when their data volume is created:
  `docker compose down -v`, then up again.
- **ActiveMQ image** — the official `apache/activemq-classic` repository is archived on Docker Hub (6.2.0 is the last tag).
  Override with `ACTIVEMQ_IMAGE` (it must honour `ACTIVEMQ_CONNECTION_USER/PASSWORD` and `ACTIVEMQ_WEB_USER/PASSWORD`).
- **Windows** — run `scripts/smoke-test.sh` from Git Bash or WSL (needs `curl`, `jq`, `openssl`).
  `.gitattributes` keeps `*.sh` and `Dockerfile` on LF line endings.

## Security notes

All defaults (`admin/admin`, demo JWT secrets, MongoDB/MySQL passwords) are for local use. Before exposing anything
beyond a laptop: change every secret, keep the ports on loopback or behind a proxy, and use a least-privilege MongoDB
user instead of the root account. Note that gym-crm's `/actuator/health` (existing setting) shows component
details to anonymous callers.

## Repository layout

```
discovery-server/            Eureka server                    (independent Maven project)
gym-crm/                     REST API, JWT, MySQL, JMS producer
trainer-workload-service/    JMS consumer, MongoDB, REST read API
Dockerfile                   one parameterized build for the three services
docker-compose.yml           the full stack: 3 services + MySQL + MongoDB + ActiveMQ
.env.example                 optional overrides for Compose
scripts/smoke-test.sh        end-to-end check of a running stack (also run in CI)
.github/workflows/ci.yml     test → smoke → publish
```
