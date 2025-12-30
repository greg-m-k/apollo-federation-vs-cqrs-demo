# Implementation Plan: Quarkus Apollo GraphQL Demo

**Branch**: `001-quarkus-apollo-graphql` | **Date**: 2025-12-21 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-quarkus-apollo-graphql/spec.md`

## Summary

Build a Quarkus-based GraphQL API demo that starts with `docker compose up` and provides a visual interface (GraphiQL) for exploring and executing GraphQL queries. Uses SmallRye GraphQL extension with in-memory sample data (Products and Categories) to demonstrate GraphQL relationships and query execution.

## Technical Context

**Language/Version**: Java 17 (Eclipse Temurin LTS)
**Primary Dependencies**: Quarkus 3.x, SmallRye GraphQL Extension, SmallRye Health
**Storage**: In-memory (ConcurrentHashMap) - no external database
**Testing**: JUnit 5 with Quarkus Test, REST Assured for GraphQL endpoint testing
**Target Platform**: Docker container (linux/amd64), accessible via localhost:8080
**Project Type**: Single Maven project
**Performance Goals**: Startup <60s, query response <2s, GraphiQL load <5s (per spec SC-001 to SC-003)
**Constraints**: No authentication required, demo purposes only
**Scale/Scope**: Single service, 2 entities, read-only queries

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Pre-Design Status**: ✅ PASS (Constitution is unfilled template - no specific gates defined)

**Post-Design Re-evaluation**: ✅ PASS

The project constitution is currently a template with placeholder values. Since no specific principles or constraints are defined, this implementation plan proceeds with industry best practices:

- Single-purpose demo application
- Minimal dependencies (3 Quarkus extensions)
- Standard Quarkus project structure (Maven conventions)
- No over-engineering (read-only queries, in-memory data)
- Clear separation of concerns (model/repository/graphql layers)

## Project Structure

### Documentation (this feature)

```text
specs/001-quarkus-apollo-graphql/
├── plan.md              # This file
├── research.md          # Technology decisions (complete)
├── data-model.md        # Entity definitions (complete)
├── quickstart.md        # Getting started guide
├── contracts/           # GraphQL schema
│   └── schema.graphql
└── tasks.md             # Implementation checklist (created by /sp.tasks)
```

### Source Code (repository root)

```text
src/main/java/com/example/graphqldemo/
├── model/
│   ├── Category.java           # Category entity
│   └── Product.java            # Product entity
├── repository/
│   ├── CategoryRepository.java # In-memory category storage
│   └── ProductRepository.java  # In-memory product storage
├── graphql/
│   └── ProductGraphQL.java     # GraphQL queries and resolvers
└── DataLoader.java             # Sample data initialization

src/main/resources/
├── application.properties      # Quarkus configuration
└── META-INF/resources/         # Static assets (if needed)

src/main/docker/
├── Dockerfile.jvm              # JVM-based Docker image
└── Dockerfile.native           # Native image (optional, not used)

src/test/java/com/example/graphqldemo/
├── GraphQLEndpointTest.java    # GraphQL query tests
└── HealthCheckTest.java        # Health endpoint tests

docker-compose.yml              # Single-command startup
pom.xml                         # Maven build configuration
README.md                       # Project documentation
```

**Structure Decision**: Standard single-project Maven layout with Quarkus conventions. The `src/main/docker/` directory follows Quarkus defaults for Docker support.

## Complexity Tracking

> No constitution violations to justify - constitution is unfilled template.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

## Implementation Phases

### Phase 1: Project Scaffolding
- Initialize Quarkus project with Maven
- Add SmallRye GraphQL and Health extensions
- Configure application.properties for GraphiQL UI
- Create docker-compose.yml

### Phase 2: Domain Model
- Create Product entity class
- Create Category entity class
- Implement in-memory repositories
- Create DataLoader for sample data

### Phase 3: GraphQL API
- Implement GraphQL resource with @GraphQLApi
- Add queries: products, product(id), categories, category(id)
- Configure field resolvers for relationships
- Add schema descriptions for documentation

### Phase 4: Docker Integration
- Create Dockerfile.jvm following Quarkus best practices
- Configure health checks in docker-compose
- Test full docker compose up workflow

### Phase 5: Validation
- Write integration tests for GraphQL queries
- Test health endpoints
- Validate GraphiQL UI accessibility
- Document in quickstart.md

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| GraphQL Framework | SmallRye GraphQL | Native Quarkus integration, includes GraphiQL |
| UI Explorer | GraphiQL (built-in) | Zero config, full-featured, bundled |
| Data Storage | In-memory | Simplifies demo, no external dependencies |
| Container Mode | JVM (not native) | Faster builds, sufficient for demo |
| Java Version | 17 LTS | Quarkus 3.x requirement, long-term support |

## Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Port 8080 already in use | Medium | Demo fails to start | Document port change in quickstart.md |
| Docker not installed | Low | Demo unusable | Clear error messaging in README |
| Slow first build | Medium | User confusion | Document expected build time |

## Success Criteria Mapping

| Spec Criteria | Implementation Verification |
|---------------|----------------------------|
| SC-001: Start <60s | Health check with start_period: 30s |
| SC-002: GraphiQL loads <5s | Lightweight bundled UI |
| SC-003: Query response <2s | In-memory data, no I/O latency |
| SC-004: Schema documented | @Description annotations on types/fields |
| SC-005: First query <2min | Pre-loaded sample data, working examples |
