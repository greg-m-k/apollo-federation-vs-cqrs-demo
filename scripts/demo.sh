#!/bin/bash
# demo.sh - Automated demo script for Architecture Comparison
# ============================================================

set -e

FEDERATION_URL="http://localhost:4000"
CDC_URL="http://localhost:8090"
HR_CDC_URL="http://localhost:8084"

echo "=== ARCHITECTURE COMPARISON DEMO ==="
echo ""

# Check services are running
echo "Checking services..."
if ! curl -s "$FEDERATION_URL/.well-known/apollo/server-health" > /dev/null 2>&1; then
    echo "ERROR: Federation Router not responding at $FEDERATION_URL"
    echo "Run 'make up' first to start services."
    exit 1
fi

if ! curl -s "$CDC_URL/health" > /dev/null 2>&1; then
    echo "ERROR: CDC Query Service not responding at $CDC_URL"
    echo "Run 'make up' first to start services."
    exit 1
fi
echo "All services are running!"
echo ""

# Step 1: Create person via Federation
echo "========================================="
echo "1. Creating person via Federation"
echo "========================================="
PERSON_RESULT=$(curl -s -X POST "$FEDERATION_URL/graphql" \
    -H "Content-Type: application/json" \
    -d '{"query":"mutation { createPerson(name: \"Demo User\", email: \"demo@example.com\") { id name email } }"}')
echo "$PERSON_RESULT" | jq . 2>/dev/null || echo "$PERSON_RESULT"
echo ""

# Step 2: Query composed view from Federation
echo "========================================="
echo "2. Query composed view from FEDERATION"
echo "   (calls HR + Employment + Security subgraphs)"
echo "========================================="
START=$(date +%s%N)
FED_RESULT=$(curl -s -X POST "$FEDERATION_URL/graphql" \
    -H "Content-Type: application/json" \
    -d '{"query":"{ persons { id name email employee { title department } badge { badgeNumber accessLevel } } }"}')
END=$(date +%s%N)
FED_LATENCY=$(( (END - START) / 1000000 ))
echo "$FED_RESULT" | jq . 2>/dev/null || echo "$FED_RESULT"
echo ""
echo "Federation latency: ${FED_LATENCY}ms (3 service calls)"
echo ""

# Step 3: Query composed view from CDC
echo "========================================="
echo "3. Query composed view from CDC"
echo "   (single local query)"
echo "========================================="
START=$(date +%s%N)
CDC_RESULT=$(curl -s "$CDC_URL/api/persons")
END=$(date +%s%N)
CDC_LATENCY=$(( (END - START) / 1000000 ))
echo "$CDC_RESULT" | jq . 2>/dev/null || echo "$CDC_RESULT"
echo ""
echo "CDC latency: ${CDC_LATENCY}ms (1 local query)"
echo ""

# Step 4: Compare latencies
echo "========================================="
echo "4. LATENCY COMPARISON"
echo "========================================="
echo "Federation: ${FED_LATENCY}ms (sync, 3 services)"
echo "CDC:        ${CDC_LATENCY}ms (local projection)"
if [ $FED_LATENCY -gt $CDC_LATENCY ]; then
    DIFF=$((FED_LATENCY - CDC_LATENCY))
    echo ""
    echo "CDC is ${DIFF}ms faster!"
fi
echo ""

# Step 5: Demonstrate failure scenario
echo "========================================="
echo "5. FAILURE SCENARIO"
echo "========================================="
echo "Killing Security service..."
kubectl scale deployment security-subgraph --replicas=0 -n federation-demo 2>/dev/null || true
sleep 3

echo ""
echo "Querying Federation (Security is DOWN):"
FED_FAIL=$(curl -s -X POST "$FEDERATION_URL/graphql" \
    -H "Content-Type: application/json" \
    -d '{"query":"{ persons { id name badge { badgeNumber } } }"}')
echo "$FED_FAIL" | jq . 2>/dev/null || echo "$FED_FAIL"

echo ""
echo "Querying CDC (Security is DOWN but data is local):"
CDC_STILL_WORKS=$(curl -s "$CDC_URL/api/persons")
echo "$CDC_STILL_WORKS" | jq . 2>/dev/null || echo "$CDC_STILL_WORKS"

echo ""
echo "Restoring Security service..."
kubectl scale deployment security-subgraph --replicas=1 -n federation-demo 2>/dev/null || true

echo ""
echo "========================================="
echo "DEMO COMPLETE"
echo "========================================="
echo ""
echo "Key Takeaways:"
echo "  - Federation: Real-time data, but coupled availability"
echo "  - CDC: Fast local queries, but eventually consistent"
echo ""
echo "Open the dashboard at http://localhost:3000 to explore more!"
