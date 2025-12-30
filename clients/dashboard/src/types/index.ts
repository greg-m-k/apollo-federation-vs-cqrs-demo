// Stage timing for query operations
export interface StageTiming {
  router?: number;
  hr?: number;
  employment?: number;
  security?: number;
  hrDb?: number;
  employmentDb?: number;
  securityDb?: number;
  total?: number;
  queryService?: number;
  db?: number;
}

// Federation metrics state
export interface FederationMetrics {
  latency: number | null;
  stageTiming: StageTiming;
  servicesUp: { hr: boolean; employment: boolean; security: boolean };
  lastQuery: unknown;
  queryCount: number;
  errorCount: number;
}

// Event-Driven (Kafka) metrics state
export interface KafkaMetrics {
  latency: number | null;
  stageTiming: StageTiming;
  servicesUp: { query: boolean; consumer: boolean; kafka: boolean };
  lastQuery: unknown;
  queryCount: number;
  dataFreshness: string;
  consumerLag: number;
}

// Federation mutation timing
export interface FederationMutationMetrics {
  mutationTime: number | null;
  totalTime: number | null;
  personName: string | null;
  personId: string | null;
  routerOverhead: number | null;
  hrTime: number | null;
  hrDbTime: number | null;
}

// Event-Driven mutation timing
export interface EventDrivenMutationMetrics {
  mutationTime: number | null;
  propagationTime: number | null;
  totalTime: number | null;
  personName: string | null;
  personId: string | null;
  dbWriteTime: number | null;
  outboxWriteTime: number | null;
  serviceTime: number | null;
  outboxToKafkaMs: number | null;
  consumerToProjectionMs: number | null;
}

// Combined mutation metrics
export interface MutationMetrics {
  federation: FederationMutationMetrics;
  eventDriven: EventDrivenMutationMetrics;
}

// Log entry for activity log
export interface LogEntry {
  timestamp: string;
  message: string;
}

// Logs state
export interface Logs {
  federation: LogEntry[];
  kafka: LogEntry[];
}

// Person entity
export interface Person {
  id: string;
  name: string;
}

// Data for creating a new person
export interface CreatePersonData {
  name: string;
  email: string;
}
