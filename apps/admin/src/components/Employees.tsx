// Manager Employees tab: list staff, add, edit role/name/active, reset PIN/password, delete.
import { useEffect, useState } from 'react';
import { api, type Staff, type StaffInput, type StaffUpdate } from '@ember/shared';
import { EmployeeForm } from './EmployeeForm';

type Editing = { staff: Staff | null } | null;

export function Employees({ currentUsername }: { currentUsername: string }) {
  const [staff, setStaff] = useState<Staff[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<Editing>(null);

  const load = () => {
    setError(null);
    api.getStaff().then(setStaff).catch(() => setError('Could not load employees.'));
  };
  useEffect(load, []);

  const close = () => setEditing(null);
  const done = () => {
    close();
    load();
  };

  if (error) {
    return (
      <div className="p-6 text-center">
        <p className="text-late">{error}</p>
        <button onClick={load} className="mt-3 min-h-11 rounded-xl bg-steel2 px-5">Retry</button>
      </div>
    );
  }
  if (!staff) return <p className="p-6 text-muted">Loading employees…</p>;

  return (
    <div className="p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-display text-2xl">Employees ({staff.length})</h2>
        <button
          onClick={() => setEditing({ staff: null })}
          className="min-h-11 rounded-xl bg-ember-gradient px-5 font-display text-lg text-[#1a0f08]"
        >
          + New employee
        </button>
      </div>

      <div className="overflow-hidden rounded-2xl border border-steel">
        <table className="w-full text-left">
          <thead className="bg-graphite text-xs uppercase tracking-wider text-muted">
            <tr>
              <th className="px-4 py-2">Name</th>
              <th className="px-4 py-2">Username</th>
              <th className="px-4 py-2">Role</th>
              <th className="px-4 py-2">Signs in with</th>
              <th className="px-4 py-2">Status</th>
              <th className="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody>
            {staff.map((s) => (
              <tr key={s.id} className="border-t border-steel/60 hover:bg-graphite2">
                <td className="px-4 py-2 font-semibold">
                  {s.displayName}
                  {s.username === currentUsername && <span className="ml-2 text-xs text-flame">you</span>}
                </td>
                <td className="px-4 py-2 font-mono text-sm text-muted">{s.username}</td>
                <td className="px-4 py-2">{s.role.charAt(0) + s.role.slice(1).toLowerCase()}</td>
                <td className="px-4 py-2 text-sm text-bone/80">
                  {[s.hasPin ? 'PIN' : null, s.hasPassword ? 'password' : null].filter(Boolean).join(' + ') || '—'}
                </td>
                <td className="px-4 py-2">
                  {s.active ? (
                    <span className="text-fresh">Active</span>
                  ) : (
                    <span className="text-muted">Inactive</span>
                  )}
                </td>
                <td className="px-4 py-2 text-right">
                  <button
                    onClick={() => setEditing({ staff: s })}
                    className="rounded-lg px-3 py-1 text-flame hover:bg-steel2"
                  >
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {editing && (
        <EmployeeForm
          staff={editing.staff}
          isSelf={editing.staff?.username === currentUsername}
          onCreate={async (input: StaffInput) => {
            await api.createStaff(input);
            done();
          }}
          onUpdate={async (update: StaffUpdate) => {
            await api.updateStaff(editing.staff!.id, update);
            done();
          }}
          onResetPin={async (pin: string) => {
            await api.setStaffPin(editing.staff!.id, pin);
            done();
          }}
          onResetPassword={async (password: string) => {
            await api.setStaffPassword(editing.staff!.id, password);
            done();
          }}
          onDelete={async () => {
            await api.deleteStaff(editing.staff!.id);
            done();
          }}
          onClose={close}
        />
      )}
    </div>
  );
}
