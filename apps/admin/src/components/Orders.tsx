// Manager Orders tab: browse recent orders and void (active) or refund (completed)
// with a reason. Voided/refunded orders drop out of net sales in the dashboard.
import { useEffect, useState } from 'react';
import { api, money, type Order, type OrderStatus } from '@ember/shared';

const STATUS_STYLE: Record<OrderStatus, string> = {
  NEW: 'text-working',
  PREP: 'text-working',
  READY: 'text-flame',
  DONE: 'text-fresh',
  VOIDED: 'text-muted line-through',
  REFUNDED: 'text-late',
};

const ACTIVE: OrderStatus[] = ['NEW', 'PREP', 'READY'];

type Action = { order: Order; kind: 'void' | 'refund' };

export function Orders() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [action, setAction] = useState<Action | null>(null);

  const load = () => {
    setError(null);
    api.getAllOrders().then(setOrders).catch(() => setError('Could not load orders.'));
  };
  useEffect(load, []);

  const confirm = async (reason: string) => {
    if (!action) return;
    if (action.kind === 'void') await api.voidOrder(action.order.id, reason);
    else await api.refundOrder(action.order.id, reason);
    setAction(null);
    load();
  };

  if (error) {
    return (
      <div className="p-6 text-center">
        <p className="text-late">{error}</p>
        <button onClick={load} className="mt-3 min-h-11 rounded-xl bg-steel2 px-5">Retry</button>
      </div>
    );
  }
  if (!orders) return <p className="p-6 text-muted">Loading orders…</p>;

  return (
    <div className="p-6">
      <h2 className="mb-4 font-display text-2xl">Orders ({orders.length})</h2>

      <div className="overflow-hidden rounded-2xl border border-steel">
        <table className="w-full text-left">
          <thead className="bg-graphite text-xs uppercase tracking-wider text-muted">
            <tr>
              <th className="px-4 py-2">Ticket</th>
              <th className="px-4 py-2">Type</th>
              <th className="px-4 py-2">Status</th>
              <th className="px-4 py-2 text-right">Total</th>
              <th className="px-4 py-2">Reason</th>
              <th className="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id} className="border-t border-steel/60 hover:bg-graphite2">
                <td className="px-4 py-2 font-display text-lg">#{o.ticketNumber}</td>
                <td className="px-4 py-2 text-bone/80">{o.type === 'TO_GO' ? 'To go' : 'Dine in'}</td>
                <td className={`px-4 py-2 font-semibold ${STATUS_STYLE[o.status]}`}>{o.status.toLowerCase()}</td>
                <td className="px-4 py-2 text-right font-mono">{money(o.total)}</td>
                <td className="px-4 py-2 text-sm italic text-muted">{o.reason ?? ''}</td>
                <td className="px-4 py-2 text-right">
                  {ACTIVE.includes(o.status) && (
                    <button onClick={() => setAction({ order: o, kind: 'void' })} className="rounded-lg px-3 py-1 text-muted hover:bg-steel2 hover:text-bone">
                      Void
                    </button>
                  )}
                  {o.status === 'DONE' && (
                    <button onClick={() => setAction({ order: o, kind: 'refund' })} className="rounded-lg px-3 py-1 text-late hover:bg-late/10">
                      Refund
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {action && (
        <ReasonModal
          title={`${action.kind === 'void' ? 'Void' : 'Refund'} order #${action.order.ticketNumber}`}
          confirmLabel={action.kind === 'void' ? 'Void order' : 'Refund order'}
          onConfirm={confirm}
          onClose={() => setAction(null)}
        />
      )}
    </div>
  );
}

function ReasonModal({
  title,
  confirmLabel,
  onConfirm,
  onClose,
}: {
  title: string;
  confirmLabel: string;
  onConfirm: (reason: string) => Promise<void>;
  onClose: () => void;
}) {
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const go = async () => {
    setBusy(true);
    setError(null);
    try {
      await onConfirm(reason.trim());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed.');
      setBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-40 grid place-items-center bg-black/60 p-4" onClick={onClose} role="presentation">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-sm rounded-3xl bg-graphite p-6 text-bone shadow-2xl"
      >
        <h2 className="font-display text-2xl">{title}</h2>
        <label className="mt-4 block text-sm">
          <span className="text-muted">Reason</span>
          <input
            value={reason}
            autoFocus
            onChange={(e) => setReason(e.target.value)}
            placeholder="e.g. wrong item, customer cancelled"
            className="input mt-1"
          />
        </label>
        {error && <p role="alert" className="mt-3 text-late">{error}</p>}
        <div className="mt-6 flex justify-end gap-2">
          <button onClick={onClose} disabled={busy} className="min-h-11 rounded-xl border border-steel2 px-4 text-bone/80">Cancel</button>
          <button onClick={go} disabled={busy} className="min-h-11 rounded-xl bg-ember-gradient px-5 font-display text-lg text-[#1a0f08] disabled:opacity-40">
            {busy ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
