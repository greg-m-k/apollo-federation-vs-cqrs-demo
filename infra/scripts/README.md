# Scripts

Utility and demo scripts.

## Files

### `demo.sh` / `demo.ps1`

Interactive demo script that walks through the comparison:
1. Queries both architectures
2. Creates new data
3. Shows propagation delay in Event-Driven
4. Simulates service failure
5. Demonstrates resilience differences

**Usage:**
```bash
./infra/scripts/demo.sh          # Linux/Mac
.\infra\scripts\demo.ps1         # Windows
make demo                         # Via Makefile
```

### `init-multiple-dbs.sh`

PostgreSQL init script that creates multiple databases from a single container.

**How it works:**
- Reads `POSTGRES_MULTIPLE_DATABASES` env var (comma-separated)
- Creates each database on container startup
- Mounted into `/docker-entrypoint-initdb.d/`

**Example:**
```yaml
# docker-compose.yml
environment:
  POSTGRES_MULTIPLE_DATABASES: hr_db,employment_db,security_db
volumes:
  - ./infra/scripts/init-multiple-dbs.sh:/docker-entrypoint-initdb.d/init-multiple-dbs.sh
```

**Why?**
Each subgraph/service needs its own database, but we use one PostgreSQL container per architecture to save resources.

## Adding New Scripts

Place utility scripts here. Update the Makefile if they should be easily invocable:

```makefile
# Makefile
my-script:
ifeq ($(OS),Windows_NT)
	@powershell -ExecutionPolicy Bypass -File infra/scripts/my-script.ps1
else
	@./infra/scripts/my-script.sh
endif
```
