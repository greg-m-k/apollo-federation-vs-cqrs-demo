import React, { useState, useEffect, useRef, FormEvent, KeyboardEvent } from 'react';

interface CreatePersonModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string; email: string }) => void;
}

function CreatePersonModal({ isOpen, onClose, onSubmit }: CreatePersonModalProps) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [autoEmail, setAutoEmail] = useState(true);
  const inputRef = useRef<HTMLInputElement>(null);

  // Focus input when modal opens
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
      setName('');
      setEmail('');
      setAutoEmail(true);
    }
  }, [isOpen]);

  // Auto-generate email from name
  useEffect(() => {
    if (autoEmail && name) {
      setEmail(`${name.toLowerCase().replace(/\s+/g, '.')}@company.com`);
    }
  }, [name, autoEmail]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (name.trim()) {
      onSubmit({ name: name.trim(), email: email.trim() });
      onClose();
    }
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      onClick={onClose}
      onKeyDown={handleKeyDown}
    >
      <div
        className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md mx-4"
        onClick={e => e.stopPropagation()}
      >
        <h2 className="text-xl font-bold text-gray-800 mb-4">Create New Person</h2>
        <p className="text-sm text-gray-600 mb-4">
          This will create the person in both architectures simultaneously to compare write performance.
        </p>

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Full Name *
            </label>
            <input
              ref={inputRef}
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. John Smith"
              className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-purple-500 focus:border-purple-500 outline-none"
              required
            />
          </div>

          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Email
              <span className="text-gray-400 font-normal ml-2">
                {autoEmail ? '(auto-generated)' : ''}
              </span>
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setAutoEmail(false);
              }}
              placeholder="john.smith@company.com"
              className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-purple-500 focus:border-purple-500 outline-none"
            />
          </div>

          <div className="flex gap-3 mt-6">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!name.trim()}
              className="flex-1 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
            >
              Create Person
            </button>
          </div>
        </form>

        <div className="mt-4 p-3 bg-gray-50 rounded-lg text-xs text-gray-500">
          <strong>What happens:</strong>
          <ul className="mt-1 space-y-1">
            <li>• <span className="text-blue-600 font-medium">Federation:</span> Mutation → Router → HR Subgraph → DB write → immediate response</li>
            <li>• <span className="text-green-600 font-medium">Event-Driven:</span> POST → DB + Outbox → CDC → Kafka → Consumer → Projection DB</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default CreatePersonModal;
