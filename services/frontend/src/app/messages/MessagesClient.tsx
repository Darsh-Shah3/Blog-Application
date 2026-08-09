'use client';

import Link from 'next/link';
import { FormEvent, useCallback, useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { api, friendlyError, User } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { DirectChat } from '@/components/DirectChat';
import { ContentPlaceholder, SpinnerLabel } from '@/components/loaders';

export type DmThread = {
  id: string;
  peerUserId: string;
  peerUsername: string;
  lastMessagePreview?: string | null;
  lastMessageAt?: string | null;
  createdAt: string;
};

function relativeTime(iso?: string | null) {
  if (!iso) return '';
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const sec = Math.max(0, Math.floor((Date.now() - then) / 1000));
  if (sec < 60) return 'just now';
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min}m`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}h`;
  return new Date(iso).toLocaleDateString();
}

export default function MessagesPage() {
  const { token, user, loading: authLoading } = useAuth();
  const { error, success } = useToast();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [threads, setThreads] = useState<DmThread[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [startUser, setStartUser] = useState('');
  const [starting, setStarting] = useState(false);
  const [bootstrapping, setBootstrapping] = useState(false);

  const active = threads.find((t) => t.id === activeId) || null;

  const loadThreads = useCallback(async () => {
    if (!token) return;
    try {
      const list = await api<DmThread[]>('/api/v1/dm/threads', {}, token);
      setThreads(list || []);
    } catch (e) {
      error(friendlyError(e, 'Could not load messages.'));
    } finally {
      setLoading(false);
    }
  }, [token, error]);

  useEffect(() => {
    if (authLoading) return;
    if (!token) {
      setLoading(false);
      return;
    }
    loadThreads();
  }, [token, authLoading, loadThreads]);

  useEffect(() => {
    if (!token || authLoading) return;
    const withUser = searchParams.get('with');
    if (!withUser || bootstrapping) return;

    let cancelled = false;
    (async () => {
      setBootstrapping(true);
      try {
        const profile = await api<User>(`/api/v1/profiles/${encodeURIComponent(withUser)}`, {}, token);
        const thread = await api<DmThread>('/api/v1/dm/threads', {
          method: 'POST',
          body: JSON.stringify({
            peerUserId: profile.id,
            peerUsername: profile.username,
          }),
        }, token);
        if (cancelled) return;
        setThreads((prev) => {
          const rest = prev.filter((t) => t.id !== thread.id);
          return [thread, ...rest];
        });
        setActiveId(thread.id);
        router.replace('/messages', { scroll: false });
      } catch (e) {
        if (!cancelled) error(friendlyError(e, `Could not open chat with ${withUser}.`));
      } finally {
        if (!cancelled) setBootstrapping(false);
      }
    })();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, authLoading, searchParams]);

  useEffect(() => {
    if (!activeId && threads.length > 0) {
      setActiveId(threads[0]!.id);
    }
  }, [threads, activeId]);

  const startByUsername = async (e: FormEvent) => {
    e.preventDefault();
    if (!token) return;
    const username = startUser.trim().replace(/^@/, '');
    if (!username) return;
    setStarting(true);
    try {
      const profile = await api<User>(`/api/v1/profiles/${encodeURIComponent(username)}`, {}, token);
      if (String(profile.id) === String(user?.id)) {
        error('You cannot message yourself.');
        return;
      }
      const thread = await api<DmThread>('/api/v1/dm/threads', {
        method: 'POST',
        body: JSON.stringify({
          peerUserId: profile.id,
          peerUsername: profile.username,
        }),
      }, token);
      setThreads((prev) => {
        const rest = prev.filter((t) => t.id !== thread.id);
        return [thread, ...rest];
      });
      setActiveId(thread.id);
      setStartUser('');
      success(`Chat with @${profile.username}`);
    } catch (err) {
      error(friendlyError(err, 'User not found or could not open chat.'));
    } finally {
      setStarting(false);
    }
  };

  if (authLoading) {
    return (
      <div className="page-shell page-shell-wide">
        <p className="muted">Checking session…</p>
      </div>
    );
  }

  if (!user || !token) {
    return (
      <div className="page-shell page-shell-wide">
        <div className="panel">
          <h1 style={{ fontFamily: 'var(--font-display)', marginTop: 0 }}>Messages</h1>
          <p className="muted">Sign in to send private messages to other members.</p>
          <div className="toolbar">
            <Link className="btn" href="/login?next=/messages">Log in</Link>
            <Link className="btn secondary" href="/register">Sign up</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="page-shell page-shell-wide dm-page">
      <header className="page-header page-header-compact">
        <div className="page-header-row">
          <div>
            <p className="page-kicker">Inbox</p>
            <h1>Messages</h1>
            <p>Private 1:1 chats — live over WebSocket, same stack as community rooms.</p>
          </div>
        </div>
      </header>

      <div className="dm-layout">
        <aside className="dm-sidebar panel">
          <form className="dm-start-form" onSubmit={startByUsername}>
            <label>
              Message a member
              <input
                value={startUser}
                onChange={(e) => setStartUser(e.target.value)}
                placeholder="username"
                autoComplete="off"
              />
            </label>
            <button className="btn" type="submit" disabled={starting || !startUser.trim()}>
              {starting || bootstrapping ? <SpinnerLabel label="Opening…" /> : 'Open chat'}
            </button>
          </form>

          <div className="dm-thread-list" role="list">
            {loading && <p className="muted">Loading inbox…</p>}
            {!loading && threads.length === 0 && (
              <p className="muted dm-empty-hint">
                No conversations yet. Open a profile and hit Message, or start one above.
              </p>
            )}
            {threads.map((t) => (
              <button
                key={t.id}
                type="button"
                role="listitem"
                className={`dm-thread-item${activeId === t.id ? ' is-active' : ''}`}
                onClick={() => setActiveId(t.id)}
              >
                <span className="dm-thread-avatar" aria-hidden>
                  {(t.peerUsername || '?')[0]!.toUpperCase()}
                </span>
                <span className="dm-thread-body">
                  <span className="dm-thread-top">
                    <strong>@{t.peerUsername}</strong>
                    <span className="muted">{relativeTime(t.lastMessageAt || t.createdAt)}</span>
                  </span>
                  <span className="dm-thread-preview muted">
                    {t.lastMessagePreview || 'No messages yet'}
                  </span>
                </span>
              </button>
            ))}
          </div>
        </aside>

        <div className="dm-main">
          {active ? (
            <DirectChat
              key={active.id}
              threadId={active.id}
              peerUsername={active.peerUsername}
              onMessageSent={(preview) => {
                setThreads((prev) => {
                  const next = prev.map((t) =>
                    t.id === active.id
                      ? {
                          ...t,
                          lastMessagePreview: preview.slice(0, 200),
                          lastMessageAt: new Date().toISOString(),
                        }
                      : t
                  );
                  const head = next.find((t) => t.id === active.id);
                  return head ? [head, ...next.filter((t) => t.id !== active.id)] : next;
                });
              }}
            />
          ) : (
            <ContentPlaceholder
              title="Select a conversation"
              description="Pick someone from the left, or open a chat from any member profile."
              action={<Link className="btn secondary" href="/communities">Find people in communities</Link>}
            />
          )}
        </div>
      </div>
    </div>
  );
}
