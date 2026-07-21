/**
 * Wire types — a hand-mirror of the backend DTOs (com.ember.web.dto.*). Kept in
 * lockstep with the Java records; if the API changes, change it here too
 * (EMBER-SPEC.md golden rule #5).
 *
 * Money arrives as JSON numbers (Java BigDecimal, scale 2). Timestamps arrive as
 * ISO-8601 strings (Java Instant, UTC) and render local in the UI.
 */

export type OrderStatus = 'NEW' | 'PREP' | 'READY' | 'DONE';
export type OrderType = 'DINE_IN' | 'TO_GO';

export type OrderEventType =
  | 'ORDER_CREATED'
  | 'ORDER_STARTED'
  | 'ORDER_READY'
  | 'ORDER_RECALLED'
  | 'ORDER_COLLECTED';

/** A selectable option on a menu item — a size or an add-on — with its price delta. */
export interface PriceModifier {
  label: string;
  priceDelta: number;
}

/** MenuItemResponse. */
export interface MenuItem {
  id: string;
  name: string;
  category: string;
  basePrice: number;
  mealAvailable: boolean;
  sizes: PriceModifier[];
  addons: PriceModifier[];
}

/** OrderResponse.OrderLineResponse — a frozen snapshot of one ordered line. */
export interface OrderLine {
  id: number;
  menuItemId: string;
  itemName: string;
  quantity: number;
  size: string | null;
  meal: boolean;
  addons: string[];
  notes: string | null;
  unitPrice: number;
  lineTotal: number;
}

/** OrderResponse. */
export interface Order {
  id: number;
  ticketNumber: number;
  type: OrderType;
  status: OrderStatus;
  lines: OrderLine[];
  subtotal: number;
  tax: number;
  total: number;
  createdAt: string;
  startedAt: string | null;
  readyAt: string | null;
  collectedAt: string | null;
}

/** Envelope broadcast on /topic/orders. */
export interface OrderEvent {
  type: OrderEventType;
  order: Order;
}

/* ----- request payloads (POS → server) ----- */

export interface OrderLineRequest {
  itemId: string;
  quantity: number;
  size?: string;
  meal?: boolean;
  addons?: string[];
  notes?: string;
}

export interface CreateOrderRequest {
  type: OrderType;
  lines: OrderLineRequest[];
}
