'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { api, friendlyError, Page, Post } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { useToast } from '@/lib/toast';
import { PostCard } from '@/components/PostCard';
import { CreatePostModal } from '@/components/CreatePostModal';
import {
  ContentPlaceholder,
  PageSpinner,
  PostSkeletonList,
  SpinnerLabel,
} from '@/components/loaders';

const SORTS_AUTH = [
  { id: 'for_you', label: 'For you' },
  { id: 'hot', label: 'Hot' },
  { id: 'trending', label: 'Trending' },
  { id: 'new', label: 'New' },
  { id: 'top', label: 'Top' },
] as const;

const SORTS_GUEST = [
  { id: 'hot', label: 'Hot' },
  { id: 'trending', label: 'Trending' },
  { id: 'new', label: 'New' },
  { id: 'top', label: 'Top' },
] as const;

type SortId = (typeof SORTS_AUTH)[number]['id'];
const PAGE_SIZE = 10;

export default function FeedPage() {
  const { token, user, loading: authLoading } = useAuth();
  const { error } = useToast();
  const [sort, setSort] = useState<SortId>('hot');
  const [q, setQ] = useState('');
  const [posts, setPosts] = useState<Post[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [composeOpen, setComposeOpen] = useState(false);

  useEffect(() => {
    if (authLoading) return;
    if (!token && sort === 'for_you') {
      setSort('hot');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, authLoading]);

  const load = async (search = q, s = sort, pageNum = 0, append = false) => {
    if (append) setLoadingMore(true);
    else setLoading(true);
    try {
      const qs = new URLSearchParams({
        sort: s,
        page: String(pageNum),
        size: String(PAGE_SIZE),
      });
      if (search.trim()) qs.set('q', search.trim());
      const result = await api<Page<Post>>(`/api/v1/posts?${qs}`, {}, token);
      const batch = result.content || [];
      setPosts((prev) => (append ? [...prev, ...batch] : batch));
      setPage(pageNum);
      setHasMore(!result.last && batch.length >= PAGE_SIZE);
    } catch (e) {
      error(friendlyError(e, 'Could not load posts.'));
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  useEffect(() => {
    if (authLoading) return;
    load(q, sort, 0, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sort, token, authLoading]);

  const sorts = token ? SORTS_AUTH : SORTS_GUEST;

  return (
    <>
      <section className="hero" style={{ paddingTop: '1.5rem' }}>
        <h1>Posts</h1>
        <p>
          Browse the global feed without signing in. For you ranks communities you joined and people you follow.
          Create posts, vote, and join communities after you log in.
        </p>
      </section>

      {!authLoading && !token && (
        <div className="panel feed-guest-banner">
          <div>
            <strong>Browsing as a guest</strong>
            <p className="muted" style={{ margin: '0.25rem 0 0' }}>
              Read full posts and communities. Sign in to create, join, vote, or reply.
            </p>
          </div>
          <div className="toolbar" style={{ margin: 0 }}>
            <Link className="btn" href="/login">Log in</Link>
            <Link className="btn secondary" href="/register">Sign up</Link>
          </div>
        </div>
      )}

      {user && (
        <div className="panel composer-shortcut">
          <div className="composer-shortcut-avatar" aria-hidden>
            {(user.displayName || user.username || '?').slice(0, 1).toUpperCase()}
          </div>
          <button type="button" className="composer-shortcut-input" onClick={() => setComposeOpen(true)}>
            Start a post…
          </button>
          <button type="button" className="btn" onClick={() => setComposeOpen(true)}>Post</button>
        </div>
      )}

      <form
        className="toolbar"
        onSubmit={(e) => {
          e.preventDefault();
          load(q, sort, 0, false);
        }}
      >
        <input
          className="input"
          style={{ minWidth: '12rem', flex: 1 }}
          placeholder="Search posts by title…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <button type="submit" className="btn secondary">Search</button>
        {sorts.map((s) => (
          <button
            key={s.id}
            type="button"
            className={`chip ${sort === s.id ? 'active' : ''}`}
            onClick={() => setSort(s.id)}
          >
            {s.label}
          </button>
        ))}
      </form>

      {authLoading && <PageSpinner label="Checking session…" />}
      {loading && !authLoading && <PostSkeletonList count={5} />}
      {!loading && !authLoading && posts.length === 0 && (
        <ContentPlaceholder
          title="No posts yet"
          description={
            token
              ? 'Join communities, follow people, or switch to Hot / Trending for global posts.'
              : 'Demo posts appear once services seed the database — refresh after backends start, or switch sorts.'
          }
          action={(
            <>
              <Link className="btn" href="/communities">Discover communities</Link>
              {token ? (
                <button type="button" className="btn secondary" onClick={() => setComposeOpen(true)}>New post</button>
              ) : (
                <Link className="btn secondary" href="/register">Sign up to post</Link>
              )}
            </>
          )}
        />
      )}
      {!loading && (
        <div className="stack">
          {posts.map((p) => (
            <PostCard key={p.id} post={p} />
          ))}
        </div>
      )}
      {hasMore && (
        <div style={{ display: 'flex', justifyContent: 'center', margin: '1.5rem 0' }}>
          <button
            type="button"
            className="btn secondary"
            disabled={loadingMore}
            onClick={() => load(q, sort, page + 1, true)}
          >
            {loadingMore ? <SpinnerLabel label="Loading more…" /> : 'Load more posts'}
          </button>
        </div>
      )}
      {loadingMore && posts.length > 0 && (
        <div style={{ marginTop: '0.75rem' }}>
          <PostSkeletonList count={2} />
        </div>
      )}

      <CreatePostModal
        open={composeOpen}
        onClose={() => setComposeOpen(false)}
        onCreated={() => {
          setComposeOpen(false);
          load(q, sort, 0, false);
        }}
      />
    </>
  );
}
