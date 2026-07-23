// Ember Kitchen Display — the rail of active tickets on an always-on dark monitor
// (EMBER-SPEC §7.3). Seeds from REST, stays live over the order stream, and heals
// on reconnect. Cooks advance tickets; a bumped ticket leaves the rail by event.
import { useEffect, useState, type ReactNode } from 'react';
import {
  ApiError,
  ClockButton,
  PinLogin,
  api,
  useActiveOrders,
  useAuth,
  useOrderStore,
  useOrderStream,
  type Order,
} from '@ember/shared';
import { TicketCard } from './components/TicketCard';
import { useNow } from './lib/useNow';

// Cooks (and managers) run the kitchen display; the backend enforces the role.
const KITCHEN_ROLES = ['COOK', 'MANAGER'];

export default function App() {
  const { session, loginWithPin, logout } = useAuth();
  if (!session) {
    return <PinLogin title="Ember Kitchen" roles={['COOK', 'MANAGER']} dark onSubmit={loginWithPin} />;
  }
  if (!KITCHEN_ROLES.includes(session.role)) {
    return (
      <main className="grid min-h-screen place-items-center bg-char text-bone font-body p-6">
        <div className="text-center">
          <p className="font-display text-3xl">This screen is for cooks</p>
          <p className="mt-1 text-muted">Signed in as {session.username} ({session.role.toLowerCase()}).</p>
          <button
            onClick={logout}
            className="mt-4 min-h-11 rounded-2xl bg-steel2 px-6 font-semibold text-bone focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          >
            Sign out
          </button>
        </div>
      </main>
    );
  }
  return <Kds />;
}

function Kds() {
  const { session, logout } = useAuth();
  const connection = useOrderStream({ syncStore: true });
  const orders = useActiveOrders();
  const now = useNow(1000);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<Set<number>>(new Set());
  const [lastReadied, setLastReadied] = useState<{ id: number; ticket: number } | null>(null);

  // Initial rail load (loading / error control); the stream keeps it live afterwards.
  const load = () => {
    setError(null);
    api
      .getActiveOrders()
      .then(useOrderStore.getState().seed)
      .then(() => setLoading(false))
      .catch(() => {
        setLoading(false);
        setError('Could not load the rail.');
      });
  };
  useEffect(load, []);

  const withBusy = (id: number, on: boolean) =>
    setBusy((prev) => {
      const next = new Set(prev);
      if (on) next.add(id);
      else next.delete(id);
      return next;
    });

  const advance = async (order: Order) => {
    if (busy.has(order.id)) return;
    withBusy(order.id, true);
    try {
      const updated = await api.advance(order.id);
      if (updated.status === 'READY') {
        setLastReadied({ id: updated.id, ticket: updated.ticketNumber });
      }
    } catch (e) {
      // 409 = someone else already advanced it; the store reconciles from the event.
      if (!(e instanceof ApiError && e.status === 409)) {
        setError('Action failed — please retry.');
      }
    } finally {
      withBusy(order.id, false);
    }
  };

  const recallLast = async () => {
    if (!lastReadied) return;
    try {
      await api.recall(lastReadied.id);
    } catch {
      /* already moved on; ignore */
    } finally {
      setLastReadied(null);
    }
  };

  return (
    <div className="flex h-screen flex-col bg-char text-bone font-body">
      <header className="flex items-center justify-between border-b border-steel px-6 py-3">
        <div className="flex items-baseline gap-3">
          <h1 className="font-display text-3xl tracking-wide text-bone">Ember Kitchen</h1>
          <span className="font-mono text-sm text-muted">{orders.length} active</span>
        </div>
        <div className="flex items-center gap-4">
          {lastReadied && (
            <button
              onClick={recallLast}
              className="min-h-9 rounded-full border border-steel2 px-4 text-sm text-bone/80 hover:bg-steel2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-bone"
            >
              Recall #{lastReadied.ticket}
            </button>
          )}
          <ConnectionPill status={connection} />
          <ClockButton dark />
          <span className="text-sm text-muted">{session?.username}</span>
          <button
            onClick={logout}
            className="min-h-9 rounded-full border border-steel2 px-3 text-sm text-bone/70 hover:bg-steel2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          >
            Sign out
          </button>
        </div>
      </header>

      <main className="flex-1 overflow-y-auto p-4">
        {error && (
          <p role="alert" className="mb-4 rounded-lg bg-late/15 px-4 py-2 text-late">
            {error}
          </p>
        )}

        {loading ? (
          <Centered>
            <span className="animate-pulse font-display text-3xl text-muted">Loading rail…</span>
          </Centered>
        ) : orders.length === 0 ? (
          <Centered>
            <div className="text-center">
              <p className="font-display text-4xl text-fresh">All caught up</p>
              <p className="mt-1 text-muted">No active tickets.</p>
            </div>
          </Centered>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
            {orders.map((order) => (
              <TicketCard
                key={order.id}
                order={order}
                now={now}
                busy={busy.has(order.id)}
                onAdvance={advance}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}

function Centered({ children }: { children: ReactNode }) {
  return <div className="grid h-full place-items-center">{children}</div>;
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
