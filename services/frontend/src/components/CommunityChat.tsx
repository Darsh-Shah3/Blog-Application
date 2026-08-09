'use client';

import { FormEvent, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { Client } from '@stomp/stompjs';
import { api, friendlyError, getCommunityChatWsUrl, Page } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { SpinnerLabel } from '@/components/loaders';
import { AuthorAvatar, AuthorName, usePrefetchAuthors } from '@/components/AuthorIdentity';

export type ChatMessage = {
  id: string;
  communityId: string;
  authorId: string;
  authorUsername?: string;
  body: string;
  createdAt: string;
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

function normalizeMessage(raw: ChatMessage): ChatMessage {
  return {
    ...raw,
    id: String(raw.id),
    communityId: String(raw.communityId),
    authorId: String(raw.authorId),
    createdAt: typeof raw.createdAt === 'string'
      ? raw.createdAt
      : new Date(raw.createdAt as unknown as string).toISOString(),
  };
}

export function CommunityChat({
  communityId,
  joined,
  communityName,
}: {
  communityId: string;
  joined?: boolean;
  communityName: string;
}) {
  const { token, user } = useAuth();
  const { error } = useToast();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [body, setBody] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [live, setLive] = useState<'connecting' | 'live' | 'fallback' | 'offline'>('connecting');
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const clientRef = useRef<Client | null>(null);
  const seenIds = useRef<Set<string>>(new Set());

  const upsert = (msg: ChatMessage) => {
    const n = normalizeMessage(msg);
    if (seenIds.current.has(n.id)) return;
    seenIds.current.add(n.id);
    setMessages((prev) => [...prev, n]);
  };

  const loadHistory = async () => {
    try {
      const page = await api<Page<ChatMessage>>(
        `/api/v1/communities/${communityId}/messages?size=50&sort=createdAt,desc`,
        {},
        token
      );
      const batch = [...(page.content || [])].reverse().map(normalizeMessage);
      seenIds.current = new Set(batch.map((m) => m.id));
      setMessages(batch);
    } catch (e) {
      error(friendlyError(e, 'Could not load chat.'));
    } finally {
      setLoading(false);
    }
  };

  const liveRef = useRef(live);
  liveRef.current = live;
  usePrefetchAuthors(messages.map((m) => m.authorUsername));

  useEffect(() => {
    setLoading(true);
    setLive('connecting');
    loadHistory();

    const client = new Client({
      brokerURL: getCommunityChatWsUrl(),
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: token
        ? { Authorization: `Bearer ${token}`, token: token }
        : {},
      debug: () => {
        // silent in UI
      },
      onConnect: () => {
        setLive('live');
        client.subscribe(`/topic/community.${communityId}`, (frame) => {
          try {
            const payload = JSON.parse(frame.body) as ChatMessage;
            upsert(payload);
          } catch {
            // ignore bad frames
          }
        });
      },
      onStompError: () => {
        setLive('fallback');
      },
      onWebSocketError: () => {
        setLive((prev) => (prev === 'live' ? 'fallback' : prev));
      },
      onDisconnect: () => {
        setLive((prev) => (prev === 'live' ? 'connecting' : prev));
      },
    });

    try {
      client.activate();
      clientRef.current = client;
    } catch {
      setLive('fallback');
    }

    // REST poll only if WebSocket cannot stay live
    const poll = setInterval(() => {
      const mode = liveRef.current;
      if (mode === 'fallback' || mode === 'offline') {
        loadHistory();
      }
    }, 12000);

    return () => {
      clearInterval(poll);
      clientRef.current = null;
      client.deactivate().catch(() => undefined);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [communityId, token]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [messages.length]);

  const send = async (e: FormEvent) => {
    e.preventDefault();
    if (!token || !joined) {
      error('Join this community to chat.');
      return;
    }
    if (!body.trim()) return;
    setSending(true);
    const text = body.trim();
    try {
      const client = clientRef.current;
      if (client?.connected) {
        client.publish({
          destination: `/app/chat/${communityId}`,
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ body: text }),
        });
        setBody('');
      } else {
        // Fallback REST (still broadcasts via server when WS peers are connected)
        const saved = await api<ChatMessage>(`/api/v1/communities/${communityId}/messages`, {
          method: 'POST',
          body: JSON.stringify({ body: text }),
        }, token);
        setBody('');
        upsert(saved);
        setLive('fallback');
      }
    } catch (err) {
      error(friendlyError(err, 'Could not send message.'));
    } finally {
      setSending(false);
    }
  };

  const statusLabel =
    live === 'live' ? 'Live (WebSocket)'
      : live === 'connecting' ? 'Connecting…'
        : live === 'fallback' ? 'Connected (fallback)'
          : 'Offline';

  return (
    <section className="panel community-chat">
      <header className="community-chat-head">
        <div>
          <h2 style={{ fontFamily: 'var(--font-display)', margin: 0, fontSize: '1.15rem' }}>
            Community chat
          </h2>
          <p className="muted" style={{ margin: '0.25rem 0 0', fontSize: '0.9rem' }}>
            Live discussion in {communityName}. Messages are saved in the database.
          </p>
        </div>
        <span className={`chat-live-badge ${live}`}>{statusLabel}</span>
      </header>

      <div className="community-chat-stream" aria-live="polite">
        {loading && <p className="muted">Loading messages…</p>}
        {!loading && messages.length === 0 && (
          <p className="muted">No messages yet. Be the first to say hello.</p>
        )}
        {messages.map((m) => {
          const name = m.authorUsername || 'user';
          const mine = user?.id === m.authorId;
          return (
            <div key={m.id} className={`chat-bubble ${mine ? 'mine' : ''}`}>
              <div className="chat-bubble-row">
                <AuthorAvatar username={name} size={32} className="chat-bubble-avatar" />
                <div className="chat-bubble-content">
                  <div className="chat-bubble-meta">
                    <AuthorName username={name} />
                    <span className="muted">{relativeTime(m.createdAt)}</span>
                  </div>
                  <p className="chat-bubble-body">{m.body}</p>
                </div>
              </div>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      {user && joined ? (
        <form className="community-chat-composer" onSubmit={send}>
          <input
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Write a message…"
            maxLength={2000}
            required
          />
          <button className="btn" type="submit" disabled={sending || !body.trim()}>
            {sending ? <SpinnerLabel label="…" /> : 'Send'}
          </button>
        </form>
      ) : (
        <p className="muted community-chat-gate">
          {user
            ? 'Join this community to participate in chat.'
            : (
              <>
                <Link href="/login">Log in</Link>
                {' '}and join to chat with members.
              </>
            )}
        </p>
      )}
    </section>
  );
}
