# Makefile for Apollo Federation vs Event-Driven Projections Demo
# ================================================================

.PHONY: help setup prereqs up down clean federation-only event-only kill-security restore-security logs-kafka lag demo test

# Default target
help:
	@echo "Apollo Federation vs Event-Driven Projections - Available Commands:"
	@echo ""
	@echo "  SETUP"
	@echo "  make setup            - Install prerequisites check + initial build"
	@echo "  make prereqs          - Just check prerequisites (no build)"
	@echo ""
	@echo "  RUNNING"
	@echo "  make up               - Start everything with Tilt"
	@echo "  make down             - Stop and clean up"
	@echo "  make clean            - Full cleanup (delete namespaces)"
	@echo ""
	@echo "  make federation-only  - Start only Federation architecture"
	@echo "  make event-only       - Start only Event-Driven Projections architecture"
	@echo ""
	@echo "  DEMO & TESTING"
	@echo "  make demo             - Run automated demo script"
	@echo "  make test             - Run Playwright tests"
	@echo ""
	@echo "  TROUBLESHOOTING"
	@echo "  make kill-security    - Simulate Security service failure"
	@echo "  make restore-security - Restore Security service"
	@echo "  make logs-kafka       - Tail Kafka logs"
	@echo "  make lag              - Show consumer lag"
	@echo ""
	@echo "  ACCESS POINTS"
	@echo "  Dashboard:    http://localhost:3000"
	@echo "  Router:       http://localhost:4000"
	@echo "  Projections:  http://localhost:8090"
	@echo "  Tilt UI:      http://localhost:10350"

# Setup - check prerequisites and pre-build
setup:
ifeq ($(OS),Windows_NT)
	@powershell -ExecutionPolicy Bypass -File infra/tilt/scripts/setup-dev.ps1
else
	@./infra/tilt/scripts/setup-dev.sh
endif

# Just check prerequisites
prereqs:
ifeq ($(OS),Windows_NT)
	@powershell -ExecutionPolicy Bypass -File infra/tilt/scripts/setup-dev.ps1 -CheckOnly
else
	@./infra/tilt/scripts/setup-dev.sh --check-only
endif

# Start everything with Tilt
up:
	@echo "Starting all services with Tilt..."
	tilt up

# Stop everything
down:
	@echo "Stopping Tilt..."
	tilt down

# Full cleanup
clean:
	@echo "Stopping Tilt and deleting namespaces..."
	tilt down --delete-namespaces

# Start only Federation side
federation-only:
	@echo "Starting Federation stack only..."
	tilt up -- --federation-only

# Start only Event-Driven Projections side
event-only:
	@echo "Starting Event-Driven Projections stack only..."
	tilt up -- --event-only

# Simulate Security service failure
kill-security:
	@echo "Killing Security services..."
	kubectl scale deployment security-subgraph --replicas=0 -n federation-demo
	kubectl scale deployment security-events-service --replicas=0 -n kafka-demo
	@echo ""
	@echo "Security services stopped!"
	@echo "Federation queries touching Security will now fail."
	@echo "Event-Driven queries will continue to work with stale data."

# Restore Security service
restore-security:
	@echo "Restoring Security services..."
	kubectl scale deployment security-subgraph --replicas=1 -n federation-demo
	kubectl scale deployment security-events-service --replicas=1 -n kafka-demo
	@echo ""
	@echo "Security services restored!"

# Show Kafka logs
logs-kafka:
	kubectl logs -f -l app=kafka -n kafka-demo

# Show consumer lag
lag:
	@echo "Consumer lag for projection-consumer:"
	kubectl exec -it kafka-0 -n kafka-demo -- /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group projection-consumer 2>/dev/null || echo "Consumer group not found yet"

# Run automated demo
demo:
ifeq ($(OS),Windows_NT)
	@powershell -ExecutionPolicy Bypass -File infra/scripts/demo.ps1
else
	@./infra/scripts/demo.sh
endif

# Run tests (installs deps if needed)
test:
	cd tests && npm install && npx playwright install --with-deps chromium && npx playwright test
