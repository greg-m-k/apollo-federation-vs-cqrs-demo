import { useState, useEffect, useCallback } from 'react';
import {
  FederationMetrics,
  KafkaMetrics,
  MutationMetrics,
  Logs,
  Person,
  CreatePersonData,
} from '../types';
import {
  queryPersonFederation,
  createPersonFederation,
  checkFederationHealth,
} from '../api/federation';
import {
  queryPersonProjection,
  createPersonEventDriven,
  waitForPropagation,
  fetchConsumerTiming,
  fetchPersons as apiFetchPersons,
  checkProjectionHealth,
} from '../api/eventDriven';

// Initial state values
const initialFederationMetrics: FederationMetrics = {
  latency: null,
  stageTiming: {},
  servicesUp: { hr: false, employment: false, security: false },
  lastQuery: null,
  queryCount: 0,
  errorCount: 0
};

const initialKafkaMetrics: KafkaMetrics = {
  latency: null,
  stageTiming: {},
  servicesUp: { query: false, consumer: false, kafka: false },
  lastQuery: null,
  queryCount: 0,
  dataFreshness: 'N/A',
  consumerLag: 0
};

const initialMutationMetrics: MutationMetrics = {
  federation: {
    mutationTime: null,
    totalTime: null,
    personName: null,
    personId: null,
    routerOverhead: null,
    hrTime: null,
    hrDbTime: null
  },
  eventDriven: {
    mutationTime: null,
    propagationTime: null,
    totalTime: null,
    personName: null,
    personId: null,
    dbWriteTime: null,
    outboxWriteTime: null,
    serviceTime: null,
    outboxToKafkaMs: null,
    consumerToProjectionMs: null
  }
};

export function useAppState() {
  // Metrics state
  const [federationMetrics, setFederationMetrics] = useState<FederationMetrics>(initialFederationMetrics);
  const [kafkaMetrics, setKafkaMetrics] = useState<KafkaMetrics>(initialKafkaMetrics);
  const [mutationMetrics, setMutationMetrics] = useState<MutationMetrics>(initialMutationMetrics);

  // UI state
  const [logs, setLogs] = useState<Logs>({ federation: [], kafka: [] });
  const [selectedPerson, setSelectedPerson] = useState<string>('person-001');
  // Store federation person ID separately since it differs from kafka ID
  const [lastCreatedFedPersonId, setLastCreatedFedPersonId] = useState<string | null>(null);
  const [availablePersons, setAvailablePersons] = useState<Person[]>([]);
  const [loadingPersons, setLoadingPersons] = useState<boolean>(true);
  const [showCreateModal, setShowCreateModal] = useState<boolean>(false);
  const [creating, setCreating] = useState<{ federation: boolean; kafka: boolean }>({ federation: false, kafka: false });
  const [querying, setQuerying] = useState<{ federation: boolean; kafka: boolean }>({ federation: false, kafka: false });
  // Direct control of diagram flow - true = write flow, false = query flow
  const [showWriteFlow, setShowWriteFlow] = useState<boolean>(false);

  // Add log entry
  const addLog = useCallback((side: 'federation' | 'kafka', message: string) => {
    const timestamp = new Date().toLocaleTimeString();
    setLogs(prev => ({
      ...prev,
      [side]: [...prev[side].slice(-20), { timestamp, message }]
    }));
  }, []);

  // Fetch available persons
  const fetchPersons = useCallback(async () => {
    try {
      const sorted = await apiFetchPersons();
      setAvailablePersons(sorted);
    } catch (e) {
      console.error('Failed to fetch persons:', e);
    } finally {
      setLoadingPersons(false);
    }
  }, []);

  // Load persons on mount
  useEffect(() => {
    fetchPersons();
  }, [fetchPersons]);

  // Health check on mount
  useEffect(() => {
    const checkHealth = async () => {
      const fedHealthy = await checkFederationHealth();
      if (fedHealthy) {
        setFederationMetrics(prev => ({
          ...prev,
          servicesUp: { hr: true, employment: true, security: true }
        }));
      }

      const projectionHealthy = await checkProjectionHealth();
      if (projectionHealthy) {
        setKafkaMetrics(prev => ({
          ...prev,
          servicesUp: { query: true, consumer: true, kafka: true }
        }));
      }
    };

    checkHealth();
  }, []);

  // Query Federation
  const queryFederation = useCallback(async (personIdOverride: string | null = null) => {
    const validOverride = typeof personIdOverride === 'string' ? personIdOverride : null;
    // Use federation ID from last create if available (since IDs differ between systems)
    const personId = validOverride || lastCreatedFedPersonId || selectedPerson;
    addLog('federation', `Querying for ${personId}...`);
    setQuerying(prev => ({ ...prev, federation: true }));

    try {
      const { data, latency, subgraphTiming } = await queryPersonFederation(personId);

      setFederationMetrics(prev => ({
        ...prev,
        latency,
        stageTiming: { ...subgraphTiming, total: latency },
        lastQuery: data,
        queryCount: prev.queryCount + 1,
        servicesUp: { hr: true, employment: true, security: true }
      }));

      // Log with detailed timing breakdown
      const maxSubgraph = Math.max(subgraphTiming.hr || 0, subgraphTiming.employment || 0, subgraphTiming.security || 0);
      const routerOverhead = maxSubgraph > 0 ? latency - maxSubgraph : null;

      const timingParts = [];
      if (routerOverhead != null) timingParts.push(`Router: ${routerOverhead}ms`);
      if (subgraphTiming.hr) timingParts.push(`HR: ${subgraphTiming.hr}ms${subgraphTiming.hrDb ? ` (DB: ${subgraphTiming.hrDb}ms)` : ''}`);
      if (subgraphTiming.employment) timingParts.push(`Emp: ${subgraphTiming.employment}ms${subgraphTiming.employmentDb ? ` (DB: ${subgraphTiming.employmentDb}ms)` : ''}`);
      if (subgraphTiming.security) timingParts.push(`Sec: ${subgraphTiming.security}ms${subgraphTiming.securityDb ? ` (DB: ${subgraphTiming.securityDb}ms)` : ''}`);
      const timingLog = timingParts.length > 0 ? timingParts.join(', ') : `${latency}ms`;
      addLog('federation', `Success: ${latency}ms total [${timingLog}]`);

    } catch (error) {
      setFederationMetrics(prev => ({
        ...prev,
        errorCount: prev.errorCount + 1
      }));
      addLog('federation', `ERROR: ${(error as Error).message}`);
    } finally {
      setQuerying(prev => ({ ...prev, federation: false }));
    }
  }, [selectedPerson, lastCreatedFedPersonId, addLog]);

  // Query Event-Driven Projections
  const queryKafka = useCallback(async (personIdOverride: string | null = null) => {
    const validOverride = typeof personIdOverride === 'string' ? personIdOverride : null;
    const personId = validOverride || selectedPerson;
    addLog('kafka', `Querying for ${personId}...`);
    setQuerying(prev => ({ ...prev, kafka: true }));

    try {
      const { data, latency, queryServiceTime, dbTime, dataFreshness } = await queryPersonProjection(personId);

      setKafkaMetrics(prev => ({
        ...prev,
        latency,
        stageTiming: { queryService: queryServiceTime, db: dbTime, total: latency },
        lastQuery: data,
        queryCount: prev.queryCount + 1,
        servicesUp: { query: true, consumer: true, kafka: true },
        dataFreshness
      }));

      // Log with detailed timing breakdown
      const networkOverhead = queryServiceTime ? Math.max(0, latency - queryServiceTime) : null;
      const timingParts = [];
      if (networkOverhead != null) timingParts.push(`Network: ${networkOverhead}ms`);
      timingParts.push(`Projection: ${queryServiceTime}ms`);
      if (dbTime != null) timingParts.push(`DB: ${dbTime}ms`);
      timingParts.push(`freshness: ${dataFreshness}`);
      addLog('kafka', `Success: ${latency}ms total [${timingParts.join(', ')}]`);

    } catch (error) {
      setKafkaMetrics(prev => ({ ...prev }));
      addLog('kafka', `ERROR: ${(error as Error).message}`);
    } finally {
      setQuerying(prev => ({ ...prev, kafka: false }));
    }
  }, [selectedPerson, addLog]);

  // User-initiated query wrappers - these switch to query flow
  const queryFederationUser = useCallback(() => {
    setShowWriteFlow(false);
    queryFederation();
  }, [queryFederation]);

  const queryKafkaUser = useCallback(() => {
    setShowWriteFlow(false);
    queryKafka();
  }, [queryKafka]);

  // Query both - user-initiated, switches to query flow
  const queryBoth = useCallback(() => {
    setShowWriteFlow(false);
    queryFederation();
    queryKafka();
  }, [queryFederation, queryKafka]);

  // Create person in both architectures
  const createPerson = useCallback(async ({ name, email }: CreatePersonData) => {
    if (!name) return;

    let fedPersonId: string | null = null;

    // Create in Federation
    setCreating(prev => ({ ...prev, federation: true }));
    addLog('federation', `Creating person: ${name}...`);
    try {
      const result = await createPersonFederation(name, email);
      fedPersonId = result.personId;

      setMutationMetrics(prev => ({
        ...prev,
        federation: {
          mutationTime: result.mutationTime,
          totalTime: result.mutationTime,
          personName: name,
          personId: fedPersonId,
          routerOverhead: result.routerOverhead,
          hrTime: result.hrTime,
          hrDbTime: result.hrDbTime
        }
      }));

      // Show write flow in diagram after create
      setShowWriteFlow(true);

      setFederationMetrics(prev => ({
        ...prev,
        servicesUp: { hr: true, employment: true, security: true }
      }));

      addLog('federation', `Person created: ${name} in ${result.mutationTime}ms (data immediately available)`);

      // Store federation ID for subsequent queries
      if (fedPersonId) {
        setLastCreatedFedPersonId(fedPersonId);
        queryFederation(fedPersonId);
      }
    } catch (e) {
      addLog('federation', `ERROR: ${(e as Error).message}`);
    } finally {
      setCreating(prev => ({ ...prev, federation: false }));
    }

    // Create in Event-Driven
    setCreating(prev => ({ ...prev, kafka: true }));
    addLog('kafka', `Creating person: ${name}...`);
    try {
      const result = await createPersonEventDriven(name, email);
      addLog('kafka', `Mutation complete: ${result.mutationTime}ms. Waiting for Kafka propagation...`);

      const propagationTime = await waitForPropagation(result.newPerson.id);
      const totalTime = result.mutationTime + (propagationTime || 0);

      const timing = await fetchConsumerTiming(result.newPerson.id);

      setMutationMetrics(prev => ({
        ...prev,
        eventDriven: {
          mutationTime: result.mutationTime,
          propagationTime,
          totalTime,
          personName: name,
          personId: result.newPerson?.id,
          dbWriteTime: result.dbWriteTime,
          outboxWriteTime: result.outboxWriteTime,
          serviceTime: result.serviceTime,
          outboxToKafkaMs: timing.outboxToKafkaMs,
          consumerToProjectionMs: timing.consumerToProjectionMs
        }
      }));

      setKafkaMetrics(prev => ({
        ...prev,
        servicesUp: { query: true, consumer: true, kafka: true }
      }));

      if (propagationTime !== null) {
        const timingDetails = timing.outboxToKafkaMs != null
          ? ` (outbox→kafka: ${timing.outboxToKafkaMs}ms, consumer→projection: ${timing.consumerToProjectionMs}ms)`
          : '';
        addLog('kafka', `Propagated in ${propagationTime}ms${timingDetails}. Total: ${totalTime}ms`);
      } else {
        addLog('kafka', `WARNING: Propagation timeout after 10s`);
      }

      await fetchPersons();
      if (result.newPerson?.id) {
        setSelectedPerson(result.newPerson.id);
        queryKafka(result.newPerson.id);
      }
    } catch (e) {
      addLog('kafka', `ERROR: ${(e as Error).message}`);
    } finally {
      setCreating(prev => ({ ...prev, kafka: false }));
    }
  }, [addLog, fetchPersons, queryKafka, queryFederation]);

  return {
    // Metrics
    federationMetrics,
    kafkaMetrics,
    mutationMetrics,
    // Logs
    logs,
    // Person selection
    selectedPerson,
    setSelectedPerson,
    availablePersons,
    loadingPersons,
    // Modal
    showCreateModal,
    setShowCreateModal,
    // Loading states
    creating,
    querying,
    // Diagram flow control
    showWriteFlow,
    // Actions - user-initiated queries switch to query flow
    queryFederation: queryFederationUser,
    queryKafka: queryKafkaUser,
    queryBoth,
    createPerson,
  };
}
