// Ember Admin — manager back-office (EMBER-SPEC §11 Phase 6): menu admin + reporting.
// Manager-only; the backend enforces the role, this app gates the UI.
import { useState } from 'react';
import { useAuth } from '@ember/shared';
import { Login } from './components/Login';
import { MenuEditor } from './components/MenuEditor';
import { Analytics } from './components/Analytics';
import { Employees } from './components/Employees';
import { Orders } from './components/Orders';
import { Schedule } from './components/Schedule';

type Tab = 'menu' | 'orders' | 'report' | 'staff' | 'schedule';

export default function App() {
  const { session, login, logout } = useAuth();

  if (!session) {
    return <Login onSubmit={login} hint="Demo: manager / manager123" />;
  }
  if (session.role !== 'MANAGER') {
    return (
      <main className="grid min-h-screen place-items-center bg-char text-bone font-body p-6">
        <div className="text-center">
          <p className="font-display text-3xl">Managers only</p>
          <p className="mt-1 text-muted">Signed in as {session.username} ({session.role.toLowerCase()}).</p>
          <button
            onClick={logout}
            className="mt-4 min-h-11 rounded-2xl bg-steel2 px-6 font-semibold text-bone focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          >
            Sign out
          </button>
        </div>
      </main>
    );
  }

  return <Admin username={session.username} onLogout={logout} />;
}

function Admin({ username, onLogout }: { username: string; onLogout: () => void }) {
  const [tab, setTab] = useState<Tab>('menu');

  const body =
    tab === 'menu' ? (
      <MenuEditor />
    ) : tab === 'orders' ? (
      <Orders />
    ) : tab === 'schedule' ? (
      <Schedule />
    ) : tab === 'report' ? (
      <Analytics />
    ) : (
      <Employees currentUsername={username} />
    );

  return (
    <div className="min-h-screen bg-char text-bone font-body">
      <header className="flex items-center justify-between border-b border-steel px-6 py-3">
        <div className="flex items-center gap-6">
          <h1 className="font-display text-3xl tracking-wide bg-ember-gradient bg-clip-text text-transparent">
            Ember Admin
          </h1>
          <nav className="flex gap-2">
            <TabButton active={tab === 'menu'} onClick={() => setTab('menu')}>Menu</TabButton>
            <TabButton active={tab === 'orders'} onClick={() => setTab('orders')}>Orders</TabButton>
            <TabButton active={tab === 'schedule'} onClick={() => setTab('schedule')}>Schedule</TabButton>
            <TabButton active={tab === 'staff'} onClick={() => setTab('staff')}>Employees</TabButton>
            <TabButton active={tab === 'report'} onClick={() => setTab('report')}>Reports</TabButton>
          </nav>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm text-muted">{username}</span>
          <button
            onClick={onLogout}
            className="min-h-9 rounded-full border border-steel2 px-3 text-sm text-bone/70 hover:bg-steel2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember"
          >
            Sign out
          </button>
        </div>
      </header>

      {body}
    </div>
  );
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: string;
}) {
  return (
    <button
      onClick={onClick}
      className={[
        'min-h-9 rounded-full px-4 text-sm font-semibold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember',
        active ? 'bg-bone text-ink' : 'text-bone/70 hover:bg-steel2',
      ].join(' ')}
    >
      {children}
    </button>
  );
}
