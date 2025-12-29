# -*- mode: python -*-
# Tiltfile for Apollo-Demo: Federation vs CDC Architecture Comparison
# ====================================================================
#
# Usage:
#   tilt up                       # Start all services
#   tilt up -- --federation-only  # Federation stack only
#   tilt up -- --cdc-only         # CDC stack only
#
# Prerequisites:
#   - kind cluster created via: .\tilt\scripts\setup-dev.ps1
#   - Maven projects pre-built (setup script does this)

# Load extensions
load('ext://restart_process', 'docker_build_with_restart')

# ============================================================================
# CONFIGURATION
# ============================================================================

# Allow local development clusters
allow_k8s_contexts(['kind-apollo-demo', 'docker-desktop'])

# Update settings for better performance
update_settings(
    max_parallel_updates=3,
    k8s_upsert_timeout_secs=120
)

# User settings (can be overridden via command line)
config.define_bool('federation-only')
config.define_bool('cdc-only')
cfg = config.parse()

# Determine which stacks to deploy
deploy_federation = not cfg.get('cdc-only', False)
deploy_cdc = not cfg.get('federation-only', False)

# ============================================================================
# NAMESPACES
# ============================================================================

k8s_yaml('k8s/namespace.yaml')

# ============================================================================
# HELPER FUNCTIONS
# ============================================================================

def quarkus_service(name, context, namespace, db_name, port_forward, resource_deps=[], extra_env={}):
    """
    Build and deploy a Quarkus JVM service.

    1. Runs Maven locally to build the quarkus-app directory
    2. Builds Docker image from the pre-built artifacts
    3. Configures live_update for JAR syncing (requires restart)
    """
    # Local Maven build
    local_resource(
        name + '-build',
        cmd='cd ' + context + ' && .\\mvnw.cmd package -DskipTests -q',
        deps=[
            context + '/src',
            context + '/pom.xml'
        ],
        resource_deps=resource_deps,
        labels=[namespace, 'build']
    )

    # Docker build with restart capability
    docker_build_with_restart(
        name + ':latest',
        context=context,
        dockerfile=context + '/src/main/docker/Dockerfile.jvm',
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
    k8s_yaml('k8s/federation/postgres.yaml')
    k8s_resource(
        'postgres-federation',
        port_forwards=['5434:5432'],
        labels=['federation', 'infra']
    )

    # HR Subgraph
    quarkus_service(
        name='hr-subgraph',
        context='./hr-subgraph',
        namespace='federation',
        db_name='hr_db',
        port_forward='8091:8080',
        resource_deps=['postgres-federation']
    )
    k8s_yaml('k8s/federation/hr-subgraph.yaml')
    k8s_resource(
        'hr-subgraph',
        port_forwards=['8091:8080'],
        resource_deps=['hr-subgraph-build', 'postgres-federation'],
        labels=['federation', 'subgraph']
    )

    # Employment Subgraph
    quarkus_service(
        name='employment-subgraph',
        context='./employment-subgraph',
        namespace='federation',
        db_name='employment_db',
        port_forward='8092:8080',
        resource_deps=['postgres-federation']
    )
    k8s_yaml('k8s/federation/employment-subgraph.yaml')
    k8s_resource(
        'employment-subgraph',
        port_forwards=['8092:8080'],
        resource_deps=['employment-subgraph-build', 'postgres-federation'],
        labels=['federation', 'subgraph']
    )

    # Security Subgraph
    quarkus_service(
        name='security-subgraph',
        context='./security-subgraph',
        namespace='federation',
        db_name='security_db',
        port_forward='8093:8080',
        resource_deps=['postgres-federation']
    )
    k8s_yaml('k8s/federation/security-subgraph.yaml')
    k8s_resource(
        'security-subgraph',
        port_forwards=['8093:8080'],
        resource_deps=['security-subgraph-build', 'postgres-federation'],
        labels=['federation', 'subgraph']
    )

    # Apollo Router
    k8s_yaml('k8s/federation/router.yaml')
    k8s_resource(
        'router',
        port_forwards=['4000:4000', '8088:8088'],
        resource_deps=['hr-subgraph', 'employment-subgraph', 'security-subgraph'],
        labels=['federation', 'gateway']
    )

# ============================================================================
# CDC STACK
# ============================================================================

if deploy_cdc:
    # PostgreSQL for CDC
    k8s_yaml('k8s/cdc/postgres.yaml')
    k8s_resource(
        'postgres-cdc',
        port_forwards=['5433:5432'],
        labels=['cdc', 'infra']
    )

    # Kafka
    k8s_yaml('k8s/cdc/kafka.yaml')
    k8s_resource(
        'kafka',
        port_forwards=['9092:9092'],
        resource_deps=['postgres-cdc'],
        labels=['cdc', 'infra']
    )

    # HR CDC Service
    quarkus_service(
        name='hr-cdc-service',
        context='./hr-cdc-service',
        namespace='cdc',
        db_name='hr_cdc_db',
        port_forward='8084:8080',
        resource_deps=['postgres-cdc', 'kafka']
    )
    k8s_yaml('k8s/cdc/hr-cdc-service.yaml')
    k8s_resource(
        'hr-cdc-service',
        port_forwards=['8084:8080'],
        resource_deps=['hr-cdc-service-build', 'postgres-cdc', 'kafka'],
        labels=['cdc', 'service']
    )

    # Employment CDC Service
    quarkus_service(
        name='employment-cdc-service',
        context='./employment-cdc-service',
        namespace='cdc',
        db_name='employment_cdc_db',
        port_forward='8085:8080',
        resource_deps=['postgres-cdc', 'kafka']
    )
    k8s_yaml('k8s/cdc/employment-cdc-service.yaml')
    k8s_resource(
        'employment-cdc-service',
        port_forwards=['8085:8080'],
        resource_deps=['employment-cdc-service-build', 'postgres-cdc', 'kafka'],
        labels=['cdc', 'service']
    )

    # Security CDC Service
    quarkus_service(
        name='security-cdc-service',
        context='./security-cdc-service',
        namespace='cdc',
        db_name='security_cdc_db',
        port_forward='8086:8080',
        resource_deps=['postgres-cdc', 'kafka']
    )
    k8s_yaml('k8s/cdc/security-cdc-service.yaml')
    k8s_resource(
        'security-cdc-service',
        port_forwards=['8086:8080'],
        resource_deps=['security-cdc-service-build', 'postgres-cdc', 'kafka'],
        labels=['cdc', 'service']
    )

    # CDC Projection Consumer
    quarkus_service(
        name='cdc-projection-consumer',
        context='./cdc-projection-consumer',
        namespace='cdc',
        db_name='projections_db',
        port_forward='8089:8080',
        resource_deps=['postgres-cdc', 'kafka']
    )
    k8s_yaml('k8s/cdc/cdc-projection-consumer.yaml')
    k8s_resource(
        'cdc-projection-consumer',
        port_forwards=['8089:8080'],
        resource_deps=['cdc-projection-consumer-build', 'hr-cdc-service', 'employment-cdc-service', 'security-cdc-service'],
        labels=['cdc', 'service']
    )

    # CDC Query Service
    quarkus_service(
        name='cdc-query-service',
        context='./cdc-query-service',
        namespace='cdc',
        db_name='projections_db',
        port_forward='8090:8080',
        resource_deps=['postgres-cdc']
    )
    k8s_yaml('k8s/cdc/cdc-query-service.yaml')
    k8s_resource(
        'cdc-query-service',
        port_forwards=['8090:8080'],
        resource_deps=['cdc-query-service-build', 'cdc-projection-consumer'],
        labels=['cdc', 'service']
    )

# ============================================================================
# DASHBOARD
# ============================================================================

if deploy_federation and deploy_cdc:
    # Build dashboard
    docker_build(
        'comparison-dashboard:latest',
        context='./comparison-dashboard',
        dockerfile='./comparison-dashboard/Dockerfile',
        live_update=[
            sync('./comparison-dashboard/src', '/app/src'),
            run('cd /app && npm run build', trigger=['./comparison-dashboard/package.json']),
        ]
    )

    # Deploy dashboard
    k8s_yaml('k8s/cdc/dashboard.yaml')
    k8s_resource(
        'comparison-dashboard',
        port_forwards=['3000:80'],
        resource_deps=['router', 'cdc-query-service'],
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

if deploy_cdc:
    local_resource(
        'cdc-ready',
        cmd='echo "CDC stack is ready at http://localhost:8090"',
        resource_deps=['cdc-query-service'],
        labels=['cdc']
    )

if deploy_federation and deploy_cdc:
    local_resource(
        'all-ready',
        cmd='echo "All services ready! Dashboard: http://localhost:3000"',
        resource_deps=['comparison-dashboard'],
        labels=['dashboard']
    )
