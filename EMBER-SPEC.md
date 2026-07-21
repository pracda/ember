# Ember — Quick-Service Management System

**Build specification for Claude Code.** Work through this document phase by phase
(Section 11). Each phase has concrete tasks and acceptance criteria; treat the
acceptance criteria as done-definitions and verify them before moving on.

There are two existing artifacts this spec is built around:

- **`ember-backend/`** — a Spring Boot 3 / Java 21 scaffold (orders, menu, pricing,
  ticketing, STOMP broadcasting). Real, runnable, but unverified — Phase 1 hardens it.
- **A single-file React prototype** (`ember-fastfood.jsx`) that renders all three
  station UIs against shared in-memory state. It is the visual + interaction
  reference for the three apps you will build. Do **not** ship it as-is; split it
  into three deployable apps wired to the backend.

---

## 1. Overview

Ember runs the floor of a small fast-food outlet across three screens that share
one live order stream:

| Surface | Device | Primary user | Job |
|---|---|---|---|
| **POS** | counter tablet / touchscreen | cashier | Build and send orders; take order type |
| **Kitchen Display (KDS)** | wall/counter monitor | line cooks | See the rail, work tickets, mark ready |
| **Pickup Board** | customer-facing screen | customers | Watch ticket numbers move to "Ready" |

**Core principle:** the server owns truth. Pricing, ticket numbers, and order
state all live server-side; clients send intent (item ids + choices, "advance",
"collect") and react to broadcast events. The POS never computes a price it sends.

**Success criteria (v1):**
1. Cashier builds an order on the POS and sends it; it appears on the KDS within ~1s with no refresh.
2. Cook taps Start then Ready; the ticket ages with a green→amber→red timer and leaves the rail on Ready.
3. The pickup board shows the number under "Ready" the moment it's bumped; tapping it clears it.
4. All three screens stay consistent through a page reload and a dropped/restored WebSocket.
5. Menu, tax, and meal upcharge are configuration/data, not hardcoded in the UI.

---

## 2. Architecture

```
┌─────────┐    ┌─────────┐    ┌─────────┐
│  POS    │    │ Kitchen │    │  Board  │      React + TypeScript + Vite (3 apps)
│  app    │    │  app    │    │  app    │      share packages/shared
└────┬────┘    └────┬────┘    └────┬────┘
     │ REST + STOMP over WebSocket │
     └──────────────┬──────────────┘
              ┌──────▼──────┐
              │ Spring Boot │   REST /api/**  +  STOMP /ws → /topic/orders
              │   backend   │
              └──────┬──────┘
              ┌──────▼──────┐
              │ PostgreSQL  │   (H2 in dev)
              └─────────────┘
```

- **REST** for commands and initial load (`GET /api/menu`, `GET /api/orders`,
  `POST /api/orders`, transition endpoints).
- **STOMP/WebSocket** for the live stream: the backend broadcasts an event on every
  order change; each app subscribes and updates its local store.
- Apps are **static bundles** (S3 + CloudFront). Backend is a jar on EC2/container.
  Same shape as `retail-shop-management`.

### Tech stack (pin these versions)

| Layer | Choice |
|---|---|
| Backend | Java 21, Spring Boot 3.4.x, Spring Web / Data JPA / WebSocket / Validation |
| DB | PostgreSQL 16 (prod, RDS), H2 (dev); Flyway for migrations |
| Frontend | React 18, TypeScript 5, Vite 5, Tailwind CSS 3 |
| State | Zustand (order store keyed by id) |
| Realtime client | `@stomp/stompjs` + `sockjs-client` |
| Auth | JWT (reuse the retail project's filter/roles) |
| Monorepo | pnpm workspaces (or npm workspaces) |
| Tests | JUnit 5 + Spring Boot Test; Vitest + Testing Library; Playwright (e2e) |

---

## 3. Repository layout (monorepo)

```
ember/
├── CLAUDE.md                 # conventions & context (see companion file)
├── EMBER-SPEC.md             # this document
├── backend/                  # the existing ember-backend project, moved here
│   └── src/main/java/com/ember/...
├── apps/
│   ├── pos/                  # Vite React app — cashier
│   ├── kitchen/              # Vite React app — KDS
│   └── board/                # Vite React app — pickup board
├── packages/
│   └── shared/               # TS types, api client, ws hook, design tokens, tailwind preset
├── package.json              # workspaces root
└── pnpm-workspace.yaml
```

`packages/shared` is the contract layer between the three apps. It contains:
`types.ts` (mirrors backend DTOs), `api.ts` (typed fetch client), `useOrderStream.ts`
(STOMP hook), `tokens.ts` + `tailwind-preset.cjs` (design system), and `format.ts`
(money/time helpers).

---

## 4. Domain model

Already implemented in the scaffold; this is the contract to preserve.

### Enums
- `OrderStatus`: `NEW → PREP → READY → DONE` (+ `READY → PREP` recall). `NEW` and `PREP` are "active".
- `OrderType`: `DINE_IN`, `TO_GO`.

### MenuItem
| Field | Type | Notes |
|---|---|---|
| `id` | string | short code, e.g. `b1`, `d3` (stable, human-readable) |
| `name` | string | |
| `category` | string | Burgers / Chicken / Sides / Drinks / Sweets |
| `basePrice` | decimal(8,2) | |
| `sizes` | list of `{label, priceDelta}` | empty if no size |
| `addons` | list of `{label, priceDelta}` | e.g. "No onion" (+0.00) |
| `mealAvailable` | boolean | if true, meal upcharge can apply |

### Order
| Field | Type | Notes |
|---|---|---|
| `id` | long | |
| `ticketNumber` | int | customer-facing, server-assigned |
| `type` | OrderType | |
| `status` | OrderStatus | |
| `lines` | OrderLine[] | |
| `subtotal`,`tax`,`total` | decimal(10,2) | server-computed snapshot |
| `createdAt`,`startedAt`,`readyAt`,`collectedAt` | Instant | lifecycle timestamps |

### OrderLine (snapshot — never rewritten by later menu changes)
| Field | Type | Notes |
|---|---|---|
| `id` | long | |
| `menuItemId` | string | reference for reporting |
| `itemName` | string | snapshot at order time |
| `quantity` | int | 1–99 |
| `size` | string? | chosen size label |
| `meal` | boolean | |
| `addons` | string[] | chosen add-on labels |
| `notes` | string? | free-text kitchen note, ≤255 |
| `unitPrice` | decimal(10,2) | server-computed, frozen |

**Money:** `BigDecimal` end-to-end, scale 2, `HALF_UP`. Never `double`, never
client-supplied prices. **Time:** store `Instant` (UTC); clients render local.

---

## 5. REST API contract

Base path `/api`. All bodies JSON. Errors use RFC 7807 `ProblemDetail`
(`{type,title,status,detail}`).

### `GET /api/menu`
→ `200` `MenuItem[]`
```json
[{ "id":"b1","name":"Ember Smash","category":"Burgers","basePrice":6.50,
   "mealAvailable":true,
   "sizes":[],
   "addons":[{"label":"Extra cheese","priceDelta":0.90},{"label":"Bacon","priceDelta":1.50},
             {"label":"No onion","priceDelta":0.00}] }]
```

### `POST /api/orders`  (POS → create ticket)
Request:
```json
{ "type":"DINE_IN",
  "lines":[
    {"itemId":"b1","quantity":1,"meal":true,"addons":["No onion","Extra cheese"],"notes":"well done"},
    {"itemId":"d1","quantity":1,"size":"Large"}
  ] }
```
→ `201` `Order` (with `ticketNumber`, per-line `unitPrice`, `subtotal`/`tax`/`total`).
Errors: `404` unknown `itemId`; `400` size/add-on not offered by the item, or validation failure.

### `GET /api/orders?status=active|ready|all`
- `active` (default): `NEW`+`PREP`, oldest first — the kitchen rail.
- `ready`: `READY`, newest `readyAt` first — the pickup board's ready calls.
- `all`: history, newest first.
→ `200` `Order[]`

### `GET /api/orders/{id}` → `200` `Order` | `404`

### Transitions (all → `200` updated `Order`, or `409` if illegal from current state)
- `POST /api/orders/{id}/advance` — `NEW→PREP` then `PREP→READY`
- `POST /api/orders/{id}/recall` — `READY→PREP`
- `POST /api/orders/{id}/collect` — `READY→DONE`

Every transition and creation also **broadcasts** (Section 6). REST responses and
broadcasts carry the same `Order` shape.

---

## 6. Real-time contract (STOMP over WebSocket)

- **Endpoint:** `/ws` (SockJS enabled).
- **Subscribe:** `/topic/orders`.
- **Message:**
```json
{ "type":"ORDER_CREATED", "order": { ...Order } }
```
- **Event types:** `ORDER_CREATED`, `ORDER_STARTED`, `ORDER_READY`, `ORDER_RECALLED`, `ORDER_COLLECTED`.

Client rules (all three apps):
1. On mount, `GET /api/orders?status=active` and seed the store (upsert by `order.id`).
2. Open the socket; on each event, **upsert by `order.id`**.
3. Derive each view by filtering the store on `status` — do not keep separate lists.
4. On disconnect, show a subtle "reconnecting" state, auto-retry with backoff, and
   **re-fetch** active orders on reconnect to heal any missed events.

> v1 uses one topic; all screens filter client-side (mirrors the prototype's shared
> state). If load grows, split into `/topic/kitchen` and `/topic/board` server-side.

---

## 7. Frontend apps

All three are Vite + React + TS, styled with the shared Tailwind preset and tokens
(Section 8), consuming `packages/shared`. Build each app to be reload-safe and
WebSocket-driven.

### 7.1 Shared package (`packages/shared`)
- `types.ts` — `Order`, `OrderLine`, `MenuItem`, `PriceModifier`, `OrderEvent`, enums; must match backend DTOs exactly.
- `api.ts` — typed client: `getMenu()`, `getActiveOrders()`, `createOrder(req)`, `advance(id)`, `recall(id)`, `collect(id)`. Base URL from env (`VITE_API_BASE`).
- `useOrderStream.ts` — hook that opens SockJS+STOMP, subscribes `/topic/orders`, calls an `onEvent(order, type)` upsert, handles reconnect/backoff + re-fetch.
- `useOrderStore.ts` — Zustand store: `Record<id, Order>`, selectors `activeOrders()`, `preparing()`, `ready()`, `upsert(order)`, `seed(orders)`.
- `format.ts` — `money(n)`, `elapsed(createdAt, now)`, `ageState(secs) → {key,color,label}`.
- `tokens.ts`, `tailwind-preset.cjs` — Section 8.

### 7.2 POS app (`apps/pos`)
Target: landscape touchscreen. Layout: menu grid (left) + order ticket panel (right).

- **Category tabs** — Burgers / Chicken / Sides / Drinks / Sweets.
- **Menu grid** — item cards (name, price, "+"). Items with sizes/addons/meal open a **Customize** modal; simple items quick-add.
- **Customize modal** — single-select size, "make it a meal" toggle, multi-select add-ons, optional kitchen note; live price preview; "Add to ticket".
- **Order ticket panel** — line items with qty steppers, per-line modifiers shown, order-type toggle (**Here / To go** → `DINE_IN`/`TO_GO`), live subtotal/tax/total, **Send to kitchen** (`POST /api/orders`), and clear-ticket.
- On successful send: clear the cart, show the assigned ticket number briefly.
- **Edge cases:** empty cart disables send; API error shows an inline, actionable message and keeps the cart; identical lines merge (same item + size + meal + addons + notes → increment qty).

Reference: the `POS`, `Customize` components in the prototype (mirror behavior/UX).

### 7.3 Kitchen app (`apps/kitchen`)
Target: always-on wall monitor, dark. No POS chrome.

- **Rail** — active orders (`NEW`+`PREP`) as ticket cards, **oldest first**, responsive grid.
- **Ticket card** — ticket number, order-type badge, live elapsed timer, item list with qty and **highlighted modifiers/notes** (the kitchen must see "No onion"), and an action button: **Start cooking** (`NEW→advance`) then **Mark ready** (`PREP→advance`).
- **Age color** — a 1s tick drives per-card color: `<5min` fresh green, `5–10min` amber, `>10min` red (pulsing). Card top-border + timer use the age color.
- On **Ready**, the card leaves the rail (event-driven).
- Optional: a small "recall last" affordance (`recall`) for a mis-bump.
- **Edge cases:** empty rail shows an "all caught up" state; a `409` (already advanced elsewhere) is swallowed and reconciled from the store.

### 7.4 Pickup board (`apps/board`)
Target: customer-facing display, dark, readable across the room.

- **Two zones:** **Preparing** (numbers for `PREP`) and **Ready — please collect** (big ember numbers for `READY`).
- New `READY` numbers animate/pulse briefly (respect `prefers-reduced-motion`); optional chime on new ready.
- Tapping a **Ready** number marks it collected (`collect` → `DONE`) and clears it.
- Sort ready by `readyAt` desc so the newest call is prominent.
- **Edge cases:** empty states in both zones; must survive reload and rebuild purely from `GET /api/orders?status=active` + stream.

---

## 8. Design system (from the prototype — keep it consistent)

Put these in `packages/shared/tokens.ts` and a Tailwind preset; all three apps extend it.

### Palette
| Token | Hex | Use |
|---|---|---|
| `char` | `#14110F` | KDS/board background |
| `graphite` | `#211C19` | raised cards (dark) |
| `graphite2` | `#2A2320` | nested surfaces |
| `steel` | `#3A322D` | borders/dividers (dark) |
| `steel2` | `#4A403A` | secondary buttons (dark) |
| `bone` | `#F6F1E8` | POS background / light text |
| `bone2` | `#ECE4D6` | POS borders |
| `ink` | `#1B1512` | POS text |
| `ember` | `#FF5722` | primary accent (start of gradient) |
| `flame` | `#FF8A3D` | accent (end of gradient) |
| `fresh` | `#2FCB86` | order age <5 min |
| `working` | `#FFB020` | order age 5–10 min |
| `late` | `#FF463B` | order age >10 min |
| `muted` | `#A99A8C` | secondary text |

Primary action = linear-gradient(150deg, `ember`, `flame`) with near-black text `#1a0f08`.

### Type
- **Barlow Condensed** (600/700) — ticket numbers, timers, board numerals, section headers.
- **Barlow** (400/500/600) — all UI/body.
- **Space Mono** — order codes / timestamps (receipt texture).

### Business constants (config, not code)
- Tax rate `0.085`; meal upcharge `3.50`.
- Age thresholds: fresh `<300s`, working `<600s`, late `≥600s`.

---

## 9. Cross-cutting

- **Auth:** JWT bearer (`POST /api/auth/login` → token; demo users cashier/cook/manager).
  Roles: `CASHIER`, `COOK`, `MANAGER`. `POST /api/orders` → CASHIER/MANAGER;
  `advance`/`recall` → COOK/MANAGER; menu writes + `/api/reports/**` → MANAGER.
  `GET /api/menu` and `GET /api/orders/**` stay public. **Decision (build):** the
  pickup board is left customer-facing, so `POST /api/orders/{id}/collect` is public
  (not staff-gated). The manager back-office lives in a separate `apps/admin`.
- **Config:** `ember.tax-rate`, `ember.meal-upcharge`, `ember.allowed-origins` (env in prod).
- **Validation:** Bean Validation on requests; `GlobalExceptionHandler` → ProblemDetail.
- **Ticket numbers:** monotonic via `TicketSequence` seeded from DB max. Add optional
  daily reset (scheduled at local midnight, or date-keyed sequence) — note the outlet timezone.
- **Broadcast timing:** move broadcasts to `@TransactionalEventListener(AFTER_COMMIT)`
  so clients never see an event for an uncommitted change.
- **Idempotency (stretch):** accept a client `Idempotency-Key` on `POST /api/orders`
  to make retries safe on flaky counter Wi-Fi.

---

## 10. Non-functional requirements

- **Latency:** POS→KDS visible in <1s on LAN.
- **Resilience:** each app reconciles from REST on load and on reconnect; no lost tickets.
- **POS offline (stretch):** queue unsent orders locally and flush on reconnect.
- **Accessibility:** visible keyboard focus, adequate contrast, `prefers-reduced-motion` honored; POS tap targets ≥44px.
- **Displays:** KDS/board assume always-on landscape monitors; avoid burn-in-heavy static bright fills; no auto-logout on these screens.
- **Browsers:** current Chrome/Edge (kiosk); mobile Safari for POS tablets.

---

## 11. Build plan

Each phase: tasks, then **Acceptance**. Verify acceptance before continuing.

### Phase 0 — Monorepo & tooling
- Create the workspace layout (Section 3); move `ember-backend/` to `backend/`.
- Set up pnpm workspaces, shared TS config, ESLint + Prettier, Tailwind.
- Add root scripts: `dev:backend`, `dev:pos`, `dev:kitchen`, `dev:board`.
- **Acceptance:** `pnpm install` works; each app scaffolds and serves an empty page; backend runs.

### Phase 1 — Backend hardening
- Run the scaffold; confirm `mvn spring-boot:run` boots and seeds the menu.
- Add tests: `PricingService` (deltas, invalid size/addon → 400), `Order` state machine
  (each legal + illegal transition), `@DataJpaTest` repo queries, `@WebMvcTest` controllers,
  one `@SpringBootTest` covering create→advance→ready→collect and a broadcast assertion.
- Add Flyway migrations mirroring the entities; set prod `ddl-auto: validate`.
- Move broadcasts to `AFTER_COMMIT`.
- **Acceptance:** all tests green; the curl example in the backend README returns a priced order; an illegal transition returns `409`.

### Phase 2 — Shared package
- Implement `types.ts` (match DTOs), `api.ts`, `useOrderStore.ts`, `useOrderStream.ts`, `format.ts`, tokens + Tailwind preset.
- **Acceptance:** a throwaway test view can fetch the menu, create an order via `api.ts`, and receive the matching `ORDER_CREATED` event through `useOrderStream`.

### Phase 3 — POS app
- Build menu grid, categories, Customize modal, ticket panel, order-type, send/clear (Section 7.2).
- **Acceptance:** cashier builds a customized order (incl. add-ons + note + meal) and sends it; totals match the server; cart clears; the created order shows on a running KDS/stream.

### Phase 4 — Kitchen app
- Build the rail, ticket cards, live age colors, Start/Ready, optional recall (Section 7.3).
- **Acceptance:** new orders appear <1s without refresh; timers advance and change color at 5/10 min; Ready removes the card; reload rebuilds the rail correctly.

### Phase 5 — Pickup board
- Build Preparing/Ready zones, live updates, new-ready pulse/chime, tap-to-collect (Section 7.4).
- **Acceptance:** a number moves to Ready the instant the kitchen bumps it; tapping clears it; reload rebuilds state.

### Phase 6 — Auth, menu admin, reporting
- Wire JWT + roles; protect mutating endpoints. Add menu CRUD (`MANAGER`) and a simple
  UI, plus an order-history/day-summary endpoint + view (counts, revenue, avg prep time from timestamps).
- **Acceptance:** unauthenticated mutations `401/403`; a manager edits a price and the POS reflects it after reload; day summary reconciles with created orders.

### Phase 7 — Deployment (AWS)
- Backend jar on EC2/container; RDS Postgres; three app bundles on S3+CloudFront; env-driven config & CORS; CI (build/test) + CD.
- **Acceptance:** the full loop works end-to-end against deployed infra from three separate devices/tabs.

### Suggested Claude Code kickoff prompts
- *"Read EMBER-SPEC.md and CLAUDE.md. Do Phase 0: set up the pnpm monorepo, move ember-backend to backend/, and scaffold the three Vite React TS apps with the Tailwind preset. Stop at the Phase 0 acceptance criteria and show me how to run each."*
- *"Do Phase 1. Get the backend building, add the tests listed, add Flyway, and move broadcasts to AFTER_COMMIT. Report the acceptance checklist with pass/fail."*
- *"Do Phase 2 then Phase 3: build packages/shared, then the POS app matching Section 7.2 and the design tokens in Section 8."*

---

## 12. Testing strategy
- **Backend:** unit (pricing, state machine), slice (`@DataJpaTest`, `@WebMvcTest`), one integration (`@SpringBootTest`) incl. a STOMP broadcast assertion.
- **Frontend:** Vitest + Testing Library for the store, the customize/pricing display, and card age logic; Playwright e2e for the full POS→KDS→Board flow across three browser contexts.
- **Contract:** a single test asserting `types.ts` matches the backend DTO JSON (snapshot of `GET /api/menu` and a created order).

## 13. Stretch / future
Payments (card/cash tender + change), receipt/kitchen ticket printing (ESC/POS),
combo/meal auto-bundling of the side+drink lines, item 86-ing (mark sold out live),
multi-station kitchen routing (grill vs fryer), customer order-ahead + SMS "ready",
and an offline-capable POS queue.

## 14. Appendix — seed menu
Categories and prices already in `DataSeeder`. Sizes: Small +0.00, Medium +1.00,
Large +1.80. Burger add-ons: Extra cheese +0.90, Bacon +1.50, No onion / No pickle /
Extra spicy +0.00. Chicken add-ons: Make it spicy +0.00, Ranch dip +0.50, Extra crispy +0.00.
Items: Burgers b1–b4, Chicken c1–c3, Sides s1–s4, Drinks d1–d4, Sweets w1–w3.
