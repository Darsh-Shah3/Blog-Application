'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api, AuditEvent, friendlyError, Page, PlatformReport, User } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { isAdmin, roleLabel, ROLES } from '@/lib/rbac';
import { AdminSkeleton, SpinnerLabel } from '@/components/loaders';
import { AdminPageShell } from '@/components/AdminShell';
import { TopBar } from '@/components/TopBar';
import {
  EyeIcon,
  PenIcon,
  PlusIcon,
  TrashIcon,
  LogInIcon,
} from '@/components/icons';

const ALL_ROLES = [ROLES.USER, ROLES.MODERATOR, ROLES.ADMIN];

function ActionGlyph({ action }: { action: string }) {
  const a = (action || '').toUpperCase();
  if (a.includes('DELETE') || a.includes('REMOVE')) {
    return <span className="audit-action-icon audit-action-delete" title={action}><TrashIcon size={14} /></span>;
  }
  if (a.includes('CREATE') || a.includes('JOIN') || a.includes('POST')) {
    return <span className="audit-action-icon audit-action-create" title={action}><PlusIcon size={14} /></span>;
  }
  if (a.includes('UPDATE') || a.includes('EDIT') || a.includes('ROLE')) {
    return <span className="audit-action-icon audit-action-update" title={action}><PenIcon size={14} /></span>;
  }
  if (a.includes('LOGIN') || a.includes('AUTH')) {
    return <span className="audit-action-icon audit-action-login" title={action}><LogInIcon size={14} /></span>;
  }
  return <span className="audit-action-icon audit-action-view" title={action}><EyeIcon size={14} /></span>;
}

export default function AdminPage() {
  const { user, token, loading } = useAuth();
  const router = useRouter();
  const { success, error, info, confirm } = useToast();
  const [users, setUsers] = useState<User[]>([]);
  const [q, setQ] = useState('');
  const [busyId, setBusyId] = useState<string | null>(null);
  const [selected, setSelected] = useState<Record<string, string[]>>({});
  const [report, setReport] = useState<PlatformReport | null>(null);
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [auditQ, setAuditQ] = useState('');
  const [auditAction, setAuditAction] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);

  useEffect(() => {
    if (loading) return;
    if (!user || !isAdmin(user)) {
      info('Admin access required.');
      router.replace('/');
    }
  }, [user, loading, router, info]);

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
      const map: Record<string, string[]> = {};
      (page.content || []).forEach((u) => {
        map[u.id] = u.roles?.length ? [...u.roles] : [ROLES.USER];
      });
      setSelected(map);
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, user?.id]);

  const toggleRole = (userId: string, role: string) => {
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

  const saveRoles = async (userId: string) => {
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

  const deleteUser = async (u: User) => {
    if (!token || !user) return;
    if (u.id === user.id) {
      error('Use your profile to delete your own account.');
      return;
    }
    const ok = await confirm({
      title: `Delete @${u.username}?`,
      message:
        'Soft-deletes the account. Posts stay on Threadly and will show as from a deleted account. The member is emailed if SMTP is enabled.',
      confirmLabel: 'Delete account',
    });
    if (!ok) return;
    setDeletingId(u.id);
    try {
      await api(`/api/v1/admin/users/${u.id}`, { method: 'DELETE' }, token);
      success(`@${u.username} account deleted.`);
      await load();
      await loadReport();
    } catch (e) {
      error(friendlyError(e, 'Could not delete user.'));
    } finally {
      setDeletingId(null);
    }
  };

  if (loading || !user || !isAdmin(user)) {
    return <AdminSkeleton />;
  }

  return (
    <AdminPageShell
      kicker="Admin console"
      title="Control center"
      blurb="User roles, live snapshot, and audit trail. Reports and Infra live in the sub-nav."
      trailing={(
        <button type="button" className="btn secondary" onClick={() => { load(); loadReport(); loadAudit(); }}>
          Refresh all
        </button>
      )}
    >
      <TopBar
        crumb="Admin"
        searchPlaceholder="Search users or audit summary…"
        defaultSearch={q}
        onSearch={(value) => {
          setQ(value);
          setAuditQ(value);
          load(value);
          loadAudit(value, auditAction);
        }}
        filters={(
          <>
            <button
              type="button"
              className={`chip ${auditAction === '' ? 'active' : ''}`}
              onClick={() => { setAuditAction(''); loadAudit(auditQ, ''); }}
            >
              All
            </button>
            {['CREATE', 'UPDATE', 'DELETE', 'LOGIN'].map((a) => (
              <button
                key={a}
                type="button"
                className={`chip ${auditAction === a ? 'active' : ''}`}
                onClick={() => { setAuditAction(a); loadAudit(auditQ, a); }}
              >
                {a}
              </button>
            ))}
          </>
        )}
      />

      {report && (
        <section className="admin-kpi-board">
          <div className="admin-kpi">
            <span className="admin-kpi-label">Users</span>
            <strong>{report.activeUsers}</strong>
          </div>
          <div className="admin-kpi">
            <span className="admin-kpi-label">Posts</span>
            <strong>{report.totalPosts}</strong>
          </div>
          <div className="admin-kpi">
            <span className="admin-kpi-label">Communities</span>
            <strong>{report.totalCommunities}</strong>
          </div>
          <div className="admin-kpi">
            <span className="admin-kpi-label">Comments</span>
            <strong>{report.totalComments}</strong>
          </div>
          <div className="admin-kpi">
            <span className="admin-kpi-label">Audit / 24h</span>
            <strong>{report.auditEventsLast24h}</strong>
          </div>
          <div className="admin-kpi">
            <span className="admin-kpi-label">Audit / 7d</span>
            <strong>{report.auditEventsLast7d}</strong>
          </div>
          <div className="admin-kpi-foot">
            <span className="muted">Snapshot {new Date(report.generatedAt).toLocaleString()}</span>
            <Link className="btn secondary" href="/admin/analytics">Open reports</Link>
          </div>
        </section>
      )}

      <div className="admin-split">
        <section className="admin-card">
          <header className="admin-card-head">
            <h2>Users & roles</h2>
            <span className="muted">{users.length} loaded</span>
          </header>
          <div className="admin-user-table">
            {users.map((u) => (
              <article key={u.id} className="admin-user-card">
                <div className="admin-user-main">
                  <div className="admin-user-avatar" aria-hidden>
                    {(u.displayName || u.username || '?').slice(0, 1).toUpperCase()}
                  </div>
                  <div>
                    <strong>{u.username}</strong>
                    <p className="muted admin-user-meta">{u.email} · karma {u.karma}</p>
                    <div className="role-badges">
                      {(u.roles || []).map((r) => (
                        <span key={r} className="role-badge">{roleLabel(r)}</span>
                      ))}
                    </div>
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
                    {busyId === u.id ? <SpinnerLabel label="Saving…" /> : 'Save'}
                  </button>
                  {u.id !== user.id && (
                    <button
                      className="btn ghost"
                      type="button"
                      disabled={deletingId === u.id}
                      onClick={() => deleteUser(u)}
                      title="Soft-delete account"
                    >
                      {deletingId === u.id ? <SpinnerLabel label="Deleting…" /> : 'Delete account'}
                    </button>
                  )}
                </div>
              </article>
            ))}
            {users.length === 0 && <p className="muted">No users found.</p>}
          </div>
        </section>

        <section className="admin-card">
          <header className="admin-card-head">
            <h2>Audit trail</h2>
            <span className="muted">{events.length} events</span>
          </header>
          <ul className="admin-audit-list">
            {events.map((ev) => (
              <li key={ev.id} className="admin-audit-row">
                <ActionGlyph action={ev.action} />
                <div className="admin-audit-body">
                  <div className="admin-audit-top">
                    <span className="admin-audit-resource">
                      {ev.resourceType}
                      {ev.resourceId ? ` / ${String(ev.resourceId).slice(0, 12)}` : ''}
                    </span>
                    <time className="muted">{new Date(ev.occurredAt).toLocaleString()}</time>
                  </div>
                  <p className="admin-audit-summary">
                    {ev.summary || `${ev.action} via ${ev.serviceName}`}
                  </p>
                  <p className="muted admin-audit-meta">
                    {ev.serviceName} · {ev.actorUsername || 'system'}
                  </p>
                </div>
              </li>
            ))}
            {events.length === 0 && <li className="muted">No audit events matched.</li>}
          </ul>
        </section>
      </div>
    </AdminPageShell>
  );
}
