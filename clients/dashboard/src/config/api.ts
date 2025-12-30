// API endpoint configuration
// Uses localhost URLs for development, proxied paths for production

export const FEDERATION_URL = window.location.hostname === 'localhost'
  ? 'http://localhost:4000'
  : '/api/federation';

export const KAFKA_URL = window.location.hostname === 'localhost'
  ? 'http://localhost:8090/api'
  : '/api/kafka';

// HR Events service for mutations (separate from query service)
export const HR_EVENTS_URL = window.location.hostname === 'localhost'
  ? 'http://localhost:8084'
  : '/api/hr-events';

// Projection consumer for timing metrics
export const CONSUMER_URL = window.location.hostname === 'localhost'
  ? 'http://localhost:8089'
  : '/api/consumer';

// Subgraph metrics endpoints (Micrometer Prometheus format)
export const SUBGRAPH_METRICS = window.location.hostname === 'localhost'
  ? {
      hr: 'http://localhost:8091/q/metrics',
      employment: 'http://localhost:8092/q/metrics',
      security: 'http://localhost:8093/q/metrics'
    }
  : {
      hr: '/api/hr-subgraph/q/metrics',
      employment: '/api/employment-subgraph/q/metrics',
      security: '/api/security-subgraph/q/metrics'
    };
