// Admin AI assistant: a chat grounded in the shop's live data (sales, staff, stock,
// menu) via server-side tools, plus a panel to store the LLM-gateway API key.
import { useEffect, useRef, useState } from 'react';
import {
  ApiError,
  api,
  type AssistantConfig,
  type AssistantConfigInput,
  type AssistantMessage,
} from '@ember/shared';

const SUGGESTIONS = [
  'How were sales over the last 7 days?',
  'Who sold the most today?',
  "What's running low on stock?",
  'What are my top-selling items this week?',
];

export function Assistant() {
  const [config, setConfig] = useState<AssistantConfig | null>(null);
  const [messages, setMessages] = useState<AssistantMessage[]>([]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showSettings, setShowSettings] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    api.getAssistantConfig().then(setConfig).catch(() => setConfig(null));
  }, []);
  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, busy]);

  const configured = config?.configured ?? false;

  const send = async (text: string) => {
    const q = text.trim();
    if (!q || busy) return;
    const next: AssistantMessage[] = [...messages, { role: 'user', content: q }];
    setMessages(next);
    setInput('');
    setBusy(true);
    setError(null);
    try {
      const { reply } = await api.assistantChat(next);
      setMessages([...next, { role: 'assistant', content: reply }]);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'The assistant is unavailable.');
      if (e instanceof ApiError && e.status === 503) setShowSettings(true);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto flex h-[calc(100vh-57px)] max-w-3xl flex-col p-6">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="font-display text-2xl">Assistant</h2>
        <button
          onClick={() => setShowSettings((s) => !s)}
          className="rounded-lg px-3 py-1 text-sm text-muted hover:bg-steel2"
        >
          ⚙ Gateway settings
        </button>
      </div>

      {config && (showSettings || !configured) && (
        <Settings
          config={config}
          onSaved={(c) => {
            setConfig(c);
            if (c.configured) setShowSettings(false);
          }}
        />
      )}

      <div
        ref={scrollRef}
        className="flex-1 space-y-3 overflow-y-auto rounded-2xl border border-steel bg-graphite/30 p-4"
      >
        {messages.length === 0 ? (
          <div className="grid h-full place-items-center text-center">
            <div>
              <p className="text-muted">
                Ask about your shop — sales, staff, stock, the menu. Answers are grounded in live data.
              </p>
              <div className="mt-4 flex flex-wrap justify-center gap-2">
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s}
                    onClick={() => send(s)}
                    disabled={!configured || busy}
                    className="rounded-full border border-steel2 px-3 py-1.5 text-sm text-bone/80 hover:bg-steel2 disabled:opacity-40"
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          </div>
        ) : (
          messages.map((m, i) => <Bubble key={i} message={m} />)
        )}
        {busy && <p className="text-sm text-muted">Thinking…</p>}
      </div>

      {error && <p role="alert" className="mt-2 text-sm text-late">{error}</p>}

      <form
        onSubmit={(e) => {
          e.preventDefault();
          send(input);
        }}
        className="mt-3 flex gap-2"
      >
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          disabled={!configured || busy}
          placeholder={configured ? 'Ask about sales, staff, stock…' : 'Add a gateway API key to start'}
          className="input flex-1"
        />
        <button
          type="submit"
          disabled={!configured || busy || !input.trim()}
          className="min-h-11 rounded-xl bg-ember-gradient px-5 font-display text-lg text-[#1a0f08] disabled:opacity-40"
        >
          Send
        </button>
      </form>
    </div>
  );
}

function Bubble({ message }: { message: AssistantMessage }) {
  const me = message.role === 'user';
  return (
    <div className={`flex ${me ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[80%] whitespace-pre-wrap rounded-2xl px-4 py-2 text-sm ${
          me ? 'bg-ember-gradient text-[#1a0f08]' : 'bg-steel2 text-bone'
        }`}
      >
        {message.content}
      </div>
    </div>
  );
}

function Settings({
  config,
  onSaved,
}: {
  config: AssistantConfig;
  onSaved: (c: AssistantConfig) => void;
}) {
  const [apiKey, setApiKey] = useState('');
  const [baseUrl, setBaseUrl] = useState(config.baseUrl);
  const [model, setModel] = useState(config.model);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const save = async () => {
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      const input: AssistantConfigInput = { baseUrl, model };
      if (apiKey.trim()) input.apiKey = apiKey.trim();
      onSaved(await api.setAssistantConfig(input));
      setApiKey('');
      setSaved(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not save.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="mb-3 rounded-2xl border border-steel bg-graphite/40 p-4">
      <h3 className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted">LLM gateway</h3>
      <p className="mb-3 text-sm text-muted">
        The assistant sends requests to your LLM gateway with the key below. It's stored on the server and never shown again.
        {config.configured && <span className="ml-1 text-fresh">Key set ({config.keyPreview}).</span>}
      </p>
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block text-sm sm:col-span-2">
          <span className="text-muted">Gateway API key</span>
          <input
            type="password"
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            placeholder={config.configured ? 'Leave blank to keep current key' : 'Paste the gateway key'}
            autoComplete="off"
            className="input mt-1"
          />
        </label>
        <label className="block text-sm">
          <span className="text-muted">Base URL</span>
          <input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} className="input mt-1" />
        </label>
        <label className="block text-sm">
          <span className="text-muted">Model</span>
          <input value={model} onChange={(e) => setModel(e.target.value)} className="input mt-1" />
        </label>
      </div>
      {error && <p role="alert" className="mt-2 text-sm text-late">{error}</p>}
      <div className="mt-3 flex items-center gap-3">
        <button
          onClick={save}
          disabled={busy}
          className="min-h-9 rounded-xl bg-ember-gradient px-4 font-display text-[#1a0f08] disabled:opacity-40"
        >
          {busy ? 'Saving…' : 'Save'}
        </button>
        {saved && <span className="text-sm text-fresh">Saved.</span>}
      </div>
    </section>
  );
}
