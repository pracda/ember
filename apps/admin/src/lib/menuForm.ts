// Pure helpers for the menu editor form.
import type { MenuItem, MenuItemInput } from '@ember/shared';

export function blankItem(): MenuItemInput {
  return {
    id: '',
    name: '',
    category: '',
    basePrice: 0,
    mealAvailable: false,
    sizes: [],
    addons: [],
    available: true,
    tracksStock: false,
    stock: 0,
    lowStockThreshold: 0,
  };
}

export function toInput(item: MenuItem): MenuItemInput {
  return {
    id: item.id,
    name: item.name,
    category: item.category,
    basePrice: item.basePrice,
    mealAvailable: item.mealAvailable,
    sizes: item.sizes.map((m) => ({ ...m })),
    addons: item.addons.map((m) => ({ ...m })),
    available: item.available,
    tracksStock: item.tracksStock,
    stock: item.stock,
    lowStockThreshold: item.lowStockThreshold,
  };
}

/** Returns an error message, or null when the item is valid to submit. */
export function validate(item: MenuItemInput): string | null {
  if (!item.id.trim()) return 'ID is required.';
  if (!item.name.trim()) return 'Name is required.';
  if (!item.category.trim()) return 'Category is required.';
  if (!(item.basePrice >= 0)) return 'Base price must be 0 or more.';
  for (const m of [...item.sizes, ...item.addons]) {
    if (!m.label.trim()) return 'Every size/add-on needs a label.';
  }
  return null;
}
