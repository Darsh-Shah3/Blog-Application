'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api, friendlyError, NotificationItem, Page } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import {
  ContentPlaceholder,
  NotificationSkeletonList,
  PageSpinner,
} from '@/components/loaders';
import { TopBar } from '@/components/TopBar';

type Filter = 'all' | 'DIRECT_MESSAGE' | 'FOLLOW_NEW_POST' | 'COMMUNITY_CHAT';

function typeLabel(type?: string) {
  switch (type) {
    case 'DIRECT_MESSAGE': return 'Message';
    case 'FOLLOW_NEW_POST': return 'Post';
    case 'COMMUNITY_CHAT': return 'Community chat';
    default: return type || 'Alert';
  }
}

function openHint(n: NotificationItem) {
  if (n.type === 'DIRECT_MESSAGE') return 'Open chat';
  if (n.type === 'COMMUNITY_CHAT') return 'Open community';
  if (n.type === 'FOLLOW_NEW_POST') return 'Open post';
  return n.linkUrl ? 'Open' : 'View';
}

export default function NotificationsPage() {
  const { token, loading: authLoading } = useAuth();
  const { error, success } = useToast();
  const router = useRouter();
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [filter, setFilter] = useState<Filter>('all');
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async (search = q) => {
    if (!token) return;
    setLoading(true);
    try {
      const params = new URLSearchParams({
        size: '50',
        unreadOnly: String(unreadOnly),
        sort: 'createdAt,desc',
      });
      if (search.trim()) params.set('q', search.trim());
      if (filter !== 'all') params.set('type', filter);
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, authLoading, unreadOnly, filter]);

  const markRead = async (id: string) => {
    if (!token) return;
    try {
      await api(`/api/v1/notifications/${id}/read`, { method: 'POST' }, token);
      setItems((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
    } catch (e) {
      error(friendlyError(e));
    }
  };

  const openNotification = async (n: NotificationItem) => {
    if (!n.read) {
      await markRead(n.id);
    }
    if (n.linkUrl) {
      router.push(n.linkUrl);
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
        description="Messages, posts, and community chats land here."
        action={<Link className="btn" href="/login">Log in</Link>}
      />
    );
  }

  const filters: { id: Filter; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'DIRECT_MESSAGE', label: 'Messages' },
    { id: 'FOLLOW_NEW_POST', label: 'Posts' },
    { id: 'COMMUNITY_CHAT', label: 'Community' },
  ];

  return (
    <div className="page-shell page-shell-wide stack">
      <TopBar
        crumb="Alerts"
        searchPlaceholder="Search notifications…"
        defaultSearch={q}
        onSearch={(value) => {
          setQ(value);
          load(value);
        }}
        filters={(
          <>
            {filters.map((f) => (
              <button
                key={f.id}
                type="button"
                className={`chip ${filter === f.id ? 'active' : ''}`}
                onClick={() => setFilter(f.id)}
              >
                {f.label}
              </button>
            ))}
            <button
              type="button"
              className={`chip ${unreadOnly ? 'active' : ''}`}
              onClick={() => setUnreadOnly((v) => !v)}
            >
              Unread
            </button>
          </>
        )}
        actions={(
          <button type="button" className="btn ghost" onClick={markAll}>
            Mark all read
          </button>
        )}
      />

      {loading && <NotificationSkeletonList count={6} />}
      {!loading && items.length === 0 && (
        <ContentPlaceholder
          title="No notifications yet"
          description="DM someone, follow a poster, or chat in a community to fill this inbox."
        />
      )}
      {!loading && (
        <div className="stack notif-stack">
          {items.map((n) => (
            <article
              key={n.id}
              className={`notif-card ${n.read ? '' : 'unread'} notif-card-clickable`}
              role={n.linkUrl ? 'link' : undefined}
              tabIndex={n.linkUrl ? 0 : undefined}
              onClick={() => {
                if (n.linkUrl) openNotification(n);
              }}
              onKeyDown={(e) => {
                if ((e.key === 'Enter' || e.key === ' ') && n.linkUrl) {
                  e.preventDefault();
                  openNotification(n);
                }
              }}
            >
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
                  <span className="notif-type-pill">{typeLabel(n.type)}</span>
                  {n.actorUsername && <strong>@{n.actorUsername}</strong>}
                  <span className="muted">{new Date(n.createdAt).toLocaleString()}</span>
                  {!n.read && <span className="notif-unread-dot" aria-label="Unread" />}
                </div>
                <div className="notif-title">{n.postTitle || n.title}</div>
                {(n.summary || n.body) && (
                  <p className="notif-summary">{n.summary || n.body}</p>
                )}
                <div className="notif-actions" onClick={(e) => e.stopPropagation()}>
                  {n.linkUrl && (
                    <button type="button" className="btn ghost" onClick={() => openNotification(n)}>
                      {openHint(n)}
                    </button>
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
        </div>
      )}
    </div>
  );
}
