# Federation vs Event-Driven CQRS Architecture Comparison Demo

A side-by-side demonstration of two distributed systems architectures:

1. **GraphQL Federation** - Synchronous composition, real-time data, coupled availability
2. **Event-Driven CQRS** - Asynchronous events, local projections, eventual consistency

Both architectures model the same domain: **Person/Employee/Badge** across HR, Employment, and Security bounded contexts.

## Prerequisites

- [Docker Desktop](https://docs.docker.com/get-docker/) with Kubernetes enabled
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Tilt](https://docs.tilt.dev/install.html)
- Java 17+

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
make cdc-only          # Just Event-Driven CQRS architecture
```

### Stop

```bash
make down              # Stop services
make clean             # Stop and delete namespaces
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
   - The dashboard shows Federation vs Event-Driven CQRS side-by-side
   - See latency differences for the same queries
   - Observe real-time vs eventual consistency

2. **Create Data**
   - Use the forms to add new people
   - Both architectures will reflect the new data
   - Event-Driven CQRS shows a brief lag before data appears

3. **Test Failure Scenarios**
   ```bash
   make kill-security     # Stop Security service
   make restore-security  # Bring it back
   ```
   - Federation queries fail when services are down
   - Event-Driven CQRS continues working with stale data

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

## Architecture Comparison

### GraphQL Federation
```
┌─────────────────────────────────────────────────────────┐
│                    Client Query                         │
└─────────────────────────┬───────────────────────────────┘
                          │ 3 network calls (sync)
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

Latency: 45-100ms (additive)
Consistency: REAL-TIME
Failure: 1 service down = query fails
```

### Event-Driven CQRS
```
┌─────────────────────────────────────────────────────────┐
│                    Client Query                         │
└─────────────────────────┬───────────────────────────────┘
                          │ 1 local query
                          ▼
                    ┌──────────┐
                    │Projection│
                    │ Service  │
                    └────┬─────┘
                         ▼
                    ┌──────────┐
                    │  Local   │ ◄─── Consumer ◄─── Kafka
                    │Projections│
                    └──────────┘

Latency: 3-10ms (local)
Consistency: 1-5s lag (eventual)
Failure: Queries work (stale data)
```

## Key Tradeoffs

| Aspect | Federation | Event-Driven CQRS |
|--------|-----------|-------------------|
| Query Latency | High (additive) | Low (local) |
| Data Freshness | Real-time | Eventually consistent |
| Service Coupling | Tight | Loose |
| Failure Mode | Cascading | Isolated |
| Complexity | Lower | Higher |

## Project Structure

```
├── hr-subgraph/              # Federation: HR Person subgraph
├── employment-subgraph/      # Federation: Employment subgraph
├── security-subgraph/        # Federation: Security subgraph
├── router/                   # Apollo Router config
│
├── hr-cdc-service/          # Event-Driven: HR service with Outbox
├── employment-cdc-service/  # Event-Driven: Employment service
├── security-cdc-service/    # Event-Driven: Security service
├── cdc-projection-consumer/ # Event-Driven: Kafka consumer
├── cdc-query-service/       # Event-Driven: Projection service
│
├── comparison-dashboard/    # React dashboard
├── k8s/                     # Kubernetes manifests
├── tilt/scripts/            # Setup scripts
├── scripts/                 # Demo scripts
├── tests/                   # Playwright tests
├── Tiltfile                 # Tilt configuration
└── Makefile                 # Make commands
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
make cdc-only         # Start Event-Driven CQRS only
make demo             # Run demo script
make test             # Run Playwright tests
make kill-security    # Stop Security service
make restore-security # Restore Security service
make logs-kafka       # Tail Kafka logs
make lag              # Show consumer lag
```

## Documentation

- [Tilt Development Guide](docs/tilt-development.md) - Detailed local dev setup
