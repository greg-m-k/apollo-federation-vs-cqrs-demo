# PowerShell script to deploy Apollo Federation demo to Kubernetes
# Requires: Docker Desktop with Kubernetes enabled

param(
    [switch]$BuildOnly,
    [switch]$DeployOnly,
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Apollo Federation K8s Deployment Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Clean up existing deployment
if ($Clean) {
    Write-Host "`nCleaning up existing deployment..." -ForegroundColor Yellow
    kubectl delete namespace apollo-demo --ignore-not-found=true
    Write-Host "Cleanup complete!" -ForegroundColor Green
    exit 0
}

# Build Docker images
if (-not $DeployOnly) {
    Write-Host "`n[1/6] Building Docker images..." -ForegroundColor Yellow

    Write-Host "Building products-subgraph..." -ForegroundColor Gray
    docker build -t products-subgraph:latest -f products-subgraph/src/main/docker/Dockerfile.jvm products-subgraph

    Write-Host "Building categories-subgraph..." -ForegroundColor Gray
    docker build -t categories-subgraph:latest -f categories-subgraph/src/main/docker/Dockerfile.jvm categories-subgraph

    Write-Host "Building products-mutation-subgraph..." -ForegroundColor Gray
    docker build -t products-mutation-subgraph:latest -f products-mutation-subgraph/src/main/docker/Dockerfile.jvm products-mutation-subgraph

    Write-Host "Building categories-mutation-subgraph..." -ForegroundColor Gray
    docker build -t categories-mutation-subgraph:latest -f categories-mutation-subgraph/src/main/docker/Dockerfile.jvm categories-mutation-subgraph

    Write-Host "Docker images built successfully!" -ForegroundColor Green
}

if ($BuildOnly) {
    Write-Host "`nBuild complete! Use -DeployOnly to deploy." -ForegroundColor Cyan
    exit 0
}

# Deploy to Kubernetes
Write-Host "`n[2/6] Creating namespace and secrets..." -ForegroundColor Yellow
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/postgres/secret.yaml

Write-Host "`n[3/6] Deploying PostgreSQL..." -ForegroundColor Yellow
kubectl apply -f k8s/postgres/configmap.yaml
kubectl apply -f k8s/postgres/statefulset.yaml
kubectl apply -f k8s/postgres/service.yaml

Write-Host "Waiting for PostgreSQL to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n apollo-demo --timeout=120s

Write-Host "`n[4/6] Deploying subgraphs..." -ForegroundColor Yellow
# Deploy query subgraphs
kubectl apply -f k8s/subgraphs/products-query/
kubectl apply -f k8s/subgraphs/categories-query/
# Deploy mutation subgraphs
kubectl apply -f k8s/subgraphs/products-mutation/
kubectl apply -f k8s/subgraphs/categories-mutation/

Write-Host "Waiting for subgraphs to be ready..."
kubectl wait --for=condition=ready pod -l type=query -n apollo-demo --timeout=180s
kubectl wait --for=condition=ready pod -l type=mutation -n apollo-demo --timeout=180s

Write-Host "`n[5/6] Deploying Apollo Router..." -ForegroundColor Yellow
kubectl apply -f k8s/router/

Write-Host "Waiting for Apollo Router to be ready..."
kubectl wait --for=condition=ready pod -l app=apollo-router -n apollo-demo --timeout=120s

Write-Host "`n[6/6] Deploying monitoring stack..." -ForegroundColor Yellow
kubectl apply -f k8s/monitoring/prometheus/rbac.yaml
kubectl apply -f k8s/monitoring/prometheus/configmap.yaml
kubectl apply -f k8s/monitoring/prometheus/deployment.yaml
kubectl apply -f k8s/monitoring/prometheus/service.yaml
kubectl apply -f k8s/monitoring/grafana/

Write-Host "Waiting for monitoring to be ready..."
kubectl wait --for=condition=ready pod -l app=prometheus -n apollo-demo --timeout=60s
kubectl wait --for=condition=ready pod -l app=grafana -n apollo-demo --timeout=60s

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Write-Host "`nServices available at:" -ForegroundColor Cyan
Write-Host "  GraphQL Playground: http://localhost:30400" -ForegroundColor White
Write-Host "  Prometheus:         http://localhost:30090" -ForegroundColor White
Write-Host "  Grafana:            http://localhost:30300 (admin/admin)" -ForegroundColor White

Write-Host "`nPod Status:" -ForegroundColor Cyan
kubectl get pods -n apollo-demo

Write-Host "`nTest query:" -ForegroundColor Cyan
Write-Host '  curl -X POST http://localhost:30400 -H "Content-Type: application/json" -d "{\"query\": \"{ products { id name category { name } } }\"}"' -ForegroundColor Gray

Write-Host "`nTest mutation:" -ForegroundColor Cyan
Write-Host '  curl -X POST http://localhost:30400 -H "Content-Type: application/json" -d "{\"query\": \"mutation { createProduct(input: {name: \\\"Test\\\", price: 99.99, categoryId: \\\"1\\\"}) { id name } }\"}"' -ForegroundColor Gray
