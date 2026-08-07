'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, AuditEvent, friendlyError, Page, PlatformReport, RoleInfo, User } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { isAdmin, roleLabel, ROLES } from '@/lib/rbac';

const ALL_ROLES = [ROLES.USER, ROLES.MODERATOR, ROLES.ADMIN];

export default function AdminPage() {
  const { user, token, loading } = useAuth();
  const router = useRouter();
  const { success, error, info } = useToast();
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<RoleInfo[]>([]);
  const [q, setQ] = useState('');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [selected, setSelected] = useState<Record<number, string[]>>({});
  const [report, setReport] = useState<PlatformReport | null>(null);
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [auditQ, setAuditQ] = useState('');
  const [auditAction, setAuditAction] = useState('');

  useEffect(() => {
    if (loading) return;
    if (!user || !isAdmin(user)) {
      info('Admin access required.');
      router.replace('/');
    }
  }, [user, loading, router]);

  const loadReport = async () => {
    if (!token || !isAdmin(user)) return;
    try {
      const r = await api<PlatformReport>('/api/v1/reports/overview', {}, token);
      setReport(r);
    } catch (e) {
      error(friendlyError(e, 'Could not load report.'));
    }
  };

  const loadAudit = async (search = auditQ, action = auditAction) => {
    if (!token || !isAdmin(user)) return;
    try {
      const params = new URLSearchParams({ size: '30' });
      if (search.trim()) params.set('q', search.trim());
      if (action.trim()) params.set('action', action.trim());
      const page = await api<Page<AuditEvent>>(`/api/v1/audit/events?${params}`, {}, token);
      setEvents(page.content || []);
    } catch (e) {
      error(friendlyError(e, 'Could not load audit events.'));
    }
  };

  const load = async (search = q) => {
    if (!token || !isAdmin(user)) return;
    try {
      const qs = search.trim() ? `&q=${encodeURIComponent(search.trim())}` : '';
      const page = await api<Page<User>>(`/api/v1/admin/users?size=50${qs}`, {}, token);
      setUsers(page.content || []);
      const map: Record<number, string[]> = {};
      (page.content || []).forEach((u) => {
        map[u.id] = u.roles?.length ? [...u.roles] : [ROLES.USER];
      });
      setSelected(map);
      const catalog = await api<RoleInfo[]>('/api/v1/admin/roles', {}, token);
      setRoles(catalog || []);
    } catch (e) {
      error(friendlyError(e, 'Could not load admin data.'));
    }
  };

  useEffect(() => {
    if (token && isAdmin(user)) {
      load();
      loadReport();
      loadAudit();
    }
  }, [token, user?.id]);

  const toggleRole = (userId: number, role: string) => {
    setSelected((prev) => {
      const current = new Set(prev[userId] || [ROLES.USER]);
      if (current.has(role)) {
        if (role === ROLES.USER && current.size === 1) return prev;
        current.delete(role);
      } else {
        current.add(role);
      }
      if (role === ROLES.ADMIN || role === ROLES.MODERATOR) {
        current.add(ROLES.USER);
      }
      return { ...prev, [userId]: Array.from(current) };
    });
  };

  const saveRoles = async (userId: number) => {
    if (!token) return;
    setBusyId(userId);
    try {
      await api<User>(
        `/api/v1/admin/users/${userId}/roles`,
        {
          method: 'PUT',
          body: JSON.stringify({ roles: selected[userId] || [ROLES.USER] }),
        },
        token
      );
      success('Roles saved. User must sign in again for new rights.');
      await load();
    } catch (e) {
      error(friendlyError(e, 'Could not update roles.'));
    } finally {
      setBusyId(null);
    }
  };

  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    load(q);
  };

  if (loading || !user || !isAdmin(user)) {
    return <p className="muted" style={{ marginTop: '1.5rem' }}>Checking access…</p>;
  }

  return (
    <div style={{ marginTop: '1.25rem' }} className="stack">
      <section className="hero" style={{ paddingTop: '1.5rem' }}>
        <h1>Admin</h1>
        <p>Roles, platform reports, and a searchable audit trail of create / update / delete events.</p>
      </section>

      {report && (
        <section className="panel">
          <h2 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Platform report</h2>
          <div className="report-grid">
            <div><strong>{report.activeUsers}</strong><span className="muted"> active users</span></div>
            <div><strong>{report.totalPosts}</strong><span className="muted"> posts</span></div>
            <div><strong>{report.totalCommunities}</strong><span className="muted"> communities</span></div>
            <div><strong>{report.totalComments}</strong><span className="muted"> comments</span></div>
            <div><strong>{report.auditEventsLast24h}</strong><span className="muted"> audit / 24h</span></div>
            <div><strong>{report.auditEventsLast7d}</strong><span className="muted"> audit / 7d</span></div>
          </div>
          <p className="muted" style={{ marginBottom: 0, fontSize: '0.85rem' }}>
            Generated {new Date(report.generatedAt).toLocaleString()}
          </p>
          <button type="button" className="btn secondary" style={{ marginTop: '0.75rem' }} onClick={loadReport}>
            Refresh report
          </button>
        </section>
      )}

      <section className="panel">
        <h2 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Audit events</h2>
        <form
          className="form"
          onSubmit={(e) => {
            e.preventDefault();
            loadAudit();
          }}
          style={{ marginBottom: '1rem' }}
        >
          <input
            value={auditQ}
            onChange={(e) => setAuditQ(e.target.value)}
            placeholder="Search summary / resource id"
          />
          <select value={auditAction} onChange={(e) => setAuditAction(e.target.value)}>
            <option value="">All actions</option>
            <option value="CREATE">CREATE</option>
            <option value="UPDATE">UPDATE</option>
            <option value="DELETE">DELETE</option>
            <option value="LOGIN">LOGIN</option>
          </select>
          <button className="btn secondary" type="submit">Filter</button>
        </form>
        <div className="stack" style={{ gap: '0.5rem' }}>
          {events.map((ev) => (
            <div key={ev.id} className="muted" style={{ fontSize: '0.9rem' }}>
              <strong>{ev.action}</strong> {ev.resourceType}/{ev.resourceId || '—'} · {ev.serviceName}
              {' · '}{ev.actorUsername || '—'} · {new Date(ev.occurredAt).toLocaleString()}
              {ev.summary ? ` — ${ev.summary}` : ''}
            </div>
          ))}
          {events.length === 0 && <p className="muted">No audit events matched.</p>}
        </div>
      </section>

      {roles.length > 0 && (
        <section className="panel">
          <h2 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Role catalog</h2>
          <div className="stack" style={{ gap: '0.65rem' }}>
            {roles.map((r) => (
              <div key={r.name}>
                <strong>{r.displayName}</strong>{' '}
                <span className="role-badge">{roleLabel(r.name)}</span>
                <p className="muted" style={{ margin: '0.25rem 0 0' }}>{r.description}</p>
                <p className="muted" style={{ margin: '0.2rem 0 0', fontSize: '0.85rem' }}>
                  {r.permissions?.join(' · ')}
                </p>
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="panel">
        <h2 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Users</h2>
        <form className="form" onSubmit={onSearch} style={{ marginBottom: '1rem' }}>
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search username, email, display name"
          />
          <button className="btn secondary" type="submit">Search</button>
        </form>
        <div className="stack">
          {users.map((u) => (
            <div key={u.id} className="admin-user-row">
              <div>
                <strong>u/{u.username}</strong>
                <span className="muted"> · {u.email} · karma {u.karma}</span>
                <div className="role-badges" style={{ marginTop: '0.35rem' }}>
                  {(u.roles || []).map((r) => (
                    <span key={r} className="role-badge">{roleLabel(r)}</span>
                  ))}
                </div>
              </div>
              <div className="role-toggles">
                {ALL_ROLES.map((role) => (
                  <label key={role} className="role-check">
                    <input
                      type="checkbox"
                      checked={(selected[u.id] || []).includes(role)}
                      onChange={() => toggleRole(u.id, role)}
                    />
                    {roleLabel(role)}
                  </label>
                ))}
                <button
                  className="btn"
                  type="button"
                  disabled={busyId === u.id}
                  onClick={() => saveRoles(u.id)}
                >
                  {busyId === u.id ? 'Saving…' : 'Save roles'}
                </button>
              </div>
            </div>
          ))}
          {users.length === 0 && <p className="muted">No users found.</p>}
        </div>
      </section>
    </div>
  );
}
