import { useAppState } from './hooks/useAppState';
import Header from './components/Header';
import InfrastructureInfo from './components/InfrastructureInfo';
import Controls from './components/Controls';
import ComparisonSummary from './components/ComparisonSummary';
import WriteComparison from './components/WriteComparison';
import ArchitecturePanel from './components/ArchitecturePanel';
import CreatePersonModal from './components/CreatePersonModal';

function App() {
  const {
    federationMetrics,
    kafkaMetrics,
    mutationMetrics,
    logs,
    selectedPerson,
    setSelectedPerson,
    availablePersons,
    loadingPersons,
    showCreateModal,
    setShowCreateModal,
    creating,
    querying,
    showWriteFlow,
    queryFederation,
    queryKafka,
    queryBoth,
    createPerson,
  } = useAppState();

  return (
    <div className="min-h-screen bg-gray-100">
      <Header />

      <div className="max-w-7xl mx-auto p-4">
        <InfrastructureInfo />

        <Controls
          selectedPerson={selectedPerson}
          onSelectPerson={setSelectedPerson}
          availablePersons={availablePersons}
          loadingPersons={loadingPersons}
          onQueryBoth={queryBoth}
          onCreatePerson={() => setShowCreateModal(true)}
        />

        <ComparisonSummary
          federationLatency={federationMetrics.latency}
          kafkaLatency={kafkaMetrics.latency}
          kafkaFreshness={kafkaMetrics.dataFreshness}
          mutationMetrics={mutationMetrics}
          isCreating={creating}
          isQuerying={querying}
          showWriteFlow={showWriteFlow}
        />

        <WriteComparison mutationMetrics={mutationMetrics} showWriteFlow={showWriteFlow} />

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4">
          <ArchitecturePanel
            title="GraphQL Federation"
            type="federation"
            metrics={federationMetrics}
            logs={logs.federation}
            onQuery={queryFederation}
            mutationTiming={mutationMetrics.federation}
            isCreating={creating.federation}
            isQuerying={querying.federation}
            showWriteFlow={showWriteFlow}
          />
          <ArchitecturePanel
            title="Event-Driven Projections"
            type="kafka"
            metrics={kafkaMetrics}
            logs={logs.kafka}
            onQuery={queryKafka}
            mutationTiming={mutationMetrics.eventDriven}
            isCreating={creating.kafka}
            isQuerying={querying.kafka}
            showWriteFlow={showWriteFlow}
          />
        </div>
      </div>

      <CreatePersonModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={createPerson}
      />
    </div>
  );
}

export default App;
