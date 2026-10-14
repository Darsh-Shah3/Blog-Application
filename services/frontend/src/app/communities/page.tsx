'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { FormEvent, useEffect, useState } from 'react';
import { api, Community, friendlyError, Page } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { CommunitySkeletonList, ContentPlaceholder, SpinnerLabel } from '@/components/loaders';
import { UsersIcon } from '@/components/icons';
import { TopBar } from '@/components/TopBar';
import { ViewIconLink } from '@/components/ActionIcon';
import { PlusIcon } from '@/components/icons';

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
  const [sort, setSort] = useState<'popular' | 'az'>('popular');

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
      setCommunities((prev) => {
        const next = append ? [...prev, ...batch] : batch;
        if (sort === 'az') {
          return [...next].sort((a, b) => a.name.localeCompare(b.name));
        }
        return [...next].sort((a, b) => (b.memberCount || 0) - (a.memberCount || 0));
      });
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

  useEffect(() => {
    setCommunities((prev) => {
      const next = [...prev];
      if (sort === 'az') next.sort((a, b) => a.name.localeCompare(b.name));
      else next.sort((a, b) => (b.memberCount || 0) - (a.memberCount || 0));
      return next;
    });
  }, [sort]);

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
    <div className="page-shell page-shell-wide communities-page">
      <div className="communities-layout">
        <section className="stack communities-main">
          <TopBar
            crumb="Communities"
            searchPlaceholder="Search communities…"
            defaultSearch={q}
            onSearch={(value) => {
              setQ(value);
              load(value, 0, false);
            }}
            filters={(
              <>
                <button
                  type="button"
                  className={`chip ${sort === 'popular' ? 'active' : ''}`}
                  onClick={() => setSort('popular')}
                >
                  Popular
                </button>
                <button
                  type="button"
                  className={`chip ${sort === 'az' ? 'active' : ''}`}
                  onClick={() => setSort('az')}
                >
                  A–Z
                </button>
              </>
            )}
            actions={
              user ? (
                <button
                  type="button"
                  className="btn feed-compose-btn"
                  onClick={() => document.getElementById('create-community-name')?.focus()}
                >
                  <PlusIcon size={15} />
                  <span>Create</span>
                </button>
              ) : (
                <Link className="btn secondary" href="/login">Log in</Link>
              )
            }
          />

          {!user && (
            <div className="feed-guest-banner soft-banner">
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
              <div className="discover-panel-head">
                <div>
                  <h2>Discover</h2>
                  <p className="muted">Growing spaces with descriptions — open any card to explore.</p>
                </div>
              </div>
              <div className="discover-grid discover-grid-rich">
                {discover.map((c) => (
                  <article
                    key={`d-${c.id}`}
                    className="discover-card discover-card-rich discover-card-clickable"
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
                    <div className="discover-card-top">
                      <div className="discover-icon" aria-hidden>
                        <UsersIcon size={18} />
                      </div>
                      <div className="discover-card-actions" onClick={(e) => e.stopPropagation()}>
                        {user && !c.joined && (
                          <button type="button" className="btn secondary" onClick={() => join(c.id)}>Join</button>
                        )}
                        {!user && <Link className="btn secondary" href="/login">Join</Link>}
                        {c.joined && <span className="page-pill teal">Joined</span>}
                        <ViewIconLink href={`/c/${c.slug}`} label={`Open ${c.name}`} />
                      </div>
                    </div>
                    <strong className="discover-card-name">{c.name}</strong>
                    <p className="discover-card-desc muted">
                      {c.description?.trim() || 'No description yet — open to see posts and chat.'}
                    </p>
                    <span className="muted discover-card-meta">{c.memberCount} members</span>
                  </article>
                ))}
              </div>
            </div>
          )}

          <div className="community-section-label">
            <h2>All communities</h2>
            <span className="muted">{communities.length}{hasMore ? '+' : ''} listed</span>
          </div>

          {loading && <CommunitySkeletonList count={4} />}
          {!loading && communities.length === 0 && (
            <ContentPlaceholder
              title="No communities yet"
              description={user ? 'Create the first one — it only takes a name.' : 'Demo data seeds on fresh backends, or sign in to create one.'}
            />
          )}
          <div className="community-list">
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
                    <h2 className="community-row-title">{c.name}</h2>
                    <p className="muted community-row-desc">{c.description || 'No description yet.'}</p>
                    <p className="muted community-row-meta">
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
                  <ViewIconLink href={`/c/${c.slug}`} label={`Open ${c.name}`} />
                </div>
              </article>
            ))}
          </div>
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
              <h2>Create community</h2>
              <p className="muted">
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
                  id="create-community-name"
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
    </div>
  );
}
