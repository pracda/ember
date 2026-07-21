// The order ticket: order-type toggle, line items with quantity steppers and
// their modifiers, live totals, and Send / Clear. Prices shown are the client
// preview; the server returns the authoritative total on send.
import { money, type OrderType } from '@ember/shared';
import { cartTotals, type CartLine } from '../lib/cart';

interface Props {
  lines: CartLine[];
  orderType: OrderType;
  sending: boolean;
  error: string | null;
  onOrderType: (type: OrderType) => void;
  onQuantity: (key: string, quantity: number) => void;
  onRemove: (key: string) => void;
  onSend: () => void;
  onClear: () => void;
}

export function TicketPanel({
  lines,
  orderType,
  sending,
  error,
  onOrderType,
  onQuantity,
  onRemove,
  onSend,
  onClear,
}: Props) {
  const totals = cartTotals(lines);
  const empty = lines.length === 0;

  return (
    <aside className="flex h-full flex-col bg-bone2/60">
      <header className="flex items-center justify-between p-4">
        <h2 className="font-display text-2xl text-ink">Ticket</h2>
        <OrderTypeToggle value={orderType} onChange={onOrderType} />
      </header>

      <div className="flex-1 overflow-y-auto px-4">
        {empty ? (
          <p className="mt-10 text-center text-muted">No items yet. Tap the menu to build an order.</p>
        ) : (
          <ul className="space-y-2">
            {lines.map((line) => (
              <TicketLine
                key={line.key}
                line={line}
                onQuantity={(q) => onQuantity(line.key, q)}
                onRemove={() => onRemove(line.key)}
              />
            ))}
          </ul>
        )}
      </div>

      <footer className="border-t border-bone2 bg-bone p-4">
        <dl className="space-y-1 font-mono text-sm">
          <Row label="Subtotal" value={money(totals.subtotal)} />
          <Row label="Tax" value={money(totals.tax)} />
          <Row label="Total" value={money(totals.total)} strong />
        </dl>

        {error && (
          <p role="alert" className="mt-3 rounded-lg bg-late/10 px-3 py-2 text-sm text-late">
            {error}
          </p>
        )}

        <div className="mt-3 flex gap-2">
          <button
            onClick={onClear}
            disabled={empty || sending}
            className="min-h-12 flex-1 rounded-2xl border border-bone2 font-semibold text-ink/70 disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          >
            Clear
          </button>
          <button
            onClick={onSend}
            disabled={empty || sending}
            className="min-h-12 flex-[2] rounded-2xl bg-ember-gradient font-display text-xl text-[#1a0f08] shadow-md disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ink"
          >
            {sending ? 'Sending…' : 'Send to kitchen'}
          </button>
        </div>
      </footer>
    </aside>
  );
}

function OrderTypeToggle({ value, onChange }: { value: OrderType; onChange: (t: OrderType) => void }) {
  const options: [OrderType, string][] = [
    ['DINE_IN', 'Here'],
    ['TO_GO', 'To go'],
  ];
  return (
    <div className="flex rounded-full bg-bone2 p-1">
      {options.map(([type, label]) => (
        <button
          key={type}
          aria-pressed={value === type}
          onClick={() => onChange(type)}
          className={[
            'min-h-9 px-4 rounded-full text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember',
            value === type ? 'bg-ink text-bone' : 'text-ink/60',
          ].join(' ')}
        >
          {label}
        </button>
      ))}
    </div>
  );
}

function TicketLine({
  line,
  onQuantity,
  onRemove,
}: {
  line: CartLine;
  onQuantity: (quantity: number) => void;
  onRemove: () => void;
}) {
  const mods = [
    line.size,
    line.meal ? 'Meal' : null,
    ...line.addons,
  ].filter(Boolean) as string[];

  return (
    <li className="rounded-xl bg-white p-3 shadow-sm">
      <div className="flex items-start justify-between gap-2">
        <span className="font-semibold text-ink">{line.name}</span>
        <span className="font-mono text-sm text-ink">{money(line.unitPrice * line.quantity)}</span>
      </div>

      {mods.length > 0 && <p className="mt-0.5 text-sm text-muted">{mods.join(' · ')}</p>}
      {line.notes && <p className="mt-0.5 text-sm italic text-ember">“{line.notes}”</p>}

      <div className="mt-2 flex items-center gap-2">
        <Stepper
          label="Decrease quantity"
          onClick={() => onQuantity(line.quantity - 1)}
        >
          −
        </Stepper>
        <span aria-live="polite" className="w-8 text-center font-mono">
          {line.quantity}
        </span>
        <Stepper label="Increase quantity" onClick={() => onQuantity(line.quantity + 1)}>
          +
        </Stepper>
        <button
          onClick={onRemove}
          className="ml-auto text-sm text-muted hover:text-late focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember rounded px-2 min-h-9"
        >
          Remove
        </button>
      </div>
    </li>
  );
}

function Stepper({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick: () => void;
  children: string;
}) {
  return (
    <button
      aria-label={label}
      onClick={onClick}
      className="grid h-9 w-9 place-items-center rounded-full bg-bone2 text-xl text-ink hover:bg-ink/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
    >
      {children}
    </button>
  );
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className={`flex justify-between ${strong ? 'text-lg font-bold text-ink' : 'text-ink/70'}`}>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
