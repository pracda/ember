// Small pure helpers for the kitchen rail. The order lifecycle itself lives on
// the server; these just decide what the card's single action button does next.
import type { Order, OrderLine } from '@ember/shared';

export interface TicketAction {
  label: string;
  /** Which REST call the button fires. */
  kind: 'advance';
}

/** NEW → "Start cooking", PREP → "Mark ready". Other states have no rail action. */
export function nextAction(status: Order['status']): TicketAction | null {
  switch (status) {
    case 'NEW':
      return { label: 'Start cooking', kind: 'advance' };
    case 'PREP':
      return { label: 'Mark ready', kind: 'advance' };
    default:
      return null;
  }
}

/** The modifiers a cook must see for a line: size, meal, add-ons (notes shown separately). */
export function lineModifiers(line: OrderLine): string[] {
  return [line.size, line.meal ? 'Meal' : null, ...line.addons].filter(Boolean) as string[];
}
