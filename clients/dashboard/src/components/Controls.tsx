import { Person } from '../types';

interface ControlsProps {
  selectedPerson: string;
  onSelectPerson: (personId: string) => void;
  availablePersons: Person[];
  loadingPersons: boolean;
  onQueryBoth: () => void;
  onCreatePerson: () => void;
}

export default function Controls({
  selectedPerson,
  onSelectPerson,
  availablePersons,
  loadingPersons,
  onQueryBoth,
  onCreatePerson,
}: ControlsProps) {
  return (
    <div className="bg-white rounded-lg shadow p-4 mb-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <div className="lg:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">Select Person:</label>
          <select
            value={selectedPerson}
            onChange={(e) => onSelectPerson(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-4 py-2.5 bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
            disabled={loadingPersons}
          >
            {loadingPersons ? (
              <option>Loading...</option>
            ) : availablePersons.length === 0 ? (
              <option value="person-001">person-001 (default)</option>
            ) : (
              availablePersons.map(person => (
                <option key={person.id} value={person.id}>
                  {person.name} ({person.id})
                </option>
              ))
            )}
          </select>
        </div>
        <div className="flex flex-col">
          <label className="block text-sm font-medium text-gray-700 mb-1 sm:invisible sm:h-5">Action</label>
          <button onClick={onQueryBoth} className="flex-1 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-medium py-2.5 px-4 rounded-lg transition-all shadow-sm hover:shadow-md">
            Query Both
          </button>
        </div>
        <div className="flex flex-col">
          <label className="block text-sm font-medium text-gray-700 mb-1 sm:invisible sm:h-5">Action</label>
          <button onClick={onCreatePerson} className="flex-1 bg-white border-2 border-purple-600 text-purple-600 hover:bg-purple-50 font-medium py-2.5 px-4 rounded-lg transition-all">
            Create Person
          </button>
        </div>
      </div>
    </div>
  );
}
