#!/bin/bash
# Compose the supergraph schema from subgraph schemas
# Requires: rover CLI (npm install -g @apollo/rover)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Composing supergraph from subgraph schemas..."

# Use rover to compose the supergraph
# Note: In production, you'd use Apollo Studio or rover supergraph compose

# For local development, we introspect running services
rover supergraph compose --config "${SCRIPT_DIR}/supergraph.yaml" > "${SCRIPT_DIR}/supergraph.graphql"

echo "Supergraph schema composed successfully: ${SCRIPT_DIR}/supergraph.graphql"
