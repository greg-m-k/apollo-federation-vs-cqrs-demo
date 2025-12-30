import React, { ReactNode } from 'react';
import { MutationMetrics } from '../types';

interface TooltipProps {
  children: ReactNode;
  text: string;
}

// Tooltip wrapper component
function Tooltip({ children, text }: TooltipProps) {
  return (
    <span className="relative group cursor-help">
      {children}
      <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-3 py-2 bg-gray-900 text-white text-xs rounded-lg opacity-0 group-hover:opacity-100 transition-opacity z-50 pointer-events-none w-56 text-center leading-relaxed shadow-lg">
        {text}
        <span className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-gray-900"></span>
      </span>
    </span>
  );
}

// Loading spinner component
function Spinner() {
  return (
    <svg className="animate-spin h-6 w-6 text-gray-400" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
    </svg>
  );
}

interface ComparisonSummaryProps {
  federationLatency: number | null;
  kafkaLatency: number | null;
  kafkaFreshness: string;
  mutationMetrics: MutationMetrics;
  isCreating: { federation: boolean; kafka: boolean };
  isQuerying: { federation: boolean; kafka: boolean };
  showWriteFlow: boolean;
}

function ComparisonSummary({
  federationLatency,
  kafkaLatency,
  kafkaFreshness,
  mutationMetrics,
  isCreating,
  isQuerying,
  showWriteFlow,
}: ComparisonSummaryProps) {
  // Show create comparison when in write flow mode (creating or just finished creating)
  const showingCreate = showWriteFlow || isCreating.federation || isCreating.kafka;

  const isLoading = isCreating.federation || isCreating.kafka || isQuerying.federation || isQuerying.kafka;

  // Query comparison calculations
  const queryLatencyDiff = federationLatency && kafkaLatency
    ? Math.round((federationLatency - kafkaLatency) / kafkaLatency * 100)
    : null;

  // Create comparison calculations
  const fedCreateTime = mutationMetrics.federation.totalTime;
  const eventCreateTime = mutationMetrics.eventDriven.totalTime;
  const createTimeDiff = fedCreateTime && eventCreateTime
    ? Math.round((eventCreateTime - fedCreateTime) / fedCreateTime * 100)
    : null;

  if (showingCreate) {
    return (
      <div className="bg-white rounded-lg shadow p-4">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-gray-800">Create Operation Comparison</h2>
          {isLoading && <Spinner />}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Write Latency */}
          <div className="bg-gradient-to-br from-purple-50 to-pink-50 rounded-lg p-4">
            <Tooltip text="Total time for the create operation to complete and data to be available for reading.">
              <h3 className="text-sm font-semibold text-gray-600 mb-2 border-b border-dotted border-gray-400">Write-to-Read Latency</h3>
            </Tooltip>
            <div className="flex justify-between items-center">
              <div className="text-center">
                <Tooltip text="Federation: single GraphQL mutation. Data is immediately available after mutation completes.">
                  <div className="text-2xl font-bold text-blue-600">
                    {isCreating.federation ? <Spinner /> : fedCreateTime ? `${fedCreateTime}ms` : '-'}
                  </div>
                </Tooltip>
                <div className="text-xs text-gray-500">Federation</div>
              </div>
              <div className="text-gray-400">vs</div>
              <div className="text-center">
                <Tooltip text="Event-Driven: write + Kafka propagation + projection update. Data available after full pipeline completes.">
                  <div className="text-2xl font-bold text-green-600">
                    {isCreating.kafka ? <Spinner /> : eventCreateTime ? `${eventCreateTime}ms` : '-'}
                  </div>
                </Tooltip>
                <div className="text-xs text-gray-500">Event-Driven</div>
              </div>
            </div>
            {createTimeDiff !== null && (
              <div className="text-center mt-2 text-sm text-gray-600">
                Federation is <span className="font-bold text-blue-600">{Math.abs(createTimeDiff)}%</span> faster for writes
              </div>
            )}
          </div>

          {/* Write Consistency */}
          <div className="bg-gradient-to-br from-yellow-50 to-orange-50 rounded-lg p-4">
            <Tooltip text="When is data available after write? This is the key tradeoff for write operations.">
              <h3 className="text-sm font-semibold text-gray-600 mb-2 border-b border-dotted border-gray-400">Write Consistency</h3>
            </Tooltip>
            <div className="flex justify-between items-center">
              <div className="text-center">
                <Tooltip text="Federation writes directly to the database. The response confirms data is persisted.">
                  <div className="text-lg font-bold text-blue-600">Immediate</div>
                </Tooltip>
                <div className="text-xs text-gray-500">Federation</div>
              </div>
              <div className="text-gray-400">vs</div>
              <div className="text-center">
                <Tooltip text="Event-Driven writes to outbox, then Kafka propagates to projection. Eventual consistency.">
                  <div className="text-lg font-bold text-yellow-600">
                    {mutationMetrics.eventDriven.propagationTime
                      ? `+${mutationMetrics.eventDriven.propagationTime}ms`
                      : 'Propagating'}
                  </div>
                </Tooltip>
                <div className="text-xs text-gray-500">Propagation</div>
              </div>
            </div>
            <div className="text-center mt-2 text-xs text-gray-500">
              Event-Driven: write first, propagate after
            </div>
          </div>

          {/* Write Failure Mode */}
          <div className="bg-gradient-to-br from-red-50 to-pink-50 rounded-lg p-4">
            <Tooltip text="What happens when something goes wrong during a write operation?">
              <h3 className="text-sm font-semibold text-gray-600 mb-2 border-b border-dotted border-gray-400">Failure Mode</h3>
            </Tooltip>
            <div className="space-y-2 text-sm">
              <Tooltip text="Federation writes fail if Router or HR subgraph is down. No write = no data. Clear failure.">
                <div className="flex items-center">
                  <span className="w-24 text-blue-600 font-medium">Federation:</span>
                  <span className="text-gray-600">Write fails immediately</span>
                </div>
              </Tooltip>
              <Tooltip text="Event-Driven writes succeed if local DB is up. If Kafka is down, outbox retries later. Write succeeds but propagation delays.">
                <div className="flex items-center">
                  <span className="w-24 text-green-600 font-medium">Event-Driven:</span>
                  <span className="text-gray-600">Write OK, propagation may lag</span>
                </div>
              </Tooltip>
            </div>
          </div>
        </div>

        {/* Key Insight */}
        <div className="mt-4 p-3 bg-gray-100 rounded-lg text-sm text-gray-700">
          <strong>Key Insight:</strong> Federation provides immediate consistency after writes but couples to subgraph availability.
          Event-Driven decouples services via Kafka but requires waiting for propagation before data is queryable.
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow p-4">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-gray-800">Query Tradeoff Comparison</h2>
        {isLoading && <Spinner />}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Latency Comparison */}
        <div className="bg-gradient-to-br from-blue-50 to-green-50 rounded-lg p-4">
          <Tooltip text="Time from request to response. Federation makes multiple network calls; Event-Driven queries local data.">
            <h3 className="text-sm font-semibold text-gray-600 mb-2 border-b border-dotted border-gray-400">Query Latency</h3>
          </Tooltip>
          <div className="flex justify-between items-center">
            <div className="text-center">
              <Tooltip text="Federation latency is additive: Router + all subgraph calls in parallel. Higher latency but always fresh data.">
                <div className="text-2xl font-bold text-blue-600">
                  {isQuerying.federation ? <Spinner /> : federationLatency ? `${federationLatency}ms` : '-'}
                </div>
              </Tooltip>
              <div className="text-xs text-gray-500">Federation</div>
            </div>
            <div className="text-gray-400">vs</div>
            <div className="text-center">
              <Tooltip text="Event-Driven latency is just one local database query. Much faster but data may be slightly stale.">
                <div className="text-2xl font-bold text-green-600">
                  {isQuerying.kafka ? <Spinner /> : kafkaLatency ? `${kafkaLatency}ms` : '-'}
                </div>
              </Tooltip>
              <div className="text-xs text-gray-500">Event-Driven</div>
            </div>
          </div>
          {queryLatencyDiff !== null && (
            <div className="text-center mt-2 text-sm text-gray-600">
              Event-Driven is <span className="font-bold text-green-600">{queryLatencyDiff}%</span> faster
            </div>
          )}
        </div>

        {/* Consistency Model */}
        <div className="bg-gradient-to-br from-yellow-50 to-orange-50 rounded-lg p-4">
          <Tooltip text="How fresh is the data? Federation always gets current data; Event-Driven uses cached projections.">
            <h3 className="text-sm font-semibold text-gray-600 mb-2 border-b border-dotted border-gray-400">Data Consistency</h3>
          </Tooltip>
          <div className="flex justify-between items-center">
            <div className="text-center">
              <Tooltip text="Federation queries source databases directly. Data is always current, but requires all services to be available.">
                <div className="text-lg font-bold text-blue-600">Real-time</div>
              </Tooltip>
              <div className="text-xs text-gray-500">Federation</div>
            </div>
            <div className="text-gray-400">vs</div>
            <div className="text-center">
              <Tooltip text="Time since last Kafka event updated the projection. High values mean no recent changes to source data.">
                <div className="text-lg font-bold text-yellow-600">{kafkaFreshness}</div>
              </Tooltip>
              <div className="text-xs text-gray-500">Data Freshness</div>
            </div>
          </div>
          <div className="text-center mt-2 text-xs text-gray-500">
            Eventually consistent with lag indicator
          </div>
        </div>

        {/* Availability */}
        <div className="bg-gradient-to-br from-red-50 to-pink-50 rounded-lg p-4">
          <Tooltip text="What happens when a source service goes down? This shows the availability tradeoff.">
            <h3 className="text-sm font-semibold text-gray-600 mb-2 border-b border-dotted border-gray-400">Failure Mode</h3>
          </Tooltip>
          <div className="space-y-2 text-sm">
            <Tooltip text="Federation requires all subgraphs to respond. If HR, Employment, or Security is down, the entire query fails.">
              <div className="flex items-center">
                <span className="w-24 text-blue-600 font-medium">Federation:</span>
                <span className="text-gray-600">1 service down = query fails</span>
              </div>
            </Tooltip>
            <Tooltip text="Event-Driven queries local database only. Even if source services are down, queries succeed with the last known data.">
              <div className="flex items-center">
                <span className="w-24 text-green-600 font-medium">Event-Driven:</span>
                <span className="text-gray-600">Query works (stale data)</span>
              </div>
            </Tooltip>
          </div>
        </div>
      </div>

      {/* Key Insight */}
      <div className="mt-4 p-3 bg-gray-100 rounded-lg text-sm text-gray-700">
        <strong>Key Insight:</strong> Federation provides real-time data consistency but couples availability
        to all services. Event-Driven Projections decouple services at the cost of eventual consistency.
      </div>
    </div>
  );
}

export default ComparisonSummary;
