'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { api, friendlyError, NotificationItem, Page } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import {
  ContentPlaceholder,
  NotificationSkeletonList,
  PageSpinner,
} from '@/components/loaders';

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
      // Newest first (YouTube-style stack)
      const params = new URLSearchParams({
        size: '40',
        unreadOnly: String(unreadOnly),
        sort: 'createdAt,desc',
      });
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

  const markRead = async (id: string) => {
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

  if (authLoading) return <PageSpinner label="Checking session…" />;
  if (!token) {
    return (
      <ContentPlaceholder
        title="Sign in to see notifications"
        description="Your inbox of follows and platform alerts will show up here."
        action={<Link className="btn" href="/login">Log in</Link>}
      />
    );
  }

  return (
    <div style={{ marginTop: '1.25rem' }} className="stack">
      <section className="hero" style={{ paddingTop: '1rem' }}>
        <h1>Notifications</h1>
        <p>Newest on top — open a card to jump into the post.</p>
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

      {loading && <NotificationSkeletonList count={6} />}
      {!loading && items.length === 0 && (
        <ContentPlaceholder
          title="No notifications yet"
          description="Follow someone and wait for their next post."
        />
      )}
      {!loading && <div className="stack notif-stack">
        {items.map((n) => (
          <article key={n.id} className={`notif-card ${n.read ? '' : 'unread'}`}>
            <div className="notif-thumb" aria-hidden>
              {n.mediaUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={n.mediaUrl} alt="" />
              ) : (
                <span className="notif-thumb-fallback">
                  {(n.actorUsername || 'U').slice(0, 1).toUpperCase()}
                </span>
              )}
            </div>
            <div className="notif-body">
              <div className="notif-line">
                {n.actorUsername && <strong>@{n.actorUsername}</strong>}
                <span className="muted">{new Date(n.createdAt).toLocaleString()}</span>
              </div>
              <div className="notif-title">{n.postTitle || n.title}</div>
              {(n.summary || n.body) && (
                <p className="notif-summary">{n.summary || n.body}</p>
              )}
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
              </div>
            </div>
          </article>
        ))}
      </div>}
    </div>
  );
}
