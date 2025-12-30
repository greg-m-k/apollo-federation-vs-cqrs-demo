import { FEDERATION_URL } from '../config/api';
import { StageTiming } from '../types';

// Extract per-subgraph timing from response headers
export function extractTimingFromHeaders(response: Response): StageTiming {
  const timing: StageTiming = {};

  const hrTime = response.headers.get('X-HR-Time-Ms');
  const empTime = response.headers.get('X-Employment-Time-Ms');
  const secTime = response.headers.get('X-Security-Time-Ms');

  if (hrTime) timing.hr = parseInt(hrTime, 10);
  if (empTime) timing.employment = parseInt(empTime, 10);
  if (secTime) timing.security = parseInt(secTime, 10);

  // Extract detailed db timing from JSON headers
  const parseDetails = (detailsHeader: string | null): number | undefined => {
    if (!detailsHeader) return undefined;
    try {
      const details = JSON.parse(detailsHeader);
      return details.db_query || details.db_resolve || undefined;
    } catch { return undefined; }
  };

  timing.hrDb = parseDetails(response.headers.get('X-HR-Timing-Details'));
  timing.employmentDb = parseDetails(response.headers.get('X-Employment-Timing-Details'));
  timing.securityDb = parseDetails(response.headers.get('X-Security-Timing-Details'));

  return timing;
}

// Query a person via GraphQL Federation
export async function queryPersonFederation(personId: string) {
  const startTime = performance.now();

  const query = `
    {
      person(id: "${personId}") {
        id
        name
        email
        hireDate
        active
        employee {
          id
          title
          department
          salary
        }
        badge {
          id
          badgeNumber
          accessLevel
          clearance
        }
      }
    }
  `;

  const response = await fetch(FEDERATION_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query })
  });

  const latency = Math.round(performance.now() - startTime);
  const subgraphTiming = extractTimingFromHeaders(response);
  const data = await response.json();

  return { data, latency, subgraphTiming };
}

// Create a person via GraphQL Federation mutation
export async function createPersonFederation(name: string, email: string) {
  const startTime = performance.now();

  const mutation = `
    mutation {
      createPerson(name: "${name}", email: "${email}") {
        id
        name
      }
    }
  `;

  const response = await fetch(FEDERATION_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query: mutation })
  });

  const mutationTime = Math.round(performance.now() - startTime);

  // Extract timing from headers (mutation only hits HR subgraph)
  const hrTime = response.headers.get('X-HR-Time-Ms');
  const hrDetails = response.headers.get('X-HR-Timing-Details');
  let hrDbTime = null;
  if (hrDetails) {
    try {
      const details = JSON.parse(hrDetails);
      hrDbTime = details.db_write || details.db_query || null;
    } catch {}
  }

  const data = await response.json();
  const personId = data?.data?.createPerson?.id;
  const hrSubgraphTime = hrTime ? parseInt(hrTime, 10) : null;
  const routerOverhead = hrSubgraphTime ? Math.max(0, mutationTime - hrSubgraphTime) : null;

  return {
    data,
    personId,
    mutationTime,
    hrTime: hrSubgraphTime,
    hrDbTime,
    routerOverhead
  };
}

// Check if Federation services are healthy
export async function checkFederationHealth(): Promise<boolean> {
  try {
    const response = await fetch(FEDERATION_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: '{ __typename }' })
    });
    return response.ok;
  } catch {
    return false;
  }
}
