# Quickstart: Quarkus GraphQL Demo

Get the GraphQL demo running in under 2 minutes.

## Prerequisites

- Docker Desktop (or Docker Engine + Docker Compose)
- No Java, Maven, or other tools required

## Start the Application

```bash
# Clone and start
git clone <repository-url>
cd apollo-demo
docker compose up
```

**First run**: Expect 2-3 minutes for Maven to download dependencies and build.
**Subsequent runs**: Starts in ~30 seconds.

## Access the GraphQL Interface

Once you see `Quarkus ... started in X.XXXs` in the logs:

1. Open your browser to: **http://localhost:8080/q/graphql-ui/**
2. You'll see GraphiQL - an interactive GraphQL explorer

## Try Your First Query

Paste this into the left panel and click the **Play** button:

```graphql
query {
  products {
    id
    name
    price
    category {
      name
    }
  }
}
```

Expected response:

```json
{
  "data": {
    "products": [
      {
        "id": "1",
        "name": "Laptop",
        "price": 999.99,
        "category": {
          "name": "Electronics"
        }
      },
      ...
    ]
  }
}
```

## More Example Queries

### Get a single product

```graphql
query {
  product(id: "1") {
    name
    description
    price
  }
}
```

### List categories with their products

```graphql
query {
  categories {
    name
    description
    products {
      name
      price
    }
  }
}
```

### Explore the schema

Click the **Docs** button (top right in GraphiQL) to browse all available types, queries, and their descriptions.

## Useful URLs

| URL | Purpose |
|-----|---------|
| http://localhost:8080/q/graphql-ui/ | GraphiQL interactive explorer |
| http://localhost:8080/graphql | GraphQL endpoint (for programmatic access) |
| http://localhost:8080/q/health | Health check endpoints |
| http://localhost:8080/q/health/ready | Readiness probe |
| http://localhost:8080/q/health/live | Liveness probe |

## Stopping the Application

```bash
# Stop containers (preserves build cache)
docker compose down

# Stop and remove everything including build cache
docker compose down --rmi all
```

## Troubleshooting

### Port 8080 is already in use

Edit `docker-compose.yml` and change the port mapping:

```yaml
ports:
  - "8081:8080"  # Change 8081 to any available port
```

Then access the app at http://localhost:8081/q/graphql-ui/

### Build fails or is slow

First build downloads ~200MB of dependencies. Ensure stable internet connection.

```bash
# Force rebuild from scratch
docker compose build --no-cache
docker compose up
```

### Container won't start

Check Docker is running:

```bash
docker info
```

View detailed logs:

```bash
docker compose logs -f
```
