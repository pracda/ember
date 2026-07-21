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
  return (
    <button
      onClick={onPick}
      className={[
        'group relative flex flex-col justify-between text-left min-h-24 p-4 rounded-2xl',
        'bg-white border border-bone2 shadow-sm hover:border-ember hover:shadow-md transition',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember',
      ].join(' ')}
    >
      <span className="font-semibold leading-tight text-ink">{item.name}</span>
      <span className="mt-3 flex items-end justify-between">
        <span className="font-mono text-sm text-muted">
          {item.sizes.length > 0 ? 'from ' : ''}
          {money(item.basePrice)}
        </span>
        <span
          aria-hidden
          className={[
            'grid place-items-center w-9 h-9 rounded-full font-display text-xl',
            'bg-ember-gradient text-[#1a0f08] group-hover:scale-105 transition-transform',
          ].join(' ')}
        >
          +
        </span>
      </span>
      {customizable && (
        <span className="absolute top-2 right-2 text-[10px] uppercase tracking-wider text-muted">
          customize
        </span>
      )}
    </button>
  );
}
