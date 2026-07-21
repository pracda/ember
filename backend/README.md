# Ember — Backend

Spring Boot 3 / Java 21 backend for the Ember quick-service (fast food) system:
a POS that opens tickets, a kitchen display that works them, and a pickup board
that calls numbers. Built to sit behind the same kind of React front-ends as
`retail-shop-management`.

Design intent:

- **The server owns pricing.** The POS never sends a price. It sends item ids and
  choices; `PricingService` computes and validates every line against the menu.
- **One state machine.** `NEW → PREP → READY → DONE` (plus `READY → PREP` recall)
  lives on the `Order` entity. Illegal transitions return `409`, never a silent
  bad state.
- **Real-time by default.** Every order change is broadcast over STOMP to
  `/topic/orders`. The kitchen and board update live; REST is for the first load
  and as a fallback.

## Run it

Requires JDK 21 and Maven.

```bash
mvn spring-boot:run
```

Starts on `http://localhost:8080` with an in-memory H2 database, seeded with the
same menu the POS was designed against. H2 console: `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:ember`, user `sa`, no password).

## REST API

| Method | Path                        | Purpose                                  |
|--------|-----------------------------|------------------------------------------|
| GET    | `/api/menu`                 | Full menu with sizes and add-ons         |
| POST   | `/api/orders`               | Create a ticket (POS)                    |
| GET    | `/api/orders?status=active` | Kitchen rail (active), default           |
| GET    | `/api/orders?status=all`    | Order history, newest first              |
| GET    | `/api/orders/{id}`          | One order                                |
| POST   | `/api/orders/{id}/advance`  | `NEW → PREP`, then `PREP → READY`         |
| POST   | `/api/orders/{id}/recall`   | `READY → PREP`                            |
| POST   | `/api/orders/{id}/collect`  | `READY → DONE`                            |

### Create an order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
        "type": "DINE_IN",
        "lines": [
          { "itemId": "b1", "quantity": 1, "meal": true,
            "addons": ["No onion", "Extra cheese"], "notes": "well done" },
          { "itemId": "d1", "quantity": 1, "size": "Large" }
        ]
      }'
```

The response includes the server-assigned `ticketNumber`, per-line `unitPrice`,
and `subtotal` / `tax` / `total`. Unknown items → `404`; a size or add-on the item
doesn't offer → `400`.

## Real-time (STOMP over WebSocket)

- Endpoint: `/ws` (SockJS)
- Subscribe: `/topic/orders`
- Message: `{ "type": "ORDER_CREATED" | "ORDER_STARTED" | "ORDER_READY" | "ORDER_RECALLED" | "ORDER_COLLECTED", "order": { ...OrderResponse } }`

Each station subscribes to the one stream and filters by `order.status` — exactly
how the prototype's three views share a single order list. Split into
`/topic/kitchen` and `/topic/board` later if you want per-screen topics.

## Wiring the React stations

Install `@stomp/stompjs` and `sockjs-client`, then in each app:

```js
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const client = new Client({
  webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
  onConnect: () => {
    client.subscribe("/topic/orders", (msg) => {
      const { type, order } = JSON.parse(msg.body);
      dispatch({ type: "APPLY_EVENT", event: type, order }); // upsert by order.id
    });
  },
});
client.activate();
```

Replace the prototype's `useReducer` seed with a `GET /api/orders?status=active`
on mount, then let the socket keep it live. The POS's "Send to kitchen" becomes a
`POST /api/orders`; the kitchen's Start/Ready buttons call `/advance`; the board's
tap calls `/collect`.

## Production (AWS)

Matches the `retail-shop-management` deployment shape:

- **Profile:** run with `SPRING_PROFILES_ACTIVE=prod` and provide `DB_URL`,
  `DB_USER`, `DB_PASSWORD`, `CORS_ORIGINS` via environment.
- **Database:** PostgreSQL on RDS. `ddl-auto` is `validate` in prod — manage
  schema with Flyway or Liquibase rather than letting Hibernate alter it.
- **API:** package with `mvn clean package` and run the jar on EC2 (or containerize).
- **Front-ends:** the three station bundles on S3 + CloudFront, pointed at the API
  and `/ws` origin. Reuse your retail JWT/roles for staff vs manager.

## Layout

```
com.ember
├── config      EmberProperties, WebSocketConfig, CorsConfig, DataSeeder
├── domain      Order, OrderLine, MenuItem, PriceModifier, OrderStatus, OrderType
├── repository  OrderRepository, MenuItemRepository
├── service     OrderService, MenuService, PricingService, TicketSequence
└── web         OrderController, MenuController, GlobalExceptionHandler
    ├── dto      requests, responses, OrderEvent, Mappers
    └── error    NotFound / InvalidTransition / BadRequest
```

## Notes / next steps

- `TicketSequence` seeds from the max ticket in the DB. For a daily reset, add a
  scheduled midnight reset or a date-keyed DB sequence.
- Broadcasts fire **after the transaction commits** via
  `OrderEventBroadcaster` (`@TransactionalEventListener(AFTER_COMMIT)`), so a client
  never sees an event for a change that later rolls back.
- Schema is owned by **Flyway** (`src/main/resources/db/migration`). Prod runs with
  `ddl-auto: validate`; dev (H2) still builds from the entities via `create-drop`.
  `FlywayMigrationIT` verifies the migration against a real PostgreSQL 16 container.
- No auth is wired in this scaffold — drop in the retail project's JWT filter and
  restrict the mutating endpoints to staff roles.

## Tests

```bash
mvn test      # unit + slice + H2 integration tests
mvn verify    # also runs FlywayMigrationIT (Testcontainers PostgreSQL; skipped if Docker is unavailable)
```
