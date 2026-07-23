// Ember POS — cashier builds an order and sends it to the kitchen (EMBER-SPEC §7.2).
// The server owns pricing and ticket numbers; this app sends only item ids + choices.
import { useEffect, useMemo, useState } from 'react';
import {
  ApiError,
  ClockButton,
  PinLogin,
  api,
  useAuth,
  useOrderStream,
  type MenuItem,
  type OrderType,
} from '@ember/shared';
import { CategoryTabs, orderedCategories } from './components/CategoryTabs';
import { MenuGrid } from './components/MenuGrid';
import { CustomizeModal } from './components/CustomizeModal';
import { TicketPanel } from './components/TicketPanel';
import { Toast } from './components/Toast';
import {
  addLine,
  needsCustomize,
  removeLine,
  setQuantity,
  toCreateRequest,
  type CartLine,
  type LineChoices,
} from './lib/cart';

// Cashiers (and managers) run the POS; the backend enforces the role on send.
const POS_ROLES = ['CASHIER', 'MANAGER'];

export default function App() {
  const { session, loginWithPin, logout } = useAuth();
  if (!session) {
    return <PinLogin title="Ember POS" roles={['CASHIER', 'MANAGER']} onSubmit={loginWithPin} />;
  }
  if (!POS_ROLES.includes(session.role)) {
    return (
      <main className="grid min-h-screen place-items-center bg-bone text-ink font-body p-6">
        <div className="text-center">
          <p className="font-display text-3xl">This screen is for cashiers</p>
          <p className="mt-1 text-muted">Signed in as {session.username} ({session.role.toLowerCase()}).</p>
          <button
            onClick={logout}
            className="mt-4 min-h-11 rounded-2xl bg-ink px-6 font-semibold text-bone focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          >
            Sign out
          </button>
        </div>
      </main>
    );
  }
  return <Pos />;
}

function Pos() {
  const { session, logout } = useAuth();
  const [menu, setMenu] = useState<MenuItem[] | null>(null);
  const [menuError, setMenuError] = useState<string | null>(null);
  const [category, setCategory] = useState<string>('');
  const [cart, setCart] = useState<CartLine[]>([]);
  const [orderType, setOrderType] = useState<OrderType>('DINE_IN');
  const [customizing, setCustomizing] = useState<MenuItem | null>(null);
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const [toast, setToast] = useState<number | null>(null);

  // Keep a live socket so the cashier sees when the kitchen link drops.
  const connection = useOrderStream({ syncStore: false });

  const loadMenu = () => {
    setMenuError(null);
    api
      .getMenu()
      .then((items) => {
        setMenu(items);
        setCategory((current) => current || orderedCategories(items.map((i) => i.category))[0] || '');
      })
      .catch(() => setMenuError('Could not load the menu.'));
  };

  useEffect(loadMenu, []);

  const categories = useMemo(
    () => (menu ? orderedCategories(menu.map((i) => i.category)) : []),
    [menu],
  );
  const items = useMemo(
    () => (menu ? menu.filter((i) => i.category === category) : []),
    [menu, category],
  );

  const pickItem = (item: MenuItem) => {
    if (needsCustomize(item)) {
      setCustomizing(item);
    } else {
      addToCart(item, { meal: false, addons: [] });
    }
  };

  const addToCart = (item: MenuItem, choices: LineChoices) => {
    setCart((lines) => addLine(lines, item, choices));
    setSendError(null);
  };

  const send = async () => {
    if (cart.length === 0 || sending) return;
    setSending(true);
    setSendError(null);
    try {
      const order = await api.createOrder(toCreateRequest(cart, orderType));
      setToast(order.ticketNumber);
      setCart([]);
    } catch (e) {
      setSendError(
        e instanceof ApiError
          ? e.message
          : 'Could not reach the kitchen. Check the connection and try again.',
      );
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="flex h-screen flex-col bg-bone text-ink font-body">
      <header className="flex items-center justify-between border-b border-bone2 px-6 py-3">
        <h1 className="font-display text-3xl tracking-wide bg-ember-gradient bg-clip-text text-transparent">
          Ember POS
        </h1>
        <div className="flex items-center gap-4">
          <ConnectionPill status={connection} />
          <ClockButton />
          <span className="text-sm text-muted">{session?.username}</span>
          <button
            onClick={logout}
            className="min-h-9 rounded-full border border-bone2 px-3 text-sm text-ink/70 hover:bg-ink/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          >
            Sign out
          </button>
        </div>
      </header>

      <div className="grid flex-1 grid-cols-1 overflow-hidden lg:grid-cols-[1fr_22rem]">
        <main className="flex flex-col overflow-hidden p-4">
          {menuError ? (
            <ErrorState message={menuError} onRetry={loadMenu} />
          ) : menu === null ? (
            <LoadingState />
          ) : (
            <>
              <CategoryTabs categories={categories} active={category} onSelect={setCategory} />
              <div className="mt-4 flex-1 overflow-y-auto pb-4">
                <MenuGrid items={items} onPick={pickItem} />
              </div>
            </>
          )}
        </main>

        <TicketPanel
          lines={cart}
          orderType={orderType}
          sending={sending}
          error={sendError}
          onOrderType={setOrderType}
          onQuantity={(key, q) => setCart((lines) => setQuantity(lines, key, q))}
          onRemove={(key) => setCart((lines) => removeLine(lines, key))}
          onSend={send}
          onClear={() => {
            setCart([]);
            setSendError(null);
          }}
        />
      </div>

      {customizing && (
        <CustomizeModal
          item={customizing}
          onClose={() => setCustomizing(null)}
          onAdd={(choices) => {
            addToCart(customizing, choices);
            setCustomizing(null);
          }}
        />
      )}

      {toast !== null && <Toast ticketNumber={toast} onDismiss={() => setToast(null)} />}
    </div>
  );
}

function ConnectionPill({ status }: { status: 'connecting' | 'connected' | 'disconnected' }) {
  const map = {
    connected: { color: '#2FCB86', label: 'Kitchen link live' },
    connecting: { color: '#FFB020', label: 'Connecting…' },
    disconnected: { color: '#FF463B', label: 'Reconnecting…' },
  } as const;
  const { color, label } = map[status];
  return (
    <span className="flex items-center gap-2 text-sm text-muted">
      <span className="inline-block h-2.5 w-2.5 rounded-full" style={{ background: color }} />
      {label}
    </span>
  );
}

function LoadingState() {
  return (
    <div className="grid flex-1 place-items-center text-muted">
      <span className="animate-pulse font-display text-2xl">Loading menu…</span>
    </div>
  );
}

function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="grid flex-1 place-items-center">
      <div className="text-center">
        <p className="text-late">{message}</p>
        <button
          onClick={onRetry}
          className="mt-3 min-h-11 rounded-2xl bg-ink px-6 font-semibold text-bone focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
        >
          Retry
        </button>
      </div>
    </div>
  );
}
