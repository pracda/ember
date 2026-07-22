// The menu grid for the active category. Simple items quick-add; items with
// sizes/add-ons/meal open the Customize modal (decided by the caller).
import { money, type MenuItem } from '@ember/shared';
import { needsCustomize } from '../lib/cart';

interface Props {
  items: MenuItem[];
  onPick: (item: MenuItem) => void;
}

export function MenuGrid({ items, onPick }: Props) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3">
      {items.map((item) => (
        <MenuCard key={item.id} item={item} onPick={() => onPick(item)} />
      ))}
    </div>
  );
}

function MenuCard({ item, onPick }: { item: MenuItem; onPick: () => void }) {
  const customizable = needsCustomize(item);
  const soldOut = item.soldOut;
  const lowLeft = item.tracksStock && !soldOut && item.lowStock;
  return (
    <button
      onClick={onPick}
      disabled={soldOut}
      aria-disabled={soldOut}
      className={[
        'group relative flex flex-col justify-between text-left min-h-24 p-4 rounded-2xl border transition',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember',
        soldOut
          ? 'cursor-not-allowed border-bone2 bg-bone2/50 opacity-60'
          : 'bg-white border-bone2 shadow-sm hover:border-ember hover:shadow-md',
      ].join(' ')}
    >
      <span className={`font-semibold leading-tight ${soldOut ? 'text-muted line-through' : 'text-ink'}`}>
        {item.name}
      </span>
      <span className="mt-3 flex items-end justify-between">
        <span className="font-mono text-sm text-muted">
          {item.sizes.length > 0 ? 'from ' : ''}
          {money(item.basePrice)}
        </span>
        {soldOut ? (
          <span className="text-xs font-semibold uppercase tracking-wider text-late">Sold out</span>
        ) : (
          <span
            aria-hidden
            className="grid place-items-center w-9 h-9 rounded-full font-display text-xl bg-ember-gradient text-[#1a0f08] group-hover:scale-105 transition-transform"
          >
            +
          </span>
        )}
      </span>
      {!soldOut && lowLeft && (
        <span className="absolute top-2 left-2 rounded bg-working/20 px-1.5 text-[10px] font-semibold text-working">
          {item.stock} left
        </span>
      )}
      {!soldOut && customizable && (
        <span className="absolute top-2 right-2 text-[10px] uppercase tracking-wider text-muted">
          customize
        </span>
      )}
    </button>
  );
}
