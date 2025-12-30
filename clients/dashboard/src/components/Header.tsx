export default function Header() {
  return (
    <header className="bg-gradient-to-r from-blue-800 to-purple-800 text-white p-4 shadow-lg">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-2xl font-bold">Live Architectural Comparison</h1>
        <p className="text-blue-200 text-sm">GraphQL Federation vs Event-Driven Projections</p>
        <p className="text-blue-200 text-sm mt-2">
          <span className="font-medium text-yellow-300">Architecture is all about tradeoffs.</span>{' '}
          This demo runs both patterns in parallel on a real Kubernetes cluster via Tilt.
          Query and create people to see timing breakdowns, then mouse over elements to explore the details.
          This is a learning tool with basic, unoptimized implementations ... not a production benchmark.
        </p>
      </div>
    </header>
  );
}
