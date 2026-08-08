'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { FormEvent, useEffect, useState } from 'react';
import { api, Community, friendlyError, Page } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { CommunitySkeletonList, ContentPlaceholder, SpinnerLabel } from '@/components/loaders';
import { UsersIcon } from '@/components/icons';

const PAGE_SIZE = 12;

export default function CommunitiesPage() {
  const router = useRouter();
  const { token, user } = useAuth();
  const { success, error } = useToast();
  const [communities, setCommunities] = useState<Community[]>([]);
  const [discover, setDiscover] = useState<Community[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [creating, setCreating] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [q, setQ] = useState('');

  const loadDiscover = () => {
    api<Page<Community>>('/api/v1/communities/discover?size=8', {}, token)
      .then((page) => setDiscover(page.content || []))
      .catch(() => setDiscover([]));
  };

  const load = async (search = q, pageNum = 0, append = false) => {
    if (append) setLoadingMore(true);
    else setLoading(true);
    try {
      const params = new URLSearchParams({
        page: String(pageNum),
        size: String(PAGE_SIZE),
      });
      if (search.trim()) params.set('q', search.trim());
      const result = await api<Page<Community>>(`/api/v1/communities?${params}`, {}, token);
      const batch = result.content || [];
      setCommunities((prev) => (append ? [...prev, ...batch] : batch));
      setPage(pageNum);
      setHasMore(!result.last && batch.length >= PAGE_SIZE);
    } catch (e) {
      error(friendlyError(e, 'Could not load communities.'));
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  useEffect(() => {
    load();
    loadDiscover();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const create = async (e: FormEvent) => {
    e.preventDefault();
    if (!token) {
      error('Please sign in to create a community.');
      return;
    }
    setCreating(true);
    try {
      await api('/api/v1/communities', {
        method: 'POST',
        body: JSON.stringify({ name, description }),
      }, token);
      setName('');
      setDescription('');
      success('Community created.');
      load(q, 0, false);
      loadDiscover();
    } catch (err) {
      error(friendlyError(err, 'Could not create community.'));
    } finally {
      setCreating(false);
    }
  };

  const join = async (id: string) => {
    if (!token) {
      error('Please sign in to join a community.');
      return;
    }
    try {
      await api(`/api/v1/communities/${id}/join`, { method: 'POST' }, token);
      success('Joined community.');
      load(q, 0, false);
      loadDiscover();
    } catch (e) {
      error(friendlyError(e, 'Could not join community.'));
    }
  };

  return (
    <div className="communities-layout">
      <section className="stack">
        <header className="communities-hero">
          <h1 style={{ fontFamily: 'var(--font-display)', margin: 0 }}>Communities</h1>
          <p className="muted" style={{ margin: '0.4rem 0 0' }}>
            Explore topic spaces without an account. Join or create once you&apos;re signed in.
          </p>
        </header>

        {!user && (
          <div className="panel feed-guest-banner">
            <div>
              <strong>Browsing as a guest</strong>
              <p className="muted" style={{ margin: '0.25rem 0 0' }}>
                You can read every community. Sign in to join or start a new one.
              </p>
            </div>
            <div className="toolbar" style={{ margin: 0 }}>
              <Link className="btn" href="/login">Log in</Link>
              <Link className="btn secondary" href="/register">Sign up</Link>
            </div>
          </div>
        )}

        {discover.length > 0 && (
          <div className="panel discover-panel">
            <h2 style={{ fontFamily: 'var(--font-display)', marginTop: 0, fontSize: '1.15rem' }}>
              Discover
            </h2>
            <p className="muted" style={{ marginTop: 0 }}>
              Growing spaces — good places to join for a better For you feed.
            </p>
            <div className="discover-grid">
              {discover.map((c) => (
                <article
                  key={`d-${c.id}`}
                  className="discover-card discover-card-clickable"
                  role="link"
                  tabIndex={0}
                  onClick={() => router.push(`/c/${c.slug}`)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      router.push(`/c/${c.slug}`);
                    }
                  }}
                >
                  <div className="discover-icon" aria-hidden>
                    <UsersIcon size={18} />
                  </div>
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <strong>{c.name}</strong>
                    <div className="muted" style={{ fontSize: '0.85rem' }}>{c.memberCount} members</div>
                  </div>
                  <div onClick={(e) => e.stopPropagation()}>
                    {user && !c.joined && (
                      <button type="button" className="btn secondary" onClick={() => join(c.id)}>Join</button>
                    )}
                    {!user && (
                      <Link className="btn secondary" href="/login">Join</Link>
                    )}
                    {c.joined && <span className="muted" style={{ fontSize: '0.85rem' }}>Joined</span>}
                  </div>
                </article>
              ))}
            </div>
          </div>
        )}

        <form
          className="toolbar"
          onSubmit={(e) => {
            e.preventDefault();
            load(q, 0, false);
          }}
        >
          <input
            className="input"
            style={{ flex: 1, minWidth: '10rem' }}
            placeholder="Search communities…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
          <button type="submit" className="btn secondary">Search</button>
        </form>

        {loading && <CommunitySkeletonList count={4} />}
        {!loading && communities.length === 0 && (
          <ContentPlaceholder
            title="No communities yet"
            description={user ? 'Create the first one — it only takes a name.' : 'Demo data seeds on fresh backends, or sign in to create one.'}
          />
        )}
        {!loading && communities.map((c) => (
          <article
            key={c.id}
            className="panel community-row community-row-clickable"
            role="link"
            tabIndex={0}
            onClick={() => router.push(`/c/${c.slug}`)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                router.push(`/c/${c.slug}`);
              }
            }}
          >
            <div className="community-row-main">
              <div className="discover-icon" aria-hidden>
                <UsersIcon size={20} />
              </div>
              <div style={{ minWidth: 0 }}>
                <h2 style={{ margin: '0 0 0.35rem', fontFamily: 'var(--font-display)', fontSize: '1.2rem' }}>
                  {c.name}
                </h2>
                <p className="muted" style={{ margin: 0 }}>{c.description || 'No description yet.'}</p>
                <p className="muted" style={{ margin: '0.45rem 0 0', fontSize: '0.9rem' }}>
                  {c.memberCount} members{c.joined ? ' · joined' : ''}
                </p>
              </div>
            </div>
            <div className="community-row-actions" onClick={(e) => e.stopPropagation()}>
              {user && !c.joined && (
                <button type="button" className="btn secondary" onClick={() => join(c.id)}>Join</button>
              )}
              {!user && (
                <Link className="btn secondary" href="/login">Sign in to join</Link>
              )}
              <Link className="btn ghost" href={`/c/${c.slug}`}>Open</Link>
            </div>
          </article>
        ))}
        {hasMore && (
          <button
            type="button"
            className="btn secondary"
            disabled={loadingMore}
            onClick={() => load(q, page + 1, true)}
          >
            {loadingMore ? <SpinnerLabel label="Loading…" /> : 'Load more'}
          </button>
        )}
      </section>

      <aside className="create-community-card">
        <div className="create-community-head">
          <div className="discover-icon create-community-mark" aria-hidden>
            <UsersIcon size={22} />
          </div>
          <div>
            <h2 style={{ fontFamily: 'var(--font-display)', margin: 0, fontSize: '1.35rem' }}>
              Create community
            </h2>
            <p className="muted" style={{ margin: '0.35rem 0 0' }}>
              A focused home for a topic — posts, members, and discussion under one slug.
            </p>
          </div>
        </div>
        {!user ? (
          <div className="create-community-gate">
            <p className="muted" style={{ marginTop: 0 }}>
              Sign in to launch a community. Guests can still browse and open every public space.
            </p>
            <div className="toolbar">
              <Link className="btn" href="/login">Log in</Link>
              <Link className="btn secondary" href="/register">Sign up</Link>
            </div>
          </div>
        ) : (
          <form className="form create-community-form" onSubmit={create}>
            <label>
              Name
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                minLength={3}
                placeholder="e.g. System Design"
              />
            </label>
            <label>
              Description
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What will people post about here?"
                rows={4}
              />
            </label>
            <button className="btn" type="submit" disabled={creating || name.trim().length < 3}>
              {creating ? <SpinnerLabel label="Creating…" /> : 'Create community'}
            </button>
            <p className="muted" style={{ margin: 0, fontSize: '0.85rem' }}>
              You become the owner and first member automatically.
            </p>
          </form>
        )}
      </aside>
    </div>
  );
}
