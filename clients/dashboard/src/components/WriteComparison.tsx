import { MutationMetrics } from '../types';

interface WriteComparisonProps {
  mutationMetrics: MutationMetrics;
  showWriteFlow: boolean;
}

export default function WriteComparison({ mutationMetrics, showWriteFlow }: WriteComparisonProps) {
  // Only show when in write flow mode and have metrics
  if (!showWriteFlow || (!mutationMetrics.federation.totalTime && !mutationMetrics.eventDriven.totalTime)) {
    return null;
  }

  return (
    <div className="bg-white rounded-lg shadow p-4 mt-4">
      <h2 className="text-lg font-bold text-gray-800 mb-2">Write Operation Comparison</h2>
      <p className="text-sm text-gray-600 mb-4">
        Created: <span className="font-semibold text-purple-600">{mutationMetrics.federation.personName || mutationMetrics.eventDriven.personName}</span>
      </p>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Federation Write */}
        <div className="bg-blue-50 rounded-lg p-4">
          <h3 className="text-sm font-semibold text-blue-800 mb-2">GraphQL Federation</h3>
          <div className="text-3xl font-bold text-blue-600">
            {mutationMetrics.federation.totalTime ? `${mutationMetrics.federation.totalTime}ms` : '-'}
          </div>
          <div className="text-xs text-gray-600 mt-1">
            Router forwards mutation to HR Subgraph which writes directly to its database
          </div>
          <div className="text-xs text-blue-700 mt-1 font-medium">
            Synchronous: data queryable immediately after response
          </div>
        </div>

        {/* Event-Driven Write */}
        <div className="bg-green-50 rounded-lg p-4">
          <h3 className="text-sm font-semibold text-green-800 mb-2">Event-Driven CQRS</h3>
          <div className="text-3xl font-bold text-green-600">
            {mutationMetrics.eventDriven.totalTime ? `${mutationMetrics.eventDriven.totalTime}ms` : '-'}
          </div>
          {mutationMetrics.eventDriven.mutationTime && (
            <div className="text-xs text-gray-600 mt-1">
              <span className="font-medium">Write:</span> {mutationMetrics.eventDriven.mutationTime}ms (DB + Outbox) +{' '}
              <span className="font-medium">Propagation:</span> {mutationMetrics.eventDriven.propagationTime}ms
            </div>
          )}
          <div className="text-xs text-gray-600 mt-1">
            Service writes to DB + Outbox, CDC publishes to Kafka, Consumer updates projection
          </div>
          <div className="text-xs text-yellow-700 mt-1 font-medium">
            Eventual consistency: projection lags behind source
          </div>
        </div>
      </div>

      {/* Insight */}
      {mutationMetrics.federation.totalTime && mutationMetrics.eventDriven.totalTime && (
        <div className="mt-4 p-3 bg-gray-100 rounded-lg text-sm text-gray-700">
          <strong>Write Tradeoff:</strong> Federation completes in{' '}
          <span className="font-bold text-blue-600">{mutationMetrics.federation.totalTime}ms</span>{' '}
          with immediate read-after-write consistency. Event-Driven returns the write in{' '}
          <span className="font-bold text-green-600">{mutationMetrics.eventDriven.mutationTime}ms</span>{' '}
          but requires{' '}
          <span className="font-bold text-yellow-600">{mutationMetrics.eventDriven.propagationTime}ms</span>{' '}
          for the projection to update. The tradeoff: Federation couples read availability to the write service, while Event-Driven decouples them via the message bus.
        </div>
      )}
    </div>
  );
}
