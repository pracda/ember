// Phase 2 acceptance harness (throwaway — Phase 3 replaces it with the real POS).
// Proves the shared package end to end: fetch the menu, create an order via api.ts,
// and receive the matching ORDER_CREATED through useOrderStream.
import { useEffect, useRef, useState, type ReactNode } from 'react';
import {
  api,
  apiBase,
  money,
  useOrderStream,
  type MenuItem,
  type Order,
  type OrderEventType,
} from '@ember/shared';

interface Seen {
  type: OrderEventType;
  orderId: number;
  ticket: number;
}

export default function App() {
  const [menu, setMenu] = useState<MenuItem[] | null>(null);
  const [created, setCreated] = useState<Order | null>(null);
  const [events, setEvents] = useState<Seen[]>([]);
  const [error, setError] = useState<string | null>(null);
  const createdOnce = useRef(false);

  const status = useOrderStream({
    onEvent: (order, type) =>
      setEvents((prev) => [...prev, { type, orderId: order.id, ticket: order.ticketNumber }]),
  });

  // 1. Fetch the menu on mount.
  useEffect(() => {
    api.getMenu().then(setMenu).catch((e) => setError(String(e)));
  }, []);

  // 2. Once the socket is live and the menu is in, create one order.
  useEffect(() => {
    if (status !== 'connected' || !menu || createdOnce.current) return;
    createdOnce.current = true;
    api
      .createOrder({
        type: 'DINE_IN',
        lines: [
          { itemId: 'b1', quantity: 1, meal: true, addons: ['No onion', 'Extra cheese'], notes: 'well done' },
          { itemId: 'd1', quantity: 1, size: 'Large' },
        ],
      })
      .then(setCreated)
      .catch((e) => setError(String(e)));
  }, [status, menu]);

  // 3. Did the matching ORDER_CREATED come back over the stream?
  const matched =
    created != null &&
    events.some((e) => e.type === 'ORDER_CREATED' && e.orderId === created.id);

  return (
    <main className="min-h-screen bg-bone text-ink font-body p-8">
      <h1 className="font-display text-4xl bg-ember-gradient bg-clip-text text-transparent">
        Ember · shared round-trip
      </h1>
      <p className="font-mono text-sm text-muted mt-1">API {apiBase}</p>

      <div className="mt-6 grid gap-3 max-w-xl">
        <Row label="WebSocket" ok={status === 'connected'}>
          {status}
        </Row>
        <Row label="Menu (GET /api/menu)" ok={!!menu}>
          {menu ? `${menu.length} items` : 'loading…'}
        </Row>
        <Row label="Created order (POST /api/orders)" ok={!!created}>
          {created
            ? `ticket #${created.ticketNumber} · total ${money(created.total)}`
            : 'waiting…'}
        </Row>
        <Row label="ORDER_CREATED received (useOrderStream)" ok={matched}>
          {matched ? `matched order ${created?.id}` : `${events.length} event(s) so far`}
        </Row>
      </div>

      {matched && (
        <p data-testid="acceptance" className="mt-6 font-display text-2xl text-fresh">
          ✓ Phase 2 acceptance PASSED
        </p>
      )}
      {error && <pre className="mt-6 text-late whitespace-pre-wrap">{error}</pre>}
    </main>
  );
}

function Row({ label, ok, children }: { label: string; ok: boolean; children: ReactNode }) {
  return (
    <div className="flex items-center gap-3 bg-bone2 rounded-lg px-4 py-3">
      <span
        className="inline-block w-3 h-3 rounded-full"
        style={{ background: ok ? '#2FCB86' : '#A99A8C' }}
      />
      <span className="font-semibold w-72">{label}</span>
      <span className="font-mono text-sm">{children}</span>
    </div>
  );
}
