/**
 * Cart + price-preview logic for the POS. The server owns the real price
 * (EMBER-SPEC.md golden rule #1) — everything here is display-only guidance,
 * computed from the menu's own deltas and the shared business constants. On send
 * we transmit only item ids + choices; the server returns the authoritative total.
 */
import { business, type CreateOrderRequest, type MenuItem, type OrderType } from '@ember/shared';

/** The customer's choices for one line. */
export interface LineChoices {
  size?: string;
  meal: boolean;
  addons: string[];
  notes?: string;
}

/** A line sitting in the ticket, with a preview unit price and quantity. */
export interface CartLine extends LineChoices {
  /** Dedupe key — identical configurations merge (spec §7.2). */
  key: string;
  itemId: string;
  name: string;
  unitPrice: number;
  quantity: number;
}

export interface Totals {
  subtotal: number;
  tax: number;
  total: number;
}

const MAX_QTY = 99;

/** Round to 2 dp, half-up for positive money (matches the server's HALF_UP). */
export function round2(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

/** Preview unit price = base + size delta + add-on deltas + meal upcharge. */
export function previewUnitPrice(item: MenuItem, choices: LineChoices): number {
  let price = item.basePrice;
  if (choices.size) {
    price += item.sizes.find((s) => s.label === choices.size)?.priceDelta ?? 0;
  }
  for (const label of choices.addons) {
    price += item.addons.find((a) => a.label === label)?.priceDelta ?? 0;
  }
  if (choices.meal && item.mealAvailable) {
    price += business.mealUpcharge;
  }
  return round2(price);
}

/** Stable identity for a configured line: same item + size + meal + add-ons + notes. */
export function lineKey(itemId: string, choices: LineChoices): string {
  return [
    itemId,
    choices.size ?? '',
    choices.meal ? 'meal' : '',
    [...choices.addons].sort().join(','),
    choices.notes?.trim() ?? '',
  ].join('|');
}

/** True when an item needs the Customize modal (otherwise it quick-adds). */
export function needsCustomize(item: MenuItem): boolean {
  return item.sizes.length > 0 || item.addons.length > 0 || item.mealAvailable;
}

/** Add a configured line, merging into an identical existing line by incrementing qty. */
export function addLine(
  lines: CartLine[],
  item: MenuItem,
  choices: LineChoices,
  quantity = 1,
): CartLine[] {
  const key = lineKey(item.id, choices);
  const existing = lines.find((l) => l.key === key);
  if (existing) {
    return lines.map((l) =>
      l.key === key ? { ...l, quantity: Math.min(MAX_QTY, l.quantity + quantity) } : l,
    );
  }
  const line: CartLine = {
    key,
    itemId: item.id,
    name: item.name,
    size: choices.size,
    meal: choices.meal,
    addons: choices.addons,
    notes: choices.notes?.trim() || undefined,
    unitPrice: previewUnitPrice(item, choices),
    quantity: Math.min(MAX_QTY, quantity),
  };
  return [...lines, line];
}

/** Set a line's quantity; removes the line when quantity hits 0. */
export function setQuantity(lines: CartLine[], key: string, quantity: number): CartLine[] {
  if (quantity <= 0) return lines.filter((l) => l.key !== key);
  return lines.map((l) => (l.key === key ? { ...l, quantity: Math.min(MAX_QTY, quantity) } : l));
}

export function removeLine(lines: CartLine[], key: string): CartLine[] {
  return lines.filter((l) => l.key !== key);
}

/** Preview subtotal / tax / total from the cart (server recomputes on send). */
export function cartTotals(lines: CartLine[]): Totals {
  const subtotal = round2(lines.reduce((sum, l) => sum + l.unitPrice * l.quantity, 0));
  const tax = round2(subtotal * business.taxRate);
  const total = round2(subtotal + tax);
  return { subtotal, tax, total };
}

/** Build the POST /api/orders payload — item ids + choices only, never prices. */
export function toCreateRequest(lines: CartLine[], type: OrderType): CreateOrderRequest {
  return {
    type,
    lines: lines.map((l) => ({
      itemId: l.itemId,
      quantity: l.quantity,
      size: l.size,
      meal: l.meal,
      addons: l.addons,
      notes: l.notes,
    })),
  };
}
