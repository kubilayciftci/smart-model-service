# smart-model-service

![ci](https://github.com/USER/REPO/actions/workflows/ci.yml/badge.svg)

> After pushing to GitHub, replace `USER/REPO` in the badge URL above with your actual repository path.

A cloud-native microservice for the Togg TruX platform that manages **Smart Models** (smart services such as OpenWeatherMap, smart devices such as a smart watch) and their **Smart Features** (callable capabilities such as "Get Weekly Forecast in a City"). The service exposes a gRPC API on Quarkus with PostgreSQL persistence, combining a strict relational core (immutable business identifiers, foreign keys, cascade semantics) with a JSONB column for domain-specific dynamic attributes. The stack ships with full observability (Prometheus metrics, OpenTelemetry traces to Jaeger, JSON logs with trace correlation), a one-command Docker Compose environment, and build-time-generated Kubernetes manifests for Minikube.

## Architecture

```mermaid
flowchart LR
    client[gRPC Client / grpcurl] -->|:9000 plaintext| grpcsvc[SmartModelGrpcService\n@RunOnVirtualThread]
    grpcsvc --> mapper[SmartModelProtoMapper]
    mapper --> modelsvc[SmartModelCatalogService]
    mapper --> featsvc[SmartFeatureCatalogService]
    modelsvc --> ports[(Domain Ports)]
    featsvc --> ports
    ports --> repo[Panache Repositories]
    repo --> pg[(PostgreSQL + JSONB)]
    grpcsvc -.traces.-> jaeger[Jaeger]
    modelsvc -.metrics.-> prom[Prometheus] --> grafana[Grafana]
```

Requests arrive on the dedicated gRPC server (port 9000, plaintext, reflection enabled) and are dispatched on Java virtual threads, so the entire call path is plain synchronous code. The adapter layer maps protobuf messages to domain commands, the service layer orchestrates validation and transactions, and Panache repositories implement the domain ports against PostgreSQL. HTTP port 8080 serves only operational endpoints (metrics, health).

## Prerequisites

| Path | Requirements |
| --- | --- |
| `make run`, `make test`, `make build` | Java 21, Docker (Quarkus Dev Services starts an ephemeral PostgreSQL via Testcontainers) |
| `make up` / `make down` | Docker only (multi-stage Dockerfile builds the application inside the container) |
| `make deploy` / `make undeploy` | Java 21, Docker, Minikube, kubectl |
| `make demo` | grpcurl, python3 (plus a running instance via `make run` or `make up`) |

## Quick Start

**Path 1 — Dev mode** (live reload, Dev Services provisions PostgreSQL automatically):

```bash
make run
```

**Path 2 — Full local stack** (application + PostgreSQL + Prometheus + Grafana + Jaeger):

```bash
make up
make demo
make down
```

**Path 3 — Minikube** (builds the image into the Minikube Docker daemon, applies the generated manifests):

```bash
minikube start
make deploy
kubectl port-forward deployment/smart-model-service 9000:9000 8080:8080 &
make demo
kill %1
make undeploy
```

The trailing `&` keeps the port-forward running in the background so the whole flow works in a single terminal; `kill %1` stops it once you are done. One or two application pod restarts while the in-cluster PostgreSQL initializes are normal — the generated manifests deliberately rely on Kubernetes restart-and-retry semantics instead of a custom init Job (see the Kubernetes manifests ADR). OpenTelemetry export warnings in the pod logs are expected on Minikube — no OTLP collector is deployed there (the compose stack is where traces land in Jaeger).

## Lifecycle Management (`make`)

| Target | What it does |
| --- | --- |
| `make run` | `./mvnw quarkus:dev` — dev mode with live reload; Dev Services auto-starts PostgreSQL |
| `make build` | `./mvnw clean package` — JVM build; auto-generates Kubernetes manifests under `target/kubernetes/` |
| `make build-native` | Container-based GraalVM native image build (`-Dnative -Dquarkus.native.container-build=true`) |
| `make test` | `./mvnw test` — unit and integration tests against a real PostgreSQL Testcontainer |
| `make up` | `docker compose up -d --build` — full stack: service, PostgreSQL, Prometheus, Grafana, Jaeger |
| `make down` | `docker compose down -v` — stops the stack and removes volumes |
| `make deploy` | Builds the image inside the Minikube daemon, applies `deploy/k8s/postgres.yaml` and `target/kubernetes/minikube.yml` |
| `make undeploy` | Deletes the deployed Kubernetes resources |
| `make demo` | Runs `scripts/demo.sh` — an end-to-end grpcurl walkthrough of the API |

## API (grpcurl)

The gRPC server listens on `localhost:9000` in plaintext with **server reflection enabled** (`quarkus.grpc.server.enable-reflection-service=true`), so no local `.proto` files are needed. All examples are copy-pasteable. Field names use the proto snake_case form; grpcurl accepts them directly.

### Discover the API via reflection

```bash
grpcurl -plaintext localhost:9000 list
grpcurl -plaintext localhost:9000 describe togg.trux.smartmodel.v1.SmartModelService
grpcurl -plaintext localhost:9000 grpc.health.v1.Health/Check
```

### CreateModel

```bash
grpcurl -plaintext -d '{
  "identifier": "demo-thermostat",
  "name": "Demo Thermostat",
  "type": "device",
  "category": "climate",
  "attributes_json": "{\"manufacturer\":\"Acme\",\"protocol\":\"zigbee\"}",
  "features": [{
    "identifier": "set-target-temperature",
    "name": "Set Target Temperature",
    "type": "command",
    "category": "climate-control",
    "attributes_json": "{\"unit\":\"celsius\",\"min\":5,\"max\":35}"
  }]
}' localhost:9000 togg.trux.smartmodel.v1.SmartModelService/CreateModel
```

The response contains the generated `id` (UUID); running the command a second time returns `ALREADY_EXISTS` by design, since `identifier` is a unique business key. To capture an `id` for the follow-up commands, query a seeded model:

```bash
MODEL_ID=$(grpcurl -plaintext -d '{"identifier":"smart-watch"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/SearchModels \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["models"][0]["id"])')
echo "$MODEL_ID"
```

### GetModel

Returns the full aggregate including all features.

```bash
grpcurl -plaintext -d '{"id": "'"$MODEL_ID"'"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/GetModel
```

### UpdateModel

`identifier` is an immutable business key and cannot be changed; all other descriptor fields are replaced.

```bash
grpcurl -plaintext -d '{
  "id": "'"$MODEL_ID"'",
  "name": "Smart Watch Pro",
  "type": "device",
  "category": "wearable",
  "attributes_json": "{\"manufacturer\":\"Acme\",\"firmwareVersion\":\"3.0.0\",\"connectivity\":\"ble\"}"
}' localhost:9000 togg.trux.smartmodel.v1.SmartModelService/UpdateModel
```

### SearchModels

All filter fields are `optional` and freely composable. `name` is a case-insensitive partial match; `identifier`, `type` and `category` are exact matches. Results are paginated summaries with a `feature_count` instead of embedded features.

```bash
grpcurl -plaintext -d '{"identifier": "openweathermap"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/SearchModels

grpcurl -plaintext -d '{"name": "movie"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/SearchModels

grpcurl -plaintext -d '{"type": "device", "category": "surveillance"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/SearchModels

grpcurl -plaintext -d '{"page": 1, "page_size": 2}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/SearchModels
```

Pages are 1-based (`0` or an absent `page` is treated as page 1) and `page_size` is capped at 100. The first three match the seeded `openweathermap`, `imdb-movie-database` and `remote-camera` models respectively; the last pages through the whole catalog.

### AddFeature

```bash
grpcurl -plaintext -d '{
  "model_id": "'"$MODEL_ID"'",
  "feature": {
    "identifier": "get-heart-rate",
    "name": "Get Heart Rate",
    "type": "telemetry",
    "category": "health",
    "attributes_json": "{\"unit\":\"bpm\",\"samplingRate\":\"1s\"}"
  }
}' localhost:9000 togg.trux.smartmodel.v1.SmartModelService/AddFeature
```

Capture the returned feature id for the next two commands:

```bash
FEATURE_ID=$(grpcurl -plaintext -d '{"model_id":"'"$MODEL_ID"'","identifier":"get-heart-rate"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/SearchFeatures \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["features"][0]["id"])')
```

### UpdateFeature

```bash
grpcurl -plaintext -d '{
  "feature_id": "'"$FEATURE_ID"'",
  "name": "Get Resting Heart Rate",
  "type": "telemetry",
  "category": "health",
  "attributes_json": "{\"unit\":\"bpm\",\"samplingRate\":\"5s\"}"
}' localhost:9000 togg.trux.smartmodel.v1.SmartModelService/UpdateFeature
```

### SearchFeatures

Same composable filter semantics as `SearchModels`, plus an optional `model_id` scope.

```bash
grpcurl -plaintext -d '{"model_id": "'"$MODEL_ID"'"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/SearchFeatures

grpcurl -plaintext -d '{"category": "forecast"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/SearchFeatures

grpcurl -plaintext -d '{"name": "screenshot"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/SearchFeatures
```

The `forecast` search returns the two seeded OpenWeatherMap features; the `screenshot` search returns the remote-camera feature.

### RemoveFeature

Removing a feature from the aggregate physically deletes the row (orphan removal).

```bash
grpcurl -plaintext -d '{"feature_id": "'"$FEATURE_ID"'"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/RemoveFeature
```

### DeleteModel

Deletes the model and cascades to all of its features.

```bash
grpcurl -plaintext -d '{"id": "'"$MODEL_ID"'"}' localhost:9000 \
  togg.trux.smartmodel.v1.SmartModelService/DeleteModel
```

Errors are mapped centrally to canonical gRPC status codes: `NOT_FOUND` for missing ids, `ALREADY_EXISTS` for duplicate identifiers, `INVALID_ARGUMENT` for validation failures.

## Architecture Decision Records (ADR)

| Decision | Choice | Alternative considered | Why |
| --- | --- | --- | --- |
| Runtime framework | Quarkus (Java 21) | Rust (tonic) | Build-time DI, Dev Services, Panache, first-class gRPC and generated k8s manifests deliver senior-grade cloud-native output at Java-ecosystem velocity; native image closes most of the footprint gap where it matters |
| Database | PostgreSQL | Document store (MongoDB) | The catalog has a strict relational core (unique business identifiers, FK with cascade, referential integrity) that a document store would push into application code; JSONB provides the flexible half within the same engine |
| Dynamic attributes | `JSONB` column (`attributes_json`) | EAV tables / fixed per-domain columns | Domain-specific attributes vary per model type; JSONB keeps them queryable and indexable next to strict columns without EAV join explosion or per-domain migrations |
| Developer experience | Makefile | Raw command list in README / task runners | One memorable verb per lifecycle stage, zero extra tooling, works identically for the evaluator and CI documentation |
| Concurrency model | Virtual threads (`@RunOnVirtualThread`) | Reactive Mutiny pipelines or `@Blocking` worker pool | Confirmed working on Quarkus 3.20.1: blocking JPA executes on `quarkus-virtual-thread-N` carriers, giving synchronous readability with event-loop-grade scalability and no `@Blocking` fallback; pgjdbc 42.7.x replaced `synchronized` with `java.util.concurrent` locks, so JDBC calls park the virtual thread instead of pinning the carrier |
| `type` / `category` typing | Free-form strings | Protobuf/Java enums | Open-Closed Principle: new domains arrive as data, not as proto and schema changes that force lock-step redeploys of every client |
| Schema management | Flyway versioned migrations | Hibernate auto-DDL | Deterministic, reviewable schema history; the ORM runs in `validate` mode and never mutates a production schema |
| Pagination | Offset + page size | Opaque page tokens (keyset) | Admin-facing catalog with modest cardinality; offset composes trivially with dynamic filters and yields an exact `total_count`, keyset complexity is unjustified here |
| Name matching | `ILIKE` partial match | Full-text search / exact equality | Case-insensitive substring is the intuitive catalog search UX; a full-text stack adds analyzers and indexes the use case does not need |
| Feature lifecycle | Aggregate root with `cascade = ALL`, `orphanRemoval = true` | Independent feature repository writes | The model owns its feature collection; removing a feature from the Java collection physically deletes the row, so orphan data cannot leak and invariants live in one place |
| Kubernetes manifests | Generated by `quarkus-kubernetes` / `quarkus-minikube` | Hand-written YAML / Helm chart | Manifests derive from `application.properties` at build time — a single source of truth that cannot drift from the application configuration |
| Port design | Split `SmartModelReadPort` / `SmartModelWritePort` | Single fat repository interface | Interface Segregation: each consumer depends only on the operations it uses, and the write surface stays small and auditable |
| Search payload | Summaries with `feature_count`, no embedded features | Full aggregates in search results | Avoids N+1 selects and Hibernate HHH000104 in-memory pagination when fetch-joining collections; `GetModel` serves the full aggregate on demand |
| UpdateModel response | Re-read the aggregate after the mutation | Partial response / protobuf `FieldMask` | The response message includes `features`, so the adapter re-reads via a fetch-join after the transactional update to cross the lazy-loading boundary safely; `FieldMask` semantics add client complexity disproportionate to a full-row update API |
| Build locale pin | `.mvn/jvm.config` with `-Duser.language=en -Duser.country=US` | Default JVM locale | Protobuf/gRPC code generation breaks under the Turkish JVM locale (dotless-i case folding); pinning makes every Maven invocation reproducible on any machine, including the evaluator's |
| SQL observability | `quarkus.datasource.jdbc.telemetry=true` | Tracing at the gRPC boundary only | Every SQL statement appears as a child span in Jaeger, exposing per-query latency inside each request trace at negligible overhead |

## Observability

Available with the Compose stack (`make up`):

| Tool | URL | Notes |
| --- | --- | --- |
| Prometheus | <http://localhost:9090> | Scrapes the service at `/q/metrics` |
| Grafana | <http://localhost:3000> | Anonymous admin access; pre-provisioned Smart Model dashboard |
| Jaeger | <http://localhost:16686> | One trace per gRPC request, with SQL child spans |
| Metrics endpoint | <http://localhost:8080/q/metrics> | Prometheus exposition format (business + JVM metrics) |
| Health | <http://localhost:8080/q/health> | Liveness `/q/health/live`, readiness `/q/health/ready`; gRPC health via `grpc.health.v1.Health/Check` |

Business metrics exposed by the service:

- `smartmodel_created_total`
- `smartmodel_deleted_total`
- `smartfeature_created_total`
- `smartmodel_search_total`
- `smartfeature_search_total`

In the prod profile every log line is structured JSON (`quarkus-logging-json`) and carries the `traceId` and `spanId` of the active OpenTelemetry span, so any log entry can be correlated with its full request trace in Jaeger. The prod profile also deliberately sets `%prod.quarkus.log.category."org.hibernate.SQL".level=DEBUG` as a demonstration choice so that per-request `traceId` correlation is immediately visible in the JSON log stream; a real deployment would drop this in favor of request-level access logging.

## JVM vs Native

Both packaging modes run the identical codebase; the native image is built inside a container (`make build-native`), so no local GraalVM installation is required.

| Metric | JVM (`make build`) | Native (`make build-native`) |
| --- | --- | --- |
| Startup time | 2.524 s | 0.094 s |
| Memory (RSS) after startup | 451.5 MiB | 71.0 MiB |
| Container image size | 592 MB | 175 MB |

Both modes were measured as Linux aarch64 containers on the same Docker host and Compose network against the same PostgreSQL instance; startup time is the Quarkus-reported boot time from the container log, and memory is the container RSS from `docker stats` after serving requests.

## Project Structure

```
smart-model-service/
├── src/main/proto/                          gRPC contract — the single source of truth for the API
├── src/main/java/com/togg/trux/smartmodel/
│   ├── domain/                              entities, ports, search criteria, domain exceptions — zero gRPC imports
│   ├── service/                             business orchestration, validation, command records
│   ├── adapter/in/grpc/                     gRPC endpoint + proto/domain mapper — the only package importing generated gRPC classes
│   ├── adapter/out/persistence/             Panache repositories + composable dynamic query builder
│   └── infrastructure/                      seed data initializer, business metrics, global gRPC exception mapping
├── src/main/resources/db/migration/         Flyway versioned schema (V1__initial_schema.sql)
├── deploy/                                  Prometheus/Grafana provisioning, Jaeger wiring, k8s PostgreSQL manifest
├── scripts/demo.sh                          end-to-end grpcurl walkthrough
├── Dockerfile · docker-compose.yml · Makefile
└── .github/workflows/ci.yml                 build-and-test pipeline
```

SOLID mapping:

- **SRP** — mapping, orchestration, persistence and telemetry each live in a dedicated class with a single reason to change.
- **OCP** — `type`/`category` as free strings plus JSONB attributes let new domains extend the system through data, never through code or proto edits.
- **LSP** — repositories are substitutable behind domain ports; tests exercise the port contract, not the Panache implementation.
- **ISP** — read and write ports are segregated, so consumers such as the seed initializer depend only on the operations they invoke.
- **DIP** — the domain defines the ports; adapters implement them; every dependency arrow points inward.

## Seed Data

On startup, if the catalog is empty, a `@Observes StartupEvent` initializer inserts four diverse models with six features:

| Model | Identifier | Type / Category | Features |
| --- | --- | --- | --- |
| OpenWeatherMap | `openweathermap` | service / weather | Get Weekly Forecast in a City, Get Daily Forecast for a State |
| IMDB Movie Database | `imdb-movie-database` | service / entertainment | Search Movie |
| Smart Watch | `smart-watch` | device / wearable | Get Calories Burned for a Day |
| Remotely Controllable Camera | `remote-camera` | device / surveillance | Take Camera Screenshot, Get Camera Live Video URL |

Each entry carries realistic JSONB attributes (API base URLs, firmware versions, protocols), demonstrating how strict relational columns and dynamic attributes coexist. Seeding is idempotent: a non-empty catalog is left untouched.

## Production Considerations

Deliberately out of scope for this demonstration, and the first items to address before production traffic:

- **TLS / mTLS on the gRPC listener** — the server runs plaintext for evaluator convenience; production requires TLS at minimum, ideally mTLS between services.
- **Authentication and authorization** — no caller identity is enforced; per-RPC authorization (token-based or mTLS-identity-based) belongs in front of every mutating call.
- **HPA and PodDisruptionBudget** — the generated manifests pin a single replica; production needs horizontal autoscaling plus a disruption budget to survive node drains.
- **Managed / HA PostgreSQL** — the in-cluster single-node PostgreSQL is demo-grade; production should use a managed or replicated HA database with connection pooling sized to the workload. It also uses `emptyDir` storage, so its data is ephemeral and vanishes with the pod — acceptable only for this demonstration.
- **Secrets management** — the PostgreSQL credentials committed in `deploy/k8s/postgres.yaml` are intentionally demo-grade; production credentials belong in a Kubernetes `Secret` backed by an external secrets manager.
- **Optimistic locking** — entities carry no `@Version` column, so concurrent updates follow last-writer-wins; version-based conflict detection should be added before multi-writer scenarios.
- **Constraint-race mapping to `ALREADY_EXISTS`** — duplicate identifiers are caught by a pre-check plus a database unique constraint; under a concurrent duplicate create the constraint violation currently surfaces as `INTERNAL` and should be translated (SQLSTATE 23505) to `ALREADY_EXISTS`.
- **Backup and restore strategy** — no WAL archiving or point-in-time recovery is configured; production needs scheduled backups with routinely tested restores.
