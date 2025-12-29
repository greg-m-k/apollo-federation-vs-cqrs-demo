---
id: 001
title: Quarkus Apollo GraphQL Demo Specification
stage: spec
date: 2025-12-21
surface: agent
model: claude-opus-4-5-20251101
feature: 001-quarkus-apollo-graphql
branch: 001-quarkus-apollo-graphql
user: user
command: /sp.specify
labels: ["graphql", "quarkus", "apollo", "docker", "demo"]
links:
  spec: specs/001-quarkus-apollo-graphql/spec.md
  ticket: null
  adr: null
  pr: null
files:
  - specs/001-quarkus-apollo-graphql/spec.md
  - specs/001-quarkus-apollo-graphql/checklists/requirements.md
tests:
  - null
---

## Prompt

create a basic quarkus based demo of an api with graph enabled by the apollo framework. the outcome is such that when you are done I can run it using docker compose up and it has some way to see, understand, or visualize that the graph is working

## Response snapshot

Created feature specification for Quarkus Apollo GraphQL Demo with:
- 3 user stories prioritized by importance (P1: Docker Compose startup, P2: Schema visualization, P3: Query execution)
- 9 functional requirements covering Docker deployment, GraphQL endpoint, visual explorer, and sample data
- 5 measurable success criteria (startup time, interface load time, query execution, schema coverage, time-to-first-query)
- 2 key entities (Product, Category) for demonstration purposes
- Edge cases for Docker issues, port conflicts, and error handling
- Assumptions documented for dependencies and environment

## Outcome

- Impact: Created complete feature specification ready for planning phase
- Tests: No tests yet - specification phase
- Files: spec.md, checklists/requirements.md
- Next prompts: /sp.plan to create technical implementation plan
- Reflection: User requirements were clear and specific; no clarifications needed

## Evaluation notes (flywheel)

- Failure modes observed: None - straightforward specification
- Graders run and results (PASS/FAIL): Manual checklist validation PASS
- Prompt variant (if applicable): Standard /sp.specify
- Next experiment (smallest change to try): N/A
