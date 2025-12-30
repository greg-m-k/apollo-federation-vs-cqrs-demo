# -*- mode: python -*-
# Tiltfile for Apollo Federation vs Event-Driven Projections Demo
# ================================================================
#
# Usage:
#   tilt up                       # Start all services
#   tilt up -- --federation-only  # Federation stack only
#   tilt up -- --event-only       # Event-Driven Projections stack only
#
# Prerequisites:
#   - kind cluster created via: .\infra\tilt\scripts\setup-dev.ps1
#   - Maven projects pre-built (setup script does this)

# Load extensions
load('ext://restart_process', 'docker_build_with_restart')

# Cross-platform Maven wrapper command
# Runs from infra/maven where .mvn/wrapper/ lives
mvn_prefix = 'cd infra\\\\maven && .\\\\mvnw.cmd' if os.name == 'nt' else 'cd infra/maven && ./mvnw'

# ============================================================================
# CONFIGURATION
# ============================================================================

# Allow local development clusters
allow_k8s_contexts(['kind-apollo-demo', 'docker-desktop'])

# Update settings - no parallelism limit
update_settings(
    max_parallel_updates=20,
    k8s_upsert_timeout_secs=120
)

# User settings (can be overridden via command line)
config.define_bool('federation-only')
config.define_bool('event-only')
cfg = config.parse()

# Determine which stacks to deploy
deploy_federation = not cfg.get('event-only', False)
deploy_event = not cfg.get('federation-only', False)

# ============================================================================
# NAMESPACES
# ============================================================================

k8s_yaml('infra/k8s/namespace.yaml')

# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

def quarkus_service(name, context, namespace, port_forward, runtime_deps=[]):
    """
    Build and deploy a Quarkus JVM service.

    1. Runs Maven locally to build the quarkus-app directory (NO DB dependency - builds in parallel)
    2. Builds Docker image from the pre-built artifacts
    3. Configures live_update for JAR syncing (requires restart)

    Args:
        runtime_deps: Dependencies for k8s deployment (e.g., postgres). Builds start immediately.
    """
    # Local Maven build - NO dependencies, starts immediately in parallel
    # Path is relative to infra/maven/ where mvnw runs from
    pom_path = '../../' + context.lstrip('./') + '/pom.xml'
    local_resource(
        name + '-build',
        cmd=mvn_prefix + ' -f ' + pom_path + ' package -DskipTests -q',
        deps=[
            context + '/src',
            context + '/pom.xml'
        ],
        resource_deps=[],  # Builds don't wait for postgres!
        labels=['build'],
        allow_parallel=True  # Run builds concurrently!
    )

    # Docker build with restart capability
    docker_build_with_restart(
        name + ':latest',
        context=context,
        dockerfile='infra/docker/Dockerfile.quarkus-jvm',  # Shared Dockerfile
        entrypoint=['/opt/jboss/container/java/run/run-java.sh'],
        only=[
            'target/quarkus-app'
        ],
        live_update=[
            sync(context + '/target/quarkus-app/lib/', '/deployments/lib/'),
            sync(context + '/target/quarkus-app/app/', '/deployments/app/'),
            sync(context + '/target/quarkus-app/quarkus/', '/deployments/quarkus/'),
            sync(context + '/target/quarkus-app/quarkus-run.jar', '/deployments/quarkus-run.jar'),
        ]
    )

# ============================================================================
# FEDERATION STACK
# ============================================================================

if deploy_federation:
    # PostgreSQL for Federation
    k8s_yaml('infra/k8s/federation/postgres.yaml')
    k8s_resource(
        'postgres-federation',
        port_forwards=['5434:5432'],
        labels=['infra']
    )

    # HR Subgraph
    quarkus_service(
        name='hr-subgraph',
        context='./services/federation/hr-subgraph',
        namespace='federation',
        port_forward='8091:8080'
    )
    k8s_yaml('infra/k8s/federation/hr-subgraph.yaml')
    k8s_resource(
        'hr-subgraph',
        port_forwards=['8091:8080'],
        resource_deps=['hr-subgraph-build', 'postgres-federation'],
        labels=['federation']
    )

    # Employment Subgraph
    quarkus_service(
        name='employment-subgraph',
        context='./services/federation/employment-subgraph',
        namespace='federation',
        port_forward='8092:8080'
    )
    k8s_yaml('infra/k8s/federation/employment-subgraph.yaml')
    k8s_resource(
        'employment-subgraph',
        port_forwards=['8092:8080'],
        resource_deps=['employment-subgraph-build', 'postgres-federation'],
        labels=['federation']
    )

    # Security Subgraph
    quarkus_service(
        name='security-subgraph',
        context='./services/federation/security-subgraph',
        namespace='federation',
        port_forward='8093:8080'
    )
    k8s_yaml('infra/k8s/federation/security-subgraph.yaml')
    k8s_resource(
        'security-subgraph',
        port_forwards=['8093:8080'],
        resource_deps=['security-subgraph-build', 'postgres-federation'],
        labels=['federation']
    )

    # Apollo Router
    k8s_yaml('infra/k8s/federation/router.yaml')
    k8s_resource(
        'router',
        port_forwards=['4000:4000', '8088:8088'],
        resource_deps=['hr-subgraph', 'employment-subgraph', 'security-subgraph'],
        labels=['federation']
    )

# ============================================================================
# EVENT-DRIVEN PROJECTIONS STACK
# ============================================================================

if deploy_event:
    # PostgreSQL for Kafka services
    k8s_yaml('infra/k8s/kafka/postgres.yaml')
    k8s_resource(
        'postgres-kafka',
        port_forwards=['5433:5432'],
        labels=['infra']
    )

    # Kafka
    k8s_yaml('infra/k8s/kafka/kafka.yaml')
    k8s_resource(
        'kafka',
        port_forwards=['9092:9092'],
        resource_deps=['postgres-kafka'],
        labels=['infra']
    )

    # HR Events Service
    quarkus_service(
        name='hr-events-service',
        context='./services/event/hr-events-service',
        namespace='kafka',
        port_forward='8084:8080'
    )
    k8s_yaml('infra/k8s/kafka/hr-events-service.yaml')
    k8s_resource(
        'hr-events-service',
        port_forwards=['8084:8080'],
        resource_deps=['hr-events-service-build', 'postgres-kafka', 'kafka'],
        labels=['event']
    )

    # Employment Events Service
    quarkus_service(
        name='employment-events-service',
        context='./services/event/employment-events-service',
        namespace='kafka',
        port_forward='8085:8080'
    )
    k8s_yaml('infra/k8s/kafka/employment-events-service.yaml')
    k8s_resource(
        'employment-events-service',
        port_forwards=['8085:8080'],
        resource_deps=['employment-events-service-build', 'postgres-kafka', 'kafka'],
        labels=['event']
    )

    # Security Events Service
    quarkus_service(
        name='security-events-service',
        context='./services/event/security-events-service',
        namespace='kafka',
        port_forward='8086:8080'
    )
    k8s_yaml('infra/k8s/kafka/security-events-service.yaml')
    k8s_resource(
        'security-events-service',
        port_forwards=['8086:8080'],
        resource_deps=['security-events-service-build', 'postgres-kafka', 'kafka'],
        labels=['event']
    )

    # Projection Consumer
    quarkus_service(
        name='projection-consumer',
        context='./services/event/projection-consumer',
        namespace='kafka',
        port_forward='8089:8080'
    )
    k8s_yaml('infra/k8s/kafka/projection-consumer.yaml')
    k8s_resource(
        'projection-consumer',
        port_forwards=['8089:8080'],
        resource_deps=['projection-consumer-build', 'hr-events-service', 'employment-events-service', 'security-events-service'],
        labels=['event']
    )

    # Query Service
    quarkus_service(
        name='query-service',
        context='./services/event/query-service',
        namespace='kafka',
        port_forward='8090:8080'
    )
    k8s_yaml('infra/k8s/kafka/query-service.yaml')
    k8s_resource(
        'query-service',
        port_forwards=['8090:8080'],
        resource_deps=['query-service-build', 'projection-consumer'],
        labels=['event']
    )

# ============================================================================
# DASHBOARD
# ============================================================================

if deploy_federation and deploy_event:
    # Build dashboard - rebuilds container on source changes
    docker_build(
        'comparison-dashboard:latest',
        context='./clients/dashboard',
        dockerfile='./clients/dashboard/Dockerfile',
    )

    # Deploy dashboard
    k8s_yaml('infra/k8s/kafka/dashboard.yaml')
    k8s_resource(
        'comparison-dashboard',
        port_forwards=['3000:80'],
        resource_deps=['router', 'query-service'],
        labels=['dashboard']
    )

# ============================================================================
# CONVENIENCE RESOURCES
# ============================================================================

if deploy_federation:
    local_resource(
        'federation-ready',
        cmd='echo "Federation stack is ready at http://localhost:4000"',
        resource_deps=['router'],
        labels=['federation']
    )

if deploy_event:
    local_resource(
        'event-ready',
        cmd='echo "Event-Driven Projections stack is ready at http://localhost:8090"',
        resource_deps=['query-service'],
        labels=['event']
    )

if deploy_federation and deploy_event:
    local_resource(
        'all-ready',
        cmd='echo "All services ready! Dashboard: http://localhost:3000"',
        resource_deps=['comparison-dashboard'],
        labels=['dashboard']
    )
