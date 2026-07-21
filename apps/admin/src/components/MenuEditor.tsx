// Manager menu editor: list items, create, edit (incl. sizes/add-ons) and delete.
import { useEffect, useState } from 'react';
import { api, money, type MenuItem, type MenuItemInput } from '@ember/shared';
import { blankItem, toInput } from '../lib/menuForm';
import { ItemForm } from './ItemForm';

type Editing = { input: MenuItemInput; isNew: boolean } | null;

export function MenuEditor() {
  const [items, setItems] = useState<MenuItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<Editing>(null);

  const load = () => {
    setError(null);
    api.getMenu().then(setItems).catch(() => setError('Could not load the menu.'));
  };
  useEffect(load, []);

  const saveItem = async (input: MenuItemInput, isNew: boolean) => {
    if (isNew) await api.createMenuItem(input);
    else await api.updateMenuItem(input.id, input);
    setEditing(null);
    load();
  };

  const deleteItem = async (id: string) => {
    await api.deleteMenuItem(id);
    setEditing(null);
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
  if (!items) return <p className="p-6 text-muted">Loading menu…</p>;

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-2xl">Menu ({items.length})</h2>
        <button
          onClick={() => setEditing({ input: blankItem(), isNew: true })}
          className="min-h-11 rounded-xl bg-ember-gradient px-5 font-display text-lg text-[#1a0f08]"
        >
          + New item
        </button>
      </div>

      <div className="overflow-hidden rounded-2xl border border-steel">
        <table className="w-full text-left">
          <thead className="bg-graphite text-xs uppercase tracking-wider text-muted">
            <tr>
              <th className="px-4 py-2">ID</th>
              <th className="px-4 py-2">Name</th>
              <th className="px-4 py-2">Category</th>
              <th className="px-4 py-2 text-right">Base price</th>
              <th className="px-4 py-2">Meal</th>
              <th className="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id} className="border-t border-steel/60 hover:bg-graphite2">
                <td className="px-4 py-2 font-mono text-sm text-muted">{item.id}</td>
                <td className="px-4 py-2 font-semibold">{item.name}</td>
                <td className="px-4 py-2 text-bone/80">{item.category}</td>
                <td className="px-4 py-2 text-right font-mono">{money(item.basePrice)}</td>
                <td className="px-4 py-2">{item.mealAvailable ? '✓' : '—'}</td>
                <td className="px-4 py-2 text-right">
                  <button
                    onClick={() => setEditing({ input: toInput(item), isNew: false })}
                    className="rounded-lg px-3 py-1 text-flame hover:bg-steel2"
                  >
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {editing && (
        <ItemForm
          initial={editing.input}
          isNew={editing.isNew}
          onSave={(input) => saveItem(input, editing.isNew)}
          onDelete={editing.isNew ? undefined : () => deleteItem(editing.input.id)}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}
