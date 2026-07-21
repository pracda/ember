// One ticket on the kitchen rail. Shows the number, order type, a live age-colored
// timer, the lines with their modifiers/notes highlighted, and a single action
// that advances the ticket (Start cooking → Mark ready).
import { ageState, clock, elapsed, type Order } from '@ember/shared';
import { lineModifiers, nextAction } from '../lib/ticket';

interface Props {
  order: Order;
  now: number;
  onAdvance: (order: Order) => void;
  busy?: boolean;
}

export function TicketCard({ order, now, onAdvance, busy }: Props) {
  const seconds = elapsed(order.createdAt, now);
  const age = ageState(seconds);
  const action = nextAction(order.status);
  const late = age.key === 'late';

  return (
    <article
      className="flex flex-col rounded-2xl bg-graphite border-t-4 shadow-lg overflow-hidden"
      style={{ borderTopColor: age.color }}
    >
      <header className="flex items-center justify-between gap-2 px-4 pt-3">
        <span className="font-display text-5xl leading-none text-bone">#{order.ticketNumber}</span>
        <div className="flex flex-col items-end gap-1">
          <span
            className={[
              'rounded-full px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wider',
              order.type === 'TO_GO' ? 'bg-flame/20 text-flame' : 'bg-steel2 text-bone/80',
            ].join(' ')}
          >
            {order.type === 'TO_GO' ? 'To go' : 'Here'}
          </span>
          <span
            className={['font-mono text-lg tabular-nums', late ? 'motion-safe:animate-pulse' : ''].join(' ')}
            style={{ color: age.color }}
            aria-label={`${Math.floor(seconds / 60)} minutes on the rail`}
          >
            {clock(seconds)}
          </span>
        </div>
      </header>

      <ul className="flex-1 space-y-2 px-4 py-3">
        {order.lines.map((line) => {
          const mods = lineModifiers(line);
          return (
            <li key={line.id} className="text-bone">
              <div className="flex items-baseline gap-2">
                <span className="font-display text-xl text-flame">{line.quantity}×</span>
                <span className="font-semibold text-lg">{line.itemName}</span>
              </div>
              {mods.length > 0 && (
                <div className="mt-0.5 flex flex-wrap gap-1 pl-7">
                  {mods.map((m) => (
                    <span key={m} className="rounded bg-working/20 px-1.5 py-0.5 text-sm text-working">
                      {m}
                    </span>
                  ))}
                </div>
              )}
              {line.notes && (
                <p className="mt-0.5 pl-7 text-sm italic text-ember">“{line.notes}”</p>
              )}
            </li>
          );
        })}
      </ul>

      {action && (
        <button
          onClick={() => onAdvance(order)}
          disabled={busy}
          className={[
            'm-3 min-h-14 rounded-xl font-display text-2xl tracking-wide transition disabled:opacity-50',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-bone',
            order.status === 'NEW'
              ? 'bg-steel2 text-bone hover:bg-steel'
              : 'bg-ember-gradient text-[#1a0f08]',
          ].join(' ')}
        >
          {action.label}
        </button>
      )}
    </article>
  );
}
