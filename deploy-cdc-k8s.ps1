# Deploy CDC comparison demo to Kubernetes
# This script deploys Kafka, Debezium, CDC Consumer, and Demo Dashboard

param(
    [switch]$BuildImages,
    [switch]$RestartPostgres
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CDC Comparison Demo Deployment" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check if kubectl is available
if (!(Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "Error: kubectl not found" -ForegroundColor Red
    exit 1
}

# Check cluster connection
Write-Host "`nChecking Kubernetes cluster..." -ForegroundColor Yellow
kubectl cluster-info | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Cannot connect to Kubernetes cluster" -ForegroundColor Red
    exit 1
}
Write-Host "Cluster connection OK" -ForegroundColor Green

# Build images if requested
if ($BuildImages) {
    Write-Host "`n--- Building Docker Images ---" -ForegroundColor Yellow

    # Build CDC Consumer
    Write-Host "Building CDC Consumer..." -ForegroundColor Cyan
    Push-Location cdc-consumer
    & ../mvnw.cmd clean package -DskipTests
    docker build -f src/main/docker/Dockerfile.jvm -t cdc-consumer:latest .
    Pop-Location

    # Build Demo Dashboard
    Write-Host "Building Demo Dashboard..." -ForegroundColor Cyan
    Push-Location demo-dashboard
    docker build -t demo-dashboard:latest .
    Pop-Location

    Write-Host "Images built successfully" -ForegroundColor Green
}

# Restart PostgreSQL if requested (needed for WAL settings)
if ($RestartPostgres) {
    Write-Host "`n--- Restarting PostgreSQL for WAL settings ---" -ForegroundColor Yellow
    kubectl apply -f k8s/postgres/statefulset.yaml
    kubectl rollout restart statefulset/postgres -n apollo-demo
    Write-Host "Waiting for PostgreSQL to restart..."
    kubectl rollout status statefulset/postgres -n apollo-demo --timeout=120s
    Write-Host "PostgreSQL restarted" -ForegroundColor Green
}

# Deploy Kafka
Write-Host "`n--- Deploying Kafka ---" -ForegroundColor Yellow
kubectl apply -f k8s/kafka/

Write-Host "Waiting for Kafka to be ready..."
kubectl wait --for=condition=ready pod -l app=kafka -n apollo-demo --timeout=120s
Write-Host "Kafka is ready" -ForegroundColor Green

# Deploy Debezium
Write-Host "`n--- Deploying Debezium ---" -ForegroundColor Yellow
kubectl apply -f k8s/debezium/deployment.yaml
kubectl apply -f k8s/debezium/service.yaml

Write-Host "Waiting for Debezium to be ready..."
kubectl wait --for=condition=ready pod -l app=debezium -n apollo-demo --timeout=180s
Write-Host "Debezium is ready" -ForegroundColor Green

# Register connector
Write-Host "`n--- Registering Debezium Connector ---" -ForegroundColor Yellow
kubectl apply -f k8s/debezium/connector-job.yaml
Write-Host "Connector registration job created"

# Wait for job to complete
kubectl wait --for=condition=complete job/register-debezium-connector -n apollo-demo --timeout=120s
Write-Host "Connector registered" -ForegroundColor Green

# Deploy CDC Consumer
Write-Host "`n--- Deploying CDC Consumer ---" -ForegroundColor Yellow
kubectl apply -f k8s/cdc-consumer/
kubectl wait --for=condition=ready pod -l app=cdc-consumer -n apollo-demo --timeout=120s
Write-Host "CDC Consumer is ready" -ForegroundColor Green

# Deploy Demo Dashboard
Write-Host "`n--- Deploying Demo Dashboard ---" -ForegroundColor Yellow
kubectl apply -f k8s/demo-dashboard/
kubectl wait --for=condition=ready pod -l app=demo-dashboard -n apollo-demo --timeout=60s
Write-Host "Demo Dashboard is ready" -ForegroundColor Green

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Access Points:" -ForegroundColor Yellow
Write-Host "  Demo Dashboard:  http://localhost:30001"
Write-Host "  GraphQL Router:  http://localhost:30400"
Write-Host "  CDC Consumer:    http://localhost:30081"
Write-Host "  Grafana:         http://localhost:30300"
Write-Host ""
Write-Host "CDC Event Stream: http://localhost:30081/events/stream"
Write-Host ""
Write-Host "Check pod status:" -ForegroundColor Yellow
kubectl get pods -n apollo-demo
