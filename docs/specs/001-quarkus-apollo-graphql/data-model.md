# Data Model: Quarkus Apollo GraphQL Demo

**Feature Branch**: `001-quarkus-apollo-graphql`
**Date**: 2025-12-21

## Entity Overview

This demo uses two related entities to demonstrate GraphQL relationships and queries.

```
┌──────────────┐       1:N        ┌──────────────┐
│   Category   │◄─────────────────│   Product    │
│              │                  │              │
│ id           │                  │ id           │
│ name         │                  │ name         │
│ description  │                  │ description  │
│ products[]   │                  │ price        │
└──────────────┘                  │ category     │
                                  └──────────────┘
```

---

## Entity: Category

**Purpose**: Grouping entity for products. Demonstrates one-to-many relationships.

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | ID | Yes | Unique identifier (auto-generated) |
| name | String | Yes | Category name (e.g., "Electronics", "Books") |
| description | String | No | Optional description of the category |
| products | [Product] | Derived | List of products in this category (relationship) |

### Validation Rules

- `name` must be non-empty, max 100 characters
- `name` should be unique across categories

### Sample Data

| id | name | description |
|----|------|-------------|
| 1 | Electronics | Gadgets and devices |
| 2 | Books | Reading materials |
| 3 | Clothing | Apparel and accessories |

---

## Entity: Product

**Purpose**: Sample entity representing items that can be queried. Core entity for demonstrating CRUD-style queries.

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | ID | Yes | Unique identifier (auto-generated) |
| name | String | Yes | Product name |
| description | String | No | Optional product description |
| price | Float | Yes | Product price (positive value) |
| category | Category | No | Reference to parent category (relationship) |

### Validation Rules

- `name` must be non-empty, max 200 characters
- `price` must be >= 0

### Sample Data

| id | name | description | price | category_id |
|----|------|-------------|-------|-------------|
| 1 | Laptop | High-performance laptop | 999.99 | 1 |
| 2 | Smartphone | Latest smartphone model | 699.99 | 1 |
| 3 | Java Book | Learn Java programming | 49.99 | 2 |
| 4 | T-Shirt | Cotton t-shirt | 29.99 | 3 |
| 5 | Headphones | Wireless headphones | 199.99 | 1 |

---

## Relationships

### Category → Product (One-to-Many)

- A Category can have zero or more Products
- A Product belongs to at most one Category
- This relationship demonstrates GraphQL nested queries:
  ```graphql
  query {
    category(id: "1") {
      name
      products {
        name
        price
      }
    }
  }
  ```

### Product → Category (Many-to-One)

- A Product optionally references a Category
- This enables bidirectional navigation in GraphQL:
  ```graphql
  query {
    product(id: "1") {
      name
      category {
        name
      }
    }
  }
  ```

---

## Storage Strategy

For this demo, entities are stored in-memory using Java collections:

```java
@ApplicationScoped
public class ProductRepository {
    private final Map<String, Product> products = new ConcurrentHashMap<>();

    // CRUD operations
}
```

**Rationale**: Simplifies Docker setup, no external database required. Data is pre-loaded at application startup and persists for the lifetime of the container.

---

## State Transitions

This demo does not include state machines. Entities are read-only after initial data loading.

If mutations were added later, the following operations would apply:

| Entity | Operations |
|--------|------------|
| Category | CREATE, READ, UPDATE, DELETE |
| Product | CREATE, READ, UPDATE, DELETE |

For the current demo scope (read-only), only Query operations are implemented.
