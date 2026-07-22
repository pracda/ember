/**
 * Wire types — a hand-mirror of the backend DTOs (com.ember.web.dto.*). Kept in
 * lockstep with the Java records; if the API changes, change it here too
 * (EMBER-SPEC.md golden rule #5).
 *
 * Money arrives as JSON numbers (Java BigDecimal, scale 2). Timestamps arrive as
 * ISO-8601 strings (Java Instant, UTC) and render local in the UI.
 */

export type OrderStatus = 'NEW' | 'PREP' | 'READY' | 'DONE' | 'VOIDED' | 'REFUNDED';
export type OrderType = 'DINE_IN' | 'TO_GO';

export type OrderEventType =
  | 'ORDER_CREATED'
  | 'ORDER_STARTED'
  | 'ORDER_READY'
  | 'ORDER_RECALLED'
  | 'ORDER_COLLECTED'
  | 'ORDER_VOIDED'
  | 'ORDER_REFUNDED';

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
  available: boolean;
  tracksStock: boolean;
  stock: number;
  lowStockThreshold: number;
  /** Effective: manually 86'd, or tracking stock and none left. */
  soldOut: boolean;
  /** Tracking stock and at/below the alert threshold. */
  lowStock: boolean;
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
  /** Void/refund reason, when the order was reversed. */
  reason: string | null;
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

/* ----- auth + admin (Phase 6) ----- */

export type Role = 'CASHIER' | 'COOK' | 'MANAGER';

/** A signed-in staff session (LoginResponse + the raw token). */
export interface AuthSession {
  token: string;
  username: string;
  role: Role;
  displayName: string;
}

/** A pickable staff member on the station sign-in roster (no secrets). */
export interface RosterEntry {
  id: number;
  displayName: string;
  role: Role;
}

/** A staff member in the admin Employees tab. */
export interface Staff {
  id: number;
  username: string;
  displayName: string;
  role: Role;
  active: boolean;
  hasPin: boolean;
  hasPassword: boolean;
  createdAt: string;
}

/** Create-staff payload (at least one of pin/password required). */
export interface StaffInput {
  username: string;
  displayName: string;
  role: Role;
  pin?: string;
  password?: string;
}

/** Update-staff payload (username is immutable). */
export interface StaffUpdate {
  displayName: string;
  role: Role;
  active: boolean;
}

/** Manager payload to create or update a menu item (mirrors MenuItemRequest). */
export interface MenuItemInput {
  id: string;
  name: string;
  category: string;
  basePrice: number;
  mealAvailable: boolean;
  sizes: PriceModifier[];
  addons: PriceModifier[];
  available: boolean;
  tracksStock: boolean;
  stock: number;
  lowStockThreshold: number;
}

/** Manager day-summary (mirrors DaySummaryResponse). */
export interface DaySummary {
  date: string;
  orderCount: number;
  collectedCount: number;
  revenue: number;
  avgPrepSeconds: number | null;
}

/** Date-range analytics for the manager dashboard (mirrors AnalyticsResponse). */
export interface Analytics {
  from: string;
  to: string;
  orderCount: number;
  revenue: number;
  avgOrderValue: number;
  voidedCount: number;
  refundedCount: number;
  refundedAmount: number;
  salesByDay: { date: string; orderCount: number; revenue: number }[];
  topItems: { menuItemId: string; itemName: string; quantity: number; revenue: number }[];
  byCategory: { category: string; quantity: number; revenue: number }[];
  byOrderType: { type: OrderType; orderCount: number; revenue: number }[];
  byHour: { hour: number; orderCount: number }[];
  byStaff: { username: string | null; displayName: string; orderCount: number; revenue: number }[];
}
