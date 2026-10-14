'use client';

import { FormEvent, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { Client } from '@stomp/stompjs';
import { api, friendlyError, getCommunityChatWsUrl, Page } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { SpinnerLabel } from '@/components/loaders';
import { AuthorAvatar, AuthorName, usePrefetchAuthors } from '@/components/AuthorIdentity';
import { ChatMessageActions } from '@/components/ChatMessageActions';

export type DmMessage = {
  id: string;
  threadId: string;
  authorId: string;
  authorUsername?: string;
  body: string;
  createdAt: string;
  editedAt?: string | null;
  deletedForEveryone?: boolean;
};

function relativeTime(iso: string) {
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

function normalizeMessage(raw: DmMessage): DmMessage {
  return {
    ...raw,
    id: String(raw.id),
    threadId: String(raw.threadId),
    authorId: String(raw.authorId),
    createdAt: typeof raw.createdAt === 'string'
      ? raw.createdAt
      : new Date(raw.createdAt as unknown as string).toISOString(),
    editedAt: raw.editedAt
      ? (typeof raw.editedAt === 'string'
        ? raw.editedAt
        : new Date(raw.editedAt as unknown as string).toISOString())
      : null,
    deletedForEveryone: Boolean(raw.deletedForEveryone),
  };
}

export function DirectChat({
  threadId,
  peerUsername,
  onMessageSent,
}: {
  threadId: string;
  peerUsername: string;
  onMessageSent?: (preview: string) => void;
}) {
  const { token, user } = useAuth();
  const { error } = useToast();
  const [messages, setMessages] = useState<DmMessage[]>([]);
  const [body, setBody] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [live, setLive] = useState<'connecting' | 'live' | 'fallback' | 'offline'>('connecting');
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const clientRef = useRef<Client | null>(null);
  const seenIds = useRef<Set<string>>(new Set());
  const liveRef = useRef(live);
  liveRef.current = live;
  usePrefetchAuthors(messages.map((m) => m.authorUsername));

  const applyMessage = (msg: DmMessage) => {
    const n = normalizeMessage(msg);
    setMessages((prev) => {
      const idx = prev.findIndex((m) => m.id === n.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = n;
        return next;
      }
      seenIds.current.add(n.id);
      return [...prev, n];
    });
  };

  const removeMessage = (id: string) => {
    seenIds.current.delete(id);
    setMessages((prev) => prev.filter((m) => m.id !== id));
  };

  const loadHistory = async () => {
    if (!token) return;
    try {
      const page = await api<Page<DmMessage>>(
        `/api/v1/dm/threads/${threadId}/messages?size=80&sort=createdAt,desc`,
        {},
        token
      );
      const batch = [...(page.content || [])].reverse().map(normalizeMessage);
      seenIds.current = new Set(batch.map((m) => m.id));
      setMessages(batch);
    } catch (e) {
      error(friendlyError(e, 'Could not load messages.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!token || !threadId) return;
    setLoading(true);
    setMessages([]);
    setLive('connecting');
    loadHistory();

    const client = new Client({
      brokerURL: getCommunityChatWsUrl(),
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
        token,
      },
      debug: () => undefined,
      onConnect: () => {
        setLive('live');
        client.subscribe(`/topic/dm.${threadId}`, (frame) => {
          try {
            const payload = JSON.parse(frame.body) as DmMessage;
            applyMessage(payload);
          } catch {
            // ignore
          }
        });
      },
      onStompError: () => setLive('fallback'),
      onWebSocketError: () => setLive((prev) => (prev === 'live' ? 'fallback' : prev)),
      onDisconnect: () => setLive((prev) => (prev === 'live' ? 'connecting' : prev)),
    });

    try {
      client.activate();
      clientRef.current = client;
    } catch {
      setLive('fallback');
    }

    const poll = setInterval(() => {
      if (liveRef.current === 'fallback' || liveRef.current === 'offline') {
        loadHistory();
      }
    }, 12000);

    return () => {
      clearInterval(poll);
      clientRef.current = null;
      client.deactivate().catch(() => undefined);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [threadId, token]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [messages.length]);

  const send = async (e: FormEvent) => {
    e.preventDefault();
    if (!token) {
      error('Please sign in to message.');
      return;
    }
    if (!body.trim()) return;
    setSending(true);
    const text = body.trim();
    try {
      const client = clientRef.current;
      if (client?.connected) {
        client.publish({
          destination: `/app/dm/${threadId}`,
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ body: text }),
        });
        setBody('');
        onMessageSent?.(text);
      } else {
        const saved = await api<DmMessage>(`/api/v1/dm/threads/${threadId}/messages`, {
          method: 'POST',
          body: JSON.stringify({ body: text }),
        }, token);
        setBody('');
        applyMessage(saved);
        setLive('fallback');
        onMessageSent?.(text);
      }
    } catch (err) {
      error(friendlyError(err, 'Could not send message.'));
    } finally {
      setSending(false);
    }
  };

  const editMessage = async (id: string, nextBody: string) => {
    if (!token) return;
    try {
      const updated = await api<DmMessage>(
        `/api/v1/dm/threads/${threadId}/messages/${id}`,
        { method: 'PATCH', body: JSON.stringify({ body: nextBody }) },
        token
      );
      applyMessage(updated);
    } catch (e) {
      error(friendlyError(e, 'Could not edit message.'));
      throw e;
    }
  };

  const deleteMessage = async (id: string, scope: 'ME' | 'EVERYONE') => {
    if (!token) return;
    try {
      const updated = await api<DmMessage | null>(
        `/api/v1/dm/threads/${threadId}/messages/${id}?scope=${scope}`,
        { method: 'DELETE' },
        token
      );
      if (scope === 'ME' || !updated) {
        removeMessage(id);
      } else {
        applyMessage(updated);
      }
    } catch (e) {
      error(friendlyError(e, 'Could not delete message.'));
      throw e;
    }
  };

  const statusLabel =
    live === 'live' ? 'Live'
      : live === 'connecting' ? 'Connecting…'
        : live === 'fallback' ? 'Fallback'
          : 'Offline';

  return (
    <section className="dm-chat panel">
      <header className="community-chat-head">
        <div>
          <h2 style={{ fontFamily: 'var(--font-display)', margin: 0, fontSize: '1.15rem' }}>
            @{peerUsername}
          </h2>
          <p className="muted" style={{ margin: '0.25rem 0 0', fontSize: '0.88rem' }}>
            Private messages · saved in Threadly
          </p>
        </div>
        <span className={`chat-live-badge ${live}`}>{statusLabel}</span>
      </header>

      <div className="community-chat-stream dm-chat-stream" aria-live="polite">
        {loading && <p className="muted">Loading conversation…</p>}
        {!loading && messages.length === 0 && (
          <p className="muted">No messages yet. Say hello to {peerUsername}.</p>
        )}
        {messages.map((m) => {
          const name = m.authorUsername || 'user';
          const mine = user?.id === m.authorId || String(user?.id) === String(m.authorId);
          const deleted = Boolean(m.deletedForEveryone);
          return (
            <div key={m.id} className={`chat-bubble ${mine ? 'mine' : ''}${deleted ? ' deleted' : ''}`}>
              <div className="chat-bubble-row">
                <AuthorAvatar username={name} size={32} className="chat-bubble-avatar" />
                <div className="chat-bubble-content">
                  <div className="chat-bubble-meta">
                    <AuthorName username={name} />
                    <span className="muted">{relativeTime(m.createdAt)}</span>
                    {m.editedAt && !deleted && <span className="muted chat-edited">edited</span>}
                    {user && !deleted && (
                      <ChatMessageActions
                        canEdit={mine}
                        canDeleteForMe
                        canDeleteForEveryone={mine}
                        initialBody={m.body}
                        onEdit={(next) => editMessage(m.id, next)}
                        onDeleteForMe={() => deleteMessage(m.id, 'ME')}
                        onDeleteForEveryone={() => deleteMessage(m.id, 'EVERYONE')}
                      />
                    )}
                  </div>
                  <p className={`chat-bubble-body${deleted ? ' is-deleted' : ''}`}>
                    {deleted ? 'This message was deleted' : m.body}
                  </p>
                </div>
              </div>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      {user ? (
        <form className="community-chat-composer" onSubmit={send}>
          <input
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder={`Message @${peerUsername}…`}
            maxLength={2000}
            required
          />
          <button className="btn" type="submit" disabled={sending || !body.trim()}>
            {sending ? <SpinnerLabel label="…" /> : 'Send'}
          </button>
        </form>
      ) : (
        <p className="muted community-chat-gate">
          <Link href="/login">Log in</Link> to send DMs.
        </p>
      )}
    </section>
  );
}
