import { KAFKA_URL, HR_EVENTS_URL, CONSUMER_URL } from '../config/api';
import { Person } from '../types';

// Query a composed person from the projection service
export async function queryPersonProjection(personId: string) {
  const startTime = performance.now();

  const response = await fetch(`${KAFKA_URL}/composed/${personId}`);
  const data = await response.json();
  const latency = Math.round(performance.now() - startTime);

  // Get backend query time and DB timing from headers
  const backendTime = response.headers.get('X-Query-Time-Ms');
  const dbTimeHeader = response.headers.get('X-Db-Time-Ms');
  const queryServiceTime = backendTime ? parseInt(backendTime) : Math.round(latency * 0.3);
  const dbTime = dbTimeHeader ? parseInt(dbTimeHeader) : null;

  return {
    data,
    latency,
    queryServiceTime,
    dbTime,
    dataFreshness: data.freshness?.dataFreshness || 'N/A'
  };
}

// Fetch all persons from the projection service
export async function fetchPersons(): Promise<Person[]> {
  const response = await fetch(`${KAFKA_URL}/persons`);
  const persons = await response.json();

  // Sort by name, with original seed data first
  return persons.sort((a: Person, b: Person) => {
    const aIsSeed = a.id.match(/^person-00[1-5]$/);
    const bIsSeed = b.id.match(/^person-00[1-5]$/);
    if (aIsSeed && !bIsSeed) return -1;
    if (!aIsSeed && bIsSeed) return 1;
    return a.name.localeCompare(b.name);
  });
}

// Create a person via the HR Events service
export async function createPersonEventDriven(name: string, email: string) {
  const startTime = performance.now();

  const response = await fetch(`${HR_EVENTS_URL}/api/persons`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, hireDate: new Date().toISOString().split('T')[0] })
  });

  const newPerson = await response.json();
  const mutationTime = Math.round(performance.now() - startTime);

  // Extract detailed timing from hr-events-service headers
  const hrEventsTime = response.headers.get('X-HR-Events-Time-Ms');
  const hrEventsDetails = response.headers.get('X-HR-Events-Timing-Details');
  let dbWriteTime = null;
  let outboxWriteTime = null;
  if (hrEventsDetails) {
    try {
      const details = JSON.parse(hrEventsDetails);
      dbWriteTime = details.db_write ?? null;
      outboxWriteTime = details.outbox_write ?? null;
    } catch {}
  }

  return {
    newPerson,
    mutationTime,
    serviceTime: hrEventsTime ? parseInt(hrEventsTime, 10) : null,
    dbWriteTime,
    outboxWriteTime
  };
}

// Poll for person to appear in projection (measures propagation delay)
export async function waitForPropagation(personId: string, maxWaitMs: number = 10000): Promise<number | null> {
  const startTime = performance.now();
  const pollInterval = 100; // Check every 100ms

  while (performance.now() - startTime < maxWaitMs) {
    try {
      const response = await fetch(`${KAFKA_URL}/persons`);
      const persons: Person[] = await response.json();
      if (persons.some(p => p.id === personId)) {
        return Math.round(performance.now() - startTime);
      }
    } catch {
      // Ignore errors during polling
    }
    await new Promise(resolve => setTimeout(resolve, pollInterval));
  }
  return null; // Timeout
}

// Fetch timing breakdown from consumer service
export async function fetchConsumerTiming(personId: string) {
  try {
    const response = await fetch(`${CONSUMER_URL}/api/metrics/timing/${personId}`);
    if (response.ok) {
      const data = await response.json();
      return {
        outboxToKafkaMs: data.outboxToKafkaMs,
        consumerToProjectionMs: data.consumerToProjectionMs
      };
    }
  } catch (e) {
    console.log('Could not fetch timing breakdown:', e);
  }
  return { outboxToKafkaMs: null, consumerToProjectionMs: null };
}

// Check if projection services are healthy
export async function checkProjectionHealth(): Promise<boolean> {
  const kafkaBaseUrl = KAFKA_URL.replace('/api', '');
  try {
    const response = await fetch(`${kafkaBaseUrl}/q/health/ready`, { method: 'GET' });
    return response.ok;
  } catch {
    return false;
  }
}
