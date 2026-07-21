// Customize modal: pick a size, make it a meal, choose add-ons, add a kitchen
// note — with a live price preview. "Add to ticket" hands the choices back up.
import { useEffect, useState, type ReactNode } from 'react';
import { business, money, type MenuItem } from '@ember/shared';
import { previewUnitPrice, type LineChoices } from '../lib/cart';

interface Props {
  item: MenuItem;
  onAdd: (choices: LineChoices) => void;
  onClose: () => void;
}

export function CustomizeModal({ item, onAdd, onClose }: Props) {
  // Default to the first size when the item is sized (matches a real till).
  const [size, setSize] = useState<string | undefined>(item.sizes[0]?.label);
  const [meal, setMeal] = useState(false);
  const [addons, setAddons] = useState<string[]>([]);
  const [notes, setNotes] = useState('');

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  const toggleAddon = (label: string) =>
    setAddons((prev) => (prev.includes(label) ? prev.filter((a) => a !== label) : [...prev, label]));

  const choices: LineChoices = { size, meal, addons, notes: notes.trim() || undefined };
  const unit = previewUnitPrice(item, choices);

  return (
    <div
      className="fixed inset-0 z-40 grid place-items-center bg-ink/50 p-4"
      onClick={onClose}
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={`Customize ${item.name}`}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md max-h-[90vh] overflow-y-auto rounded-3xl bg-bone p-6 shadow-2xl motion-safe:animate-[fade_120ms_ease-out]"
      >
        <div className="flex items-start justify-between gap-4">
          <h2 className="font-display text-3xl text-ink">{item.name}</h2>
          <button
            onClick={onClose}
            aria-label="Close"
            className="min-h-11 min-w-11 rounded-full text-2xl text-muted hover:bg-ink/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          >
            ×
          </button>
        </div>

        {item.sizes.length > 0 && (
          <Section title="Size">
            <div className="flex flex-wrap gap-2">
              {item.sizes.map((s) => (
                <Chip key={s.label} selected={size === s.label} onClick={() => setSize(s.label)}>
                  {s.label}
                  {s.priceDelta > 0 && <span className="ml-1 text-xs text-muted">+{money(s.priceDelta)}</span>}
                </Chip>
              ))}
            </div>
          </Section>
        )}

        {item.mealAvailable && (
          <Section title="Meal">
            <label className="flex items-center gap-3 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={meal}
                onChange={(e) => setMeal(e.target.checked)}
                className="w-5 h-5 accent-ember"
              />
              <span>Make it a meal</span>
              <span className="ml-auto font-mono text-sm text-muted">+{money(business.mealUpcharge)}</span>
            </label>
          </Section>
        )}

        {item.addons.length > 0 && (
          <Section title="Add-ons">
            <div className="grid gap-1.5">
              {item.addons.map((a) => (
                <label
                  key={a.label}
                  className="flex items-center gap-3 min-h-11 px-2 rounded-lg cursor-pointer hover:bg-bone2"
                >
                  <input
                    type="checkbox"
                    checked={addons.includes(a.label)}
                    onChange={() => toggleAddon(a.label)}
                    className="w-5 h-5 accent-ember"
                  />
                  <span>{a.label}</span>
                  <span className="ml-auto font-mono text-sm text-muted">
                    {a.priceDelta > 0 ? `+${money(a.priceDelta)}` : '—'}
                  </span>
                </label>
              ))}
            </div>
          </Section>
        )}

        <Section title="Kitchen note">
          <input
            type="text"
            value={notes}
            maxLength={255}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="e.g. well done, no salt"
            className="w-full rounded-lg border border-bone2 bg-white px-3 py-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          />
        </Section>

        <button
          onClick={() => onAdd(choices)}
          className="mt-6 flex w-full items-center justify-between rounded-2xl bg-ember-gradient px-6 py-4 font-display text-xl text-[#1a0f08] shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ink"
        >
          <span>Add to ticket</span>
          <span className="font-mono">{money(unit)}</span>
        </button>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-5">
      <h3 className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted">{title}</h3>
      {children}
    </section>
  );
}

function Chip({
  selected,
  onClick,
  children,
}: {
  selected: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      aria-pressed={selected}
      className={[
        'min-h-11 px-4 rounded-full border transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember',
        selected ? 'border-ember bg-ember/10 text-ink' : 'border-bone2 bg-white text-ink/80 hover:border-ember',
      ].join(' ')}
    >
      {children}
    </button>
  );
}
