# Tasks: Quarkus Apollo GraphQL Demo

**Input**: Design documents from `/specs/001-quarkus-apollo-graphql/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested in spec - omitting test tasks per template guidelines.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Based on plan.md structure (Single Maven project):
- Java sources: `src/main/java/com/example/graphqldemo/`
- Resources: `src/main/resources/`
- Docker: `src/main/docker/`
- Tests: `src/test/java/com/example/graphqldemo/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Initialize Quarkus project with Maven using `quarkus create app` in repository root
- [X] T002 Add SmallRye GraphQL extension via `quarkus ext add io.quarkus:quarkus-smallrye-graphql`
- [X] T003 [P] Add SmallRye Health extension via `quarkus ext add io.quarkus:quarkus-smallrye-health`
- [X] T004 [P] Configure application.properties in src/main/resources/application.properties with GraphiQL UI settings

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core entities and repositories that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 [P] Create Category model class in src/main/java/com/example/graphqldemo/model/Category.java with id, name, description fields and @Description annotations
- [X] T006 [P] Create Product model class in src/main/java/com/example/graphqldemo/model/Product.java with id, name, description, price, category fields and @Description annotations
- [X] T007 [P] Create CategoryRepository in src/main/java/com/example/graphqldemo/repository/CategoryRepository.java with ConcurrentHashMap storage and findAll, findById methods
- [X] T008 [P] Create ProductRepository in src/main/java/com/example/graphqldemo/repository/ProductRepository.java with ConcurrentHashMap storage and findAll, findById, findByCategory methods
- [X] T009 Create DataLoader in src/main/java/com/example/graphqldemo/DataLoader.java with @Startup @PostConstruct to load sample categories and products

**Checkpoint**: Foundation ready - entities, repositories, and sample data in place

---

## Phase 3: User Story 1 - Start Demo with Single Command (Priority: P1) 🎯 MVP

**Goal**: Enable starting the entire application stack with `docker compose up` command

**Independent Test**: Run `docker compose up` and verify application starts within 60 seconds, logs show GraphQL endpoint ready

### Implementation for User Story 1

- [X] T010 [US1] Create Dockerfile.jvm in src/main/docker/Dockerfile.jvm using eclipse-temurin:17-jre-alpine base image with multi-stage build
- [X] T011 [US1] Create docker-compose.yml in repository root with graphql-api service, port 8080 mapping, and health check configuration
- [X] T012 [US1] Add health check endpoint configuration to src/main/resources/application.properties for /q/health/ready and /q/health/live
- [X] T013 [US1] Create README.md in repository root with quick start instructions and docker compose up command

**Checkpoint**: User Story 1 complete - `docker compose up` starts application with health checks

---

## Phase 4: User Story 2 - Visualize and Explore GraphQL Schema (Priority: P2)

**Goal**: Provide visual interface (GraphiQL) to browse and understand the GraphQL schema

**Independent Test**: Navigate to http://localhost:8080/q/graphql-ui/ and verify schema documentation is visible with types, queries, and descriptions

### Implementation for User Story 2

- [X] T014 [US2] Create ProductGraphQL resource in src/main/java/com/example/graphqldemo/graphql/ProductGraphQL.java with @GraphQLApi annotation
- [X] T015 [US2] Add @Query products() method returning List<Product> with @Description in ProductGraphQL.java
- [X] T016 [US2] Add @Query product(id) method returning Product with @Description in ProductGraphQL.java
- [X] T017 [US2] Add @Query categories() method returning List<Category> with @Description in ProductGraphQL.java
- [X] T018 [US2] Add @Query category(id) method returning Category with @Description in ProductGraphQL.java
- [X] T019 [US2] Configure GraphiQL UI to always include via quarkus.smallrye-graphql.ui.always-include=true in application.properties

**Checkpoint**: User Story 2 complete - GraphiQL shows full schema with documented types and queries

---

## Phase 5: User Story 3 - Execute GraphQL Queries and See Results (Priority: P3)

**Goal**: Enable query execution through GraphiQL with sample data returning meaningful results

**Independent Test**: Execute `{ products { id name price } }` query in GraphiQL and receive JSON response with 5 sample products

### Implementation for User Story 3

- [X] T020 [US3] Wire ProductGraphQL queries to ProductRepository for products() and product(id) methods
- [X] T021 [US3] Wire ProductGraphQL queries to CategoryRepository for categories() and category(id) methods
- [X] T022 [US3] Add products field resolver to Category for nested product queries (category → products relationship)
- [X] T023 [US3] Add category field resolver to Product for nested category queries (product → category relationship)
- [X] T024 [US3] Verify GraphQL error handling returns proper GraphQL error responses for invalid queries

**Checkpoint**: User Story 3 complete - queries execute and return sample data with relationship navigation

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and documentation

- [X] T025 [P] Update README.md with complete usage instructions, example queries, and troubleshooting
- [X] T026 [P] Validate quickstart.md scenarios work end-to-end
- [X] T027 Run full docker compose up workflow and verify all success criteria (SC-001 through SC-005)
- [X] T028 Add .dockerignore file to exclude unnecessary files from Docker build context

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User Story 1 (Docker) and User Story 2 (GraphQL API) can proceed in parallel
  - User Story 3 (Query execution) depends on US2 GraphQL API being in place
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 3 (P3)**: Depends on User Story 2 (GraphQL queries must exist to wire to repositories)

### Within Each User Story

- Models before services/repositories
- Repositories before GraphQL resource
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- T002 and T003 (extensions) can run in parallel
- T003 and T004 can run in parallel (different concerns)
- T005, T006, T007, T008 (models and repositories) can ALL run in parallel
- T025 and T026 (documentation) can run in parallel

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Launch all model and repository tasks together:
Task: "Create Category model in src/main/java/com/example/graphqldemo/model/Category.java"
Task: "Create Product model in src/main/java/com/example/graphqldemo/model/Product.java"
Task: "Create CategoryRepository in src/main/java/com/example/graphqldemo/repository/CategoryRepository.java"
Task: "Create ProductRepository in src/main/java/com/example/graphqldemo/repository/ProductRepository.java"

# Wait for all above, then:
Task: "Create DataLoader with sample data"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T009)
3. Complete Phase 3: User Story 1 (T010-T013)
4. **STOP and VALIDATE**: Run `docker compose up` - application should start
5. Deploy/demo if ready (basic Docker functionality working)

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Docker works → Deployable container
3. Add User Story 2 → GraphQL schema visible → Interactive exploration
4. Add User Story 3 → Queries return data → Full demo complete
5. Each story adds value without breaking previous stories

### Suggested Execution Order (Single Developer)

1. T001 → T002 → T003 → T004 (Setup - sequential due to Quarkus CLI)
2. T005, T006, T007, T008 in parallel → T009 (Foundational)
3. T010 → T011 → T012 → T013 (User Story 1)
4. T014 → T015-T018 in parallel → T019 (User Story 2)
5. T020-T024 sequential (User Story 3 - wiring)
6. T025, T026 in parallel → T027 → T028 (Polish)

---

## Summary

| Phase | Tasks | Parallel Opportunities |
|-------|-------|----------------------|
| Setup | 4 | 2 tasks parallelizable |
| Foundational | 5 | 4 tasks parallelizable |
| User Story 1 (P1) | 4 | Sequential (Docker depends on files) |
| User Story 2 (P2) | 6 | 4 query methods parallelizable |
| User Story 3 (P3) | 5 | Sequential (wiring) |
| Polish | 4 | 2 tasks parallelizable |
| **Total** | **28** | |

**MVP Scope**: Tasks T001-T013 (13 tasks) = Setup + Foundational + User Story 1
