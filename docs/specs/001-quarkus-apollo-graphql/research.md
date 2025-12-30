# Research: Quarkus Apollo GraphQL Demo

**Feature Branch**: `001-quarkus-apollo-graphql`
**Date**: 2025-12-21

## 1. GraphQL Framework Selection

### Decision: Use SmallRye GraphQL Extension (Native Quarkus)

**Rationale:**
- SmallRye GraphQL is the official, native GraphQL solution for Quarkus
- Implements MicroProfile GraphQL specification with full Quarkus integration
- Provides built-in GraphiQL UI for schema exploration (enabled by default in dev mode)
- Supports both standard GraphQL operations and subscriptions
- Zero additional configuration required for basic setup

**Alternatives Considered:**
- **graphql-java directly**: Rejected - SmallRye provides higher-level abstractions with less boilerplate
- **Apollo Server (Node.js)**: Rejected - would require Node.js runtime, conflicts with Quarkus (Java) architecture
- **Custom GraphQL implementation**: Rejected - unnecessary complexity when SmallRye is production-ready

**Implementation:**
```bash
quarkus ext add io.quarkus:quarkus-smallrye-graphql
```

---

## 2. Apollo Framework Clarification

### Decision: No Apollo Components Required; Use Built-in GraphiQL

**Rationale:**
The user mentioned "Apollo framework" but likely meant "GraphQL with a visual explorer." Clarification:

| Apollo Product | Description | Our Use |
|----------------|-------------|---------|
| Apollo Client | Frontend state management (React/JS) | Not needed - no frontend |
| Apollo Server | Node.js GraphQL server | Conflicts with Quarkus |
| Apollo Federation | Distributed GraphQL architecture | Overkill for basic demo |
| Apollo Router | High-performance gateway | Not needed for single service |

SmallRye GraphQL includes GraphiQL UI at `http://localhost:8080/q/graphql-ui/` which provides exactly what was requested: "some way to see, understand, or visualize that the graph is working."

**Alternatives Considered:**
- **Apollo Sandbox**: Modern cloud-based explorer, could be pointed at Quarkus endpoint. Rejected - adds external dependency when GraphiQL is bundled
- **GraphQL Playground**: Deprecated since 2022, EOL December 31, 2022. Rejected due to end-of-life status

---

## 3. GraphQL UI/Playground

### Decision: Use Built-in GraphiQL

**Rationale:**
- Zero configuration - automatically enabled in dev mode
- Can be enabled in production via `quarkus.smallrye-graphql.ui.always-include=true`
- Full-featured: schema introspection, query execution, auto-completion, syntax highlighting
- Supports WebSocket subscriptions

**Accessible Paths:**
- Dev UI: `http://localhost:8080/q/dev-ui/io.quarkus.quarkus-smallrye-graphql/graphql-ui`
- Direct: `http://localhost:8080/q/graphql-ui/`

**Configuration:**
```properties
quarkus.smallrye-graphql.ui.always-include=true
```

---

## 4. Docker Compose Strategy

### Decision: Multi-Stage JVM Build with Health Checks

**Rationale:**
- JVM mode provides faster build times suitable for demo purposes
- Multi-stage Dockerfile minimizes image size (~200MB vs ~800MB single-stage)
- Health checks ensure GraphQL endpoint is ready before marking service healthy
- Native image compilation (GraalVM) rejected - adds complexity, longer build times for basic demo

**Docker Compose Pattern:**
```yaml
version: '3.8'
services:
  graphql-api:
    build:
      context: .
      dockerfile: src/main/docker/Dockerfile.jvm
    ports:
      - "8080:8080"
    environment:
      QUARKUS_HTTP_HOST: 0.0.0.0
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/q/health/ready"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
```

**Alternatives Considered:**
- **Native Image (GraalVM)**: 10-100x faster startup, lower memory. Rejected for demo due to 5-10 minute build times and GraalVM requirement
- **Single-stage Dockerfile**: Simpler but larger image. Rejected for production-like demo
- **Dev mode in container**: Not representative of production deployment

---

## 5. Sample Data Strategy

### Decision: In-Memory Data with @Startup Loading

**Rationale:**
- Spec requires "sample data so queries return meaningful results immediately"
- No external database required - simplifies Docker setup
- Data loaded at application startup via Quarkus `@Startup` bean
- Sufficient for demo purposes, can be replaced with real database later

**Implementation Pattern:**
```java
@ApplicationScoped
@Startup
public class DataLoader {
    @PostConstruct
    void loadData() {
        // Create sample Products and Categories
    }
}
```

**Alternatives Considered:**
- **PostgreSQL/H2 database**: Adds complexity, requires docker-compose dependency management
- **JSON fixtures**: Requires file parsing, less type-safe
- **Hardcoded in resolvers**: Less maintainable, harder to modify

---

## Summary: Technology Stack

| Component | Choice | Version |
|-----------|--------|---------|
| Runtime | Quarkus | 3.x (latest LTS) |
| GraphQL | SmallRye GraphQL Extension | Bundled with Quarkus |
| UI Explorer | GraphiQL (built-in) | Bundled with SmallRye |
| Container | Docker with JVM image | eclipse-temurin:17-jre-alpine |
| Orchestration | Docker Compose | 3.8 |
| Data | In-memory (no database) | N/A |
| Build | Maven | 3.9.x |
| Java | Eclipse Temurin | 17 LTS |

---

## Architecture Diagram

```
┌─────────────────────────────────────────────┐
│  User Browser                               │
│  └─> http://localhost:8080/q/graphql-ui/   │ ← GraphiQL UI
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│  Docker Container: quarkus-graphql-demo     │
│  ┌───────────────────────────────────────┐  │
│  │  Quarkus Application (Port 8080)      │  │
│  │  ├─ SmallRye GraphQL (/graphql)       │  │
│  │  ├─ GraphiQL UI (/q/graphql-ui/)      │  │
│  │  ├─ Health Checks (/q/health/*)       │  │
│  │  └─ Sample Data (Products/Categories) │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```
