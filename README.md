# Apollo Federation vs Event-Driven Projections Demo

> **Architecture is all about tradeoffs.** This demo lets you experience those tradeoffs firsthand.

A side-by-side demonstration of two distributed systems architectures:

1. **GraphQL Federation** — Synchronous composition, real-time data, coupled availability
2. **Event-Driven Projections** — Asynchronous events, local materialized views, eventual consistency

Both architectures model the same domain: **Person/Employee/Badge** across HR, Employment, and Security bounded contexts.

### ⚠️ What This Is (and Isn't)

This is a **learning tool**, not a production benchmark. The implementations are intentionally basic and unoptimized to clearly illustrate architectural patterns and their inherent tradeoffs. Real-world systems would include caching, connection pooling, optimized queries, and many other improvements.

**The goal:** Help you understand *when* each pattern shines and *why* you might choose one over the other—not to declare a winner.

---

## Root Configuration Files

The project root contains three orchestration files for different use cases:

| File | Purpose | When to Use |
|------|---------|-------------|
| **`Makefile`** | Cross-platform command shortcuts | Primary interface for most operations (`make up`, `make down`, etc.) |
| **`Tiltfile`** | Kubernetes development orchestration | Used by Tilt for live-reload local development on K8s |
| **`docker-compose.yml`** | Standalone Docker orchestration | When you want to run without Kubernetes |

---

## Prerequisites

- [Docker Desktop](https://docs.docker.com/get-docker/) with Kubernetes enabled
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Tilt](https://docs.tilt.dev/install.html)
- Java 17+
- `make` (included on Mac/Linux; Windows users: use Git Bash, WSL, or [install make](https://gnuwin32.sourceforge.net/packages/make.htm))

## Quick Start

```bash
# 1. Check prerequisites and pre-build (first time only)
make setup

# 2. Start all services
make up
```

Or just check prerequisites without building:
```bash
make prereqs
```

### Start Specific Stacks

```bash
make federation-only   # Just Federation architecture
make event-only        # Just Event-Driven Projections architecture
```

### Stop

```bash
make down              # Stop services
make clean             # Stop and delete namespaces
```

### Alternative: Docker Compose (No Kubernetes)

If you don't want to use Tilt/Kubernetes:

```bash
# Build and start all services
docker compose up --build

# Stop
docker compose down
```

### Manual Setup (No Make Required)

If you can't or don't want to use `make`:

**Windows (PowerShell):**
```powershell
# 1. Check prerequisites and pre-build
.\infra\tilt\scripts\setup-dev.ps1

# 2. Start with Tilt
tilt up

# Or start with Docker Compose
docker compose up --build
```

**Mac/Linux (Bash):**
```bash
# 1. Check prerequisites and pre-build
./infra/tilt/scripts/setup-dev.sh

# 2. Start with Tilt
tilt up

# Or start with Docker Compose
docker compose up --build
```

**Skip pre-build entirely** (Tilt/Docker will build on first run, just slower):
```bash
tilt up
# or
docker compose up --build
```

## Access Points

| Service | URL |
|---------|-----|
| Dashboard | http://localhost:3000 |
| Federation Router | http://localhost:4000 |
| Projection Service | http://localhost:8090 |
| Tilt UI | http://localhost:10350 |

## Using the Demo

Once running, open the **Dashboard** at http://localhost:3000

### What You Can Do

1. **Compare Query Performance**
   - The dashboard shows Federation vs Event-Driven side-by-side
   - See latency differences for the same queries
   - Observe real-time vs eventual consistency

2. **Create Data**
   - Use the forms to add new people
   - Both architectures will reflect the new data
   - Event-Driven shows a brief lag before data appears (Kafka propagation)

3. **Test Failure Scenarios**
   ```bash
   make kill-security     # Stop Security service
   make restore-security  # Bring it back
   ```
   - Federation queries fail when services are down
   - Event-Driven continues working with stale data

4. **Run Automated Demo**
   ```bash
   make demo
   ```

5. **Explore GraphQL**
   - Open Apollo Sandbox at http://localhost:4000
   - Run queries across federated subgraphs

### Example Query (Federation)

```graphql
{
  persons {
    id
    name
    email
    employee {
      title
      department
    }
    badge {
      badgeNumber
      accessLevel
    }
  }
}
```

---

## Architecture Comparison

Both architectures serve the same data but make fundamentally different tradeoffs.

### GraphQL Federation

```
┌─────────────────────────────────────────────────────────┐
│                    Client Query                         │
└─────────────────────────┬───────────────────────────────┘
                          │ Router orchestrates subgraph calls
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │    HR    │    │Employment│    │ Security │
    │ Subgraph │    │ Subgraph │    │ Subgraph │
    └────┬─────┘    └────┬─────┘    └────┬─────┘
         ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ HR DB    │    │ Emp DB   │    │ Sec DB   │
    └──────────┘    └──────────┘    └──────────┘

✓ Data is always fresh (real-time)
✗ Latency is additive across services
✗ One service down = entire query fails
```

### Event-Driven Projections

```
WRITE PATH (async):
┌──────────┐     ┌──────────┐     ┌──────────┐
│ HR Event │ ──► │  Kafka   │ ──► │ Consumer │ ──► Local Projection
│ Service  │     │          │     │          │
└──────────┘     └──────────┘     └──────────┘

READ PATH (sync):
┌─────────────────────────────────────────────────────────┐
│                    Client Query                         │
└─────────────────────────┬───────────────────────────────┘
                          │ Single local query
                          ▼
                    ┌──────────┐
                    │Projection│
                    │ Service  │
                    └────┬─────┘
                         ▼
                    ┌──────────┐
                    │  Local   │
                    │   DB     │
                    └──────────┘

✓ Blazing fast reads (local data)
✓ Services can be down; queries still work
✗ Data may be stale (eventual consistency)
✗ More complex infrastructure (Kafka, consumers)
```

---

## Key Tradeoffs

| Aspect | Federation | Event-Driven Projections |
|--------|-----------|-------------------|
| **Query Latency** | Higher (multiple network hops) | Lower (local database) |
| **Data Freshness** | Real-time | Eventually consistent |
| **Service Coupling** | Tight (all must be up) | Loose (async via Kafka) |
| **Failure Mode** | Cascading failures | Graceful degradation |
| **Write Complexity** | Simple (direct mutation) | Complex (event + propagation) |
| **Infrastructure** | Simpler | More moving parts |

**Neither is "better"** — the right choice depends on your requirements for consistency, availability, and latency.

---

## Project Structure

```
├── clients/
│   └── dashboard/               # React comparison dashboard
│
├── services/
│   ├── federation/              # GraphQL Federation subgraphs
│   │   ├── hr-subgraph/
│   │   ├── employment-subgraph/
│   │   └── security-subgraph/
│   └── event/                   # Event-Driven services
│       ├── hr-events-service/
│       ├── employment-events-service/
│       ├── security-events-service/
│       ├── projection-consumer/
│       └── query-service/
│
├── infra/                       # Infrastructure configs
│   ├── docker/                  # Shared Dockerfiles — [README](infra/docker/README.md)
│   ├── k8s/                     # Kubernetes manifests — [README](infra/k8s/README.md)
│   ├── maven/                   # Shared Maven wrapper
│   ├── router/                  # Apollo Router config — [README](infra/router/README.md)
│   ├── scripts/                 # Demo scripts — [README](infra/scripts/README.md)
│   └── tilt/                    # Tilt setup scripts — [README](infra/tilt/README.md)
│
├── tests/                       # Playwright E2E tests
│
├── Makefile                     # Command shortcuts
├── Tiltfile                     # Tilt/K8s orchestration
└── docker-compose.yml           # Docker orchestration
```

## Technology Stack

- **Subgraphs**: Quarkus 3.x + SmallRye GraphQL
- **Router**: Apollo Router v1.57.1
- **Event Streaming**: Apache Kafka 3.9.0 (KRaft mode)
- **Database**: PostgreSQL 15
- **Local Dev**: Tilt + Docker Desktop Kubernetes

## All Make Commands

```bash
make help             # Show all commands
make setup            # Check prerequisites + pre-build
make prereqs          # Just check prerequisites
make up               # Start all services
make down             # Stop services
make clean            # Full cleanup
make federation-only  # Start Federation only
make event-only       # Start Event-Driven Projections only
make demo             # Run demo script
make test             # Run Playwright tests
make kill-security    # Stop Security service
make restore-security # Restore Security service
make logs-kafka       # Tail Kafka logs
make lag              # Show consumer lag
```

## Troubleshooting

**`make` command not found (Windows)**
- Use Git Bash instead of PowerShell/CMD, or
- Run PowerShell scripts directly: `.\infra\tilt\scripts\setup-dev.ps1`

**Docker build fails**
- Ensure Docker Desktop is running
- Try `docker system prune` to free up space

**Services not starting**
- Check Docker Desktop has enough memory (4GB+ recommended)
- Verify Kubernetes is enabled in Docker Desktop settings

**Port already in use**
- Stop other services using ports 3000, 4000, 8080-8085, 8090
- Run `docker compose down` to clean up orphaned containers

## Documentation

- [Tilt Development Guide](docs/tilt-development.md) — Detailed local dev setup
