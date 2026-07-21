// Ember Pickup Board — the customer-facing display (EMBER-SPEC §7.4). Two zones:
// Preparing (PREP) and Ready — please collect (READY, newest first). New ready
// calls pulse + chime; tapping a ready number collects it. Rebuilds on reload
// from active + ready, and stays live over the order stream.
import { useEffect, useRef, useState, type ReactNode } from 'react';
import {
  ApiError,
  api,
  usePreparing,
  useReady,
  useOrderStore,
  useOrderStream,
  type Order,
} from '@ember/shared';
import { NumberTile } from './components/NumberTile';
import { diffNewIds } from './lib/board';
import { playChime } from './lib/chime';

const PULSE_MS = 2500;

/** Seed source for the board: NEW+PREP (for Preparing) plus READY (for Ready). */
const seedBoard = (): Promise<Order[]> =>
  Promise.all([api.getActiveOrders(), api.getReadyOrders()]).then(([active, ready]) => [
    ...active,
    ...ready,
  ]);

export default function App() {
  const connection = useOrderStream({ syncStore: true, seedOnConnect: seedBoard });
  const preparing = usePreparing();
  const ready = useReady();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [soundOn, setSoundOn] = useState(true);
  const [pulsing, setPulsing] = useState<Set<number>>(new Set());

  const prevReadyIds = useRef<number[]>([]);

  // Initial load (loading / error); the stream keeps it live afterwards.
  useEffect(() => {
    seedBoard()
      .then(useOrderStore.getState().seed)
      .then(() => setLoading(false))
      .catch(() => {
        setLoading(false);
        setError('Could not load the board.');
      });
  }, []);

  // Detect newly-ready tickets → pulse (and chime, if enabled).
  useEffect(() => {
    const currentIds = ready.map((o) => o.id);
    const fresh = diffNewIds(prevReadyIds.current, currentIds);
    prevReadyIds.current = currentIds;
    if (fresh.length === 0) return;

    if (soundOn) playChime();
    setPulsing((prev) => new Set([...prev, ...fresh]));
    const timers = fresh.map((id) =>
      setTimeout(() => {
        setPulsing((prev) => {
          const next = new Set(prev);
          next.delete(id);
          return next;
        });
      }, PULSE_MS),
    );
    return () => timers.forEach(clearTimeout);
  }, [ready, soundOn]);

  const collect = async (order: Order) => {
    try {
      await api.collect(order.id);
    } catch (e) {
      // 409 = already collected elsewhere; the store reconciles from the event.
      if (!(e instanceof ApiError && e.status === 409)) {
        setError('Could not collect — please retry.');
      }
    }
  };

  return (
    <div className="flex h-screen flex-col bg-char text-bone font-body">
      <header className="flex items-center justify-between border-b border-steel px-6 py-3">
        <h1 className="font-display text-3xl tracking-wide bg-ember-gradient bg-clip-text text-transparent">
          Ember · Order Up
        </h1>
        <div className="flex items-center gap-4">
          <button
            onClick={() => setSoundOn((s) => !s)}
            aria-pressed={soundOn}
            aria-label={soundOn ? 'Mute chime' : 'Unmute chime'}
            className="min-h-9 min-w-9 rounded-full text-xl hover:bg-steel2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-bone"
          >
            {soundOn ? '🔔' : '🔕'}
          </button>
          <ConnectionPill status={connection} />
        </div>
      </header>

      {error && (
        <p role="alert" className="bg-late/15 px-6 py-2 text-late">
          {error}
        </p>
      )}

      {loading ? (
        <div className="grid flex-1 place-items-center">
          <span className="animate-pulse font-display text-3xl text-muted">Loading…</span>
        </div>
      ) : (
        <div className="grid flex-1 grid-cols-1 gap-px overflow-hidden bg-steel lg:grid-cols-[2fr_1fr]">
          {/* Ready — the prominent zone */}
          <Zone title="Ready — please collect">
            {ready.length === 0 ? (
              <Empty>No orders ready yet</Empty>
            ) : (
              <TileGrid>
                {ready.map((order) => (
                  <NumberTile
                    key={order.id}
                    ticketNumber={order.ticketNumber}
                    variant="ready"
                    isNew={pulsing.has(order.id)}
                    onCollect={() => collect(order)}
                  />
                ))}
              </TileGrid>
            )}
          </Zone>

          {/* Preparing — secondary zone */}
          <Zone title="Preparing" muted>
            {preparing.length === 0 ? (
              <Empty>Nothing cooking</Empty>
            ) : (
              <TileGrid>
                {preparing.map((order) => (
                  <NumberTile key={order.id} ticketNumber={order.ticketNumber} variant="preparing" />
                ))}
              </TileGrid>
            )}
          </Zone>
        </div>
      )}
    </div>
  );
}

function Zone({ title, muted, children }: { title: string; muted?: boolean; children: ReactNode }) {
  return (
    <section className="flex flex-col overflow-hidden bg-char p-6">
      <h2
        className={[
          'mb-4 font-display uppercase tracking-widest',
          muted ? 'text-xl text-muted' : 'text-2xl text-flame',
        ].join(' ')}
      >
        {title}
      </h2>
      <div className="flex-1 overflow-y-auto">{children}</div>
    </section>
  );
}

function TileGrid({ children }: { children: ReactNode }) {
  return (
    <div className="grid grid-cols-[repeat(auto-fill,minmax(9rem,1fr))] gap-4 content-start">
      {children}
    </div>
  );
}

function Empty({ children }: { children: ReactNode }) {
  return <div className="grid h-full place-items-center text-2xl text-muted">{children}</div>;
}

function ConnectionPill({ status }: { status: 'connecting' | 'connected' | 'disconnected' }) {
  const map = {
    connected: { color: '#2FCB86', label: 'Live' },
    connecting: { color: '#FFB020', label: 'Connecting…' },
    disconnected: { color: '#FF463B', label: 'Reconnecting…' },
  } as const;
  const { color, label } = map[status];
  return (
    <span className="flex items-center gap-2 text-sm text-muted">
      <span className="inline-block h-2.5 w-2.5 rounded-full" style={{ background: color }} />
      {label}
    </span>
  );
}
