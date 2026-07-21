// Create / edit a single menu item, including its sizes and add-ons.
import { useState, type ReactNode } from 'react';
import { money, type MenuItemInput, type PriceModifier } from '@ember/shared';
import { validate } from '../lib/menuForm';

interface Props {
  initial: MenuItemInput;
  isNew: boolean;
  onSave: (item: MenuItemInput) => Promise<void>;
  onDelete?: () => Promise<void>;
  onClose: () => void;
}

export function ItemForm({ initial, isNew, onSave, onDelete, onClose }: Props) {
  const [item, setItem] = useState<MenuItemInput>(initial);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const set = <K extends keyof MenuItemInput>(key: K, value: MenuItemInput[K]) =>
    setItem((prev) => ({ ...prev, [key]: value }));

  const save = async () => {
    const problem = validate(item);
    if (problem) {
      setError(problem);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSave(item);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Save failed.');
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!onDelete) return;
    setBusy(true);
    try {
      await onDelete();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Delete failed.');
      setBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-40 grid place-items-center bg-black/60 p-4" onClick={onClose} role="presentation">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={isNew ? 'New menu item' : `Edit ${initial.name}`}
        onClick={(e) => e.stopPropagation()}
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-3xl bg-graphite p-6 text-bone shadow-2xl"
      >
        <h2 className="font-display text-3xl">{isNew ? 'New item' : `Edit ${initial.name}`}</h2>

        <div className="mt-4 grid grid-cols-2 gap-3">
          <Field label="ID">
            <input
              value={item.id}
              disabled={!isNew}
              onChange={(e) => set('id', e.target.value)}
              className="input disabled:opacity-50"
            />
          </Field>
          <Field label="Category">
            <input value={item.category} onChange={(e) => set('category', e.target.value)} className="input" />
          </Field>
          <Field label="Name">
            <input value={item.name} onChange={(e) => set('name', e.target.value)} className="input" />
          </Field>
          <Field label="Base price">
            <input
              type="number"
              step="0.01"
              min="0"
              value={item.basePrice}
              onChange={(e) => set('basePrice', Number(e.target.value))}
              className="input"
            />
          </Field>
        </div>

        <label className="mt-3 flex items-center gap-2">
          <input
            type="checkbox"
            checked={item.mealAvailable}
            onChange={(e) => set('mealAvailable', e.target.checked)}
            className="h-4 w-4 accent-ember"
          />
          <span>Meal available</span>
        </label>

        <ModifierList
          title="Sizes"
          mods={item.sizes}
          onChange={(sizes) => set('sizes', sizes)}
        />
        <ModifierList
          title="Add-ons"
          mods={item.addons}
          onChange={(addons) => set('addons', addons)}
        />

        {error && <p role="alert" className="mt-3 text-late">{error}</p>}

        <div className="mt-6 flex items-center gap-2">
          {onDelete && (
            <button
              onClick={remove}
              disabled={busy}
              className="min-h-11 rounded-xl border border-late/50 px-4 text-late hover:bg-late/10 disabled:opacity-40"
            >
              Delete
            </button>
          )}
          <div className="ml-auto flex gap-2">
            <button onClick={onClose} disabled={busy} className="min-h-11 rounded-xl border border-steel2 px-4 text-bone/80">
              Cancel
            </button>
            <button
              onClick={save}
              disabled={busy}
              className="min-h-11 rounded-xl bg-ember-gradient px-5 font-display text-lg text-[#1a0f08] disabled:opacity-40"
            >
              {busy ? 'Saving…' : 'Save'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block text-sm">
      <span className="text-muted">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  );
}

function ModifierList({
  title,
  mods,
  onChange,
}: {
  title: string;
  mods: PriceModifier[];
  onChange: (mods: PriceModifier[]) => void;
}) {
  const update = (i: number, patch: Partial<PriceModifier>) =>
    onChange(mods.map((m, idx) => (idx === i ? { ...m, ...patch } : m)));
  return (
    <section className="mt-4">
      <div className="flex items-center justify-between">
        <h3 className="text-xs font-semibold uppercase tracking-wider text-muted">{title}</h3>
        <button
          onClick={() => onChange([...mods, { label: '', priceDelta: 0 }])}
          className="text-sm text-flame hover:underline"
        >
          + Add
        </button>
      </div>
      <div className="mt-2 space-y-2">
        {mods.map((m, i) => (
          <div key={i} className="flex items-center gap-2">
            <input
              value={m.label}
              placeholder="Label"
              onChange={(e) => update(i, { label: e.target.value })}
              className="input flex-1"
            />
            <input
              type="number"
              step="0.01"
              value={m.priceDelta}
              onChange={(e) => update(i, { priceDelta: Number(e.target.value) })}
              className="input w-24"
              aria-label={`${title} price delta`}
            />
            <span className="w-14 text-right text-xs text-muted">{money(m.priceDelta)}</span>
            <button
              onClick={() => onChange(mods.filter((_, idx) => idx !== i))}
              aria-label="Remove option"
              className="min-h-9 min-w-9 rounded-full text-muted hover:bg-steel2"
            >
              ×
            </button>
          </div>
        ))}
        {mods.length === 0 && <p className="text-sm text-muted">None</p>}
      </div>
    </section>
  );
}
