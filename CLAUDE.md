# CLAUDE.md — Ember

Context and working rules for this repo. The full spec is in **EMBER-SPEC.md** —
read it before starting, and work through its Section 11 phases in order, stopping
at each phase's acceptance criteria.

## What this is
A quick-service (fast food) management system: a **POS**, a **kitchen display
(KDS)**, and a **customer pickup board**, sharing one live order stream. Monorepo:
`backend/` (Spring Boot) + `apps/{pos,kitchen,board}` (React/TS/Vite) +
`packages/shared` (types, api client, ws hook, design tokens).

## Golden rules
1. **Server owns pricing, ticket numbers, and order state.** Clients send intent
   (item ids + choices, "advance", "collect"), never prices or totals. Do not add a
   client-side price the server then trusts.
2. **One order stream.** Every order change broadcasts to `/topic/orders`. Each app
   keeps a single store keyed by `order.id` and derives views by filtering on
   `status`. Don't maintain parallel lists per screen.
3. **Reload- and reconnect-safe.** Apps seed from `GET /api/orders?status=active`
   on mount and re-fetch on socket reconnect. No state lives only in memory of one screen.
4. **Money = BigDecimal (backend) / integer-cent-safe formatting (frontend).**
   Scale 2, HALF_UP. Never `double`. **Time = `Instant` (UTC)** stored; render local.
5. **Preserve the API/WS/domain contract** in EMBER-SPEC.md Sections 4–6. If you must
   change it, update the spec and `packages/shared/types.ts` in the same change.

## Conventions
- **Backend:** Java 21, Spring Boot 3.4. Records for DTOs; plain entities with
  getters/setters. Status transitions only via `Order` methods. New exceptions map
  through `GlobalExceptionHandler` to RFC 7807 ProblemDetail. Keep controllers thin;
  logic in services.
- **Frontend:** TypeScript strict. Functional components + hooks. Zustand for the
  order store. Tailwind via the shared preset — use design tokens (Section 8), no ad-hoc hex.
- **Naming stays consistent across the whole flow:** the button that says "Send to
  kitchen" produces an order the KDS shows; a "Mark ready" action produces a board
  "Ready" call. Same vocabulary end to end.
- **Formatting:** Prettier + ESLint (frontend), standard Spring/Java style (backend).
- **Commits:** small and scoped; conventional-commit style (`feat:`, `fix:`, `test:`,
  `chore:`). One phase ≠ one commit — commit per coherent unit.

## Commands
```bash
# backend
cd backend && mvn spring-boot:run          # http://localhost:8080 (H2 + seeded menu)
cd backend && mvn test

# frontend (from repo root, pnpm workspaces)
pnpm install
pnpm dev:pos       # / dev:kitchen / dev:board
pnpm --filter @ember/shared test
pnpm lint && pnpm test
```

## Definition of done (per feature)
- Meets the relevant EMBER-SPEC acceptance criteria.
- Has tests (see spec Section 12) and they pass.
- No hardcoded prices/tax/menu in the UI; reads from API/config.
- Handles empty, error, loading, and reconnect states.
- Keyboard-focusable and honors `prefers-reduced-motion`.

## Don'ts
- Don't ship the single-file prototype (`ember-fastfood.jsx`) as an app; it's the UX
  reference only. Split it into the three apps against the real backend.
- Don't use `localStorage`/`sessionStorage` for order state — the socket + REST are the source of truth.
- Don't trust client-sent prices, totals, or ticket numbers.
- Don't invent new order statuses or event types without updating the spec + shared types.
