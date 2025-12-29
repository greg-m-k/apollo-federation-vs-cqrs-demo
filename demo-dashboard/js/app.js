// Configuration - update these for your environment
const GRAPHQL_ENDPOINT = 'http://localhost:30400';
const CDC_ENDPOINT = 'http://localhost:30081';

// State
let eventSource = null;
let eventCount = 0;
let lastMutationTime = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    updateEndpointDisplay();
    refreshGraphQL();
    loadEventHistory().then(() => connectToSSE());
});

// Load historical CDC events
async function loadEventHistory() {
    const statusEl = document.getElementById('cdcStatus');
    const eventsEl = document.getElementById('cdcEvents');

    try {
        statusEl.textContent = 'Loading...';
        const response = await fetch(`${CDC_ENDPOINT}/events/history`);
        const events = await response.json();

        // Add events in reverse order (oldest first, so newest ends up on top)
        events.forEach(event => addCdcEvent(event, false));

        console.log(`Loaded ${events.length} historical events`);
    } catch (error) {
        console.error('Failed to load event history:', error);
        eventsEl.innerHTML = `<div class="event-item">Failed to load history: ${error.message}</div>`;
    }
}

// Update endpoint display in footer
function updateEndpointDisplay() {
    document.getElementById('graphqlEndpoint').textContent = GRAPHQL_ENDPOINT;
    document.getElementById('cdcEndpoint').textContent = CDC_ENDPOINT;
}

// GraphQL Functions
async function refreshGraphQL() {
    const start = performance.now();
    const resultsEl = document.getElementById('graphqlResults');
    const latencyEl = document.getElementById('graphqlLatency');
    const lastUpdateEl = document.getElementById('graphqlLastUpdate');

    try {
        const response = await fetch(GRAPHQL_ENDPOINT, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                query: `{
                    products {
                        id
                        name
                        price
                        category {
                            id
                            name
                        }
                    }
                }`
            })
        });

        const data = await response.json();
        const latency = Math.round(performance.now() - start);

        resultsEl.textContent = JSON.stringify(data, null, 2);
        latencyEl.textContent = `Latency: ${latency}ms`;
        lastUpdateEl.textContent = `Last: ${formatTime(new Date())}`;

    } catch (error) {
        resultsEl.textContent = `Error: ${error.message}\n\nMake sure Apollo Router is running at ${GRAPHQL_ENDPOINT}`;
        latencyEl.textContent = 'Latency: Error';
    }
}

async function createProduct() {
    const name = document.getElementById('productName').value;
    const price = parseFloat(document.getElementById('productPrice').value);
    const categoryId = document.getElementById('categoryId').value;

    lastMutationTime = performance.now();

    try {
        const response = await fetch(GRAPHQL_ENDPOINT, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                query: `
                    mutation CreateProduct($input: CreateProductInput!) {
                        createProduct(input: $input) {
                            id
                            name
                            price
                            category {
                                id
                                name
                            }
                        }
                    }
                `,
                variables: {
                    input: {
                        name: name,
                        price: price,
                        categoryId: categoryId
                    }
                }
            })
        });

        const data = await response.json();
        const latency = Math.round(performance.now() - lastMutationTime);

        // Update GraphQL panel immediately
        document.getElementById('graphqlResults').textContent =
            `Mutation completed in ${latency}ms\n\n` +
            JSON.stringify(data, null, 2);
        document.getElementById('graphqlLatency').textContent = `Latency: ${latency}ms`;
        document.getElementById('graphqlLastUpdate').textContent = `Last: ${formatTime(new Date())}`;

        // Schedule a refresh to show updated list
        setTimeout(refreshGraphQL, 500);

    } catch (error) {
        document.getElementById('graphqlResults').textContent = `Error: ${error.message}`;
    }
}

// CDC SSE Functions
function connectToSSE() {
    const statusEl = document.getElementById('cdcStatus');
    const eventsEl = document.getElementById('cdcEvents');

    if (eventSource) {
        eventSource.close();
    }

    try {
        eventSource = new EventSource(`${CDC_ENDPOINT}/events/stream`);

        eventSource.onopen = () => {
            statusEl.textContent = 'Connected';
            statusEl.classList.add('connected');
        };

        eventSource.onmessage = (event) => {
            const cdcEvent = JSON.parse(event.data);
            addCdcEvent(cdcEvent);
        };

        eventSource.onerror = (error) => {
            statusEl.textContent = 'Disconnected';
            statusEl.classList.remove('connected');

            // Try to reconnect after 3 seconds
            setTimeout(connectToSSE, 3000);
        };

    } catch (error) {
        statusEl.textContent = 'Error';
        eventsEl.innerHTML = `<div class="event-item">Error connecting to CDC stream: ${error.message}</div>`;
    }
}

function addCdcEvent(cdcEvent, isNew = true) {
    eventCount++;
    const eventsEl = document.getElementById('cdcEvents');
    const countEl = document.getElementById('cdcEventCount');
    const lagEl = document.getElementById('cdcLag');

    // Calculate lag if we know when mutation was triggered (only for new events)
    if (isNew && lastMutationTime && cdcEvent.operation !== 'READ') {
        const lag = Math.round(performance.now() - lastMutationTime);
        lagEl.textContent = `Lag: ~${lag}ms`;
        lastMutationTime = null; // Reset after showing lag
    }

    countEl.textContent = `Events: ${eventCount}`;

    // Create event item
    const eventItem = document.createElement('div');
    const opClass = cdcEvent.operation ? cdcEvent.operation.toLowerCase() : 'unknown';
    eventItem.className = `event-item ${opClass}`;

    const timestamp = cdcEvent.timestamp ? new Date(cdcEvent.timestamp) : new Date();
    const payload = typeof cdcEvent.payload === 'string'
        ? cdcEvent.payload
        : JSON.stringify(cdcEvent.payload, null, 2);

    eventItem.innerHTML = `
        <div class="event-header">
            <span class="event-op">${cdcEvent.operation || 'EVENT'}</span>
            <span class="event-time">${formatTime(timestamp)} | ${cdcEvent.table || 'unknown'}</span>
        </div>
        <div class="event-payload">${truncatePayload(payload)}</div>
    `;

    // Add to top of list
    eventsEl.insertBefore(eventItem, eventsEl.firstChild);

    // Keep only last 50 events
    while (eventsEl.children.length > 50) {
        eventsEl.removeChild(eventsEl.lastChild);
    }
}

// Utility Functions
function formatTime(date) {
    return date.toLocaleTimeString('en-US', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

function truncatePayload(payload, maxLength = 200) {
    if (payload.length <= maxLength) return payload;
    return payload.substring(0, maxLength) + '...';
}
