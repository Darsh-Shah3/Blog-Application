'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { api, friendlyError, NotificationItem, Page } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';

export default function NotificationsPage() {
  const { token, loading: authLoading } = useAuth();
  const { error, success } = useToast();
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async () => {
    if (!token) return;
    setLoading(true);
    try {
      const params = new URLSearchParams({ size: '40', unreadOnly: String(unreadOnly) });
      if (q.trim()) params.set('q', q.trim());
      const page = await api<Page<NotificationItem>>(`/api/v1/notifications?${params}`, {}, token);
      setItems(page.content || []);
    } catch (e) {
      error(friendlyError(e, 'Could not load notifications.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!authLoading && token) load();
    if (!authLoading && !token) setLoading(false);
  }, [token, authLoading, unreadOnly]);

  const markRead = async (id: number) => {
    if (!token) return;
    try {
      await api(`/api/v1/notifications/${id}/read`, { method: 'POST' }, token);
      await load();
    } catch (e) {
      error(friendlyError(e));
    }
  };

  const markAll = async () => {
    if (!token) return;
    try {
      await api(`/api/v1/notifications/read-all`, { method: 'POST' }, token);
      success('All notifications marked read.');
      await load();
    } catch (e) {
      error(friendlyError(e));
    }
  };

  if (authLoading) return <p className="muted" style={{ marginTop: '1.5rem' }}>Loading…</p>;
  if (!token) {
    return (
      <div className="panel" style={{ marginTop: '1.5rem' }}>
        <p>Sign in to see notifications.</p>
        <Link className="btn" href="/login">Log in</Link>
      </div>
    );
  }

  return (
    <div style={{ marginTop: '1.25rem' }} className="stack">
      <section className="hero" style={{ paddingTop: '1rem' }}>
        <h1>Notifications</h1>
        <p>Follow alerts, platform notices, and updates from people you follow.</p>
      </section>

      <form
        className="toolbar"
        onSubmit={(e) => {
          e.preventDefault();
          load();
        }}
      >
        <input
          className="input"
          placeholder="Search notifications…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          style={{ flex: 1, minWidth: '10rem' }}
        />
        <button type="submit" className="btn secondary">Search</button>
        <button
          type="button"
          className={`chip ${unreadOnly ? 'active' : ''}`}
          onClick={() => setUnreadOnly((v) => !v)}
        >
          unread only
        </button>
        <button type="button" className="btn ghost" onClick={markAll}>Mark all read</button>
      </form>

      {loading && <p className="muted">Loading…</p>}
      {!loading && items.length === 0 && (
        <div className="panel muted">No notifications yet. Follow someone and wait for their next post.</div>
      )}
      <div className="stack">
        {items.map((n) => (
          <article key={n.id} className={`panel notif-item ${n.read ? '' : 'unread'}`}>
            <div className="notif-head">
              <strong>{n.title}</strong>
              <span className="muted">{new Date(n.createdAt).toLocaleString()}</span>
            </div>
            {n.body && <p style={{ margin: '0.35rem 0' }}>{n.body}</p>}
            <div className="toolbar">
              {n.linkUrl && (
                <Link className="btn secondary" href={n.linkUrl} onClick={() => markRead(n.id)}>
                  Open
                </Link>
              )}
              {!n.read && (
                <button type="button" className="btn ghost" onClick={() => markRead(n.id)}>
                  Mark read
                </button>
              )}
              <span className="chip">{n.type}</span>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
